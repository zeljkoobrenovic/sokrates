package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.DuplicationReportUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.duplication.DuplicationDependenciesUtils;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;

import java.util.ArrayList;
import java.util.List;

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
            report.addHtmlContent("<td>" + formatDisplayStringSimple(instance.getFilesDisplayString()) + "</td>");
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
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("For duplication, we look at places in code where there are six or more lines of code that are exactly the same.");
        report.addListItem("Before duplication is calculated, the code is cleaned to remove empty lines, comments, and frequently duplicated constructs such as imports.");
        report.addListItem("You should aim at having as little as possible (<5%) of duplicated code as high-level of duplication can lead to maintenance difficulties, poor factoring, and logical contradictions.");
        report.endUnorderedList();
        report.startUnorderedList();
        report.addListItem("To learn more about duplications and techniques for eliminating duplication, Sokrates recommends the following resources:");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='https://martinfowler.com/ieeeSoftware/repetition.pdf'>Avoid Repetition</a>, MartinFlower.com (IEEE Software article)");
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/refactoring/smells/duplicate-code'>Refactoring duplicated code</a>, sourcemaking.com");
        report.addListItem("<a target='_blank' href='https://martinfowler.com/bliki/BeckDesignRules.html'>Beck Design Rules</a>, MartinFowler.com");
        report.addListItem("<a target='_blank' href='https://en.wikipedia.org/wiki/Don%27t_repeat_yourself'>DRY (Don't Repeat Yourself) Principle</a>, Wikipedia");
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/antipatterns/cut-and-paste-programming'>The Cut-and-Paste Programming Software Development Anti-Pattern</a>, sourcemaking.com");

        // Duplicated code is the bane of software development. Stamp out duplication whenever possible. You should always be on the lookout for more subtle cases of near-duplication, too. Don't Repeat Yourself!
        report.endUnorderedList();
        report.endUnorderedList();
        report.endSection();

        if (codeAnalysisResults.getCodeConfiguration().getAnalysis().isSkipDuplication()) {
            report.addParagraph("Duplication analysis has been skipped.");
            return;
        }

        report.startSection("Duplication Overall", "");
        DuplicationReportUtils.addOverallDuplication(report, codeAnalysisResults.getDuplicationAnalysisResults().getOverallDuplication());
        report.endSection();

        report.startSection("Duplication per Extension", "");
        List<DuplicationMetric> duplicationPerExtension = codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerExtension();
        if (duplicationPerExtension.size() > 0) {
            DuplicationReportUtils.addDuplicationPerAspect(report, duplicationPerExtension);
        }
        report.endSection();

        codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerComponent().forEach(duplicationPerComponent -> {
            String logicalDecompositionName = getLogicalDecompositionName(codeAnalysisResults.getDuplicationAnalysisResults().getDuplicationPerComponent().indexOf(duplicationPerComponent));
            report.startSection("Duplication per Component (" + logicalDecompositionName + ")", "");
            DuplicationReportUtils.addDuplicationPerAspect(report, duplicationPerComponent);
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("crimson");
            report.addGraphvizFigure("Duplication between components", graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(),
                    DuplicationDependenciesUtils.extractDependencies(logicalDecompositionName, codeAnalysisResults.getDuplicationAnalysisResults().getAllDuplicates())));
            report.endSection();
        });

        addLongestDuplicatesList(report);

        addMostFrequentDuplicatesList(report);
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
        report.startSection("Most Frequent Duplicates", "The list of " + duplicates.size() + " most frequently found duplicates.");
        getDuplicatesTable(report, duplicates, "most_frequent_duplicates");
        report.endSection();
    }
}
