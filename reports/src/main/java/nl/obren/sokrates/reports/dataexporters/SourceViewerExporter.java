package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.ReportTheme;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicateFragmentExport;
import nl.obren.sokrates.reports.dataexporters.units.FragmentExport;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.function.Consumer;

/**
 * The per-repository source viewer (src/viewer.html): the referenced source files per aspect and the unit and
 * duplicate fragment bundles are collected into one archive that is embedded base64 inside the page, plus the
 * Structure.html shell. One instance per analysis, so an exporter serving many repositories in one run (analyzeLandscape
 * -urls) never carries another repository's entries. Moved out of {@link DataExporter}.
 */
class SourceViewerExporter {
    private static final Log LOG = LogFactory.getLog(SourceViewerExporter.class);
    private final CodeAnalysisResults analysisResults;
    private final CodeConfiguration codeConfiguration;
    private final File reportsFolder;
    private final File dataFolder;
    private final File codeCacheFolder;
    private final Consumer<String> detailedInfo;
    private final Map<String, String> viewerArchiveEntries = new LinkedHashMap<>();

    SourceViewerExporter(CodeAnalysisResults analysisResults, CodeConfiguration codeConfiguration, File reportsFolder, File dataFolder, File codeCacheFolder,
                         Consumer<String> detailedInfo) {
        this.analysisResults = analysisResults;
        this.codeConfiguration = codeConfiguration;
        this.reportsFolder = reportsFolder;
        this.dataFolder = dataFolder;
        this.codeCacheFolder = codeCacheFolder;
        this.detailedInfo = detailedInfo;
    }

    void export() throws IOException {
        detailedInfo.accept("Saving details and source code cache:");

        saveStructureFile();

        // Collect the viewer's data into viewerArchiveEntries first, then write viewer.html last
        // with that whole archive embedded base64 inside it (saveViewerFile), so the page extracts
        // its ?aspect=&file= / ?bundle=&i= view from inline bytes — no fetch, opens from file://.
        if (codeConfiguration.getAnalysis().isSaveSourceFiles()) {
            Set<SourceFile> referencedFiles = getReferencedFiles();

            collectAspectSourceFiles(codeConfiguration.getMain(), "main", referencedFiles);
            collectAspectSourceFiles(codeConfiguration.getTest(), "test", referencedFiles);
            collectAspectSourceFiles(codeConfiguration.getGenerated(), "generated", referencedFiles);
            collectAspectSourceFiles(codeConfiguration.getBuildAndDeployment(), "buildAndDeployment", referencedFiles);
            collectAspectSourceFiles(codeConfiguration.getOther(), "other", referencedFiles);
        }

        if (codeConfiguration.getAnalysis().isSaveCodeFragments()) {
            UnitsAnalysisResults unitsAnalysisResults = analysisResults.getUnitsAnalysisResults();
            collectUnitFragments(unitsAnalysisResults.getLongestUnits(), "longest_unit");
            collectUnitFragments(unitsAnalysisResults.getMostComplexUnits(), "most_complex_units");

            DuplicationAnalysisResults duplicationAnalysisResults = analysisResults.getDuplicationAnalysisResults();
            collectDuplicateFragments(duplicationAnalysisResults.getLongestDuplicates(), "longest_duplicates");
            collectDuplicateFragments(duplicationAnalysisResults.getMostFrequentDuplicates(), "most_frequent_duplicates");
            collectDuplicateFragments(duplicationAnalysisResults.getUnitDuplicates(), "unit_duplicates");
        }

        saveViewerFile();
    }

    private Set<SourceFile> getReferencedFiles() {
        return ReferencedFiles.of(analysisResults);
    }

    // Collects the unit fragment bundle into the viewer archive under "fragments/<type>.json"
    // (array order preserved so the report's 1-based ?i= index maps to the same array position).
    private void collectUnitFragments(List<UnitInfo> units, String fragmentType) throws IOException {
        detailedInfo.accept(" - saving source code cache for the " + fragmentType + " fragments");
        List<FragmentExport> fragments = new ArrayList<>();
        units.forEach(unit -> {
            fragments.add(new FragmentExport(
                    unit.getShortName(),
                    unit.getSourceFile().getRelativePath(),
                    unit.getStartLine(),
                    unit.getEndLine(),
                    unit.getLinesOfCode(),
                    unit.getMcCabeIndex(),
                    unit.getSourceFile().getExtension(),
                    unit.getBody()));
        });

        viewerArchiveEntries.put("fragments/" + fragmentType + ".json", new JsonGenerator().generate(fragments));
    }

