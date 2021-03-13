/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.FilesReportUtils;
import nl.obren.sokrates.reports.utils.PieChartUtils;
import nl.obren.sokrates.reports.utils.RiskDistributionStatsReportUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileSizeReportGenerator {
    public static final int LIST_LIMIT = 100;
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("1001+", "501-1000", "201-500", "101-200", "1-100");

    public FileSizeReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileSizeToReport(RichTextReport report) {
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("File size measurements show the distribution of size of files.");
        report.addListItem("Files are classified in four categories based on their size (lines of code): " +
                "1-100 (very small files), 100-200 (small files), 200-500 (medium size files), 501-1000 (long files), 1001+ (very long files).");
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

        addGraphOverall(report, codeAnalysisResults.getFilesAnalysisResults().getOveralFileSizeDistribution());
        addGraphPerExtension(report, codeAnalysisResults.getFilesAnalysisResults().getFileSizeDistributionPerExtension());
        addGraphsPerLogicalComponents(report, codeAnalysisResults.getFilesAnalysisResults().getFileSizeDistributionPerLogicalDecomposition());

        report.startSection("Alternative Visuals", "");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='visuals/files_3d.html'>3D view of all files</a>");
        report.endUnorderedList();
        report.endSection();

        addLongestFilesList(report);
        addFilesWithMostUnitsList(report);
        addFilesWithMostLongLines(report);
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
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " small files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " very small files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, labels));
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
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(longestFiles, cacheSourceFiles, false, false).toString());
        report.endSection();
    }

    private void addFilesWithMostUnitsList(RichTextReport report) {
        List<SourceFile> filesList = codeAnalysisResults.getFilesAnalysisResults().getAllFiles()
                .stream().filter(sourceFile -> sourceFile.getUnitsCount() > 0).collect(Collectors.toList());
        filesList.sort((o1, o2) -> o2.getUnitsCount() - o1.getUnitsCount());
        filesList = filesList.subList(0, Math.min(LIST_LIMIT, filesList.size()));
        report.startSection("Files With Most Units (Top " + filesList.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(filesList, cacheSourceFiles, false, false).toString());
        report.endSection();
    }

    private void addFilesWithMostLongLines(RichTextReport report) {
        int threshold = 120;
        List<SourceFile> filesList = codeAnalysisResults.getFilesAnalysisResults().getAllFiles()
                .stream().filter(sourceFile -> sourceFile.getLongLinesCount(threshold) > 0).collect(Collectors.toList());
        int count = filesList.size();
        int sum = filesList.stream().map(s -> (int) s.getLongLinesCount(120)).collect(Collectors.summingInt(Integer::intValue));
        filesList.sort((o1, o2) -> (int) (o2.getLongLinesCount(threshold) - o1.getLongLinesCount(threshold)));
        filesList = filesList.subList(0, Math.min(LIST_LIMIT, filesList.size()));
        report.startSection("Files With Long Lines (Top " + filesList.size() + ")", "");
        report.addParagraph("There " + (count == 1 ? "is only one file" : "are <b>" + count + "</b> files") + " with lines longer than 120 characters. In total, there " + (sum == 1 ? "is only one long line" : "are <b>" + sum + "</b> long lines") + ".");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(filesList, cacheSourceFiles, false, true).toString());
        report.endSection();
    }
}
