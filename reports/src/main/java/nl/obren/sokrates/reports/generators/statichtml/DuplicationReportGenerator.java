/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.DuplicationReportUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.duplication.DuplicationDependenciesHelper;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DuplicationReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private RichTextReport report;
    private int graphCounter = 1;
    private int componentDuplicatesCount = 1;
    private int filePairsCount = 1;

    public DuplicationReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void getDuplicatesTable(RichTextReport report, List<DuplicationInstance> sourceFiles, String fragmentType) {
        report.startDiv("width: 100%; overflow-x: auto");
        report.addHtmlContent("<table style='width: 80%'>\n");
        report.addHtmlContent("<th>Size</th><th>#</th><th>Folders</th><th>Files</th><th>Lines</th><th>Code</th>");
        int count[] = {0};
        sourceFiles.forEach(instance -> {
            count[0]++;
            report.addHtmlContent("<tr>\n");

            String extension = instance.getDuplicatedFileBlocks().get(0).getSourceFile().getExtension();
            String url = "../src/fragments/" + fragmentType + "/" + fragmentType + "_" + count[0] + "." + extension;

            report.addHtmlContent("<td>" + instance.getBlockSize() + "</td>");
            report.addHtmlContent("<td>x&nbsp;" + instance.getDuplicatedFileBlocks().size() + "</td>");
            String folderString = formatDisplayString(instance.getFoldersDisplayString());
            folderString = StringUtils.abbreviateMiddle(folderString, "...", 50);
            report.addHtmlContent("<td>" + folderString + "</td>");
            boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
            report.addHtmlContent("<td>" + formatDisplayStringSimple(instance.getFilesDisplayString(cacheSourceFiles)) + "</td>");
            report.addHtmlContent("<td>" + formatDisplayString(instance.getLinesDisplayString()) + "</td>");
            report.addHtmlContent("<td><a target='_blank' href='" + url + "'>view</a></td>");

            report.addHtmlContent("</tr>\n");
        });
        report.addHtmlContent("</table>\n");
        report.endDiv();
    }

    private String formatDisplayStringSimple(String text) {
        return text.replace("\n", "</br>");
    }

    private String formatDisplayString(String text) {
        return text.replace(" ", "&nbsp;").replace("\n", "</br>");
    }

    public void addDuplicationToReport(RichTextReport report) {
        this.report = report;
        addIntro(report);

        if (codeAnalysisResults.getCodeConfiguration().getAnalysis().isSkipDuplication()) {
            report.addParagraph("Duplication analysis has been skipped.");
            return;
        }

        addOverallDuplicationSection(report);
        addDuplicationPerExtensionSection(report);
        addDuplicationPerLogicalDecomposition(report);
        addLongestDuplicatesList(report);
        addMostFrequentDuplicatesList(report);
    }

    private void addDuplicationPerLogicalDecomposition(RichTextReport report) {
        codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerComponent().forEach(duplicationPerComponent -> {
            LogicalDecomposition logicalDecomposition = getLogicalDecomposition(codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerComponent().indexOf(duplicationPerComponent));
            String logicalDecompositionName = logicalDecomposition.getName();
            report.startSection("Duplication per Component (" + logicalDecompositionName + ")", "");
            DuplicationReportUtils.addDuplicationPerAspect(report, duplicationPerComponent);
            report.startDiv("");
            renderDependenciesViaDuplication(report, logicalDecomposition);
            report.endDiv();
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
        report.addListItem("<b>" + FormattingUtils.getFormattedCount(duplicationAnalysisResults.getOverallDuplication().getCleanedLinesOfCode()) + "</b> cleaned lines of cleaned code (without empty lines, comments, and frequently duplicated constructs such as imports)");
        report.addListItem("<b>" + FormattingUtils.getFormattedCount(duplicationAnalysisResults.getOverallDuplication().getDuplicatedLinesOfCode()) + "</b> duplicated lines");
        report.endUnorderedList();
        report.addListItem("<a href='../data/duplicates.txt'><b>" + FormattingUtils.getFormattedCount(duplicationAnalysisResults.getAllDuplicates().size()) + " duplicates</b></a>");
        report.endUnorderedList();
        DuplicationReportUtils.addOverallDuplication(report, duplicationAnalysisResults.getOverallDuplication());
        report.endSection();
    }

    private void addIntro(RichTextReport report) {
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("For duplication, we look at places in code where there are six or more lines of code that are exactly the same.");
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

    private void renderDependenciesViaDuplication(RichTextReport report, LogicalDecomposition logicalDecomposition) {
        DuplicationDependenciesHelper duplicationDependenciesHelper = new DuplicationDependenciesHelper(logicalDecomposition.getName());
        List<ComponentDependency> allDuplicates = duplicationDependenciesHelper.extractDependencies(codeAnalysisResults.getDuplicationAnalysisResults().getAllDuplicates());
        int threshold = logicalDecomposition.getDuplicationLinkThreshold();
        List<ComponentDependency> componentDependencies = allDuplicates.stream().filter(d -> d.getCount() >= threshold).collect(Collectors.toList());

        List<DuplicationInstance> instances = duplicationDependenciesHelper.getInstances();

        if (componentDependencies.size() > 0) {
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("crimson");
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), componentDependencies);
            report.addLevel3Header("Duplication Between Components (" + threshold + "+ lines)", "margin-top: 30px");

            String graphId = "duplication_dependencies_" + graphCounter++;
            report.addGraphvizFigure(graphId, "Duplication between components", graphvizContent);

            report.addLineBreak();

            addDownloadLinks(graphId);

            report.addLineBreak();

            addMoreDetailsSection(report, componentDependencies, logicalDecomposition.getName(), instances);

            report.addLineBreak();
        }
    }

    private void addMoreDetailsSection(RichTextReport report, List<ComponentDependency> componentDependencies, String logicalDecompositionName, List<DuplicationInstance> instances) {
        Collections.sort(componentDependencies, (o1, o2) -> o2.getCount() - o1.getCount());

        report.startShowMoreBlock("Show more details on duplication between components...");
        report.startDiv("width: 100%; overflow-x: auto");
        report.startTable();
        report.addTableHeader("From Component<br/>&nbsp;--> To Component", "Duplicated<br/>Lines", "File Pairs", "Details");

        componentDependencies.forEach(componentDependency -> {
            report.startTableRow();

            report.addTableCell(componentDependency.getFromComponent() + "<br/>&nbsp&nbsp;-->&nbsp"
                    + componentDependency.getToComponent());

            report.addTableCell(componentDependency.getCount() + "", "text-align: center");

            int pairsCount = componentDependency.getPathsFrom().size();
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
        File file = new File(this.report.getReportsFolder(), "data/intercomponent_duplicated_file_pairs_" + filePairsCount++ + ".txt");

        try {
            String content = componentDependency.getPathsFrom().stream().collect(Collectors.joining("\n\n"));
            FileUtils.write(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "../data/" + file.getName();
    }

    private String saveDuplicates(ComponentDependency componentDependency, String logicalDecompositionName, List<DuplicationInstance> allInstances) {
        File file = new File(this.report.getReportsFolder(), "data/intercomponent_duplicates_" + componentDuplicatesCount++ + ".txt");
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
                stringBuilder.append(")\n");
            });
            stringBuilder.append("\n");
        });

        try {
            FileUtils.write(file, stringBuilder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "../data/" + file.getName();
    }

    private void addDownloadLinks(String graphId) {
        report.startDiv("");
        report.addHtmlContent("Download: ");
        report.addNewTabLink("SVG", "visuals/" + graphId + ".svg");
        report.addHtmlContent(" ");
        report.addNewTabLink("DOT", "visuals/" + graphId + ".dot.txt");
        report.addHtmlContent(" ");
        report.addNewTabLink("(open online Graphviz editor)", "https://www.zeljkoobrenovic.com/tools/graphviz/");
        report.endDiv();
    }


    private LogicalDecomposition getLogicalDecomposition(int index) {
        return codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().get(index);
    }

    private void addLongestDuplicatesList(RichTextReport report) {
        List<DuplicationInstance> longestDuplicates = codeAnalysisResults.getDuplicationAnalysisResults().getLongestDuplicates();
        report.startSection("Longest Duplicates", "The list of " + longestDuplicates.size() + " longest duplicates.");
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
