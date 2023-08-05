/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.ReportConstants;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.ContributorPerExtensionStats;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommitsReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private File reportsFolder;
    private RichTextReport report;
    private Map<String, List<Pair<String, ContributorPerExtensionStats>>> emailStatsMap = new HashMap<>();

    public CommitsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
        codeAnalysisResults.getContributorsAnalysisResults().getCommitsPerExtensions().forEach(commitsPerExtension -> {
            commitsPerExtension.getContributorPerExtensionStats().forEach(contributorPerExtensionStats -> {
                String email = contributorPerExtensionStats.getContributor();
                if (!emailStatsMap.containsKey(email)) {
                    emailStatsMap.put(email, new ArrayList<>());
                }
                emailStatsMap.get(email).add(Pair.of(commitsPerExtension.getExtension(), contributorPerExtensionStats));
            });
        });
    }

    public void addContributorsAnalysisToReport(File reportsFolder, RichTextReport report) {
        this.reportsFolder = reportsFolder;
        this.report = report;

        int index[] = {1};
        codeAnalysisResults.getLogicalDecompositionsAnalysisResults().forEach(compResult -> {
            new RacingComponentsBarChartsExporter(codeAnalysisResults, compResult.getKey(), index[0]).export(reportsFolder);
            index[0] += 1;
        });

        report.addParagraph("An overview of commit trends.", "margin-top: 12px; color: grey");

        report.startTabGroup();
        report.addTab("visuals", "Overview", true);
        report.addTab("per_language", "Trend Per Language", false);
        report.addTab("per_component", "Trend Per Component", false);
        report.addTab("data", "Data", false);
        report.endTabGroup();

        ContributorsAnalysisResults analysis = codeAnalysisResults.getContributorsAnalysisResults();
        addVisualsSection(report, analysis);

        addPerLanguageTabContent(report);
        addPerComponentTabContent(report);

        addDataSection(report);
    }

    private void addVisualsSection(RichTextReport report, ContributorsAnalysisResults analysis) {
        report.startTabContentSection("visuals", true);
        report.addLineBreak();

        addZoomableCircleLinks(report);

        report.addLevel2Header("Per Year", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        ContributorsReportUtils.addContributorsPerTimeSlot(report, analysis.getContributorsPerYear(), 20, true, 4);
        report.addLevel2Header("Per Month", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        List<ContributionTimeSlot> contributorsPerMonth = getContributionMonths(analysis, 25);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, contributorsPerMonth, 24, true, 2);
        report.addLevel2Header("Per Week", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        int pastWeeks = 104;
        List<ContributionTimeSlot> contributorsPerWeek = getContributionWeeks(analysis, pastWeeks);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, contributorsPerWeek, pastWeeks, true, 1);
        report.addLevel2Header("Per Day", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        int pastDays = 365;
        List<ContributionTimeSlot> contributorsPerDay = getContributionDays(analysis, pastDays);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, contributorsPerDay, pastDays, true, 1);
        report.endTabContentSection();
    }

    private void addZoomableCircleLinks(RichTextReport report) {
        report.startDiv("");
        report.addHtmlContent("Zoomable circles (commit counts per file): ");
        report.addNewTabLink("30 days", "visuals/zoomable_circles_commits_30_main.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("90 days", "visuals/zoomable_circles_commits_90_main.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("6 months", "visuals/zoomable_circles_commits_180_main.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("past year", "visuals/zoomable_circles_commits_365_main.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("all time", "visuals/zoomable_circles_commits_main.html");
        report.addContentInDiv("Files with only one commit are shown as grey.", "color: grey; font-size: 80%");
        report.endDiv();
    }

    private void addDataSection(RichTextReport report) {
        report.startTabContentSection("data", false);
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("Contributors' details...", "../data/text/contributors.txt");
        report.endListItem();
        report.endUnorderedList();
        report.endTabContentSection();
    }

    public void addPerLanguageTabContent(RichTextReport report) {
        report.startTabContentSection("per_language", false);
        report.startTable();
        report.startTableRow();
        report.addTableCell("Commits", "border: none");
        report.startTableCell("border: none");
        List<HistoryPerExtension> historyPerExtensionPerYear = codeAnalysisResults.getFilesHistoryAnalysisResults().getHistoryPerExtensionPerYear();
        List<String> extensions = codeAnalysisResults.getMainAspectAnalysisResults().getExtensions();
        HistoryPerLanguageGenerator.getInstanceCommits(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(report);
        report.endTableCell();
        report.endTableRow();
        report.startTableRow();
        report.addTableCell("&nbsp;", "border: none");
        report.addTableCell("&nbsp;", "border: none");
        report.endTableRow();
        report.startTableRow();
        report.addTableCell("Contributors", "border: none");
        report.startTableCell("border: none");
        HistoryPerLanguageGenerator.getInstanceContributors(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(report);
        report.endTableCell();
        report.endTableRow();
        report.endTable();
        report.endTabContentSection();
    }

    public void addPerComponentTabContent(RichTextReport report) {
        report.startTabContentSection("per_component", false);
        int index[] = {1};
        codeAnalysisResults.getLogicalDecompositionsAnalysisResults().forEach(compResult -> {
            String key = compResult.getKey();
            report.addLevel2Header(key);
            report.addContentInDiv(ReportConstants.ANIMATION_SVG_ICON, "display: inline-block; vertical-align: middle; margin: 4px;");
            report.addHtmlContent("animated commit history: ");
            report.addNewTabLink("all time cumulative", "visuals/racing_charts_component_commits_" + index[0] + ".html?tickDuration=600");
            report.addHtmlContent(" | ");
            report.addNewTabLink("12 months window", "visuals/racing_charts_component_commits_12_months_window_" + index[0] + ".html?tickDuration=600");
            report.startTable();
            report.startTableRow();
            report.startTableCell("border: none");
            List<HistoryPerExtension> historyPerExtensionPerYear = new ArrayList<>();
            Map<String, Map<String, Integer>> commitsPerYear = new CommitTrendsExtractors(codeAnalysisResults).getCommitsPerYear(key);
            commitsPerYear.keySet().forEach(component -> {
                Map<String, Integer> componentYears = commitsPerYear.get(component);
                componentYears.keySet().forEach(year -> {
                    int count = componentYears.get(year);
                    historyPerExtensionPerYear.add(new HistoryPerExtension(component, year, count));
                });
            });
            List<String> components = new ArrayList<>(commitsPerYear.keySet());
            HistoryPerLanguageGenerator.getInstanceCommits(historyPerExtensionPerYear, components).addHistoryPerComponent(report);
            report.endTableCell();
            report.endTableRow();
            report.endTable();
            index[0] += 1;
            report.addLineBreak();
            report.addLineBreak();
        });
        report.endTabContentSection();
    }

    private List<ContributionTimeSlot> getContributionWeeks(ContributorsAnalysisResults analysis, int pastWeeks) {
        List<ContributionTimeSlot> activeWeeks = analysis.getContributorsPerWeek();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeWeeks.forEach(week -> map.put(week.getTimeSlot(), week));

        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastWeeks(pastWeeks, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerWeek.add(contributionTimeSlot);
            } else {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerWeek;
    }

    private List<ContributionTimeSlot> getContributionMonths(ContributorsAnalysisResults analysis, int pastMonths) {
        List<ContributionTimeSlot> activeMonth = analysis.getContributorsPerMonth();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeMonth.forEach(month -> map.put(month.getTimeSlot(), month));

        List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastMonths(pastMonths, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerMonth.add(contributionTimeSlot);
            } else {
                contributorsPerMonth.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerMonth;
    }

    private List<ContributionTimeSlot> getContributionDays(ContributorsAnalysisResults analysis, int pastDays) {
        List<ContributionTimeSlot> activeDays = analysis.getContributorsPerDay();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeDays.forEach(activeDay -> map.put(activeDay.getTimeSlot(), activeDay));

        List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastDays(pastDays, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerDay.add(contributionTimeSlot);
            } else {
                contributorsPerDay.add(new ContributionTimeSlot(pastDate));
            }
        });
        return contributorsPerDay;
    }
}
