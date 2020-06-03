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
import nl.obren.sokrates.sourcecode.analysis.results.FileAgeDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;

import java.util.Arrays;
import java.util.List;

public class FileAgeReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("181+", "91-180", "31-90", "1-30");

    public FileAgeReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileAgeToReport(RichTextReport report) {
        report.startSection("Intro", "");

        report.startUnorderedList();
        report.addListItem("File age measurements show the distribution of age of files.");
        report.addListItem("Files are classified in four categories based on their age (in days): " +
                "1-30 (fresh files), 31-90 (recent files), 91-180 (old files), 181+ (very old files).");
        report.endUnorderedList();
        report.endSection();

        addGraphOverall(report, codeAnalysisResults.getFilesAgeAnalysisResults().getOverallFileAgeDistribution());
        addGraphPerExtension(report, codeAnalysisResults.getFilesAgeAnalysisResults().getFileAgeDistributionPerExtension());
        addGraphsPerLogicalComponents(report, codeAnalysisResults.getFilesAgeAnalysisResults().getFileAgeDistributionPerLogicalDecomposition());

        addOldestFilesList(report);
        addYoungestFilesList(report);
    }

    private void addGraphOverall(RichTextReport report, SourceFileAgeDistribution distribution) {
        report.startSection("File Age Overall", "");
        report.startUnorderedList();
        report.addListItem("There are " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + " files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code in files" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " very old files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + " old files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " recent files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " fresh files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, labels));
        report.endSection();
    }

    private void addGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileAgeDistribution) {
        report.startSection("File Age per Extension", "");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(sourceFileAgeDistribution, labels));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report, List<FileAgeDistributionPerLogicalDecomposition> fileDistributionPerLogicalDecompositions) {
        report.startSection("File Age per Logical Decomposition", "");
        fileDistributionPerLogicalDecompositions.forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName() + "", "");
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getFileAgeDistributionPerComponent(), labels));
            report.endSection();
        });
        report.endSection();
    }

    private void addOldestFilesList(RichTextReport report) {
        List<SourceFile> longestFiles = codeAnalysisResults.getFilesAgeAnalysisResults().getOldestFiles();
        report.startSection("Oldest Files (Top " + longestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(longestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addYoungestFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesAgeAnalysisResults().getYoungestFiles();
        report.startSection("Youngest Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }
}
