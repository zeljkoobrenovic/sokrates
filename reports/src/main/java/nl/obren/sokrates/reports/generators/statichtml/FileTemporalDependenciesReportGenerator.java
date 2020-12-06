/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import nl.obren.sokrates.sourcecode.filehistory.TemporalDependenciesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileTemporalDependenciesReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private int graphCounter = 1;

    public FileTemporalDependenciesReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileHistoryToReport(RichTextReport report) {
        report.addParagraph("A temporal dependency occurs when developers change two or more files at the same time (i.e. they are a part of the same commit).");

        report.startTabGroup();
        report.addTab("all_time", "All Time", true);
        report.addTab("30_days", "Past 30 Days", false);
        report.addTab("90_days", "Past 3 Months", false);
        report.addTab("180_days", "Past 6 Months", false);
        report.endTabGroup();

        List<FilePairChangedTogether> filePairsChangedTogether = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether();
        List<FilePairChangedTogether> filePairsChangedTogether30Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days();
        List<FilePairChangedTogether> filePairsChangedTogether90Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether90Days();
        List<FilePairChangedTogether> filePairsChangedTogether180Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days();

        addTab(report, "all_time", filePairsChangedTogether, true);
        addTab(report, "30_days", filePairsChangedTogether30Days, false);
        addTab(report, "90_days", filePairsChangedTogether90Days, false);
        addTab(report, "180_days", filePairsChangedTogether180Days, false);
    }

    private void addTab(RichTextReport report, String id, List<FilePairChangedTogether> filePairs, boolean active) {
        report.startTabContentSection(id, active);
        addFileChangedTogetherList(report, filePairs);
        addGraphsPerLogicalComponents(report, filePairs);
        // addFileChangedTogetherInDifferentFoldersList(report, filePairs);
        report.endTabGroup();

    }

    private void addFileChangedTogetherList(RichTextReport report, List<FilePairChangedTogether> filePairs) {
        if (filePairs.size() == 0) {
            report.addParagraph("No file pairs changed together.");
            return;
        }
        final int maxListSize = 50;
        if (filePairs.size() > maxListSize) {
            filePairs = filePairs.subList(0, maxListSize);
        }
        report.startSubSection("Files Most Frequently Changed Together (Top " + filePairs.size() + ")", "");
        report.addParagraph("<a href='../data/text/temporal_dependencies.txt' target='_blank'>data...</a>");
        addTable(report, filePairs);
    }

    private void addFileChangedTogetherInDifferentFoldersList(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether) {
        List<FilePairChangedTogether> filePairs = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogetherInDifferentFolders(filePairsChangedTogether);
        if (filePairs.size() > 20) {
            filePairs = filePairs.subList(0, 20);
        }
        report.startSection("Files from Different Folders Most Frequently Changed Together (Top " + filePairs.size() + ")", "");
        report.addParagraph("<a href='../data/text/temporal_dependencies_different_folders.txt' target='_blank'>data...</a>");
        addTable(report, filePairs);
    }

    private void addTable(RichTextReport report, List<FilePairChangedTogether> filePairs) {
        report.startTable();
        report.addTableHeader("Pairs", "# same commits", "# commits 1", "# commits 2", "latest commit");
        filePairs.forEach(filePair -> {
            report.startTableRow();

            report.addTableCell(filePair.getSourceFile1().getRelativePath() + "<br/>" + filePair.getSourceFile2().getRelativePath());

            int commitsCount = filePair.getCommits().size();
            report.addTableCell("" + commitsCount, "text-align: center");
            int commitsCountFile1 = filePair.getCommitsCountFile1();
            report.addTableCell("" + commitsCountFile1 +
                    (commitsCountFile1 > 0 && commitsCountFile1 >= commitsCount
                            ? " (" + FormattingUtils.getFormattedPercentage(100.0 * commitsCount / commitsCountFile1) + "%)"
                            : ""));
            int commitsCountFile2 = filePair.getCommitsCountFile2();
            report.addTableCell("" + commitsCountFile2
                    + (commitsCountFile2 > 0 && commitsCountFile2 >= commitsCount
                    ? " (" + FormattingUtils.getFormattedPercentage(100.0 * commitsCount / commitsCountFile2) + "%)"
                    : ""));
            report.addTableCell("" + filePair.getLatestCommit());

            report.endTableRow();
        });
        report.endTable();
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether) {
        String components = codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().stream().map(c -> c.getName()).collect(Collectors.joining(", "));

        report.startSubSection("File Change History per Logical Decomposition", components);

        addChangesPerLogicalDecomposition(report, filePairsChangedTogether);

        report.endSection();
    }

    private void addChangesPerLogicalDecomposition(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether) {
        int[] logicalDecompositionCounter = {0};
        codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().forEach(logicalDecomposition -> {
            logicalDecompositionCounter[0]++;

            String name = logicalDecomposition.getName();
            codeAnalysisResults.getFilesHistoryAnalysisResults()
                    .getChangeDistributionPerLogicalDecomposition().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name)).forEach(distribution -> {
                addDependenciesSection(report, logicalDecomposition, filePairsChangedTogether);
            });
        });
    }

    private void addDependenciesSection(RichTextReport report, LogicalDecomposition logicalDecomposition, List<FilePairChangedTogether> filePairsChangedTogether) {
        int threshold = logicalDecomposition.getTemporalLinkThreshold();

        report.startDiv("margin: 10px;");

        report.startSubSection(logicalDecomposition.getName() + " (" + threshold + "+ commits)", "");
        addChangeDependencies(report, logicalDecomposition, filePairsChangedTogether);
        report.endDiv();

        report.endSection();
    }

    private void addChangeDependencies(RichTextReport report, LogicalDecomposition logicalDecomposition, List<FilePairChangedTogether> filePairsChangedTogether) {
        int threshold = logicalDecomposition.getTemporalLinkThreshold();
        renderDependencies(report, logicalDecomposition.getName(), threshold, filePairsChangedTogether);
    }

    private void renderDependencies(RichTextReport report, String logicalDecompositionName, int threshold, List<FilePairChangedTogether> filePairsChangedTogether) {
        TemporalDependenciesHelper dependenciesHelper = new TemporalDependenciesHelper(logicalDecompositionName);
        List<ComponentDependency> dependencies = dependenciesHelper.extractDependencies(filePairsChangedTogether);
        List<ComponentDependency> componentDependencies = dependencies.stream().filter(d -> d.getCount() >= threshold).collect(Collectors.toList());

        if (componentDependencies.size() > 0) {
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("deepskyblue4");
            graphvizDependencyRenderer.setMaxNumberOfDependencies(50);
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), componentDependencies);

            String graphId = "file_changed_together_dependencies_" + graphCounter++;
            report.addGraphvizFigure(graphId, "File changed together in different components", graphvizContent);

            report.addLineBreak();
        } else {
            report.addParagraph("No temporal cross-component dependencies found.");
        }
    }


}
