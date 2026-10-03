package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.reports.generators.statichtml.CommitsReportGenerator;
import nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import java.io.File;
import java.util.*;
import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

/**
 * The Activity tab of the repository report index: the summary activity table with its per-window totals and the
 * per-year commit charts per scope, with the month/week/day details. Moved out of {@link ReportFileExporter}.
 */
class ReportActivityTab {
    private ReportActivityTab() {
    }

    static void addActivityTab(RichTextReport indexReport, CodeAnalysisResults analysisResults) {
        ContributorsAnalysisResults contributorsAnalysisResults = analysisResults.getContributorsAnalysisResults();
        indexReport.startTabContentSection("commits", false);

        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            indexReport.startDiv("margin: 32px; font-size: 110%");
            indexReport.addLevel2Header("Overall Activity Per Year", "");

            indexReport.addParagraph("Latest commit date: " + contributorsAnalysisResults.getLatestCommitDate() + "",
                    "color: grey; font-size: 80%; margin-bottom: 2px;");
            indexReport.addParagraph("Reference analysis date: " + DateUtils.getAnalysisDate() + "",
                    "color: grey; font-size: 80%;");
            indexReport.startDiv("font-size: 80%; margin-bottom: 14px");
            indexReport.addHtmlContent("More details: ");
            indexReport.addNewTabLink("Commits Report", "Commits.html");
            indexReport.addHtmlContent("&nbsp;|&nbsp;");
            indexReport.addNewTabLink("Contributors Report", "Contributors.html");
            indexReport.endDiv();

            int commitsCount30Days = contributorsAnalysisResults.getCommitsCount30Days();

            indexReport.startTable();
            indexReport.startTableRow();

            indexReport.startTableCell("border: none");
            // Scope selector (one tab per present scope, then "All" last) above the per-year activity
            // graph. Scope tabs appear only when the analysis carried that scope's time slots (older
            // analyses have none). Main is the default-visible tab (first entry); "All" goes last.
            boolean fade = commitsCount30Days == 0;
            java.util.LinkedHashMap<String, Runnable> scopePanels = new java.util.LinkedHashMap<>();
            ContributorsReportUtils.SCOPE_LABELS.forEach((scope, label) -> {
                List<ContributionTimeSlot> perYear = contributorsAnalysisResults.getContributorsPerYearByScope().get(scope);
                if (perYear != null && !perYear.isEmpty()) {
                    // Like the Overview tab but with the wider window set (30 days … all time), every window
                    // as a leading total column, all in the icon tooltips, and each metric icon linking to its
                    // detailed report.
                    ContributorsReportUtils.ActivitySummary summary = ContributorsReportUtils.buildActivitySummary(contributorsAnalysisResults, scope,
                            ContributorsReportUtils.ACTIVITY_WINDOW_DAYS, ContributorsReportUtils.ACTIVITY_WINDOW_DAYS.length);
                    scopePanels.put(label, () -> {
                        ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, perYear, 20, true, true, 8, fade, summary);
                        addPerMonthWeekDayDetails(indexReport, contributorsAnalysisResults,
                                contributorsAnalysisResults.getContributorsPerMonthByScope().getOrDefault(scope, new java.util.ArrayList<>()),
                                contributorsAnalysisResults.getContributorsPerWeekByScope().getOrDefault(scope, new java.util.ArrayList<>()),
                                contributorsAnalysisResults.getContributorsPerDayByScope().getOrDefault(scope, new java.util.ArrayList<>()));
                    });
                }
            });
            ContributorsReportUtils.ActivitySummary allSummary = ContributorsReportUtils.buildActivitySummary(contributorsAnalysisResults, null,
                    ContributorsReportUtils.ACTIVITY_WINDOW_DAYS, ContributorsReportUtils.ACTIVITY_WINDOW_DAYS.length);
            scopePanels.put("All", () -> {
                ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, contributorsAnalysisResults.getContributorsPerYear(), 20, true, true, 8, fade, allSummary);
                addPerMonthWeekDayDetails(indexReport, contributorsAnalysisResults,
                        contributorsAnalysisResults.getContributorsPerMonth(),
                        contributorsAnalysisResults.getContributorsPerWeek(),
                        contributorsAnalysisResults.getContributorsPerDay());
            });
            ContributorsReportUtils.addScopeToggle(indexReport, "overview_activity_scope", scopePanels);
            indexReport.endTableCell();
            indexReport.endTableRow();
            indexReport.endTable();

            indexReport.startDiv("font-size: 110%");
            indexReport.addLineBreak();
            indexReport.addLevel3Header("Activity Per File Extension");

            indexReport.startTable();

            indexReport.startTableRow();
            indexReport.addTableCell(ReportFileExporter.getIconSvg("commits") + "<div style='font-size: 80%'>commits</div>", "border: none; text-align: center");
            indexReport.startTableCell("border: none");
            List<HistoryPerExtension> historyPerExtensionPerYear = analysisResults.getFilesHistoryAnalysisResults().getHistoryPerExtensionPerYear();
            List<String> extensions = analysisResults.getMainAspectAnalysisResults().getExtensions();
            HistoryPerLanguageGenerator.getInstanceCommits(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(indexReport);
            indexReport.endTableCell();
            indexReport.endTableRow();

            indexReport.startTableRow();
            indexReport.addTableCell("&nbsp;", "border: none");
            indexReport.addTableCell("&nbsp;", "border: none");
            indexReport.endTableRow();

            indexReport.startTableRow();
            indexReport.addTableCell(ReportFileExporter.getIconSvg("contributors") + "<div style='font-size: 80%'>contributors</div>", "border: none; text-align: center");
            indexReport.startTableCell("border: none");
            HistoryPerLanguageGenerator.getInstanceContributors(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(indexReport);
            indexReport.endTableCell();
            indexReport.endTableRow();

            indexReport.endTable();

            indexReport.endTabContentSection();
            indexReport.endDiv();
            indexReport.endDiv();
        } else {
            indexReport.addParagraph("No commit history found.", "color: grey; margin-left: 10px; margin: 15px");
        }
        indexReport.endTabContentSection();
    }

    static void addSummaryActivityTable(CodeAnalysisResults analysisResults, RichTextReport indexReport) {
        ContributorsAnalysisResults contributorsAnalysisResults = analysisResults.getContributorsAnalysisResults();
        List<ContributionTimeSlot> contributorsPerYear = contributorsAnalysisResults.getContributorsPerYear();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        contributorsPerYear.forEach(c -> map.put(c.getTimeSlot(), c));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String year = currentYear + "";

        while (!map.containsKey(year)) {
            contributorsPerYear.add(new ContributionTimeSlot(year, Thresholds.defaultCommitFilesCountThresholds()));
            currentYear -= 1;
            year = currentYear + "";
        }

        boolean fade = contributorsAnalysisResults.getContributors().stream().noneMatch(c -> !c.isBot() && c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS));

        // The per-year chart shows the summary windows (30 days / 90 days / all time) in each metric icon's
        // hover tooltip, and each icon links to its detailed report (no leading summary columns). The scope
        // toggle swaps the whole panel (per-scope language icons + chart-with-summary) per scope. Build
        // chart panels (scopes present, then "All" last); each gets an ActivitySummary for its scope and
        // its own language icons (that scope's aspect extensions) rendered inside the panel.
        java.util.LinkedHashMap<String, Runnable> scopePanels = new java.util.LinkedHashMap<>();
        ContributorsReportUtils.SCOPE_LABELS.forEach((scope, label) -> {
            List<ContributionTimeSlot> perYearScope = contributorsAnalysisResults.getContributorsPerYearByScope().get(scope);
            if (perYearScope != null && !perYearScope.isEmpty()) {
                // Pad with empty trailing years so this scope's x-axis matches the all-scope graph.
                padTrailingYears(perYearScope);
                ContributorsReportUtils.ActivitySummary summary = ContributorsReportUtils.buildActivitySummary(contributorsAnalysisResults, scope);
                scopePanels.put(label, () -> {
                    ReportFileExporter.addScopeLanguageIcons(indexReport, analysisResults, scope);
                    ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, perYearScope, 20, true, true, 8, fade, summary);
                });
            }
        });
        ContributorsReportUtils.ActivitySummary allSummary = ContributorsReportUtils.buildActivitySummary(contributorsAnalysisResults, null);
        scopePanels.put("All", () -> {
            ReportFileExporter.addScopeLanguageIcons(indexReport, analysisResults, "All");
            ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, contributorsPerYear, 20, true, true, 8, fade, allSummary);
        });

        indexReport.startTable("margin-bottom: -20px; border-top: 1px dashed grey; border-bottom: 1px dashed grey; padding-top: 10px; margin-top: 10px; margin-bottom: 10px;");
        indexReport.startTableRow();
        indexReport.startTableCell("border: none");
        // Scope selector (one tab per present scope, then "All" last) above the per-year activity
        // graph. Scope tabs appear only when the analysis carried that scope's time slots (older
        // analyses have none). Main is the default-visible tab (first entry); "All" goes last.
        ContributorsReportUtils.addScopeToggle(indexReport, "summary_activity_scope", scopePanels);
        indexReport.endTableCell();
        indexReport.endTableRow();
        indexReport.endTable();
    }

    // Wraps the Per Month / Per Week / Per Day activity diagrams (for the selected scope) in a collapsed
    // details block on the Overview Activity tab, mirroring the Landscape Activity tab. The diagrams
    // themselves are the same ones the Commits report renders (shared static helper). No-op when there is
    // no month/week/day data.
    private static void addPerMonthWeekDayDetails(RichTextReport indexReport, ContributorsAnalysisResults analysis,
                                                  List<ContributionTimeSlot> perMonth, List<ContributionTimeSlot> perWeek,
                                                  List<ContributionTimeSlot> perDay) {
        if (isEmpty(perMonth) && isEmpty(perWeek) && isEmpty(perDay)) {
            return;
        }
        indexReport.startDetailsBlock("activity per month, week and day...");
        CommitsReportGenerator.addPerMonthWeekDayDiagrams(indexReport, analysis, orEmpty(perMonth), orEmpty(perWeek), orEmpty(perDay));
        indexReport.endDetailsBlock();
    }

    private static boolean isEmpty(List<ContributionTimeSlot> slots) {
        return slots == null || slots.isEmpty();
    }

    private static List<ContributionTimeSlot> orEmpty(List<ContributionTimeSlot> slots) {
        return slots != null ? slots : new java.util.ArrayList<>();
    }

    // Pads a per-year time-slot list with empty entries up to the current year (in place), matching
    // the padding addSummaryActivityTable applies to the all-scope list so both graphs share an x-axis.
    private static void padTrailingYears(List<ContributionTimeSlot> contributorsPerYear) {
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        contributorsPerYear.forEach(c -> map.put(c.getTimeSlot(), c));
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String year = currentYear + "";
        while (!map.containsKey(year)) {
            contributorsPerYear.add(new ContributionTimeSlot(year, Thresholds.defaultCommitFilesCountThresholds()));
            currentYear -= 1;
            year = currentYear + "";
        }
    }
}
