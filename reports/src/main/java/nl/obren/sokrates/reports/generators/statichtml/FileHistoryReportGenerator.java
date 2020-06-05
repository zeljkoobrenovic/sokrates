/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.FilesReportUtils;
import nl.obren.sokrates.reports.utils.PieChartUtils;
import nl.obren.sokrates.reports.utils.RiskDistributionStatsReportUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAgeAnalysisResults;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileChangeDistribution;

import java.util.Arrays;
import java.util.List;

public class FileHistoryReportGenerator {
    public static final String LATEST_CHANGE_DISTRIBUTION = "Latest Change Distribution";
    public static final String FILE_AGE_DISTRIBUTION = "File Age Distribution";
    public static final String FILE_AGE_DESCRIPTION = "Days since first update";
    public static final String LATEST_CHANGE_DESCRIPTION = "Days since last update";
    public static final String THE_NUMBER_OF_FILE_CHANGES = "The Number of File Changes";
    public static final String THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION = "The number of recorded file updates";
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("> 1y", "6-12m", "91-180d", "31-90d", "1-30d");
    private List<String> labelsChange = Arrays.asList("101+", "51-100", "21-50", "6-20", "1-5 updates");

    public FileHistoryReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileHistoryToReport(RichTextReport report) {
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

        addChangesGraphPerExtension(report, ageAnalysisResults.getChangeDistributionPerExtension(),
                THE_NUMBER_OF_FILE_CHANGES + " per Extension", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        addGraphPerExtension(report, ageAnalysisResults.getFirstModifiedDistributionPerExtension(),
                FILE_AGE_DISTRIBUTION + " per Extension", FILE_AGE_DESCRIPTION, Palette.getAgePalette());
        addGraphPerExtension(report, ageAnalysisResults.getLastModifiedDistributionPerExtension(),
                LATEST_CHANGE_DISTRIBUTION + " per Extension", LATEST_CHANGE_DESCRIPTION, Palette.getFreshnessPalette());
    }

    private void addOverallSections(RichTextReport report) {
        addGraphOverallChange(report, codeAnalysisResults.getFilesAgeAnalysisResults().getOverallFileChangeDistribution(),
                THE_NUMBER_OF_FILE_CHANGES + " Overall", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        addGraphOverall(report, codeAnalysisResults.getFilesAgeAnalysisResults().getOverallFileFirstModifiedDistribution(),
                FILE_AGE_DISTRIBUTION + " Overall", FILE_AGE_DESCRIPTION, Palette.getAgePalette());
        addGraphOverall(report, codeAnalysisResults.getFilesAgeAnalysisResults().getOverallFileLastModifiedDistribution(), "" +
                LATEST_CHANGE_DISTRIBUTION + " Overall", LATEST_CHANGE_DESCRIPTION, Palette.getFreshnessPalette());
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

    private void addGraphOverallChange(RichTextReport report, SourceFileChangeDistribution distribution, String title, String subtitle) {
        report.startSection(title, subtitle);
        report.startUnorderedList();
        report.addListItem("There are "
                + "<a href='../data/mainFilesWithHistory.txt' target='_blank'>"
                + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + " files</a> with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " files changed more than 100 times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + " files 51 to 100 times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " files changed 21 to 50 times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " files changed 6 to 20 times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " files changed 5 or fewer times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, labelsChange, Palette.getHeatPalette()));
        report.endSection();
    }

    private void addGraphOverall(RichTextReport report, SourceFileAgeDistribution distribution, String title, String subtitle, Palette palette) {
        report.startSection(title, subtitle);
        report.startUnorderedList();
        report.addListItem("There are " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + " files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code in files" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " files older than 1 year (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + " 180 days to 1 year old files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " 90 to 180 days old files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " 30 to 90 days old files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " less than 30 days old files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, labels, palette));
        report.endSection();
    }

    private void addGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileAgeDistribution, String title, String subtitle, Palette palette) {
        report.startSection(title, subtitle);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(sourceFileAgeDistribution, labels, palette));
        report.endSection();
    }

    private void addChangesGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileAgeDistribution, String title, String subtitle) {
        report.startSection(title, subtitle);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(sourceFileAgeDistribution, labelsChange, Palette.getHeatPalette()));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report) {
        report.startSection("File Change History per Logical Decomposition", "");

        codeAnalysisResults.getFilesAgeAnalysisResults().getChangeDistributionPerLogicalDecomposition().forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName()
                    + " (" + THE_NUMBER_OF_FILE_CHANGES.toLowerCase() + ")", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getDistributionPerComponent(), labelsChange, Palette.getHeatPalette()));
            report.endSection();
        });

        codeAnalysisResults.getFilesAgeAnalysisResults().getFirstModifiedDistributionPerLogicalDecomposition().forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName()
                    + " (" + FILE_AGE_DISTRIBUTION.toLowerCase() + ")", FILE_AGE_DESCRIPTION);
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getDistributionPerComponent(), labels, Palette.getAgePalette()));
            report.endSection();
        });

        codeAnalysisResults.getFilesAgeAnalysisResults().getLastModifiedDistributionPerLogicalDecomposition().forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName()
                    + " (" + LATEST_CHANGE_DISTRIBUTION.toLowerCase() + ")", LATEST_CHANGE_DESCRIPTION);
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getDistributionPerComponent(), labels, Palette.getFreshnessPalette()));
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
