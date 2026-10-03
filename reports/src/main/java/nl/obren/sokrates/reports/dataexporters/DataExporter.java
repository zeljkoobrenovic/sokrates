/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.dataexporters.dependencies.DependenciesExporter;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicateExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicateFileBlockExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExporter;
import nl.obren.sokrates.reports.dataexporters.files.FileListExporter;
import nl.obren.sokrates.reports.dataexporters.units.UnitListExporter;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SymbolicLink;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DataExporter {
    public static final String INTERACTIVE_HTML_FOLDER_NAME = "explorers";
    public static final String SRC_CACHE_FOLDER_NAME = "src";
    public static final String DATA_FOLDER_NAME = "data";
    public static final String HISTORY_FOLDER_NAME = "history";
    public static final String SEPARATOR = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n";
    public static final String FOUND_TEXT_PER_FILE_SUFFIX = "_found_text_per_file";
    public static final String FOUND_TEXT_SUFFIX = "_found_text";
    public static final int MAX_EXPORT_LIST_SIZE = 10000;
    private static final Log LOG = LogFactory.getLog(DataExporter.class);
    private ProgressFeedback progressFeedback;
    private File sokratesConfigFile;
    private CodeConfiguration codeConfiguration;
    private File reportsFolder;
    private CodeAnalysisResults analysisResults;
    private File dataFolder;
    private File codeCacheFolder;
    private File textDataFolder;
    // Accumulates the source viewer's data (source files keyed "<aspect>/<relativePath>", fragment
    // bundles keyed "fragments/<type>.json") so it can be embedded base64 into the single shared
    // viewer.html instead of written as sibling zips/JSON the viewer would fetch(). Lets the source
    // viewer open from file:// with no web server.
    // -dataOnly: write nothing outside data/ (no html/Structure.html, src/viewer.html or data-preview.html).
    private boolean dataOnly = false;
    public DataExporter(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }

    public static String dependenciesFileNamePrefix(String fromComponent, String toComponent, String logicalDecompositionName) {
        String fileNamePrefix = "dependencies_" + SystemUtils.getSafeFileName(logicalDecompositionName);
        if (StringUtils.isNotBlank(fromComponent) && StringUtils.isNotBlank(toComponent)) {
            fileNamePrefix += "_" + SystemUtils.getSafeFileName(fromComponent + "_" + toComponent);
        }
        return fileNamePrefix;
    }

    public void saveData(File sokratesConfigFile, CodeConfiguration codeConfiguration, File reportsFolder, CodeAnalysisResults analysisResults) throws IOException {
        this.sokratesConfigFile = sokratesConfigFile;
        this.codeConfiguration = codeConfiguration;
        this.reportsFolder = reportsFolder;
        this.analysisResults = analysisResults;
        this.dataFolder = getDataFolder();
        this.textDataFolder = getTextDataFolder();

        LOG.info("Saving file lists");
        new FileListsDataExporter(analysisResults, textDataFolder, this::saveSymbolicLinks, this::info, this::detailedInfo).exportFileLists();
        LOG.info("Saving metrics data");
        exportMetrics();
        LOG.info("Saving controls data");
        exportControls();
        LOG.info("Saving contributors data");
        exportContributors();
        LOG.info("Saving JSON data");
        exportJson();
        LOG.info("Saving duplication data");
        exportDuplicates();
        LOG.info("Saving units data");
        exportUnits();
        // exportInteractiveExplorers();
        LOG.info("Saving source files");
        exportSourceFile();
        DependenciesDataExporter dependencies = new DependenciesDataExporter(analysisResults, textDataFolder, this::info);
        LOG.info("Saving logical dependencies data");
        dependencies.exportDependencies(analysisResults);
        LOG.info("Saving temporal dependencies data");
        dependencies.saveTemporalDependencies(analysisResults);
    }

    public static final String DATA_ZIP_FILE_NAME = "data.zip";

    // Collapses the per-repository data/ folder (all JSON + text/*.txt + nested zips) into a single
    // data/data.zip and removes the loose files. Drastically cuts the per-repo file count. The HTML
    // reports fetch+extract individual entries on demand (downloadDataFile in ReportConstants), and
    // the landscape analyzer reads each repo's data from this zip (LandscapeAnalyzer). Entry names
    // are paths relative to data/ (e.g. "analysisResults.json", "text/aspect_main.txt"). Called by
    // the CLI as the final data step, AFTER textual-summary + execution-stats are written, so those
    // land inside the zip too.
    public void zipDataFolder() {
        try {
            File zipFile = new File(dataFolder, DATA_ZIP_FILE_NAME);
            ZipUtils.zipFolder(dataFolder, zipFile);

            // Remove the now-redundant loose files/subfolders, keeping only data.zip.
            File[] children = dataFolder.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.equals(zipFile)) {
                        continue;
                    }
                    if (child.isDirectory()) {
                        FileUtils.deleteDirectory(child);
                    } else {
                        FileUtils.deleteQuietly(child);
                    }
                }
            }

            if (!dataOnly) {
                writeDataPreview(dataFolder, zipFile);
            }
        } catch (Exception e) {
            LOG.warn(e);
        }
    }

    public void setDataOnly(boolean dataOnly) {
        this.dataOnly = dataOnly;
    }

    public boolean isDataOnly() {
        return dataOnly;
    }

    // The data-preview.html file name (sits next to data.zip; report data links open it with ?entry=).
    public static final String DATA_PREVIEW_FILE_NAME = "data-preview.html";

    // Writes data/data-preview.html next to data.zip with the whole archive embedded inline as
    // base64. data.zip stays as the raw-data contract (whole-archive download / landscape reads);
    // the preview lets a data link show one entry (pretty JSON / raw text / binary) with a per-entry
    // Download button, working from file:// (no fetch). Shared by the repository and landscape
    // data-folder packaging so both produce a preview. Called after data.zip is built and the loose
    // files removed (the preview must survive that cleanup).
    public static void writeDataPreview(File dataFolder, File dataZip) {
        try {
            String archiveB64 = VisualizationTemplate.base64(FileUtils.readFileToByteArray(dataZip));
            String html = HtmlTemplateUtils.getResource("/templates/data-preview.html")
                    .replace("${sokrates-unzip-lib}", VisualizationTemplate.embedZipLib())
                    .replace("${embedded-archive}", "var SOKRATES_ARCHIVE = \"" + archiveB64 + "\";");
            FileUtils.write(new File(dataFolder, DATA_PREVIEW_FILE_NAME), html, UTF_8);
        } catch (Exception e) {
            LOG.warn(e);
        }
    }

    private void exportMetrics() {
        StringBuilder content = new StringBuilder();

        analysisResults.getMetricsList().getMetrics().forEach(metric -> {
            content.append(metric.getId());
            content.append(": ");
            content.append(metric.getValue());
            content.append("\n");
        });
        try {
            FileUtils.write(new File(textDataFolder, "metrics.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportControls() {
        StringBuilder content = new StringBuilder();

        analysisResults.getControlResults().getGoalsAnalysisResults().forEach(goalsAnalysisResults -> {
            goalsAnalysisResults.getControlStatuses().forEach(status -> {
                content.append("goal: " + goalsAnalysisResults.getMetricsWithGoal().getGoal() + "\n");
                content.append("control metric: " + status.getMetric().getId() + "\n");
                content.append("status: " + status.getStatus() + "\n");
                content.append("desired range: " + status.getControl().getDesiredRange().getTextDescription() + "\n");
                content.append("value: " + status.getMetric().getValue() + "\n");
                content.append("description: " + status.getControl().getDescription() + "\n");
                content.append("\n");
            });
        });
        try {
            FileUtils.write(new File(textDataFolder, "controls.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void exportContributors() {
        StringBuilder content = new StringBuilder();

        List<Contributor> contributors = analysisResults.getContributorsAnalysisResults().getContributors();
        int total = contributors.stream().mapToInt(c -> c.getCommitsCount()).sum();

        content.append("Contributor\t#commits (all time)\t#commits (30 days)\t#commits (90 days)\t#commits (180 days)\t#commits (365 days)\tfirst commit\tlast commit\n");

        contributors.forEach(contributor -> {
            content.append(contributor.getEmail() + "\t");
            content.append(contributor.getCommitsCount() + "\t");
            content.append(contributor.getCommitsCount30Days() + "\t");
            content.append(contributor.getCommitsCount90Days() + "\t");
            content.append(contributor.getCommitsCount180Days() + "\t");
            content.append(contributor.getCommitsCount365Days() + "\t");
            content.append(contributor.getFirstCommitDate() + "\t");
            content.append(contributor.getLatestCommitDate() + "\t");
            double percentage = 100.0 * contributor.getCommitsCount() / total;
            content.append(FormattingUtils.getFormattedPercentage(percentage) + "%\n");
        });
        try {
            FileUtils.write(new File(textDataFolder, "contributors.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportDuplicates() {
        exportDuplicates(analysisResults.getDuplicationAnalysisResults().getAllDuplicates(), "duplicates");
        exportDuplicates(analysisResults.getDuplicationAnalysisResults().getUnitDuplicates(), "unit_duplicates");
    }

    private void exportDuplicates(List<DuplicationInstance> instances, final String fileName) {
        // Cap the INPUT before building the export objects. On very large repositories there can be
        // far more than MAX_EXPORT_LIST_SIZE duplicates, each expanding into many FileExportInfo
        // objects; capping only the output (as before) still materialised the full list first and
        // exhausted the heap. Keep the largest duplicates (by block size), matching duplicates.json.
        if (instances.size() > MAX_EXPORT_LIST_SIZE) {
            instances = instances.stream()
                    .sorted((a, b) -> b.getBlockSize() - a.getBlockSize())
                    .limit(MAX_EXPORT_LIST_SIZE)
                    .collect(Collectors.toList());
        }
        DuplicationExportInfo duplicationExportInfo = new DuplicationExporter(instances).getDuplicationExportInfo();
        List<DuplicateExportInfo> duplicates = duplicationExportInfo.getDuplicates();
        StringBuilder content = new StringBuilder();

        int id[] = {1};
        duplicates.forEach(duplicate -> {
            List<DuplicateFileBlockExportInfo> duplicatedFileBlocks = duplicate.getDuplicatedFileBlocks();
            content.append("duplicated block id: " + id[0] + "\n");
            content.append("size: " + duplicate.getBlockSize() + " cleaned lines of code\n");
            content.append("in " + duplicatedFileBlocks.size() + " files:\n");
            duplicatedFileBlocks.forEach(duplicateFileBlock -> {
                content.append(" - " + duplicateFileBlock.getFile().getRelativePath());
                content.append(" (" + duplicateFileBlock.getStartLine() + ":" + duplicateFileBlock.getEndLine() + ")\n");
            });

            content.append("\n");

            id[0]++;
        });
        try {
            FileUtils.write(new File(textDataFolder, fileName + ".txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportUnits() {
        UnitListExporter units = new UnitListExporter(analysisResults.getUnitsAnalysisResults().getAllUnits());
        int id[] = {1};
        StringBuilder content = new StringBuilder();
        units.getAllUnitsData(MAX_EXPORT_LIST_SIZE).forEach(unit -> {
            content.append("id: " + id[0] + "\n");
            content.append("unit: " + unit.getShortName() + "\n");
            content.append("file: " + unit.getRelativeFileName() + "\n");
            content.append("start line: " + unit.getStartLine() + "\n");
            content.append("end line: " + unit.getEndLine() + "\n");
            content.append("size: " + unit.getLinesOfCode() + " LOC\n");
            content.append("McCabe index: " + unit.getMcCabeIndex() + "\n");
            content.append("number of parameters: " + unit.getNumberOfParameters() + "\n");
            content.append("\n");

            id[0]++;
        });
        try {
            FileUtils.write(new File(textDataFolder, "units.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Names the symbolic links the source code walk did not follow, so a file count smaller than
     * the checkout can be explained from the report rather than from the run's log output.
     *
     * <p>Unlike its two siblings above, this writes nothing when there is no link to name: a
     * repository without symbolic links should produce exactly the output it produced before this
     * file existed, and the report only links here under the same condition.
     */
    private void saveSymbolicLinks() {
        writeSymbolicLinks(textDataFolder, analysisResults.getSkippedSymbolicLinks());
    }

    /**
     * Writes the list, or writes nothing at all when there is none. The "nothing at all" is the
     * point: an empty file here would add an entry to every repository's data.zip, including the
     * ones that have no symbolic links and whose output must not change.
     */
    static void writeSymbolicLinks(File textDataFolder, List<SymbolicLink> symbolicLinks) {
        String content = symbolicLinksContent(symbolicLinks);
        if (content.isEmpty()) {
            return;
        }

        try {
            FileUtils.write(new File(textDataFolder, "symbolic_links.txt"), content, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Renders the skipped links in the grouped, counted shape the other excluded-file lists use.
     * Empty for an empty or absent list, which is what keeps the file from being written at all.
     */
    static String symbolicLinksContent(List<SymbolicLink> symbolicLinks) {
        if (symbolicLinks == null || symbolicLinks.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();

        // Links pointing out of the analysis root first: those are the ones whose files are missing
        // from the measurement altogether, rather than sitting under their real path in the tree.
        // "Pointing inside" says where the path leads, not that anything exists there.
        appendSymbolicLinksGroup(content, "pointing OUTSIDE", symbolicLinks.stream()
                .filter(link -> !link.isInsideAnalysisRoot()).collect(Collectors.toList()));
        appendSymbolicLinksGroup(content, "pointing INSIDE", symbolicLinks.stream()
                .filter(SymbolicLink::isInsideAnalysisRoot).collect(Collectors.toList()));

        return content.toString();
    }

    /**
     * POSIX allows a newline in a file name and in a link target. Left raw, one would split into
     * lines that read as further entries and leave the group's own count disagreeing with what the
     * reader can see under it.
     */
    private static String oneLine(String text) {
        return text.replaceAll("[\\r\\n]+", " ");
    }

    private static void appendSymbolicLinksGroup(StringBuilder content, String where, List<SymbolicLink> symbolicLinks) {
        if (symbolicLinks.isEmpty()) {
            return;
        }
        int total = symbolicLinks.size();
        List<SymbolicLink> shown = total > MAX_EXPORT_LIST_SIZE
                ? symbolicLinks.subList(0, MAX_EXPORT_LIST_SIZE) : symbolicLinks;

        content.append(SEPARATOR);
        content.append("Symbolic links " + where + " the analysis root (" + total + ")");
        // A vendored dependency tree can hold tens of thousands of links (a pnpm store is almost
        // entirely links), none of them actionable. The heading keeps the true count; the list is
        // capped so one such repository cannot put megabytes into data.zip.
        if (shown.size() < total) {
            content.append(" - showing the first " + shown.size());
        }
        content.append(":\n\n");
        shown.forEach(link -> {
            content.append(oneLine(link.getPath()));
            // An unreadable link still belongs in the list; only its target is unknown.
            content.append(StringUtils.isNotBlank(link.getTarget()) ? " -> " + oneLine(link.getTarget()) : " -> ?");
            content.append("\n");
        });
        content.append(SEPARATOR);
        content.append("\n\n\n");
    }

    private void exportSourceFile() throws IOException {
        if (dataOnly) {
            return;
        }
        this.codeCacheFolder = getCodeCacheFolder();
        new SourceViewerExporter(analysisResults, codeConfiguration, reportsFolder, dataFolder, codeCacheFolder, this::detailedInfo).export();
    }

    private void exportJson() throws IOException {
        // Stream the (potentially multi-GB) analysisResults JSON straight to disk — building it as
        // a single String could exceed Java's ~2 GB array limit on very large repositories.
        new JsonGenerator().generateToFile(analysisResults, new File(dataFolder, "analysisResults.json"));

        FileUtils.copyFile(sokratesConfigFile, new File(dataFolder, "config.json"));

        List<SourceFile> mainSourceFiles = analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        new JsonGenerator().generateToFile(mainSourceFiles, new File(dataFolder, "mainFiles.json"));

        FileUtils.write(new File(textDataFolder, "mainFiles.txt"), FileListsDataExporter.getFilesAsTxt(mainSourceFiles), UTF_8);
        FileUtils.write(new File(textDataFolder, "mainFilesWithHistory.txt"), FileListsDataExporter.getFilesWithHistoryAsTxt(mainSourceFiles), UTF_8);
        FileUtils.write(new File(textDataFolder, "mainFilesWithoutHistory.txt"), FileListsDataExporter.getFilesWithoutHistoryAsTxt(mainSourceFiles), UTF_8);
        try {
            List<SourceFile> testSourceFile = analysisResults.getTestAspectAnalysisResults().getAspect().getSourceFiles();
            List<SourceFile> generatedSourceFiles = analysisResults.getGeneratedAspectAnalysisResults().getAspect().getSourceFiles();
            List<SourceFile> buildAndDeploymentSourceFiles = analysisResults.getBuildAndDeployAspectAnalysisResults().getAspect().getSourceFiles();
            List<SourceFile> otherSourceFiles = analysisResults.getOtherAspectAnalysisResults().getAspect().getSourceFiles();

            new JsonGenerator().generateToFile(testSourceFile, new File(dataFolder, "testFiles.json"));
            new JsonGenerator().generateToFile(generatedSourceFiles, new File(dataFolder, "generatedFiles.json"));
            new JsonGenerator().generateToFile(buildAndDeploymentSourceFiles, new File(dataFolder, "buildAndDeploymentFiles.json"));
            new JsonGenerator().generateToFile(otherSourceFiles, new File(dataFolder, "otherFiles.json"));

            // Per-scope history exports (same columns as mainFilesWithHistory.txt), so the landscape
            // file explorer can show commits/age/contributors/churn for non-main files too. Files in
            // every scope are now enriched with history (FileHistoryAnalyzer.enrichFilesWithAge).
            FileUtils.write(new File(textDataFolder, "testFilesWithHistory.txt"), FileListsDataExporter.getFilesWithHistoryAsTxt(testSourceFile), UTF_8);
            FileUtils.write(new File(textDataFolder, "generatedFilesWithHistory.txt"), FileListsDataExporter.getFilesWithHistoryAsTxt(generatedSourceFiles), UTF_8);
            FileUtils.write(new File(textDataFolder, "buildAndDeploymentFilesWithHistory.txt"), FileListsDataExporter.getFilesWithHistoryAsTxt(buildAndDeploymentSourceFiles), UTF_8);
            FileUtils.write(new File(textDataFolder, "otherFilesWithHistory.txt"), FileListsDataExporter.getFilesWithHistoryAsTxt(otherSourceFiles), UTF_8);

            new JsonGenerator().generateToFile(new UnitListExporter(analysisResults.getUnitsAnalysisResults().getAllUnits()).getAllUnitsData(MAX_EXPORT_LIST_SIZE), new File(dataFolder, "units.json"));
            new JsonGenerator().generateToFile(new FileListExporter(analysisResults.getFilesAnalysisResults().getAllFiles()).getAllFilesData(), new File(dataFolder, "files.json"));
            List<DuplicationInstance> allDuplicates = analysisResults.getDuplicationAnalysisResults().getAllDuplicates();
            Collections.sort(allDuplicates, (a, b) -> b.getBlockSize() - a.getBlockSize());
            allDuplicates = allDuplicates.stream().limit(10000).collect(Collectors.toList());
            new JsonGenerator().generateToFile(new DuplicationExporter(allDuplicates).getDuplicationExportInfo(),
                    new File(dataFolder, "duplicates.json"));
            new JsonGenerator().generateToFile(analysisResults.getLogicalDecompositionsAnalysisResults(),
                    new File(dataFolder, "logical_decompositions.json"));
            new JsonGenerator().generateToFile(new DependenciesExporter(analysisResults.getAllDependencies()).getDependenciesExportInfo(),
                    new File(dataFolder, "dependencies.json"));
            new JsonGenerator().generateToFile(analysisResults.getContributorsAnalysisResults().getContributors(), new File(dataFolder, "contributors.json"));
            new JsonGenerator().generateToFile(analysisResults.getConcernsAnalysisResults(), new File(dataFolder, "concerns.json"));

            File zipFolder = new File(dataFolder, "zips");
            zipFolder.mkdirs();

            ZipUtils.stringToZipFile(new File(zipFolder, "all_files.zip"), FileListsDataExporter.aspectFileLists(codeConfiguration, textDataFolder));
            File gitHistoryFile = new File(reportsFolder, "../../git-history.txt");
            if (gitHistoryFile.exists()) {
                // Stream the file into the zip; on huge repositories git-history.txt can exceed the
                // ~2 GB String/array limit, so it must never be read into a single String.
                ZipUtils.fileToZipFile(new File(zipFolder, "git-history.zip"), "git-history.txt", gitHistoryFile);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public File getTextDataFolder() {
        File textDataFolder = new File(dataFolder, "text");
        textDataFolder.mkdirs();
        return textDataFolder;
    }

    public File getCodeCacheFolder() {
        File codeCacheFolder = new File(reportsFolder, SRC_CACHE_FOLDER_NAME);
        codeCacheFolder.mkdirs();
        return codeCacheFolder;
    }

    public File getInteractiveHtmlFolder() {
        File codeCacheFolder = new File(reportsFolder, INTERACTIVE_HTML_FOLDER_NAME);
        codeCacheFolder.mkdirs();
        return codeCacheFolder;
    }

    public File getDataFolder() {
        File dataFolder = new File(reportsFolder, DATA_FOLDER_NAME);
        dataFolder.mkdirs();
        return dataFolder;
    }

    private void info(String text) {
        LOG.info(text);
        if (progressFeedback != null) {
            progressFeedback.setText(text);
        }
    }

    public void detailedInfo(String text) {
        LOG.info(text.replaceAll("<.*?>", ""));
        if (progressFeedback != null) {
            progressFeedback.setDetailedText(text);
        }
    }

}
