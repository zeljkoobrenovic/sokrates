/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.*;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileAgeDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.*;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileChangeDistribution;

import java.util.*;
import java.util.stream.Collectors;

public class FileHistoryReportGenerator {
    public static final String LATEST_CHANGE_DISTRIBUTION = "Latest Change Distribution";
    public static final String FILE_AGE_DISTRIBUTION = "File Age Distribution";
    public static final String FILE_AGE_DESCRIPTION = "Days since first update";
    public static final String LATEST_CHANGE_DESCRIPTION = "Days since last update";

    private CodeAnalysisResults codeAnalysisResults;
    private List<String> ageLabels = Arrays.asList("> 1y", "6-12m", "91-180d", "31-90d", "1-30d");
    private int daysBetween;
    private int estimatedWorkingDays;

    public FileHistoryReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileHistoryToReport(RichTextReport report) {
        describe(report);

        addOverallSections(report);

        addGraphsPerExtension(report);

        addGraphsPerLogicalComponents(report);

        addOldestFilesList(report);
        addMostPreviouslyChangedFilesList(report);
        addYoungestFilesList(report);
        addMostRecentlyChangedFilesList(report);
    }

    private void addSummary(RichTextReport report) {
        FileHistoryComponentsHelper helper = new FileHistoryComponentsHelper();

        List<FileModificationHistory> history = codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory();
        List<String> uniqueDates = helper.getUniqueDates(history);

        if (uniqueDates.size() > 1) {
            String firstDateString = uniqueDates.get(0);
            String latestDateString = uniqueDates.get(uniqueDates.size() - 1);

            Date firstDate = FileHistoryUtils.getDateFromString(firstDateString);
            Date latestDate = FileHistoryUtils.getDateFromString(latestDateString);

            this.daysBetween = FileHistoryUtils.daysBetween(firstDate, latestDate);

            int weeks = daysBetween / 7;
            estimatedWorkingDays = weeks * 5;

            report.startSection("Summary", "");

            report.startUnorderedList();

            report.addListItem("Number of files: <b>" + codeAnalysisResults.getMainAspectAnalysisResults().getFilesCount() + "</b>");
            report.addListItem("Daily file updates (only one update per file and date counted): <b>" + history.size() + "</b>");
            report.addListItem("First update: <b>" + firstDateString + "</b>");
            report.addListItem("Latest update: <b>" + latestDateString + "</b>");
            report.addListItem("Days between first and latest update: <b>" + daysBetween + "</b> (" + weeks + " weeks, estimated " + estimatedWorkingDays + " working days)");
            report.addListItem("Active days (at least one file change): <b>" + uniqueDates.size() + "</b>");

            report.addListItem("Data:");
            report.startUnorderedList();
            report.addListItem("<a href='../data/text/mainFilesWithHistory.txt' target='_blank'>Organized per file</a>");
            report.endUnorderedList();
            report.endUnorderedList();
            report.endSection();
        }
    }

    private void addGraphsPerExtension(RichTextReport report) {
        FilesHistoryAnalysisResults ageAnalysisResults = codeAnalysisResults.getFilesHistoryAnalysisResults();

        String extensions = codeAnalysisResults.getCodeConfiguration().getExtensions().stream().collect(Collectors.joining(", "));
        report.startSection("File Change History per File Extension", extensions);

        addGraphPerExtension(report, ageAnalysisResults.getFirstModifiedDistributionPerExtension(),
                FILE_AGE_DISTRIBUTION + " per Extension", FILE_AGE_DESCRIPTION, Palette.getAgePalette());
        addGraphPerExtension(report, ageAnalysisResults.getLastModifiedDistributionPerExtension(),
                LATEST_CHANGE_DISTRIBUTION + " per Extension", LATEST_CHANGE_DESCRIPTION, Palette.getFreshnessPalette());

        report.endSection();
    }

    private void addOverallSections(RichTextReport report) {
        addSummary(report);

        report.startSection("File Change History Overall", "");
        addAgeGraphOverall(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution(),
                FILE_AGE_DISTRIBUTION + " Overall", FILE_AGE_DESCRIPTION, Palette.getAgePalette());
        addFreshnessGraphOverall(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution(), "" +
                LATEST_CHANGE_DISTRIBUTION + " Overall", LATEST_CHANGE_DESCRIPTION, Palette.getFreshnessPalette());

        report.endSection();
    }

