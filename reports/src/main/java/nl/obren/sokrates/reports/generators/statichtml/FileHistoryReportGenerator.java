/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
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
    public static final String THE_NUMBER_OF_FILE_CHANGES = "The Number of File Changes";
    public static final String THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION = "The number of recorded file updates";
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("> 1y", "6-12m", "91-180d", "31-90d", "1-30d");
    private List<String> labelsChange = Arrays.asList("101+", "51-100", "21-50", "6-20", "1-5 updates");
    private int graphCounter = 1;
    private int daysBetween;
    private int estimatedWorkingDays;

    public FileHistoryReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileHistoryToReport(RichTextReport report) {
        addIntro(report);

        addSummary(report);

        addOverallSections(report);

        addGraphsPerExtension(report);

        addGraphsPerLogicalComponents(report);

        addMostChangedFilesList(report);
        addOldestFilesList(report);
        addMostPreviouslyChangedFilesList(report);
        addYoungestFilesList(report);
        addMostRecentlyChangedFilesList(report);
        addFileChangedTogetherList(report);
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

            report.startSection("Basic Data", "");

            report.startUnorderedList();

            report.addListItem("Number of files: <b>" + codeAnalysisResults.getMainAspectAnalysisResults().getFilesCount() + "</b>");
            report.addListItem("Daily file updates (only one update per file and date counted): <b>" + history.size() + "</b>");
            report.addListItem("First update: <b>" + firstDateString + "</b>");
            report.addListItem("Latest update: <b>" + latestDateString + "</b>");
            report.addListItem("Days between first and latest update: <b>" + daysBetween + "</b> (" + weeks + " weeks, estimated " + estimatedWorkingDays + " working days)");
            report.addListItem("Active days (at least one file change): <b>" + uniqueDates.size() + "</b>");

            report.endUnorderedList();
            report.endSection();
        }
    }

    private void addFileChangedTogetherList(RichTextReport report) {
        List<FilePairChangedTogether> filePairs = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether();
        if (filePairs.size() > 50) {
            filePairs = filePairs.subList(0, 50);
        }
        report.startSection("Files Most Frequently Changed Together (Top " + filePairs.size() + ")", "");
        report.startTable();
        report.addTableHeader("Pairs", "# same days");
        filePairs.forEach(filePair -> {
            report.startTableRow();

            report.addTableCell(filePair.getSourceFile1().getRelativePath() + "<br/>" + filePair.getSourceFile2().getRelativePath());

            report.addTableCell("" + filePair.getDates().size());

            report.endTableRow();
        });
        report.endTable();
        report.endSection();
    }

    private void addGraphsPerExtension(RichTextReport report) {
        FilesHistoryAnalysisResults ageAnalysisResults = codeAnalysisResults.getFilesHistoryAnalysisResults();

        addChangesGraphPerExtension(report, ageAnalysisResults.getChangeDistributionPerExtension(),
                THE_NUMBER_OF_FILE_CHANGES + " per Extension", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        addGraphPerExtension(report, ageAnalysisResults.getFirstModifiedDistributionPerExtension(),
                FILE_AGE_DISTRIBUTION + " per Extension", FILE_AGE_DESCRIPTION, Palette.getAgePalette());
        addGraphPerExtension(report, ageAnalysisResults.getLastModifiedDistributionPerExtension(),
                LATEST_CHANGE_DISTRIBUTION + " per Extension", LATEST_CHANGE_DESCRIPTION, Palette.getFreshnessPalette());
    }

    private void addOverallSections(RichTextReport report) {
        addGraphOverallChange(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution(),
                THE_NUMBER_OF_FILE_CHANGES + " Overall", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        addAgeGraphOverall(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution(),
                FILE_AGE_DISTRIBUTION + " Overall", FILE_AGE_DESCRIPTION, Palette.getAgePalette());
        addFreshnessGraphOverall(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution(), "" +
                LATEST_CHANGE_DISTRIBUTION + " Overall", LATEST_CHANGE_DESCRIPTION, Palette.getFreshnessPalette());
    }

    private void addIntro(RichTextReport report) {
        report.startSection("Intro", "");

        describe(report);

        report.endSection();
    }

    private void describe(RichTextReport report) {
        report.startUnorderedList();
        report.addListItem("File history measurements show the file age (in days) and frequency of file updates.");
        report.endUnorderedList();
    }

    private void addGraphOverallChange(RichTextReport report, SourceFileChangeDistribution distribution, String title, String subtitle) {
        report.startSection(title, subtitle);
        report.startUnorderedList();
        report.addListItem("There are "
                + "<a href='../data/text/mainFilesWithHistory.txt' target='_blank'>"
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

    private void addAgeGraphOverall(RichTextReport report, SourceFileAgeDistribution distribution, String title, String subtitle, Palette palette) {
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
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, labels, palette));
        report.endSection();
    }

    private void addFreshnessGraphOverall(RichTextReport report, SourceFileAgeDistribution distribution, String title, String subtitle, Palette palette) {
        report.startSection(title, subtitle);
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

        addChangesPerLogicalDecomposition(report);

        addFirsModifiedPerLogicalDecomposition(report);

        addLastModifiedPerLogicalComponent(report);

        report.endSection();
    }

    private void addLastModifiedPerLogicalComponent(RichTextReport report) {
        codeAnalysisResults.getFilesHistoryAnalysisResults().getLastModifiedDistributionPerLogicalDecomposition().forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName()
                    + " (" + LATEST_CHANGE_DISTRIBUTION.toLowerCase() + ")", LATEST_CHANGE_DESCRIPTION);
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getDistributionPerComponent(), labels, Palette.getFreshnessPalette()));
            report.endSection();
        });
    }

    private void addFirsModifiedPerLogicalDecomposition(RichTextReport report) {
        codeAnalysisResults.getFilesHistoryAnalysisResults().getFirstModifiedDistributionPerLogicalDecomposition().forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName()
                    + " (" + FILE_AGE_DISTRIBUTION.toLowerCase() + ")", FILE_AGE_DESCRIPTION);
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getDistributionPerComponent(), labels, Palette.getAgePalette()));
            report.endSection();
        });
    }

    private LogicalDecomposition getLogicalDecompositionByName(String name) {
        for (LogicalDecomposition logicalDecomposition : this.codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions()) {
            if (logicalDecomposition.getName().equalsIgnoreCase(name)) {
                return logicalDecomposition;
            }
        }

        return null;
    }

    private void addChangesPerLogicalDecomposition(RichTextReport report) {
        codeAnalysisResults.getFilesHistoryAnalysisResults().getChangeDistributionPerLogicalDecomposition().forEach(logicalDecompositionDistribution -> {
            addLogicalDecompositionBasicData(report, logicalDecompositionDistribution.getName());
            addChangeDetailsForLogicalDecomposition(report, logicalDecompositionDistribution);
        });
    }

    private void addLogicalDecompositionBasicData(RichTextReport report, String logicalDecompositionName) {
        List<FileModificationHistory> history = codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory();
        LogicalDecomposition logicalDecomposition = getLogicalDecompositionByName(logicalDecompositionName);
        if (logicalDecomposition != null) {
            List<ComponentUpdateHistory> componentHistories = new ArrayList<>();
            logicalDecomposition.getComponents().forEach(component -> {
                ComponentUpdateHistory componentUpdateHistory = new ComponentUpdateHistory(component);
                componentUpdateHistory.addDates(history);
                componentHistories.add(componentUpdateHistory);
            });

            Collections.sort(componentHistories, (a, b) -> b.getDates().size() - a.getDates().size());
            report.startSubSection(logicalDecomposition.getName() + " (basic data)", "");

            componentHistories.forEach(componentUpdateHistory -> {
                String name = componentUpdateHistory.getComponent().getName();

                List<String> dates = componentUpdateHistory.getDates();
                int activeDays = dates.size();
                report.startDiv("margin-bottom: 12px");
                report.addHtmlContent(name + ": <b>" + activeDays + "</b> active days ("
                        + dates.get(0) + " to "
                        + dates.get(dates.size() - 1) +
                        ")<br/>");
                report.addHtmlContent(ReportUtils.getSvgBar(activeDays, estimatedWorkingDays, "skyblue"));
                report.endDiv();
            });

            report.endSection();
        }
    }

    private void addChangeDetailsForLogicalDecomposition(RichTextReport report, FileAgeDistributionPerLogicalDecomposition logicalDecomposition) {
        report.startSubSection("" + logicalDecomposition.getName()
                + " (" + THE_NUMBER_OF_FILE_CHANGES.toLowerCase() + ")", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getDistributionPerComponent(), labelsChange, Palette.getHeatPalette()));
        report.endSection();

        report.startSubSection(logicalDecomposition.getName() + " (temporal dependencies)", "");
        renderDependenciesViaDuplication(report, logicalDecomposition.getName());
        report.endSection();
    }

    private void renderDependenciesViaDuplication(RichTextReport report, String logicalDecompositionName) {
        TemporalDependenciesHelper dependenciesHelper = new TemporalDependenciesHelper(logicalDecompositionName);
        List<ComponentDependency> dependencies = dependenciesHelper.extractDependencies(codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether());
        int threshold = 20;
        List<ComponentDependency> componentDependencies = dependencies.stream().filter(d -> d.getCount() >= threshold).collect(Collectors.toList());

        if (componentDependencies.size() > 0) {
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("crimson");
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), componentDependencies);
            report.addLevel3Header("File changed together in different components (" + threshold + "+ days)", "margin-top: 30px");

            String graphId = "file_changed_together_dependencies_" + graphCounter++;
            report.addGraphvizFigure(graphId, "File changed together in different components", graphvizContent);

            report.addLineBreak();
        }
    }

    private void addOldestFilesList(RichTextReport report) {
        List<SourceFile> longestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getOldestFiles();
        report.startSection("Oldest Files (Top " + longestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(longestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addYoungestFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getYoungestFiles();
        report.startSection("Most Recently Created Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addMostRecentlyChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostRecentlyChangedFiles();
        report.startSection("Most Recently Changed Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addMostPreviouslyChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostPreviouslyChangedFiles();
        report.startSection("Files Not Recently Changed (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addMostChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostChangedFiles();
        report.startSection("Most Frequently Changed Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }
}
