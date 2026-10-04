package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.renderingutils.Thresholds;
import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DLink;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DNode;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import nl.obren.sokrates.common.renderingutils.x3d.Unit3D;
import nl.obren.sokrates.common.renderingutils.x3d.X3DomExporter;
import nl.obren.sokrates.common.utils.*;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.stats.SourceFileComplexityDistribution;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Writes the per-repository visuals folder ({@code <reports>/visuals}): the zoomable circles and sunbursts
 * per scope, per commit window and per contributor window, the risk-colored circles, the all-files view with
 * one color per scope ({@code SCOPE_COLOR_*}, kept in sync with the legend in {@code Structure.html}), and
 * the x3dom 3D unit views. Moved out of {@link CommandLineInterface}, which only calls {@link #generateVisuals}.
 */
public class ReportVisualsGenerator {
    private static final Log LOG = LogFactory.getLog(ReportVisualsGenerator.class);
    public static final int THOUSAND_YEARS = 365 * 1000;

    private final CodeConfiguration codeConfiguration;

    public ReportVisualsGenerator(CodeConfiguration codeConfiguration) {
        this.codeConfiguration = codeConfiguration;
    }

    public void generateVisuals(File reportsFolder, CodeAnalysisResults analysisResults) {
        AtomicInteger index = new AtomicInteger();
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            index.getAndIncrement();
            generateComponentVisuals(reportsFolder, analysisResults, logicalDecomposition, index.toString());
        });

        try {
            File folder = new File(reportsFolder, "html/visuals");
            folder.mkdirs();
            generateFileVisuals(folder, analysisResults);
            generate3DUnitsView(folder, analysisResults);
        } catch (IOException e) {
            LOG.warn(e);
        }

    }

    /** One decomposition's bubble chart, tree map and 2D/3D dependency graphs (plus the 3D units view, as before). */
    private void generateComponentVisuals(File reportsFolder, CodeAnalysisResults analysisResults, LogicalDecompositionAnalysisResults logicalDecomposition, String index) {
        List<VisualizationItem> items = new ArrayList<>();
        Force3DObject force3DObject = new Force3DObject();
        logicalDecomposition.getComponents().forEach(component -> {
            items.add(new VisualizationItem(component.getName(), component.getLinesOfCode()));
            force3DObject.getNodes().add(new Force3DNode(component.getName(), component.getLinesOfCode()));
        });
        logicalDecomposition.getComponentDependencies().forEach(dependency -> {
            force3DObject.getLinks().add(new Force3DLink(dependency.getFromComponent(), dependency.getToComponent(), dependency.getCount()));
        });
        try {
            String nameSuffix = "components_" + index + ".html";
            String nameSuffixDependencies = "dependencies_" + index + ".html";
            File folder = new File(reportsFolder, "html/visuals");
            folder.mkdirs();
            FileUtils.write(new File(folder, "bubble_chart_" + nameSuffix), new VisualizationTemplate().renderBubbleChart(items), UTF_8);
            FileUtils.write(new File(folder, "tree_map_" + nameSuffix), new VisualizationTemplate().renderTreeMap(items), UTF_8);
            FileUtils.write(new File(folder, "force_2d_" + nameSuffixDependencies), new VisualizationTemplate().render2DForceGraph(force3DObject), UTF_8);
            FileUtils.write(new File(folder, "force_3d_" + nameSuffixDependencies), new VisualizationTemplate().render3DForceGraph(force3DObject), UTF_8);

            generate3DUnitsView(folder, analysisResults);
        } catch (IOException e) {
            LOG.warn(e);
        }
    }

    /** The per-scope, all-scopes, commit-, contributor- and risk-colored circle/sunburst views, packaged per family. */
    private void generateFileVisuals(File folder, CodeAnalysisResults analysisResults) throws IOException {
        List<SourceFile> mainSourceFiles = analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        List<SourceFile> testSourceFiles = analysisResults.getTestAspectAnalysisResults().getAspect().getSourceFiles();
        List<SourceFile> generatedSourceFiles = analysisResults.getGeneratedAspectAnalysisResults().getAspect().getSourceFiles();
        List<SourceFile> buildSourceFiles = analysisResults.getBuildAndDeployAspectAnalysisResults().getAspect().getSourceFiles();
        List<SourceFile> otherSourceFiles = analysisResults.getOtherAspectAnalysisResults().getAspect().getSourceFiles();

        // Plain zoomable circles/sunburst views are no longer written as one HTML file per
        // view. Instead each view's data is collected here (key = the old filename suffix) and
        // embedded once (as a base64 archive) into a single shared template HTML per family,
        // which extracts the view selected via ?key= in-browser (no fetch, opens from file://).
        // (zoomable_circles_all_files uses a different (colored) template and stays separate.)
        Map<String, String> circlesEntries = new LinkedHashMap<>();
        Map<String, String> sunburstEntries = new LinkedHashMap<>();

        generateFileStructureExplorers("main", circlesEntries, sunburstEntries, mainSourceFiles);
        generateFileStructureExplorers("test", circlesEntries, sunburstEntries, testSourceFiles);
        generateFileStructureExplorers("generated", circlesEntries, sunburstEntries, generatedSourceFiles);
        generateFileStructureExplorers("build", circlesEntries, sunburstEntries, buildSourceFiles);
        generateFileStructureExplorers("other", circlesEntries, sunburstEntries, otherSourceFiles);

        generateAllScopesZoomableCircles(folder, mainSourceFiles, testSourceFiles, buildSourceFiles, generatedSourceFiles, otherSourceFiles);

        for (int days : new int[]{30, 90, 180, 365, 0}) {
            addCommitZoomableCircles("main", circlesEntries, sunburstEntries, mainSourceFiles, days);
        }
        for (int days : new int[]{30, 90, 180, 365, 0}) {
            addContributorsZoomableCircles("main", circlesEntries, sunburstEntries, mainSourceFiles, days);
        }

        addRiskColoredZoomableCircles(circlesEntries, mainSourceFiles, "loc", codeConfiguration.getAnalysis().getFileSizeThresholds(), Palette.getRiskPalette(), (sourceFile) -> sourceFile.getLinesOfCode(), (sourceFile) -> sourceFile.getLinesOfCode());
        // File complexity: only files with units have one (see SourceFileComplexityDistribution).
        addRiskColoredZoomableCircles(circlesEntries, SourceFileComplexityDistribution.filesWithUnits(mainSourceFiles), "mccabe", codeConfiguration.getAnalysis().getFileComplexityThresholds(), Palette.getRiskPalette(), (sourceFile) -> sourceFile.getUnitsMcCabeIndexSum(), (sourceFile) -> sourceFile.getLinesOfCode());

        addRiskColoredZoomableCircles(circlesEntries, mainSourceFiles, "age", codeConfiguration.getAnalysis().getFileAgeThresholds(), Palette.getAgePalette(), (sourceFile) -> sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().daysSinceFirstUpdate() : 0, (sourceFile) -> sourceFile.getLinesOfCode());
        addRiskColoredZoomableCircles(circlesEntries, mainSourceFiles, "freshness", codeConfiguration.getAnalysis().getFileAgeThresholds(), Palette.getFreshnessPalette(),
                (sourceFile) -> sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().daysSinceLatestUpdate() : 0, (sourceFile) -> sourceFile.getLinesOfCode());

        addRiskColoredZoomableCircles(circlesEntries, mainSourceFiles, "update_frequency", codeConfiguration.getAnalysis().getFileUpdateFrequencyThresholds(), Palette.getHeatPalette(),
                (sourceFile) -> sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().getDates().size() : 0, (sourceFile) -> sourceFile.getLinesOfCode());

        addRiskColoredZoomableCircles(circlesEntries, mainSourceFiles, "contributors_count", codeConfiguration.getAnalysis().getFileContributorsCountThresholds(), Palette.getHeatPalette(),
                (sourceFile) -> sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().countContributors() : 0, (sourceFile) -> sourceFile.getLinesOfCode());

        writeZoomableFamily(folder, "zoomable_circles", circlesEntries);
        writeZoomableFamily(folder, "zoomable_sunburst", sunburstEntries);
    }

    // Writes the shared <family>.html template with the per-view archive (one <key>.json entry per
    // view) embedded inline as base64. The page extracts the ?key= view from that embedded archive
    // in-browser (sokratesUnzip) — no sibling .zip and no fetch(), so the report opens from file://.
    private void writeZoomableFamily(File folder, String family, Map<String, String> entries) throws IOException {
        String[][] zipEntries = entries.entrySet().stream()
                .map(e -> new String[]{e.getKey() + ".json", e.getValue()})
                .toArray(String[][]::new);
        String archiveB64 = VisualizationTemplate.base64(ZipUtils.stringEntriesToZipBytes(zipEntries));
        // Embedded-archive page: leave the inline-data placeholder empty (SOKRATES_INLINE_DATA stays
        // undefined) and fill the embedded archive so the page extracts its view from inline bytes.
        String template = new VisualizationTemplate().rawTemplate(family + ".html")
                .replace("${sokrates-inline-data}", "")
                .replace("${embedded-archive}", "var SOKRATES_ARCHIVE = \"" + archiveB64 + "\";");
        FileUtils.write(new File(folder, family + ".html"), template, UTF_8);
    }

    private void generateFileStructureExplorers(String nameSuffix, Map<String, String> circlesEntries, Map<String, String> sunburstEntries, List<SourceFile> sourceFiles) throws IOException {
        List<VisualizationItem> items = getZoomableCirclesItems(sourceFiles);
        String json = VisualizationTemplate.zoomableItemsJson(items);
        circlesEntries.put(nameSuffix, json);
        sunburstEntries.put(nameSuffix, json);
    }

    // Colors used to distinguish scopes in the "all files" zoomable circles view.
    // Keep these in sync with the legend in reports' Structure.html template.
    private static final String SCOPE_COLOR_MAIN = "#ffffff";
    private static final String SCOPE_COLOR_TEST = "#b2df8a";
    private static final String SCOPE_COLOR_BUILD = "#fdbf6f";
    private static final String SCOPE_COLOR_GENERATED = "#cab2d6";
    private static final String SCOPE_COLOR_OTHER = "#d9d9d9";

    private void generateAllScopesZoomableCircles(File folder, List<SourceFile> mainSourceFiles, List<SourceFile> testSourceFiles,
                                                  List<SourceFile> buildSourceFiles, List<SourceFile> generatedSourceFiles,
                                                  List<SourceFile> otherSourceFiles) throws IOException {
        // Group by folder structure (one shared directory tree across all scopes),
        // color-coding each file leaf by the scope it belongs to.
        List<SourceFile> allSourceFiles = new ArrayList<>();
        Map<SourceFile, String> colors = new LinkedHashMap<>();
        collectScope(allSourceFiles, colors, mainSourceFiles, SCOPE_COLOR_MAIN);
        collectScope(allSourceFiles, colors, testSourceFiles, SCOPE_COLOR_TEST);
        collectScope(allSourceFiles, colors, buildSourceFiles, SCOPE_COLOR_BUILD);
        collectScope(allSourceFiles, colors, generatedSourceFiles, SCOPE_COLOR_GENERATED);
        collectScope(allSourceFiles, colors, otherSourceFiles, SCOPE_COLOR_OTHER);

        List<VisualizationItem> items = new ArrayList<>();
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(allSourceFiles);
        if (directoryTree != null) {
            items = directoryTree.toVisualizationItems(colors);
        }

        FileUtils.write(new File(folder, "zoomable_circles_all_files.html"), new VisualizationTemplate().renderZoomableCirclesColored(items), UTF_8);
    }

    private void collectScope(List<SourceFile> allSourceFiles, Map<SourceFile, String> colors, List<SourceFile> sourceFiles, String color) {
        if (sourceFiles == null) {
            return;
        }
        sourceFiles.forEach(sourceFile -> {
            allSourceFiles.add(sourceFile);
            colors.put(sourceFile, color);
        });
    }

    private void addCommitZoomableCircles(String nameSuffix, Map<String, String> circlesEntries, Map<String, String> sunburstEntries, List<SourceFile> sourceFiles, int daysAgo) throws IOException {
        List<VisualizationItem> commitItems = getZoomableCirclesCommitItems(sourceFiles, daysAgo > 0 ? daysAgo : THOUSAND_YEARS);
        String suffix = daysAgo > 0 ? "_" + daysAgo + "_" + nameSuffix : "";
        String json = VisualizationTemplate.zoomableItemsJson(commitItems);
        circlesEntries.put("commits" + suffix, json);
        sunburstEntries.put("commits" + suffix, json);
    }

    private void addContributorsZoomableCircles(String nameSuffix, Map<String, String> circlesEntries, Map<String, String> sunburstEntries, List<SourceFile> sourceFiles, int daysAgo) throws IOException {
        List<VisualizationItem> commitItems = getZoomableCirclesContributorItems(sourceFiles, daysAgo > 0 ? daysAgo : THOUSAND_YEARS);
        String suffix = (daysAgo > 0 ? ("_" + daysAgo) : "") + ("_" + nameSuffix);
        String json = VisualizationTemplate.zoomableItemsJson(commitItems);
        circlesEntries.put("contributors" + suffix, json);
        sunburstEntries.put("contributors" + suffix, json);
    }

    private void addRiskColoredZoomableCircles(Map<String, String> circlesEntries, List<SourceFile> sourceFiles, String type,
                                               nl.obren.sokrates.sourcecode.threshold.Thresholds thresholds, Palette palette, DirectoryNode.SourceFileValueExtractor colorValueExtractor, DirectoryNode.SourceFileValueExtractor sizeValueExtractor) throws IOException {
        List<VisualizationItem> items = getZoomableCirclesRiskProfileItems(sourceFiles, thresholds, palette, colorValueExtractor, sizeValueExtractor);
        circlesEntries.put("main_" + type + "_coloring", VisualizationTemplate.zoomableItemsJson(items));

        List<VisualizationItem> itemsByCategory = getZoomableCirclesRiskProfileItemsCategories(sourceFiles, thresholds, palette, colorValueExtractor);
        circlesEntries.put("main_" + type + "_coloring_categories", VisualizationTemplate.zoomableItemsJson(itemsByCategory));
    }

    private List<VisualizationItem> getZoomableCirclesItems(List<SourceFile> sourceFiles) {
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(sourceFiles);
        if (directoryTree != null) {
            return directoryTree.toVisualizationItems();
        }

        return new ArrayList<>();
    }

    private List<VisualizationItem> getZoomableCirclesCommitItems(List<SourceFile> sourceFiles, int daysAgo) {
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(sourceFiles);
        if (directoryTree != null) {
            return directoryTree.toVisualizationCommitItems(daysAgo);
        }

        return new ArrayList<>();
    }

    private List<VisualizationItem> getZoomableCirclesContributorItems(List<SourceFile> sourceFiles, int daysAgo) {
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(sourceFiles);
        if (directoryTree != null) {
            return directoryTree.toVisualizationContributorItems(daysAgo);
        }

        return new ArrayList<>();
    }

    private List<VisualizationItem> getZoomableCirclesRiskProfileItems(
            List<SourceFile> sourceFiles, nl.obren.sokrates.sourcecode.threshold.Thresholds thresholds, Palette palette, DirectoryNode.SourceFileValueExtractor colorValueExtractor, DirectoryNode.SourceFileValueExtractor sizeValueExtractor) {
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(sourceFiles);
        if (directoryTree != null) {
            return directoryTree.toVisualizationRiskColoringItems(thresholds, palette, colorValueExtractor, sizeValueExtractor);
        }

        return new ArrayList<>();
    }

    private List<VisualizationItem> getZoomableCirclesRiskProfileItemsCategories(
            List<SourceFile> sourceFiles, nl.obren.sokrates.sourcecode.threshold.Thresholds thresholds, Palette palette, DirectoryNode.SourceFileValueExtractor valueExtractor) {

        VisualizationItem item1 = new VisualizationItem(thresholds.getNegligibleRiskLabel(), 0);
        VisualizationItem item2 = new VisualizationItem(thresholds.getLowRiskLabel(), 0);
        VisualizationItem item3 = new VisualizationItem(thresholds.getMediumRiskLabel(), 0);
        VisualizationItem item4 = new VisualizationItem(thresholds.getHighRiskLabel(), 0);
        VisualizationItem item5 = new VisualizationItem(thresholds.getVeryHighRiskLabel(), 0);

        sourceFiles.forEach(sourceFile -> {
            int value = valueExtractor.getValue(sourceFile);
            String path = sourceFile.getRelativePath();
            int loc = sourceFile.getLinesOfCode();
            if (value <= thresholds.getLow()) {
                item1.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            } else if (value <= thresholds.getMedium()) {
                item2.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            } else if (value <= thresholds.getHigh()) {
                item3.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            } else if (value <= thresholds.getVeryHigh()) {
                item4.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            } else {
                item5.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            }
        });

        item1.setName(item1.getName() + " (" + item1.getChildren().size() + ")");
        item2.setName(item2.getName() + " (" + item2.getChildren().size() + ")");
        item3.setName(item3.getName() + " (" + item3.getChildren().size() + ")");
        item4.setName(item4.getName() + " (" + item4.getChildren().size() + ")");
        item5.setName(item5.getName() + " (" + item5.getChildren().size() + ")");

        return new ArrayList<>(Arrays.asList(item1, item2, item3, item4, item5));
    }

    private void generate3DUnitsView(File visualsFolder, CodeAnalysisResults analysisResults) {
        AnalysisConfig analysisConfig = analysisResults.getCodeConfiguration().getAnalysis();

        List<Unit3D> unit3DConditionalComplexity = new ArrayList<>();
        analysisResults.getUnitsAnalysisResults().getAllUnits().forEach(unit -> {
            BasicColorInfo color = Thresholds.getColor(Thresholds.UNIT_MCCABE, unit.getMcCabeIndex());
            unit3DConditionalComplexity.add(new Unit3D(unit.getLongName(), unit.getLinesOfCode(), color));
        });

        List<Unit3D> unit3DSize = new ArrayList<>();
        analysisResults.getUnitsAnalysisResults().getAllUnits().forEach(unit -> {
            BasicColorInfo color = Thresholds.getColor(Thresholds.UNIT_LINES, unit.getLinesOfCode());
            unit3DSize.add(new Unit3D(unit.getLongName(), unit.getLinesOfCode(), color));
        });

        List<Unit3D> files3D = new ArrayList<>();
        analysisResults.getCodeConfiguration().getMain().getSourceFiles().forEach(file -> {
            SourceFileSizeDistribution sourceFileSizeDistribution = new SourceFileSizeDistribution(analysisConfig.getFileSizeThresholds());
            BasicColorInfo color = getFileSizeColor(sourceFileSizeDistribution, file.getLinesOfCode());
            files3D.add(new Unit3D(file.getFile().getPath(), file.getLinesOfCode(), color));
        });

        new X3DomExporter(new File(visualsFolder, "units_3d_complexity.html"), "A 3D View of All Units (Conditional Complexity)", "Each block is one unit. The height of the block represents the file unit size in lines of code. The color of the unit represents its conditional complexity category.").export(unit3DConditionalComplexity, false, 10);

        new X3DomExporter(new File(visualsFolder, "units_3d_size.html"), "A 3D View of All Units (Unit Size)", "Each block is one unit. The height of the block represents the file unit size in lines of code. The color of the unit represents its size category.").export(unit3DSize, false, 10);

        new X3DomExporter(new File(visualsFolder, "files_3d.html"), "A 3D View of All Files", "Each block is one file. The height of the block represents the file relative size in lines of code. The color of the file represents its size category.").export(files3D, false, 50);
    }

    public BasicColorInfo getFileSizeColor(SourceFileSizeDistribution distribution, int linesOfCode) {
        if (linesOfCode <= distribution.getLowRiskThreshold()) {
            return Thresholds.RISK_GREEN;
        } else if (linesOfCode <= distribution.getMediumRiskThreshold()) {
            return Thresholds.RISK_LIGHT_GREEN;
        } else if (linesOfCode <= distribution.getHighRiskThreshold()) {
            return Thresholds.RISK_YELLOW;
        } else if (linesOfCode <= distribution.getVeryHighRiskThreshold()) {
            return Thresholds.RISK_ORANGE;
        } else {
            return Thresholds.RISK_RED;
        }
    }
}
