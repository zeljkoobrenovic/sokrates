/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import nl.obren.sokrates.sourcecode.filehistory.TemporalDependenciesHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileTemporalDependenciesReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private int graphCounter = 1;
    private File reportsFolder;

    public FileTemporalDependenciesReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addTemporalDependenciesToReport(File reportsFolder, RichTextReport report) {
        this.reportsFolder = reportsFolder;
        report.addParagraph("A temporal dependency occurs when developers change two or more files " +
                "at the same time (i.e. they are a part of the same commit).", "margin-top: 12px; color: grey");

        int maxTemporalDependenciesDepthDays = codeAnalysisResults.getCodeConfiguration().getAnalysis().getMaxTemporalDependenciesDepthDays();

        report.startTabGroup();
        report.addTab("30_days", "Past 30 Days", true);
        if (maxTemporalDependenciesDepthDays >= 90) {
            report.addTab("90_days", "Past 3 Months", false);
        }
        if (maxTemporalDependenciesDepthDays >= 180) {
            report.addTab("180_days", "Past 6 Months", false);
        }
        if (maxTemporalDependenciesDepthDays > 180) {
            report.addTab("all_time", "Past " + maxTemporalDependenciesDepthDays + " Days", false);
        }
        report.endTabGroup();

        List<FilePairChangedTogether> filePairsChangedTogether = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether();
        List<FilePairChangedTogether> filePairsChangedTogether30Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days();
        List<FilePairChangedTogether> filePairsChangedTogether90Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether90Days();
        List<FilePairChangedTogether> filePairsChangedTogether180Days = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days();

        addTab(report, "30_days", filePairsChangedTogether30Days, true);
        if (maxTemporalDependenciesDepthDays >= 90) {
            addTab(report, "90_days", filePairsChangedTogether90Days, false);
        }
        if (maxTemporalDependenciesDepthDays >= 180) {
            addTab(report, "180_days", filePairsChangedTogether180Days, false);
        }
        if (maxTemporalDependenciesDepthDays > 180) {
            addTab(report, "all_time", filePairsChangedTogether, false);
        }
    }

    private void addTab(RichTextReport report, String id, List<FilePairChangedTogether> filePairs, boolean active) {
        report.startTabContentSection(id, active);
        addFileChangedTogetherList(report, filePairs);
        addDependenciesSection(report, filePairs);
        report.endTabGroup();

    }

    private void addFileChangedTogetherList(RichTextReport report, List<FilePairChangedTogether> filePairs) {
        if (filePairs.size() == 0) {
            report.addParagraph("No file pairs changed together.");
            return;
        }
        final int maxListSize = codeAnalysisResults.getCodeConfiguration().getAnalysis().getMaxTopListSize();
        if (filePairs.size() > maxListSize) {
            filePairs = filePairs.subList(0, maxListSize);
        }
        report.addLineBreak();
        report.startSubSection("Files Most Frequently Changed Together (Top " + filePairs.size() + ")", "");
        report.addParagraph("<a href='../data/text/temporal_dependencies.txt' target='_blank'>data...</a>");
        addTable(report, filePairs);
        report.endSection();
    }

    private void addFileChangedTogetherInDifferentFoldersList(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether) {
        List<FilePairChangedTogether> filePairs = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogetherInDifferentFolders(filePairsChangedTogether);
        if (filePairs.size() > 20) {
            filePairs = filePairs.subList(0, 20);
        }
        report.startSection("Files from Different Folders Most Frequently Changed Together (Top " + filePairs.size() + ")", "");
        report.addParagraph("<a href='../data/text/temporal_dependencies_different_folders.txt' target='_blank'>data...</a>");
        addTable(report, filePairs);
        report.endSection();
    }

    private void addTable(RichTextReport report, List<FilePairChangedTogether> filePairs) {
        report.startDiv("max-height: 400px; overflow-y: auto");
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
        report.endDiv();
    }

    private void addDependenciesSection(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether) {
        report.startDiv("margin: 10px;");

        report.startSubSection("Dependencies between files in same commits", "The number on the lines shows the number of shared commits.");
        renderDependencies(report, filePairsChangedTogether);
        report.endDiv();

        report.endSection();
    }

    private void renderDependencies(RichTextReport report, List<FilePairChangedTogether> filePairsChangedTogether) {
        TemporalDependenciesHelper dependenciesHelper = new TemporalDependenciesHelper();
        List<ComponentDependency> dependencies = dependenciesHelper.extractDependencies(filePairsChangedTogether);

        if (dependencies.size() > 0) {
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("#00688b");
            graphvizDependencyRenderer.setMaxNumberOfDependencies(50);
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), dependencies);

            String graphId = "file_changed_together_dependencies_" + graphCounter++;
            report.addGraphvizFigure(graphId, "File changed together in different components", graphvizContent);

            VisualizationTools.addDownloadLinks(report, graphId);
            report.addLineBreak();
            String force3DGraphFilePath = ForceGraphExporter.export3DForceGraph(dependencies, reportsFolder, graphId);
            report.addNewTabLink("Open 3D force graph...", force3DGraphFilePath);
            report.addLineBreak();
        } else {
            report.addParagraph("No temporal cross-component dependencies found.");
        }

        ProcessingStopwatch.start("reporting/temporal dependencies/extract dependencies with commits");
        List<ComponentDependency> dependenciesWithCommits = dependenciesHelper.extractDependenciesWithCommits(filePairsChangedTogether);
        ProcessingStopwatch.end("reporting/temporal dependencies/extract dependencies with commits");
        if (dependenciesWithCommits.size() > 0) {
            String graphId = "file_changed_together_dependencies_with_commits_" + graphCounter++;
            String force3DGraphFilePath = ForceGraphExporter.export3DForceGraph(dependenciesWithCommits, reportsFolder, graphId);
            report.addNewTabLink("Open 3D force graph (with commits)...", force3DGraphFilePath);
            report.addLineBreak();
        }
    }


}
