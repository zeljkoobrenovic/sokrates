/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.Force3DGraphExporter;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.DuplicationReportUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.duplication.DuplicationDependenciesHelper;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class DuplicationReportGenerator {
    public static final int MAX_TABLE_ROWS_COUNT = 100;
    private CodeAnalysisResults codeAnalysisResults;
    private File reportsFolder;
    private RichTextReport report;
    private int graphCounter = 1;
    private int componentDuplicatesCount = 1;
    private int filePairsCount = 1;

    public DuplicationReportGenerator(CodeAnalysisResults codeAnalysisResults, File reportsFolder) {
        this.codeAnalysisResults = codeAnalysisResults;
        this.reportsFolder = reportsFolder;
    }

    public void getDuplicatesTable(RichTextReport report, List<DuplicationInstance> duplicationInstances, String fragmentType) {
        report.startDiv("width: 100%; overflow-x: auto");
        report.startScrollingDiv();
        report.addHtmlContent("<table style='width: 80%'>\n");
        boolean saveCodeFragments = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveCodeFragments();
        report.addHtmlContent("<th>Size</th><th>#</th><th>Folders</th><th>Files</th><th>Lines</th>" + (saveCodeFragments ? "<th>Code</th>" : ""));
        int count[] = {0};
        duplicationInstances.stream().limit(MAX_TABLE_ROWS_COUNT).forEach(instance -> {
            count[0]++;
            report.addHtmlContent("<tr>\n");

            SourceFile firstSourceFile = instance.getDuplicatedFileBlocks().get(0).getSourceFile();
            String extension = firstSourceFile.getExtension();

            report.addHtmlContent("<td>" + instance.getBlockSize() + "</td>");
            report.addHtmlContent("<td>x&nbsp;" + instance.getDuplicatedFileBlocks().size() + "</td>");
            String folderString = formatDisplayString(instance.getFoldersDisplayString());
            report.addHtmlContent("<td>" + folderString + "</td>");
            boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
            report.addHtmlContent("<td>" +
                    "<div style='white-space: nowrap;'><div style='display: inline-block; vertical-align: top; margin-top: 3px; margin-right: 4px;'>" +
                    DataImageUtils.getLangDataImageDiv30(extension) +
                    "</div><div style='display: inline-block;'>"
                    + formatDisplayStringSimple(instance.getFilesDisplayString(cacheSourceFiles))
                    + "</div></div></td>");
            report.addHtmlContent("<td>" + formatDisplayString(instance.getLinesDisplayString()) + "</td>");
            if (saveCodeFragments) {
                String url = "../src/fragments/" + fragmentType + "/" + fragmentType + "_" + count[0] + "." + extension;
                report.addHtmlContent("<td><a target='_blank' href='" + url + "'>view</a></td>");
            }
            report.addHtmlContent("</tr>\n");
        });
        report.addHtmlContent("</table>\n");
        report.endDiv();
        report.endDiv();
    }

    private String formatDisplayStringSimple(String text) {
        return text.replace("\n", "</br>");
    }

    private String formatDisplayString(String text) {
        text = text.replace(" ", "&nbsp;");
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : text.split("\n")) {
            stringBuilder
                    .append("<span title='" + line + "'>")
                    .append(StringUtils.abbreviateMiddle(line, "...", 50))
                    .append("</span>")
                    .append("\n");
        }
        return stringBuilder.toString().replace("\n", "</br>");
    }

    public void addDuplicationToReport(RichTextReport report) {
        this.report = report;
        addIntro(report);

        if (codeAnalysisResults.skipDuplicationAnalysis()) {
            report.addParagraph("Duplication analysis has been skipped.");
            return;
        }

        ProcessingStopwatch.start("reporting/duplication/overall");
        addOverallDuplicationSection(report);
        ProcessingStopwatch.end("reporting/duplication/overall");
        ProcessingStopwatch.start("reporting/duplication/per extension");
        addDuplicationPerExtensionSection(report);
        ProcessingStopwatch.end("reporting/duplication/per extension");
        ProcessingStopwatch.start("reporting/duplication/per logical decomposition");
        addDuplicationPerLogicalDecomposition(report);
        ProcessingStopwatch.end("reporting/duplication/per logical decomposition");
        ProcessingStopwatch.start("reporting/duplication/longest duplicates");
        addLongestDuplicatesList(report);
        ProcessingStopwatch.end("reporting/duplication/longest duplicates");
        ProcessingStopwatch.start("reporting/duplication/duplicated units");
        addDuplicatedUnitsList(report);
        ProcessingStopwatch.end("reporting/duplication/duplicated units");
    }

    private void addDuplicatedUnitsList(RichTextReport report) {
        DuplicationAnalysisResults duplicationAnalysisResults = codeAnalysisResults.getDuplicationAnalysisResults();
        if (duplicationAnalysisResults.getUnitDuplicates().size() > 0) {
            List<DuplicationInstance> unitDuplicates = duplicationAnalysisResults.getUnitDuplicates();
            int originalSize = unitDuplicates.size();
            int maxTopListSize = codeAnalysisResults.getCodeConfiguration().getAnalysis().getMaxTopListSize();
            if (unitDuplicates.size() > maxTopListSize) {
                unitDuplicates = unitDuplicates.subList(0, maxTopListSize);
            }
            report.startSection("Duplicated Units", "The list of top " + unitDuplicates.size() + " duplicated units.");
            report.addContentInDiv("<a href='../data/text/unit_duplicates.txt'>See data for all <b>" + FormattingUtils.formatCount(originalSize) + "</b> unit " + (originalSize == 1 ? "duplicate" : "duplicates...") + "</b></a>", "margin-bottom: 16px");
            getDuplicatesTable(report, unitDuplicates, "unit_duplicates");
            report.endSection();
        }
    }

    private void addDuplicationPerLogicalDecomposition(RichTextReport report) {
        int index[] = {0};
        codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerComponent().forEach(duplicationPerComponent -> {
            index[0] += 1;
            String prefix = "reporting/duplication/per logical decomposition/" + index[0] + "/";

            ProcessingStopwatch.start(prefix + "table");
            LogicalDecomposition logicalDecomposition = getLogicalDecomposition(codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerComponent().indexOf(duplicationPerComponent));
            String logicalDecompositionName = logicalDecomposition.getName();
            report.startSection("Duplication per Component (" + logicalDecompositionName + ")", "");
            DuplicationReportUtils.addDuplicationPerAspect(report, duplicationPerComponent);
            ProcessingStopwatch.end(prefix + "table");

            ProcessingStopwatch.start(prefix + "rendering");
            report.startDiv("");
            renderDependenciesViaDuplication(report, duplicationPerComponent, logicalDecomposition, prefix + "rendering");
            report.endDiv();
            ProcessingStopwatch.end(prefix + "rendering");
            report.endSection();
        });
    }

    private void addDuplicationPerExtensionSection(RichTextReport report) {
        report.startSection("Duplication per Extension", "");
        List<DuplicationMetric> duplicationPerExtension = codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerExtension();
        if (duplicationPerExtension.size() > 0) {
            DuplicationReportUtils.addDuplicationPerAspect(report, duplicationPerExtension);
        }
        report.endSection();
    }

    private void addOverallDuplicationSection(RichTextReport report) {
        DuplicationAnalysisResults duplicationAnalysisResults = codeAnalysisResults.getDuplicationAnalysisResults();

        report.startSection("Duplication Overall", "");
        report.startUnorderedList();
        report.addListItem("<b>" + FormattingUtils.getFormattedPercentage(duplicationAnalysisResults.getOverallDuplication().getDuplicationPercentage().doubleValue()) + "%</b> duplication:");
        report.startUnorderedList();
        report.addListItem("<b>" + FormattingUtils.formatCount(duplicationAnalysisResults.getOverallDuplication().getCleanedLinesOfCode()) + "</b> cleaned lines of cleaned code (without empty lines, comments, and frequently duplicated constructs such as imports)");
        report.addListItem("<b>" + FormattingUtils.formatCount(duplicationAnalysisResults.getOverallDuplication().getDuplicatedLinesOfCode()) + "</b> duplicated lines");
        report.endUnorderedList();
        report.addListItem("<a href='../data/text/duplicates.txt'><b>" + FormattingUtils.formatCount(duplicationAnalysisResults.getAllDuplicates().size()) + " duplicates</b></a>");
        report.endUnorderedList();
        DuplicationReportUtils.addOverallDuplication(report, duplicationAnalysisResults.getOverallDuplication());
        export3DFileDependencies();
        report.addHtmlContent("dependency graphs: ");
        report.addNewTabLink("2D graph", "visuals/duplication_among_files.svg" );
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/duplication_among_files_force_3d.html" );
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with duplicates)...", "visuals/duplication_among_files_with_duplicates_force_3d.html" );
        report.endSection();
    }

    private void addIntro(RichTextReport report) {
        int locDuplicationThreshold = codeAnalysisResults.getCodeConfiguration().getAnalysis().getMinDuplicationBlockLoc();
        report.addParagraph("Places in code with " + locDuplicationThreshold + " or more lines that " +
                "are exactly the same.", "margin-top: 12px; color: grey");
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("For duplication, we look at places in code where there are " + locDuplicationThreshold + " or more lines of code that are exactly the same.");
        report.addListItem("Before duplication is calculated, the code is cleaned to remove empty lines, comments, and frequently duplicated constructs such as imports.");
        report.addListItem("You should aim at having as little as possible (<5%) of duplicated code as high-level of duplication can lead to maintenance difficulties, poor factoring, and logical contradictions.");
        report.endUnorderedList();
        report.startShowMoreBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("To learn more about duplications and techniques for eliminating duplication, Sokrates recommends the following resources:");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='https://martinfowler.com/ieeeSoftware/repetition.pdf'>Avoid Repetition</a>, MartinFlower.com (IEEE Software article)");
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/refactoring/smells/duplicate-code'>Refactoring duplicated code</a>, sourcemaking.com");
        report.addListItem("<a target='_blank' href='https://martinfowler.com/bliki/BeckDesignRules.html'>Beck Design Rules</a>, MartinFowler.com");
        report.addListItem("<a target='_blank' href='https://en.wikipedia.org/wiki/Don%27t_repeat_yourself'>DRY (Don't Repeat Yourself) Principle</a>, Wikipedia");
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/antipatterns/cut-and-paste-programming'>The Cut-and-Paste Programming Software Development Anti-Pattern</a>, sourcemaking.com");
        report.addListItem("<a target='_blank' href='https://blog.codinghorror.com/code-smells/'>Code Smells / Duplicated Code</a>, Jeff Atwood, codinghorror.com:");
        report.startUnorderedList();
        report.addListItem("<i>\"Duplicated code is the bane of software development. Stamp out duplication whenever possible. You should always be on the lookout for more subtle cases of near-duplication, too. Don't Repeat Yourself!\"</i>");
        report.endUnorderedList();
        report.addListItem("<a target='_blank' href='https://martinfowler.com/ieeeSoftware/coupling.pdf'>Reducing Coupling</a>, MartinFlower.com (IEEE Software article):");
        report.startUnorderedList();
        report.addListItem("<i>\"There are several ways to describe coupling, but it boils down to this: If changing one module in a program requires changing another module, then coupling exists.  ... Duplication always implies coupling, because changing one piece of duplicate code implies changing the other.\"</i>");

        report.endUnorderedList();

        report.endUnorderedList();
        report.endUnorderedList();
        report.endShowMoreBlock();

        report.endSection();
    }

    private void renderDependenciesViaDuplication(RichTextReport report, List<DuplicationMetric> duplicationPerComponent,
                                                  LogicalDecomposition logicalDecomposition, String monitoringPrefix) {
        ProcessingStopwatch.start(monitoringPrefix + "/extracting dependencies");
        DuplicationDependenciesHelper duplicationDependenciesHelper = new DuplicationDependenciesHelper(logicalDecomposition.getName());
        List<ComponentDependency> allDuplicates = duplicationDependenciesHelper.extractDependencies(codeAnalysisResults.getDuplicationAnalysisResults().getAllDuplicates());
        ProcessingStopwatch.end(monitoringPrefix + "/extracting dependencies");
        ProcessingStopwatch.start(monitoringPrefix + "/updating dependencies");
        allDuplicates.forEach(dependency -> {
            duplicationPerComponent.stream().filter(duplication -> duplication.getKey().equalsIgnoreCase(dependency.getFromComponent())).findFirst()
                    .ifPresent(c -> {
                        int cleanedLinesOfCode = c.getCleanedLinesOfCode();
                        dependency.setValueFrom(cleanedLinesOfCode > 0 ? 100.0 * (dependency.getCount() / 2.0) / cleanedLinesOfCode : 0);
                    });
            duplicationPerComponent.stream().filter(duplication -> duplication.getKey().equalsIgnoreCase(dependency.getToComponent())).findFirst()
                    .ifPresent(c -> {
                        int cleanedLinesOfCode = c.getCleanedLinesOfCode();
                        dependency.setValueTo(cleanedLinesOfCode > 0 ? 100.0 * (dependency.getCount() / 2.0) / cleanedLinesOfCode : 0);
                    });
        });
        int threshold = logicalDecomposition.getDuplicationLinkThreshold();
        List<ComponentDependency> componentDependencies = allDuplicates.stream().filter(d -> d.getCount() >= threshold).collect(Collectors.toList());
        ProcessingStopwatch.end(monitoringPrefix + "/updating dependencies");

        List<DuplicationInstance> instances = duplicationDependenciesHelper.getInstances();

        ProcessingStopwatch.start(monitoringPrefix + "/rendering");
        if (componentDependencies.size() > 0) {
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("#DC143C");
            graphvizDependencyRenderer.setMaxNumberOfDependencies(50);
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), componentDependencies);
            report.addLevel3Header("Duplication Between Components (" + threshold + "+ lines)", "margin-top: 30px");

            String graphId = "duplication_dependencies_" + graphCounter++;
            report.addGraphvizFigure(graphId, "Duplication between components", graphvizContent);

            report.addLineBreak();

            VisualizationTools.addDownloadLinks(report, graphId);
            report.addLineBreak();
            Pair<String,String> force3DGraphFilePath = ForceGraphExporter.export3DForceGraph(componentDependencies, reportsFolder, graphId);
            report.addNewTabLink("Open 2D force graph...", force3DGraphFilePath.getFirst());
            report.addNewTabLink("Open 3D force graph...", force3DGraphFilePath.getSecond());

            report.addLineBreak();
            report.addLineBreak();

            addMoreDetailsSection(report, componentDependencies, logicalDecomposition.getName(), instances);

            report.addLineBreak();
        }

        ProcessingStopwatch.end(monitoringPrefix + "/rendering");
    }

    private void export3DFileDependencies() {
        List<ComponentDependency> duplicatesAsFileDependencies = getDuplicatesAsFileDependencies();
        new Force3DGraphExporter().export2D3DForceGraph(duplicatesAsFileDependencies, new File(reportsFolder, "html"), "duplication_among_files");
        new Force3DGraphExporter().export2D3DForceGraph(getDuplicatesAsDependencies(), new File(reportsFolder, "html"), "duplication_among_files_with_duplicates");
        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
        graphvizDependencyRenderer.setType("graph");
        graphvizDependencyRenderer.setArrow("--");
        graphvizDependencyRenderer.setArrowColor("#DC143C");
        graphvizDependencyRenderer.setMaxNumberOfDependencies(200);
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), duplicatesAsFileDependencies);
        String svgContent = GraphvizUtil.getSvgFromDot(graphvizContent);
        try {
            FileUtils.write(new File(reportsFolder, "html/visuals/duplication_among_files.svg"), svgContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ComponentDependency> getDuplicatesAsDependencies() {
        List<DuplicationInstance> duplicates = codeAnalysisResults.getDuplicationAnalysisResults().getAllDuplicates();
        List<ComponentDependency> dependencies = new ArrayList<>();

        int index[] = {0};
        duplicates.forEach(duplicate -> {
            index[0] += 1;
            duplicate.getDuplicatedFileBlocks().forEach(block -> {
                ComponentDependency dependency = new ComponentDependency();
                dependency.setFromComponent("[" + block.getSourceFile().getRelativePath() + "]");
                dependency.setToComponent("duplicate" + index[0] + "");
                dependencies.add(dependency);
            });
        });
        return dependencies;
    }

    private List<ComponentDependency> getDuplicatesAsFileDependencies() {
        List<DuplicationInstance> duplicates = codeAnalysisResults.getDuplicationAnalysisResults().getAllDuplicates();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();
        List<ComponentDependency> dependencies = new ArrayList<>();

        int index[] = {0};
        duplicates.forEach(duplicate -> {
            duplicate.getDuplicatedFileBlocks().forEach(block1 -> {
                duplicate.getDuplicatedFileBlocks().stream().filter(block2 -> block1 != block2).forEach(block2 -> {
                    String path1 = "[" + block1.getSourceFile().getRelativePath() + "]";
                    String path2 = "[" + block2.getSourceFile().getRelativePath() + "]";
                    String key1 = path1 + "::" + path2;
                    String key2 = path2 + "::" + path1;

                    if (dependenciesMap.containsKey(key1)) {
                        dependenciesMap.get(key1).increment(duplicate.getBlockSize());
                    } else if (dependenciesMap.containsKey(key2)) {
                        dependenciesMap.get(key2).increment(duplicate.getBlockSize());
                    } else {
                        ComponentDependency dependency = new ComponentDependency(path1, path2);
                        dependency.setCount(duplicate.getBlockSize());
                        dependencies.add(dependency);
                        dependenciesMap.put(key1, dependency);
                    }
                });
            });
        });
        return dependencies;
    }


    private void addMoreDetailsSection(RichTextReport report, List<ComponentDependency> componentDependencies, String logicalDecompositionName, List<DuplicationInstance> instances) {
        Collections.sort(componentDependencies, (o1, o2) -> o2.getCount() - o1.getCount());

        report.startShowMoreBlock("Show more details on duplication between components...");
        report.startDiv("width: 100%; overflow-x: auto");
        report.startTable();
        report.addTableHeader("From Component<br/>&nbsp;--> To Component", "Duplicated<br/>Lines", "File Pairs", "Details");

        componentDependencies.forEach(componentDependency -> {
            report.startTableRow();

            String formattedPercentageFrom = FormattingUtils.getFormattedPercentage(componentDependency.getValueFrom());
            String formattedPercentageTo = FormattingUtils.getFormattedPercentage(componentDependency.getValueTo());
            report.addTableCell(componentDependency.getFromComponent()
                    + (!formattedPercentageFrom.equals("0") ? " (" + formattedPercentageFrom + "%)" : "")
                    + "<br/>&nbsp&nbsp;-->&nbsp"
                    + componentDependency.getToComponent() + (!formattedPercentageTo.equals("0") ? " (" + formattedPercentageTo + "%)" : ""));

            report.addTableCell(componentDependency.getCount() + "", "text-align: center");

            int pairsCount = componentDependency.getEvidence().size();
            String filePairsText = pairsCount + (pairsCount == 1 ? " file pair" : " file pairs");

            report.startTableCell("text-align: center");
            report.addNewTabLink(filePairsText, saveFilePairs(componentDependency));
            report.endTableCell();

            report.startTableCell();
            report.addNewTabLink("details...", saveDuplicates(componentDependency, logicalDecompositionName, instances));
            report.endTableCell();

            report.endTableRow();
        });
        report.endTable();
        report.endDiv();
        report.endShowMoreBlock();
    }

    private String saveFilePairs(ComponentDependency componentDependency) {
        File file = new File(this.report.getReportsFolder(), "data/text/intercomponent_duplicated_file_pairs_" + filePairsCount++ + ".txt");

        try {
            String content = componentDependency.getEvidence().stream()
                    .map(d -> d.getPathFrom())
                    .collect(Collectors.joining("\n\n"));
            FileUtils.write(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "../data/text/" + file.getName();
    }

    private String saveDuplicates(ComponentDependency componentDependency, String logicalDecompositionName, List<DuplicationInstance> allInstances) {
        File file = new File(this.report.getReportsFolder(), "data/text/intercomponent_duplicates_" + componentDuplicatesCount++ + ".txt");
        List<DuplicationInstance> duplicates = this.codeAnalysisResults.getDuplicationAnalysisResults().getAllDuplicates();

        String from = componentDependency.getFromComponent();
        String to = componentDependency.getToComponent();

        List<DuplicationInstance> instances = new ArrayList<>();

        allInstances.forEach(duplicate -> {
            boolean fromPresent[] = {false};
            boolean toPresent[] = {false};
            duplicate.getDuplicatedFileBlocks().forEach(duplicatedFileBlock -> {
                SourceFile sourceFile = duplicatedFileBlock.getSourceFile();
                List<NamedSourceCodeAspect> components = sourceFile.getLogicalComponents(logicalDecompositionName);
                if (components.stream().filter(c -> c.getName().equalsIgnoreCase(from)).findAny().isPresent()) {
                    fromPresent[0] = true;
                }
                if (components.stream().filter(c -> c.getName().equalsIgnoreCase(to)).findAny().isPresent()) {
                    toPresent[0] = true;
                }
            });

            if (toPresent[0] && fromPresent[0]) {
                instances.add(duplicate);
            }
        });

        StringBuilder stringBuilder = new StringBuilder();

        Collections.sort(instances, (o1, o2) -> o2.getBlockSize() - o1.getBlockSize());

        instances.forEach(instance -> {
            stringBuilder.append(instance.getBlockSize() + " duplicated lines in:\n");
            instance.getDuplicatedFileBlocks().forEach(block -> {
                stringBuilder.append("  - ");
                stringBuilder.append(block.getSourceFile().getRelativePath());
                stringBuilder.append(" (");
                stringBuilder.append(block.getStartLine());
                stringBuilder.append(":");
                stringBuilder.append(block.getEndLine());
                stringBuilder.append(", ");
                stringBuilder.append(FormattingUtils.getFormattedPercentage(block.getPercentage()) + "%");
                stringBuilder.append(")\n");
            });
            stringBuilder.append("\n");
        });

        try {
            FileUtils.write(file, stringBuilder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "../data/text/" + file.getName();
    }

    private LogicalDecomposition getLogicalDecomposition(int index) {
        return codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().get(index);
    }

    private void addLongestDuplicatesList(RichTextReport report) {
        DuplicationAnalysisResults duplicationAnalysisResults = codeAnalysisResults.getDuplicationAnalysisResults();
        List<DuplicationInstance> longestDuplicates = duplicationAnalysisResults.getLongestDuplicates();
        report.startSection("Longest Duplicates", "The list of " + longestDuplicates.size() + " longest duplicates.");
        int size = duplicationAnalysisResults.getAllDuplicates().size();
        report.addContentInDiv("<a href='../data/text/duplicates.txt'>See data for all <b>" + FormattingUtils.formatCount(size)
                + "</b> " + (size == 1 ? "duplicate" : "duplicates...") + "</a>", "margin-bottom: 16px");
        getDuplicatesTable(report, longestDuplicates, "longest_duplicates");
        report.endSection();
    }


    private void addMostFrequentDuplicatesList(RichTextReport report) {
        List<DuplicationInstance> duplicates = codeAnalysisResults.getDuplicationAnalysisResults().getMostFrequentDuplicates();
        if (duplicates.size() > 0) {
            report.startSection("Most Frequent Duplicates", "The list of " + duplicates.size() + " most frequently found duplicates.");
            getDuplicatesTable(report, duplicates, "most_frequent_duplicates");
            report.endSection();
        }
    }
}
