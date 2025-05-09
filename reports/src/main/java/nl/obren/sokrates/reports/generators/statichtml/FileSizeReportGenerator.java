/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.CorrelationDiagramGenerator;
import nl.obren.sokrates.reports.utils.FilesReportUtils;
import nl.obren.sokrates.reports.utils.PieChartUtils;
import nl.obren.sokrates.reports.utils.RiskDistributionStatsReportUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSizeReportGenerator {
    private final Thresholds fileSizeThresholds;
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels;

    public FileSizeReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
        fileSizeThresholds = codeAnalysisResults.getCodeConfiguration().getAnalysis().getFileSizeThresholds();
        this.labels = fileSizeThresholds.getLabels();
    }

    public void addFileSizeToReport(RichTextReport report) {
        report.addParagraph("The distribution of size of files (measured in lines of code).", "margin-top: 12px; color: grey");
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("File size measurements show the distribution of size of files.");
        report.addListItem("Files are classified in four categories based on their size (lines of code): " +
                fileSizeThresholds.getNegligibleRiskLabel() + " (very small files), " +
                fileSizeThresholds.getLowRiskLabel() + " (small files), " +
                fileSizeThresholds.getMediumRiskLabel() + " (medium size files), " +
                fileSizeThresholds.getHighRiskLabel() + " (long files), " +
                fileSizeThresholds.getVeryHighRiskLabel() + "(very long files).");
        report.addListItem("It is a good practice to keep files small. Long files may become \"bloaters\", code that have increased to such gargantuan proportions that they are hard to work with.");
        report.endUnorderedList();
        report.endUnorderedList();

        report.startShowMoreBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("To learn more about bloaters and how to deal with long code structures, Sokrates recommends the following resources:");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/refactoring/smells/bloaters'>Refactoring bloaters</a>, sourcemaking.com");
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/antipatterns/the-blob'>The Blob Software Development Anti-Pattern</a>, sourcemaking.com");

        report.endUnorderedList();
        report.endUnorderedList();
        report.endShowMoreBlock();
        report.endSection();

        ProcessingStopwatch.start("reporting/file size/overall");
        addGraphOverall(report, codeAnalysisResults.getFilesAnalysisResults().getOverallFileSizeDistribution());
        ProcessingStopwatch.end("reporting/file size/overall");
        ProcessingStopwatch.start("reporting/file size/per extension");
        addGraphPerExtension(report, codeAnalysisResults.getFilesAnalysisResults().getFileSizeDistributionPerExtension());
        ProcessingStopwatch.end("reporting/file size/per extension");
        ProcessingStopwatch.start("reporting/file size/per logical component");
        addGraphsPerLogicalComponents(report, codeAnalysisResults.getFilesAnalysisResults().getFileSizeDistributionPerLogicalDecomposition());
        ProcessingStopwatch.end("reporting/file size/per logical component");

        ProcessingStopwatch.start("reporting/file size/longest files");
        addLongestFilesList(report);
        ProcessingStopwatch.end("reporting/file size/longest files");
        ProcessingStopwatch.start("reporting/file size/files with most units");
        addFilesWithMostUnitsList(report);
        ProcessingStopwatch.end("reporting/file size/files with most units");
        ProcessingStopwatch.start("reporting/file size/files with most long lines");
        addFilesWithMostLongLines(report);
        ProcessingStopwatch.end("reporting/file size/files with most long lines");

        if (!codeAnalysisResults.getCodeConfiguration().getAnalysis().isSkipCorrelations() && codeAnalysisResults.getContributorsAnalysisResults().getContributors().size() > 0) {
            report.startSection("Correlations", "");
            CorrelationDiagramGenerator<FileModificationHistory> correlationDiagramGenerator = new CorrelationDiagramGenerator<>(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory(Integer.MAX_VALUE));

            ProcessingStopwatch.start("reporting/file size/correlations");
            correlationDiagramGenerator.addCorrelations("File Size vs. Commits (all time)", "commits (all time)", "lines of code",
                    p -> p.getCommits().size(),
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode;
                    },
                    p -> p.getPath());

            correlationDiagramGenerator.addCorrelations("File Size vs. Contributors (all time)", "contributors (all time)", "lines of code",
                    p -> p.countContributors(),
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode;
                    },
                    p -> p.getPath());


            report.addHorizontalLine();

            correlationDiagramGenerator.addCorrelations("File Size vs. Commits (30 days)", "commits (30d)", "lines of code",
                    p -> p.getCommits().stream().filter(c -> DateUtils.isDateWithinRange(c.getDate(), 30)).count(),
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode;
                    },
                    p -> p.getPath());

            correlationDiagramGenerator.addCorrelations("File Size vs. Contributors (30 days)", "contributors (30d)", "lines of code",
                    p -> countContributors(p, 30),
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode;
                    },
                    p -> p.getPath());

            report.addHorizontalLine();

            correlationDiagramGenerator.addCorrelations("File Size vs. Commits (90 days)", "commits (90d)", "lines of code",
                    p -> p.getCommits().stream().filter(c -> DateUtils.isDateWithinRange(c.getDate(), 90)).count(),
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode;
                    },
                    p -> p.getPath());


            correlationDiagramGenerator.addCorrelations("File Size vs. Contributors (90 days)", "contributors (90d)", "lines of code",
                    p -> countContributors(p, 90),
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode;
                    },
                    p -> p.getPath());


            ProcessingStopwatch.end("reporting/file size/correlations");
            report.endSection();
        }
    }

    private long countContributors(FileModificationHistory p, int rangeInDays) {
        Stream<CommitInfo> commitInfoStream = p.getCommits().stream().filter(c -> DateUtils.isDateWithinRange(c.getDate(), rangeInDays));
        Set<String> contributorIds = new HashSet<>();
        commitInfoStream.forEach(commit -> contributorIds.add(commit.getEmail()));
        return contributorIds.size();
    }

    private void addGraphOverall(RichTextReport report, SourceFileSizeDistribution distribution) {
        report.startSection("File Size Overall", "");
        report.startUnorderedList();
        report.addListItem("There are "
                + "<a href='../data/text/mainFiles.txt' target='_blank'>"
                + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + " files</a> with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " very long files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + " long files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " medium size files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of codeclsfd_ftr_w_mp_ins)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " small files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " very small files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionChart(distribution, labels));
        report.addLineBreak();
        report.addLineBreak();
        report.addHtmlContent("explore: ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles_main_loc_coloring.html'>grouped by folders</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles_main_loc_coloring_categories.html'>grouped by size</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_sunburst_main.html'>sunburst</a> | ");
        report.addHtmlContent("<a target='_blank' href='visuals/files_3d.html'>3D view</a>");
        report.endSection();
    }

    private void addGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileSizeDistribution) {
        report.startSection("File Size per Extension", "");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(sourceFileSizeDistribution, labels));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report, List<FileDistributionPerLogicalDecomposition> fileDistributionPerLogicalDecompositions) {
        report.startSection("File Size per Logical Decomposition", "");
        fileDistributionPerLogicalDecompositions.forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName() + "", "");
            report.startScrollingDiv();
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getFileSizeDistributionPerComponent(), labels));
            report.endDiv();
            report.endSection();
        });
        report.endSection();
    }

    private void addLongestFilesList(RichTextReport report) {
        List<SourceFile> longestFiles = codeAnalysisResults.getFilesAnalysisResults().getLongestFiles();
        report.startSection("Longest Files (Top " + longestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(longestFiles, cacheSourceFiles, false, false).toString());
        report.endSection();
    }

    private void addFilesWithMostUnitsList(RichTextReport report) {
        List<SourceFile> filesList = codeAnalysisResults.getFilesAnalysisResults().getFilesWithMostUnits();
        filesList.sort((o1, o2) -> o2.getUnitsCount() - o1.getUnitsCount());
        filesList = filesList.subList(0, Math.min(getMaxTopListSize(), filesList.size()));
        report.startSection("Files With Most Units (Top " + filesList.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(filesList, cacheSourceFiles, false, false).toString());
        report.endSection();
    }

    private int getMaxTopListSize() {
        return codeAnalysisResults.getCodeConfiguration().getAnalysis().getMaxTopListSize();
    }

    private void addFilesWithMostLongLines(RichTextReport report) {
        int threshold = 120;
        List<SourceFile> filesList = codeAnalysisResults.getFilesAnalysisResults().getAllFiles()
                .stream().filter(sourceFile -> sourceFile.getLongLinesCount(threshold) > 0).collect(Collectors.toList());
        int count = filesList.size();
        int sum = filesList.stream().map(s -> (int) s.getLongLinesCount(120)).collect(Collectors.summingInt(Integer::intValue));
        filesList.sort((o1, o2) -> (int) (o2.getLongLinesCount(threshold) - o1.getLongLinesCount(threshold)));
        filesList = filesList.subList(0, Math.min(getMaxTopListSize(), filesList.size()));
        report.startSection("Files With Long Lines (Top " + filesList.size() + ")", "");
        report.addParagraph("There " + (count == 1 ? "is only one file" : "are <b>" + count + "</b> files") + " with lines longer than 120 characters. In total, there " + (sum == 1 ? "is only one long line" : "are <b>" + sum + "</b> long lines") + ".");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(filesList, cacheSourceFiles, false, true).toString());
        report.endSection();
    }
}
