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
import nl.obren.sokrates.sourcecode.analysis.results.FilesAgeAnalysisResults;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;

import java.util.Arrays;
import java.util.List;

public class FileAgeReportGenerator {
    public static final String LATEST_CHANGE_DISTRIBUTION = "Latest Change Distribution";
    public static final String FILE_AGE_DISTRIBUTION = "File Age Distribution";
    public static final String FILE_AGE_DESCRIPTION = "Days since first update";
    public static final String LATEST_CHANGE_DESCRIPTION = "Days since last update";
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("181+", "91-180", "31-90", "1-30");

    public FileAgeReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileAgeToReport(RichTextReport report) {
        addIntro(report);

        addOverallSections(report);

        addGraphsPerExtension(report);

        addGraphsPerLogicalComponents(report);

        addMostChangedFilesList(report);
        addOldestFilesList(report);
        addMostPreviouslyChangedFilesList(report);
        addYoungestFilesList(report);
        addMostRecentlyChangedFilesList(report);
    }

    private void addGraphsPerExtension(RichTextReport report) {
        FilesAgeAnalysisResults ageAnalysisResults = codeAnalysisResults.getFilesAgeAnalysisResults();

        addGraphPerExtension(report, ageAnalysisResults.getFirstModifiedDistributionPerExtension(),
                FILE_AGE_DISTRIBUTION + " per Extension", FILE_AGE_DESCRIPTION);
        addGraphPerExtension(report, ageAnalysisResults.getLastModifiedDistributionPerExtension(),
                LATEST_CHANGE_DISTRIBUTION + " per Extension", LATEST_CHANGE_DESCRIPTION);
    }

    private void addOverallSections(RichTextReport report) {
        addGraphOverall(report, codeAnalysisResults.getFilesAgeAnalysisResults().getOverallFileFirstModifiedDistribution(),
                FILE_AGE_DISTRIBUTION + " Overall", FILE_AGE_DESCRIPTION);
        addGraphOverall(report, codeAnalysisResults.getFilesAgeAnalysisResults().getOverallFileLastModifiedDistribution(), "" +
                LATEST_CHANGE_DISTRIBUTION + " Overall", LATEST_CHANGE_DESCRIPTION);
    }

    private void addIntro(RichTextReport report) {
        report.startSection("Intro", "");

        describe(report);

        report.endSection();
    }

    private void describe(RichTextReport report) {
        report.startUnorderedList();
        report.addListItem("File age measurements show the distribution of age of files.");
        report.addListItem("Files are classified in four categories based on their age (in days): " +
                "1-30 (fresh files), 31-90 (recent files), 91-180 (old files), 181+ (very old files).");
        report.endUnorderedList();
    }

    private void addGraphOverall(RichTextReport report, SourceFileAgeDistribution distribution, String title, String subtitle) {
        report.startSection(title, subtitle);
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
        report.addHtmlContent(PieChartUtils.getAgeRiskDistributionPieChart(distribution, labels));
        report.endSection();
    }

    private void addGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileAgeDistribution, String title, String subtitle) {
        report.startSection(title, subtitle);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getAgeRiskDistributionPerKeySvgBarChart(sourceFileAgeDistribution, labels));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report) {
        report.startSection("File Change History per Logical Decomposition", "");
        codeAnalysisResults.getFilesAgeAnalysisResults().getFirstModifiedDistributionPerLogicalDecomposition().forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName()
                    + " (" + FILE_AGE_DISTRIBUTION.toLowerCase() + ")", FILE_AGE_DESCRIPTION);
            report.addHtmlContent(RiskDistributionStatsReportUtils.getAgeRiskDistributionPerKeySvgBarChart(logicalDecomposition.getFirstModifiedDistributionPerComponent(), labels));
            report.endSection();
        });

        codeAnalysisResults.getFilesAgeAnalysisResults().getLastModifiedDistributionPerLogicalDecomposition().forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName()
                    + " (" + LATEST_CHANGE_DISTRIBUTION.toLowerCase() + ")", LATEST_CHANGE_DESCRIPTION);
            report.addHtmlContent(RiskDistributionStatsReportUtils.getAgeRiskDistributionPerKeySvgBarChart(logicalDecomposition.getLastModifiedDistributionPerComponent(), labels));
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

    private void addMostRecentlyChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesAgeAnalysisResults().getMostRecentlyChangedFiles();
        report.startSection("Most Recently Changed File (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addMostPreviouslyChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesAgeAnalysisResults().getMostPreviouslyChangedFiles();
        report.startSection("Most Previously Changed File (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addMostChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesAgeAnalysisResults().getMostChangedFiles();
        report.startSection("Most Changed File (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }
}
