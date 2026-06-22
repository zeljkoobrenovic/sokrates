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
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommitsReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private File reportsFolder;
    private RichTextReport report;

    public CommitsReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addContributorsAnalysisToReport(File reportsFolder, RichTextReport report) {
        this.reportsFolder = reportsFolder;
        this.report = report;

        int index[] = {1};
        codeAnalysisResults.getLogicalDecompositionsAnalysisResults().forEach(compResult -> {
            new RacingComponentsBarChartsExporter(codeAnalysisResults, compResult.getKey(), index[0]).export(reportsFolder);
            index[0] += 1;
        });

        report.addParagraph("An overview of commit trends.", "margin-top: 12px; color: grey; font-size: 94%");

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

        // Scope selector (one tab per present scope, then "All" last) above the per
        // Year/Month/Week/Day diagrams. A scope tab appears only when the analysis carried that
        // scope's time slots (older analyses have none, so only "All" shows). Main is the
        // default-visible tab (first entry); "All" goes last.
        java.util.LinkedHashMap<String, Runnable> scopePanels = new java.util.LinkedHashMap<>();
        ContributorsReportUtils.SCOPE_LABELS.forEach((scope, label) -> {
            List<ContributionTimeSlot> perYear = analysis.getContributorsPerYearByScope().get(scope);
            if (perYear != null && !perYear.isEmpty()) {
                scopePanels.put(label, () -> addActivityDiagrams(report, analysis,
                        perYear,
                        analysis.getContributorsPerMonthByScope().getOrDefault(scope, new java.util.ArrayList<>()),
                        analysis.getContributorsPerWeekByScope().getOrDefault(scope, new java.util.ArrayList<>()),
                        analysis.getContributorsPerDayByScope().getOrDefault(scope, new java.util.ArrayList<>())));
            }
        });
        scopePanels.put("All", () -> addActivityDiagrams(report, analysis,
                analysis.getContributorsPerYear(), analysis.getContributorsPerMonth(),
                analysis.getContributorsPerWeek(), analysis.getContributorsPerDay()));
        ContributorsReportUtils.addScopeToggle(report, "commits_activity_scope", scopePanels);

        report.endTabContentSection();
    }

    // Renders the Per Year / Month / Week / Day activity diagrams for one scope's time-slot lists (all
    // flat, as on the Commits report). The per-repository Overview Activity tab reuses the static pieces
    // below to show Per Year inline and Per Month/Week/Day inside a details block.
    private void addActivityDiagrams(RichTextReport report, ContributorsAnalysisResults analysis,
                                     List<ContributionTimeSlot> perYear, List<ContributionTimeSlot> perMonth,
                                     List<ContributionTimeSlot> perWeek, List<ContributionTimeSlot> perDay) {
        addPerYearDiagram(report, analysis, perYear);
        addPerMonthWeekDayDiagrams(report, analysis, perMonth, perWeek, perDay);
    }

    public static void addPerYearDiagram(RichTextReport report, ContributorsAnalysisResults analysis,
                                         List<ContributionTimeSlot> perYear) {
        report.addLevel2Header("Per Year", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        ContributorsReportUtils.addContributorsPerTimeSlot(report, perYear, 20, true, true, 4, false);
    }

    public static void addPerMonthWeekDayDiagrams(RichTextReport report, ContributorsAnalysisResults analysis,
                                                  List<ContributionTimeSlot> perMonth, List<ContributionTimeSlot> perWeek,
                                                  List<ContributionTimeSlot> perDay) {
        report.addLevel2Header("Per Month", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        ContributorsReportUtils.addContributorsPerTimeSlot(report, getContributionMonths(analysis, perMonth, 60), 60, true, true, 2, false);
        report.addLevel2Header("Per Week", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        int pastWeeks = 104;
        ContributorsReportUtils.addContributorsPerTimeSlot(report, getContributionWeeks(analysis, perWeek, pastWeeks), pastWeeks, true, true, 1, false);
        report.addLevel2Header("Per Day", "margin-bottom: 0;");
        report.addParagraph("Latest commit date: " + analysis.getLatestCommitDate(), "color: grey; font-size: 80%; margin-top: 0;");
        int pastDays = 365;
        ContributorsReportUtils.addContributorsPerTimeSlot(report, getContributionDays(analysis, perDay, pastDays), pastDays, true, true, 1, false);
    }

    private void addZoomableCircleLinks(RichTextReport report) {
        report.startDiv("");
        report.addHtmlContent("Zoomable circles (commit counts per file): ");
        report.addNewTabLink("30 days", "visuals/zoomable_circles.html#commits_30_main");
        report.addHtmlContent(" | ");
        report.addNewTabLink("90 days", "visuals/zoomable_circles.html#commits_90_main");
        report.addHtmlContent(" | ");
        report.addNewTabLink("6 months", "visuals/zoomable_circles.html#commits_180_main");
        report.addHtmlContent(" | ");
        report.addNewTabLink("past year", "visuals/zoomable_circles.html#commits_365_main");
//        report.addHtmlContent(" | ");
//        report.addNewTabLink("all time", "visuals/zoomable_circles.html#commits_main");
        report.addContentInDiv("Files with only one commit are shown as grey.", "color: grey; font-size: 80%");
        report.endDiv();
    }

    private void addDataSection(RichTextReport report) {
        report.startTabContentSection("data", false);
        report.startUnorderedList();
        report.startListItem();
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/contributors.txt')\">" + "Contributors' details..." + "</a>");
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
        return getContributionWeeks(analysis, analysis.getContributorsPerWeek(), pastWeeks);
    }

    private static List<ContributionTimeSlot> getContributionWeeks(ContributorsAnalysisResults analysis, List<ContributionTimeSlot> activeWeeks, int pastWeeks) {
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeWeeks.forEach(week -> map.put(week.getTimeSlot(), week));

        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastWeeks(pastWeeks, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerWeek.add(contributionTimeSlot);
            } else {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate, Thresholds.defaultCommitFilesCountThresholds()));
            }
        });
        return contributorsPerWeek;
    }

    private List<ContributionTimeSlot> getContributionMonths(ContributorsAnalysisResults analysis, int pastMonths) {
        return getContributionMonths(analysis, analysis.getContributorsPerMonth(), pastMonths);
    }

    private static List<ContributionTimeSlot> getContributionMonths(ContributorsAnalysisResults analysis, List<ContributionTimeSlot> activeMonth, int pastMonths) {
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeMonth.forEach(month -> map.put(month.getTimeSlot(), month));

        List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastMonths(pastMonths, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerMonth.add(contributionTimeSlot);
            } else {
                contributorsPerMonth.add(new ContributionTimeSlot(pastDate, Thresholds.defaultCommitFilesCountThresholds()));
            }
        });
        return contributorsPerMonth;
    }

    private List<ContributionTimeSlot> getContributionDays(ContributorsAnalysisResults analysis, int pastDays) {
        return getContributionDays(analysis, analysis.getContributorsPerDay(), pastDays);
    }

    private static List<ContributionTimeSlot> getContributionDays(ContributorsAnalysisResults analysis, List<ContributionTimeSlot> activeDays, int pastDays) {
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        activeDays.forEach(activeDay -> map.put(activeDay.getTimeSlot(), activeDay));

        List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>();
        List<String> pastDates = DateUtils.getPastDays(pastDays, analysis.getLatestCommitDate());
        pastDates.forEach(pastDate -> {
            ContributionTimeSlot contributionTimeSlot = map.get(pastDate);
            if (contributionTimeSlot != null) {
                contributorsPerDay.add(contributionTimeSlot);
            } else {
                contributorsPerDay.add(new ContributionTimeSlot(pastDate, Thresholds.defaultCommitFilesCountThresholds()));
            }
        });
        return contributorsPerDay;
    }
}