    private void describe(RichTextReport report) {
        report.addParagraph("File age measurements show the distribution of file ages (days since the first commit) and the recency of file updates (days since the latest commit).\n");
    }

    private void addAgeGraphOverall(RichTextReport report, SourceFileAgeDistribution distribution, String title, String subtitle, Palette palette) {
        report.startSubSection(title, subtitle);
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
                + " files are 180 days to 1 year old (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " files are 90 to 180 days old (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " files are 30 to 90 days old (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " files are less than 30 days old (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, ageLabels, palette));
        report.endSection();
    }

    private void addFreshnessGraphOverall(RichTextReport report, SourceFileAgeDistribution distribution, String title, String subtitle, Palette palette) {
        report.startSubSection(title, subtitle);
        report.startUnorderedList();
        report.addListItem("There are " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + " files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code in files" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " files have been last changed more than 1 year ago (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + "  files have been last changed 180 days to 1 year ago (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " files have been last changed 90 to 180 days ago (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " files have been last changed 30 to 90 days ago (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " files have been last changed less than 30 days ago (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, ageLabels, palette));
        report.endSection();
    }

    private void addGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileAgeDistribution, String title, String subtitle, Palette palette) {
        report.startSubSection(title, subtitle);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(sourceFileAgeDistribution, ageLabels, palette));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report) {
        String components = codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().stream().map(c -> c.getName()).collect(Collectors.joining(", "));

        report.startSection("File Change History per Logical Decomposition", components);

        addChangesPerLogicalDecomposition(report);

        report.endSection();
    }

    private void addChangesPerLogicalDecomposition(RichTextReport report) {
        int logicalDecompositionCounter[] = {0};
        codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().forEach(logicalDecomposition -> {
            logicalDecompositionCounter[0]++;

            String name = logicalDecomposition.getName();
            codeAnalysisResults.getFilesHistoryAnalysisResults().getFirstModifiedDistributionPerLogicalDecomposition().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name)).forEach(distribution -> {
                addFirstModifiedDetailsForLogicalDecomposition(report, name, distribution.getDistributionPerComponent());
            });
            codeAnalysisResults.getFilesHistoryAnalysisResults().getLastModifiedDistributionPerLogicalDecomposition().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name)).forEach(distribution -> {
                addLastModifiedPerLogicalComponent(report, name, distribution.getDistributionPerComponent());
            });
        });
    }

    private void addFirstModifiedDetailsForLogicalDecomposition(RichTextReport report, String name, List<RiskDistributionStats> distributionsPerComponent) {
        report.startSubSection("" + name + " (" + FILE_AGE_DISTRIBUTION.toLowerCase() + ")", FILE_AGE_DESCRIPTION);
        report.startDiv("max-height: 300px; overflow-x: none; overflow-y: auto");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(distributionsPerComponent, ageLabels, Palette.getAgePalette()));
        report.endDiv();
        report.endSection();
    }

    private void addLastModifiedPerLogicalComponent(RichTextReport report, String name, List<RiskDistributionStats> distributionsPerComponent) {
        report.startSubSection("" + name + " (" + LATEST_CHANGE_DISTRIBUTION.toLowerCase() + ")", LATEST_CHANGE_DESCRIPTION);
        report.startDiv("max-height: 300px; overflow-x: none; overflow-y: auto");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(distributionsPerComponent, ageLabels, Palette.getFreshnessPalette()));
        report.endDiv();
        report.endSection();
    }

    private void addOldestFilesList(RichTextReport report) {
        List<SourceFile> longestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getOldestFiles();
        report.startSection("Oldest Files (Top " + longestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(longestFiles, cacheSourceFiles, true, false).toString());
        report.endSection();
    }

    private void addYoungestFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getYoungestFiles();
        report.startSection("Most Recently Created Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true, false).toString());
        report.endSection();
    }

    private void addMostRecentlyChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostRecentlyChangedFiles();
        report.startSection("Most Recently Changed Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true, false).toString());
        report.endSection();
    }

    private void addMostPreviouslyChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostPreviouslyChangedFiles();
        report.startSection("Files Not Recently Changed (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true, false).toString());
        report.endSection();
    }
}