    private void saveStructureFile() {
        try {

            String html = ReportTheme.apply(HtmlTemplateUtils.getResource("/templates/Structure.html"));

            File htmlFile = new File(new File(reportsFolder, "html"), "Structure.html");
            FileUtils.write(htmlFile, html, UTF_8);

        } catch (IOException e) {
            LOG.warn(e);
        }
    }

    // Writes the single shared source viewer with its whole data archive (source files +
    // fragment bundles, accumulated in viewerArchiveEntries) embedded inline as base64. The page
    // extracts its ?aspect=&file= / ?bundle=&i= view from those inline bytes (sokratesUnzip) — no
    // sibling zips/JSON and no fetch(), so it opens from file://.
    private void saveViewerFile() {
        try {
            String[][] entries = viewerArchiveEntries.entrySet().stream()
                    .map(e -> new String[]{e.getKey(), e.getValue()})
                    .toArray(String[][]::new);
            String archiveB64 = VisualizationTemplate.base64(ZipUtils.stringEntriesToZipBytes(entries));
            String html = ReportTheme.apply(HtmlTemplateUtils.getResource("/templates/viewer.html"))
                    .replace("${sokrates-unzip-lib}", VisualizationTemplate.embedZipLib())
                    .replace("${embedded-archive}", "var SOKRATES_ARCHIVE = \"" + archiveB64 + "\";");
            FileUtils.write(new File(codeCacheFolder, "viewer.html"), html, UTF_8);
        } catch (IOException e) {
            LOG.warn(e);
        }
    }

    // Collects the duplicate fragment bundle into the viewer archive under "fragments/<type>.json"
    // (one entry per duplicate, in order, so the 1-based ?i= index in the report's "view" link
    // (DuplicationReportGenerator) maps to the same array position here).
    private void collectDuplicateFragments(List<DuplicationInstance> duplicates, String fragmentType) throws IOException {
        detailedInfo.accept(" - saving source code cache for the " + fragmentType + " fragments");
        List<DuplicateFragmentExport> fragments = new ArrayList<>();
        duplicates.forEach(duplicate -> {
            DuplicatedFileBlock firstFileBlock = duplicate.getDuplicatedFileBlocks().get(0);
            DuplicateFragmentExport fragment = new DuplicateFragmentExport(firstFileBlock.getSourceFile().getExtension());
            duplicate.getDuplicatedFileBlocks().forEach(block -> {
                List<String> lines = block.getSourceFile().getLines();
                int fromIndex = block.getStartLine() - 1;
                int endLine = block.getEndLine();
                if (fromIndex >= 0 && endLine > fromIndex && endLine < lines.size()) {
                    String code = String.join("\n", lines.subList(fromIndex, endLine));
                    fragment.addBlock(block.getSourceFile().getRelativePath(), block.getStartLine(), endLine, code);
                }
            });
            fragments.add(fragment);
        });

        viewerArchiveEntries.put("fragments/" + fragmentType + ".json", new JsonGenerator().generate(fragments));
    }

    // Collects an aspect's referenced source files into the viewer archive, keyed
    // "<aspect>/<relativePath>" (the viewer computes this key from ?aspect=&file=). Also writes the
    // aspect's file-list JSON to data/ (a separate external-tooling contract, kept as a loose file).
    private void collectAspectSourceFiles(NamedSourceCodeAspect aspect, String aspectName, Set<SourceFile> referencedFiles) throws IOException {
        File filesListFile = new File(dataFolder, aspectName + "FilesPaths.json");
        detailedInfo.accept(" - storing the file list for the <b>" + aspectName + "</b> aspect in <a href='" + filesListFile.getPath() + "'>" + filesListFile.getPath() + "</a>");
        List<String> files = new ArrayList<>();
        aspect.getSourceFiles().forEach(sourceFile -> {
            files.add(sourceFile.getRelativePath());
        });
        FileUtils.write(filesListFile, new JsonGenerator().generate(files), UTF_8);

        detailedInfo.accept(" - saving source code cache for the <b>" + aspectName + "</b> aspect (embedded in the viewer)");
        aspect.getSourceFiles().stream().filter(referencedFiles::contains).forEach(sourceFile -> {
            viewerArchiveEntries.put(aspectName + "/" + sourceFile.getRelativePath(), sourceFile.getContent());
        });
    }
}
