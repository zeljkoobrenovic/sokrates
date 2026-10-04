/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.FileReadsForChanges;
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

import java.util.*;
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
        report.setDescription("The distribution of size of files (measured in lines of code).");

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
        ProcessingStopwatch.start("reporting/file size/large files that change often");
        addLargeFilesThatChangeOften(report);
        ProcessingStopwatch.end("reporting/file size/large files that change often");
        ProcessingStopwatch.start("reporting/file size/files with most units");
        addFilesWithMostUnitsList(report);
        ProcessingStopwatch.end("reporting/file size/files with most units");
        ProcessingStopwatch.start("reporting/file size/files with most long lines");
        addFilesWithMostLongLines(report);
        ProcessingStopwatch.end("reporting/file size/files with most long lines");

        if (!codeAnalysisResults.getCodeConfiguration().getAnalysis().isSkipCorrelations() && codeAnalysisResults.getContributorsAnalysisResults().getContributors().size() > 0) {
            report.startSection("Correlations", "");
            CorrelationDiagramGenerator<FileModificationHistory> correlationDiagramGenerator = new CorrelationDiagramGenerator<>(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory(Integer.MAX_VALUE));

            final Map<String,Integer> linesOfCodeMap = new HashMap<>();

            codeAnalysisResults.getFilesAnalysisResults().getAllFiles().forEach(sourceFile -> {
                linesOfCodeMap.put(sourceFile.getRelativePath(), sourceFile.getLinesOfCode());
            });

            ProcessingStopwatch.start("reporting/file size/correlations");
            correlationDiagramGenerator.addCorrelations("File Size vs. Commits (all time)", "commits (all time)", "lines of code",
                    p -> p.getCommits().size(),
                    p -> linesOfCodeMap.getOrDefault(p.getPath(), 0),
                    p -> p.getPath());

            correlationDiagramGenerator.addCorrelations("File Size vs. Contributors (all time)", "contributors (all time)", "lines of code",
                    p -> p.countContributors(),
                    p -> linesOfCodeMap.getOrDefault(p.getPath(), 0),
                    p -> p.getPath());


            report.addHorizontalLine();

            correlationDiagramGenerator.addCorrelations("File Size vs. Commits (30 days)", "commits (30d)", "lines of code",
                    p -> p.getCommits().stream().filter(c -> DateUtils.isDateWithinRange(c.getDate(), 30)).count(),
                    p -> linesOfCodeMap.getOrDefault(p.getPath(), 0),
                    p -> p.getPath());

            correlationDiagramGenerator.addCorrelations("File Size vs. Contributors (30 days)", "contributors (30d)", "lines of code",
                    p -> countContributors(p, 30),
                    p -> linesOfCodeMap.getOrDefault(p.getPath(), 0),
                    p -> p.getPath());

            report.addHorizontalLine();

            correlationDiagramGenerator.addCorrelations("File Size vs. Commits (90 days)", "commits (90d)", "lines of code",
                    p -> p.getCommits().stream().filter(c -> DateUtils.isDateWithinRange(c.getDate(), 90)).count(),
                    p -> linesOfCodeMap.getOrDefault(p.getPath(), 0),
                    p -> p.getPath());


            correlationDiagramGenerator.addCorrelations("File Size vs. Contributors (90 days)", "contributors (90d)", "lines of code",
                    p -> countContributors(p, 90),
                    p -> linesOfCodeMap.getOrDefault(p.getPath(), 0),
                    p -> p.getPath());


            ProcessingStopwatch.end("reporting/file size/correlations");
            report.endSection();
        }

        addAboutSection(report);
    }

    private void addAboutSection(RichTextReport report) {
        report.startSection("About This Analysis", "");
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

        report.startDetailsBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("To learn more about bloaters and how to deal with long code structures, Sokrates recommends the following resources:");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/refactoring/smells/bloaters'>Refactoring bloaters</a>, sourcemaking.com");
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/antipatterns/the-blob'>The Blob Software Development Anti-Pattern</a>, sourcemaking.com");

        report.endUnorderedList();
        report.endUnorderedList();
        report.endDetailsBlock();
        report.endSection();
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
                + "<a href='#' onclick=\"return downloadDataFile('text/mainFiles.txt')\" target='_blank'>"
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
                + " lines of code)");
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
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles.html#main_loc_coloring'>grouped by folders</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles.html#main_loc_coloring_categories'>grouped by size</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_sunburst.html#main'>sunburst</a> | ");
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
            report.startSubSectionText(logicalDecomposition.getName(), "");
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

    // The files read most for changes (lines × changes); omitted when switched off (analysis.fileReadsForChanges.enabled),
    // without git history or without changes in the window.
    private void addLargeFilesThatChangeOften(RichTextReport report) {
        if (!codeAnalysisResults.getCodeConfiguration().getAnalysis().getFileReadsForChanges().isEnabled()) {
            return;
        }
        FileReadsForChanges reads = FileReadsForChanges.of(codeAnalysisResults);
        List<FileReadsForChanges.ChangedFile> files = reads.topFiles();
        if (files.isEmpty()) {
            return;
        }
        report.startSection("Large Files That Change Often (Top " + files.size() + ")",
                "To change a file, an AI coding agent (or a person) has to read it: these files are read the most for the changes "
                        + reads.windowLabel() + " (lines × changes).");
        boolean linkToFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        StringBuilder table = new StringBuilder();
        // Long folder paths wrap (the shared file cell keeps them on one line), so the numeric columns stay in view.
        table.append("<style>.sk-reads-table td:first-child > div { display: flex; }"
                + " .sk-reads-table td:first-child > div > div:last-child { min-width: 0; }"
                + " .sk-reads-table td:first-child > div > div:last-child div { white-space: normal !important; word-break: break-all; }</style>\n");
        table.append("<div style='width: 100%; overflow-x: auto;'>\n");
        table.append("<table class='sk-data-table sk-reads-table' style='width: 100%'>\n");
        table.append("<tr><th>File</th><th># lines</th><th>≈ tokens<br>to read</th><th># changes<br>(" + reads.windowShortLabel() + ")</th>"
                + "<th>≈ lines edited<br>per change</th><th>lines read per<br>line changed</th></tr>\n");
        files.forEach(file -> {
            double edited = file.getEditedLinesPerChange();
            double ratio = file.getReadPerEditedLine();
            table.append("<tr>\n");
            table.append(FilesReportUtils.fileNameCell(file.getFile(), linkToFiles));
            table.append("<td style='text-align: center'>" + file.getLinesOfCode() + "</td>\n");
            table.append("<td style='text-align: center; white-space: nowrap' data-sort='" + file.getLinesOfCode() + "'>"
                    + reads.tokenRange(file.getLinesOfCode()) + "</td>\n");
            table.append("<td style='text-align: center'>" + file.getChanges() + "</td>\n");
            table.append("<td style='text-align: center' data-sort='" + edited + "'>" + (edited > 0 ? FileReadsForChanges.about(edited) : "-") + "</td>\n");
            table.append("<td style='text-align: center' data-sort='" + ratio + "'>" + (ratio > 0 ? FileReadsForChanges.about(ratio) : "-") + "</td>\n");
            table.append("</tr>\n");
        });
        table.append("</table>\n</div>\n");
        report.addHtmlContent(table.toString());
        report.addParagraph("<span style='font-size: 90%; color: var(--sk-text-muted, #666)'>Tokens are lines × "
                + reads.getTokensPerLineMin() + " to " + reads.getTokensPerLineMax()
                + " (<code>analysis.fileReadsForChanges</code>). Lines edited per change are the file's lines added and deleted divided by its commits over the whole history. "
                + "Agents read about 2,000 lines at a time: when a 17,000-line file was split into modules, the tokens an agent read for "
                + "the same change fell by 83% (<a target='_blank' href='https://martinfowler.com/articles/exploring-gen-ai/refactoring-economic-benefit.html'>"
                + "The Economic Benefit of Refactoring</a>, 2026).</span>");
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
