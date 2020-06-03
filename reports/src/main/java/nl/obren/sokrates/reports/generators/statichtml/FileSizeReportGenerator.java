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
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("1001+", "501-1000", "201-500", "1-200");

    public FileSizeReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileSizeToReport(RichTextReport report) {
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("File size measurements show the distribution of size of files.");
        report.addListItem("Files are classified in four categories based on their size (lines of code): " +
                "1-200 (small files), 200-500 (medium size files), 501-1000 (long files), 1001+ (very long files).");
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
    }

    private void addGraphOverall(RichTextReport report, SourceFileSizeDistribution distribution) {
        report.startSection("File Size Overall", "");
        report.startUnorderedList();
        report.addListItem("There are " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + " files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code in files" +
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
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getFileSizeDistributionPerComponent(), labels));
            report.endSection();
        });
        report.endSection();
    }

    private void addLongestFilesList(RichTextReport report) {
        List<SourceFile> longestFiles = codeAnalysisResults.getFilesAnalysisResults().getLongestFiles();
        report.startSection("Longest Files (Top " + longestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(longestFiles, cacheSourceFiles, false).toString());
        report.endSection();
    }

    private void addFilesWithMostUnitsList(RichTextReport report) {
        List<SourceFile> filesList = codeAnalysisResults.getFilesAnalysisResults().getAllFiles()
                .stream().filter(sourceFile -> sourceFile.getUnitsCount() > 0).collect(Collectors.toList());
        filesList.sort((o1, o2) -> o2.getUnitsCount() - o1.getUnitsCount());
        filesList = filesList.subList(0, Math.min(50, filesList.size()));
        report.startSection("Files With Most Units (Top " + filesList.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(filesList, cacheSourceFiles, false).toString());
        report.endSection();
    }
}
