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
    public static final String THE_NUMBER_OF_FILE_CHANGES = "File Change Frequency";
    public static final String THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION = "The number of recorded file updates";
    public static final String ANCHOR_MOST_FREQUENTLY_CHANGED_FILES = "most_frequently_changed_files";
    public static final String ANCHOR_FILES_NOT_RECENTLY_CHANGED = "Files Not Recently Changed";
    public static final String ANCHOR_MOST_RECENTLY_CHANGED_FILES = "Most Recently Changed Files";
    public static final String ANCHOR_MOST_RECENTLY_CREATED_FILES = "Most Recently Created Files";
    public static final String ANCHOR_OLDEST_FILES = "Oldest Files";
    public static final String ANCHOR_LOGICAL_DECOMPOSITIONS = "Logical Decompositions";
    public static final String ANCHOR_EXTENSIONS = "Extensions";
    public static final String ANCHOR_EXTENSIONS_1 = "Extensions 1";
    public static final String ANCHOR_EXTENSIONS_2 = "Extensions 2";
    public static final String ANCHOR_EXTENSIONS_3 = "Extensions 3";
    public static final String ANCHOR_EXTENSIONS_4 = "Extensions 4";
    public static final String ANCHOR_OVERALL = "Overall";
    public static final String ANCHOR_OVERALL_1 = "Overall 1";
    public static final String ANCHOR_OVERALL_2 = "Overall 2";
    public static final String ANCHOR_OVERALL_3 = "Overall 3";
    public static final String ANCHOR_OVERALL_4 = "Overall 4";

    private CodeAnalysisResults codeAnalysisResults;
    private List<String> ageLabels = Arrays.asList("> 1y", "6-12m", "91-180d", "31-90d", "1-30d");
    private List<String> changeFrequencyLabels = Arrays.asList("101+", "51-100", "21-50", "6-20", "1-5 updates");
    private int graphCounter = 1;
    private int daysBetween;
    private int estimatedWorkingDays;

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

            report.startSubSection("Basic Data", "");

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

    private void addFileChangedTogetherList(RichTextReport report) {
        List<FilePairChangedTogether> filePairs = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether();
        if (filePairs.size() > 50) {
            filePairs = filePairs.subList(0, 50);
        }
        report.startSection("Files Most Frequently Changed Together (Top " + filePairs.size() + ")", "");
        report.startTable();
        report.addTableHeader("Pairs", "# same commits", "# commits 1", "# commits 2");
        filePairs.forEach(filePair -> {
            report.startTableRow();

            report.addTableCell(filePair.getSourceFile1().getRelativePath() + "<br/>" + filePair.getSourceFile2().getRelativePath());

            int commitsCount = filePair.getCommits().size();
            report.addTableCell("" + commitsCount);
            int commitsCountFile1 = filePair.getCommitsCountFile1();
            report.addTableCell("" + commitsCountFile1
                    + (commitsCountFile1 > 0 && commitsCountFile1 > commitsCount  ? " (" + FormattingUtils.getFormattedPercentage(100.0 * commitsCount / commitsCountFile1) + "%)" : ""));
            int commitsCountFile2 = filePair.getCommitsCountFile2();
            report.addTableCell("" + commitsCountFile2
                    + (commitsCountFile2 > 0 && commitsCountFile2 > commitsCount ? " (" + FormattingUtils.getFormattedPercentage(100.0 * commitsCount / commitsCountFile2) + "%)" : ""));

            report.endTableRow();
        });
        report.endTable();
        report.endSection();
    }

    private void addGraphsPerExtension(RichTextReport report) {
        report.addAnchor(ANCHOR_EXTENSIONS);
        FilesHistoryAnalysisResults ageAnalysisResults = codeAnalysisResults.getFilesHistoryAnalysisResults();

        String extensions = codeAnalysisResults.getCodeConfiguration().getExtensions().stream().collect(Collectors.joining(", "));
        report.startSection("File Change History per File Extension", extensions);

        report.addAnchor(ANCHOR_EXTENSIONS_1);
        addChangesGraphPerExtension(report, ageAnalysisResults.getChangeDistributionPerExtension(),
                THE_NUMBER_OF_FILE_CHANGES + " per Extension", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        report.addAnchor(ANCHOR_EXTENSIONS_2);
        addGraphPerExtension(report, ageAnalysisResults.getFirstModifiedDistributionPerExtension(),
                FILE_AGE_DISTRIBUTION + " per Extension", FILE_AGE_DESCRIPTION, Palette.getAgePalette());
        report.addAnchor(ANCHOR_EXTENSIONS_3);
        addGraphPerExtension(report, ageAnalysisResults.getLastModifiedDistributionPerExtension(),
                LATEST_CHANGE_DISTRIBUTION + " per Extension", LATEST_CHANGE_DESCRIPTION, Palette.getFreshnessPalette());

        report.endSection();
    }

    private void addOverallSections(RichTextReport report) {
        report.addAnchor(ANCHOR_OVERALL);

        report.startSection("File Change History Overall", "");
        report.addAnchor(ANCHOR_OVERALL_1);
        addSummary(report);
        report.addAnchor(ANCHOR_OVERALL_2);
        addGraphOverallChange(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution(),
                THE_NUMBER_OF_FILE_CHANGES + " Overall", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        report.addAnchor(ANCHOR_OVERALL_3);
        addAgeGraphOverall(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution(),
                FILE_AGE_DISTRIBUTION + " Overall", FILE_AGE_DESCRIPTION, Palette.getAgePalette());
        report.addAnchor(ANCHOR_OVERALL_4);
        addFreshnessGraphOverall(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution(), "" +
                LATEST_CHANGE_DISTRIBUTION + " Overall", LATEST_CHANGE_DESCRIPTION, Palette.getFreshnessPalette());

        report.endSection();
    }

    private void addIntro(RichTextReport report) {
        describe(report);
        report.startTocSection();

        report.addContentInDiv("<a href='#" + ANCHOR_OVERALL + "'><b>File Change History Overall</b></a>", "margin-bottom: 0; margin-top: 12px");
        report.startUnorderedList("margin-top: 2px");
        report.addListItem("<a href='#" + ANCHOR_OVERALL_1 + "'>Basic Data</a>");
        report.addListItem("<a href='#" + ANCHOR_OVERALL_2 + "'>The File Change Frequency Overall</a>");
        report.addListItem("<a href='#" + ANCHOR_OVERALL_3 + "'>File Age Distribution Overall</a>");
        report.addListItem("<a href='#" + ANCHOR_OVERALL_4 + "'>Latest Change Distribution Overall</a>");
        report.endUnorderedList();
        report.addContentInDiv("<a href='#" + ANCHOR_EXTENSIONS + "'><b>File Change History per File Extension</b></a>", "margin-bottom: 0; margin-top: 12px");
        report.startUnorderedList("margin-top: 2px");
        report.addListItem("<a href='#" + ANCHOR_EXTENSIONS_1 + "'>The File Change Frequency per Extension</a>");
        report.addListItem("<a href='#" + ANCHOR_EXTENSIONS_2 + "'>File Age Distribution per Extension</a>");
        report.addListItem("<a href='#" + ANCHOR_EXTENSIONS_3 + "'>Latest Change Distribution per Extension</a>");
        report.endUnorderedList();
        report.addContentInDiv("<a href='#" + ANCHOR_LOGICAL_DECOMPOSITIONS + "'><b>File Change History per Logical Decomposition</b></a>", "margin-bottom: 0; margin-top: 12px");
        report.startUnorderedList("margin-top: 2px");
        final int[] logicalDecompositionCounter = {0};
        codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().forEach(logicalDecomposition -> {
            logicalDecompositionCounter[0]++;
            String anchorPrefix = ANCHOR_LOGICAL_DECOMPOSITIONS + "_" + logicalDecompositionCounter[0];
            report.addListItem("<a href='#" + anchorPrefix + "'>" + logicalDecomposition.getName().toUpperCase() + "</a>:");
            report.startUnorderedList("margin-top: 2px");
            report.addListItem("<a href='#" + anchorPrefix + "_basic'>Basic Data</a>");
            report.addListItem("<a href='#" + anchorPrefix + "_changes'>File Change Frequency</a>");
            report.addListItem("<a href='#" + anchorPrefix + "_change_dependencies'>File Change Dependencies</a>");
            report.addListItem("<a href='#" + anchorPrefix + "_first'>File Age Distribution</a>");
            report.addListItem("<a href='#" + anchorPrefix + "_latest'>Latest Change Distribution</a>");
            report.endUnorderedList();
        });
        report.endUnorderedList();
        report.addContentInDiv("<a href='#" + ANCHOR_MOST_FREQUENTLY_CHANGED_FILES + "'><b>Top Lists</b></a>", "margin-bottom: 0; margin-top: 12px");
        report.startUnorderedList("margin-top: 2px");
        report.addListItem("<a href='#" + ANCHOR_MOST_FREQUENTLY_CHANGED_FILES + "'>Most Frequently Changed Files</a>");
        report.addListItem("<a href='#" + ANCHOR_OLDEST_FILES + "'>Oldest Files</a>");
        report.addListItem("<a href='#" + ANCHOR_FILES_NOT_RECENTLY_CHANGED + "'>Files Not Recently Changed</a>");
        report.addListItem("<a href='#" + ANCHOR_MOST_RECENTLY_CREATED_FILES + "'>Most Recently Created Files</a>");
        report.addListItem("<a href='#" + ANCHOR_MOST_RECENTLY_CHANGED_FILES + "'>Most Recently Changed Files</a>");
        report.addListItem("<a href='#" + ANCHOR_FILES_NOT_RECENTLY_CHANGED + "'>Files Most Frequently Changed Together</a>");
        report.endUnorderedList();

        report.endSection();
    }

    private void describe(RichTextReport report) {
        report.addParagraph("File history measurements show the file age (in days) and frequency of file updates.");
    }

    private void addGraphOverallChange(RichTextReport report, SourceFileChangeDistribution distribution, String title, String subtitle) {
        report.startSubSection(title, subtitle);
        report.startUnorderedList();
        report.addListItem("There are <b>"
                + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + "</b> files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
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
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, changeFrequencyLabels, Palette.getHeatPalette()));
        report.endSection();
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

    private void addChangesGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileAgeDistribution, String title, String subtitle) {
        report.startSubSection(title, subtitle);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(sourceFileAgeDistribution, changeFrequencyLabels, Palette.getHeatPalette()));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report) {
        report.addAnchor(ANCHOR_LOGICAL_DECOMPOSITIONS);
        String components = codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().stream().map(c -> c.getName()).collect(Collectors.joining(", "));

        report.startSection("File Change History per Logical Decomposition", components);

        addChangesPerLogicalDecomposition(report);

        report.endSection();
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
        int logicalDecompositionCounter[] = {0};
        codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().forEach(logicalDecomposition -> {
            logicalDecompositionCounter[0]++;
            String anchorPrefix = ANCHOR_LOGICAL_DECOMPOSITIONS + "_" + logicalDecompositionCounter[0];

            report.addAnchor(anchorPrefix);

            String name = logicalDecomposition.getName();
            report.startSubSection(name, "");
            report.addAnchor(anchorPrefix + "_basic");
            addLogicalDecompositionBasicData(report, name);
            codeAnalysisResults.getFilesHistoryAnalysisResults().getChangeDistributionPerLogicalDecomposition().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name)).forEach(distribution -> {
                report.addAnchor(anchorPrefix + "_changes");
                addChangeDetailsForLogicalDecomposition(report, distribution);
                report.addAnchor(anchorPrefix + "_change_dependencies");
                addChangeDependencies(report, logicalDecomposition);
            });
            codeAnalysisResults.getFilesHistoryAnalysisResults().getFirstModifiedDistributionPerLogicalDecomposition().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name)).forEach(distribution -> {
                report.addAnchor(anchorPrefix + "_first");
                addFirstModifiedDetailsForLogicalDecomposition(report, name, distribution.getDistributionPerComponent());
            });
            codeAnalysisResults.getFilesHistoryAnalysisResults().getLastModifiedDistributionPerLogicalDecomposition().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name)).forEach(distribution -> {
                report.addAnchor(anchorPrefix + "_latest");
                addLastModifiedPerLogicalComponent(report, name, distribution.getDistributionPerComponent());
            });
            report.endSection();
        });
    }

    private void addChangeDependencies(RichTextReport report, LogicalDecomposition logicalDecomposition) {
        report.startSubSection(logicalDecomposition.getName() + " (temporal dependencies, # commits)", "");
        report.startShowMoreBlock("show dependencies...");
        renderDependencies(report, logicalDecomposition.getName(), logicalDecomposition.getFileChangeHistoryLinkThreshold());
        report.endShowMoreBlock();
        report.endSection();
    }

    private void addFirstModifiedDetailsForLogicalDecomposition(RichTextReport report, String name, List<RiskDistributionStats> distributionsPerComponent) {
        report.startSubSection("" + name + " (" + FILE_AGE_DISTRIBUTION.toLowerCase() + ")", FILE_AGE_DESCRIPTION);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(distributionsPerComponent, ageLabels, Palette.getAgePalette()));
        report.endSection();
    }

    private void addLastModifiedPerLogicalComponent(RichTextReport report, String name, List<RiskDistributionStats> distributionsPerComponent) {
        report.startSubSection("" + name + " (" + LATEST_CHANGE_DISTRIBUTION.toLowerCase() + ")", LATEST_CHANGE_DESCRIPTION);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(distributionsPerComponent, ageLabels, Palette.getFreshnessPalette()));
        report.endSection();
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
                if (activeDays > 0) {
                    report.startDiv("margin-bottom: 12px");
                    report.addHtmlContent(name + ": <b>" + activeDays + "</b> active days ("
                            + dates.get(0) + " to "
                            + dates.get(dates.size() - 1) +
                            ")<br/>");
                    report.addHtmlContent(ReportUtils.getSvgBar(activeDays, estimatedWorkingDays, "black"));
                    report.endDiv();
                }
            });

            report.endSection();
        }
    }

    private void addChangeDetailsForLogicalDecomposition(RichTextReport report, FileAgeDistributionPerLogicalDecomposition logicalDecomposition) {
        report.startSubSection("" + logicalDecomposition.getName()
                + " (" + THE_NUMBER_OF_FILE_CHANGES.toLowerCase() + ")", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getDistributionPerComponent(), changeFrequencyLabels, Palette.getHeatPalette()));
        report.endSection();

    }

    private void renderDependencies(RichTextReport report, String logicalDecompositionName, int threshold) {
        TemporalDependenciesHelper dependenciesHelper = new TemporalDependenciesHelper(logicalDecompositionName);
        List<ComponentDependency> dependencies = dependenciesHelper.extractDependencies(codeAnalysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether());
        List<ComponentDependency> componentDependencies = dependencies.stream().filter(d -> d.getCount() >= threshold).collect(Collectors.toList());

        if (componentDependencies.size() > 0) {
            GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
            graphvizDependencyRenderer.setDefaultNodeFillColor("deepskyblue2");
            graphvizDependencyRenderer.setType("graph");
            graphvizDependencyRenderer.setArrow("--");
            graphvizDependencyRenderer.setArrowColor("crimson");
            String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), componentDependencies);

            String graphId = "file_changed_together_dependencies_" + graphCounter++;
            report.addGraphvizFigure(graphId, "File changed together in different components", graphvizContent);

            report.addLineBreak();
        }
    }

    private void addOldestFilesList(RichTextReport report) {
        List<SourceFile> longestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getOldestFiles();
        report.addAnchor(ANCHOR_OLDEST_FILES);
        report.startSection("Oldest Files (Top " + longestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(longestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addYoungestFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getYoungestFiles();
        report.addAnchor(ANCHOR_MOST_RECENTLY_CREATED_FILES);
        report.startSection("Most Recently Created Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addMostRecentlyChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostRecentlyChangedFiles();
        report.addAnchor(ANCHOR_MOST_RECENTLY_CHANGED_FILES);
        report.startSection("Most Recently Changed Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addMostPreviouslyChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostPreviouslyChangedFiles();
        report.addAnchor(ANCHOR_FILES_NOT_RECENTLY_CHANGED);
        report.startSection("Files Not Recently Changed (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }

    private void addMostChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostChangedFiles();
        report.addAnchor(ANCHOR_MOST_FREQUENTLY_CHANGED_FILES);
        report.startSection("Most Frequently Changed Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true).toString());
        report.endSection();
    }
}
