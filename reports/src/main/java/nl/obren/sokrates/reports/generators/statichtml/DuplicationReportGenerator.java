/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.DuplicationReportUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.duplication.DuplicationDependenciesHelper;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DuplicationReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;

    public DuplicationReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void getDuplicatesTable(RichTextReport report, List<DuplicationInstance> sourceFiles, String fragmentType) {
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
            report.addHtmlContent("<td>" + formatDisplayString(instance.getFoldersDisplayString()) + "</td>");
            boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
            report.addHtmlContent("<td>" + formatDisplayStringSimple(instance.getFilesDisplayString(cacheSourceFiles)) + "</td>");
            report.addHtmlContent("<td>" + formatDisplayString(instance.getLinesDisplayString()) + "</td>");
            report.addHtmlContent("<td><a target='_blank' href='" + url + "'>view</a></td>");

            report.addHtmlContent("</tr>\n");
        });
        report.addHtmlContent("</table>\n");
    }

    private String formatDisplayStringSimple(String text) {
        return text.replace("\n", "</br>");
    }

    private String formatDisplayString(String text) {
        return text.replace(" ", "&nbsp;").replace("\n", "</br>");
    }

    public void addDuplicationToReport(RichTextReport report) {
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
            String logicalDecompositionName = getLogicalDecompositionName(codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerComponent().indexOf(duplicationPerComponent));
            report.startSection("Duplication per Component (" + logicalDecompositionName + ")", "");
            DuplicationReportUtils.addDuplicationPerAspect(report, duplicationPerComponent);
            report.startDiv("");
            renderDependenciesViaDuplication(report, logicalDecompositionName);
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
        report.startShowMoreBlock("", "Learn more...");
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

    private void renderDependenciesViaDuplication(RichTextReport report, String logicalDecompositionName) {
        List<ComponentDependency> componentDependencies = new DuplicationDependenciesHelper(logicalDecompositionName).extractDependencies(codeAnalysisResults.getDuplicationAnalysisResults().getAllDuplicates());
        if (componentDependencies.size() > 0) {
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("crimson");
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), componentDependencies);
            report.addLevel3Header("Duplication Between Components", "margin-top: 30px");
            report.addGraphvizFigure("Duplication between components", graphvizContent);

            report.addShowMoreBlock("",
                    "<textarea style='width:100%; height: 20em;'>"
                            + graphvizContent
                            + "</textarea>", "graphviz code...");

            report.addLineBreak();
            report.addLineBreak();

            Collections.sort(componentDependencies, (o1, o2) -> o2.getCount() - o1.getCount());

            report.startTable();
            report.addTableHeader("From Component<br/>&nbsp;--> To Component", "Duplicated<br/>Lines", "Duplication<br/>File Pairs");
            componentDependencies.forEach(componentDependency -> {
                report.startTableRow();
                report.addTableCell(
                        componentDependency.getFromComponent()
                                + "<br/>&nbsp&nbsp;-->&nbsp"
                                + componentDependency.getToComponent()
                );
                report.addTableCell(componentDependency.getCount() + "", "text-align: center");
                int pairsCount = componentDependency.getPathsFrom().size();
                String filePairsText = pairsCount + (pairsCount == 1 ? " file pair" : " file pairs");
                report.addHtmlContent("<td style='text-align: center'>");
                report.addShowMoreBlock("", "<textarea style='width:400px; height: 20em;'>"
                        + componentDependency.getPathsFrom().stream().collect(Collectors.joining("\n\n"))
                        + "</textarea>", filePairsText);
                report.addHtmlContent("</td>");
                report.endTableRow();
            });
            report.endTable();
        }
    }

    private String getLogicalDecompositionName(int index) {
        return codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().get(index).getName();
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
