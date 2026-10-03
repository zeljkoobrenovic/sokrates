package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.reports.generators.statichtml.CommitsReportGenerator;
import nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.contributors.GitContributorsUtil;
import nl.obren.sokrates.sourcecode.contributors.ScopePaths;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.lang3.StringUtils;

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

    /**
     * The index's Activity tab: one scope bar on top (Main, Test, …, Unscoped, All) switching the
     * sections of that scope — Per Year (window totals and yearly bars) and Per Month as open section
     * cards, Per Week and Per Day collapsed, and Per File Extension (the history of the scope's files,
     * matched with the same ScopePaths as the time charts).
     */
    static void addActivityTab(RichTextReport indexReport, CodeAnalysisResults analysisResults) {
        ContributorsAnalysisResults contributorsAnalysisResults = analysisResults.getContributorsAnalysisResults();
        indexReport.startTabContentSection("commits", false);

        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            String latestCommit = contributorsAnalysisResults.getLatestCommitDate();
            String analysisDate = DateUtils.getAnalysisDate();
            indexReport.addParagraph("Latest commit: " + latestCommit
                            + (StringUtils.isNotBlank(analysisDate) && !analysisDate.equals(latestCommit) ? " &middot; analysis date: " + analysisDate : ""),
                    "color: var(--sk-text-muted); font-size: 13px; margin: 4px 0 0 10px;");

            boolean fade = contributorsAnalysisResults.getCommitsCount30Days() == 0;
            Map<String, Set<String>> pathsByScope = ScopePaths.byScope(analysisResults.getCodeConfiguration());
            Set<String> allScopePaths = new HashSet<>();
            if (pathsByScope != null) {
                pathsByScope.values().forEach(allScopePaths::addAll);
            }
            java.util.LinkedHashMap<String, Runnable> scopePanels = new java.util.LinkedHashMap<>();
            ContributorsReportUtils.SCOPE_LABELS.forEach((scope, label) -> {
                List<ContributionTimeSlot> perYear = contributorsAnalysisResults.getContributorsPerYearByScope().get(scope);
                if (perYear != null && !perYear.isEmpty()) {
                    // The wider window set (30 days … all time), every window as a leading total column,
                    // all in the icon tooltips, each metric icon linking to its detailed report.
                    ContributorsReportUtils.ActivitySummary summary = ContributorsReportUtils.buildActivitySummary(contributorsAnalysisResults, scope,
                            ContributorsReportUtils.ACTIVITY_WINDOW_DAYS, ContributorsReportUtils.ACTIVITY_WINDOW_DAYS.length);
                    scopePanels.put(label, () -> {
                        addTimeSections(indexReport, contributorsAnalysisResults, perYear, summary, fade,
                                contributorsAnalysisResults.getContributorsPerMonthByScope().getOrDefault(scope, new java.util.ArrayList<>()),
                                contributorsAnalysisResults.getContributorsPerWeekByScope().getOrDefault(scope, new java.util.ArrayList<>()),
                                contributorsAnalysisResults.getContributorsPerDayByScope().getOrDefault(scope, new java.util.ArrayList<>()));
                        Set<String> scopePaths = pathsByScope != null ? pathsByScope.get(scope) : null;
                        if (GitContributorsUtil.UNSCOPED.equals(scope)) {
                            addPerFileExtensionSection(indexReport, analysisResults, label, path -> !allScopePaths.contains(path), null);
                        } else if (scopePaths != null) {
                            addPerFileExtensionSection(indexReport, analysisResults, label, scopePaths::contains, scopeExtensions(analysisResults, scope));
                        }
                    });
                }
            });
            ContributorsReportUtils.ActivitySummary allSummary = ContributorsReportUtils.buildActivitySummary(contributorsAnalysisResults, null,
                    ContributorsReportUtils.ACTIVITY_WINDOW_DAYS, ContributorsReportUtils.ACTIVITY_WINDOW_DAYS.length);
            scopePanels.put("All", () -> {
                addTimeSections(indexReport, contributorsAnalysisResults, contributorsAnalysisResults.getContributorsPerYear(),
                        allSummary, fade, contributorsAnalysisResults.getContributorsPerMonth(), contributorsAnalysisResults.getContributorsPerWeek(),
                        contributorsAnalysisResults.getContributorsPerDay());
                addPerFileExtensionSection(indexReport, analysisResults, "All", path -> true, null);
            });
            ContributorsReportUtils.addScopeToggle(indexReport, "overview_activity_scope", scopePanels);
        } else {
            indexReport.addParagraph("No commit history found.", "color: grey; margin-left: 10px; margin: 15px");
        }
        indexReport.endTabContentSection();
    }

    private static void addTimeSections(RichTextReport indexReport, ContributorsAnalysisResults analysis,
                                        List<ContributionTimeSlot> perYear, ContributorsReportUtils.ActivitySummary summary, boolean fade,
                                        List<ContributionTimeSlot> perMonth, List<ContributionTimeSlot> perWeek,
                                        List<ContributionTimeSlot> perDay) {
        indexReport.startSection("Per Year", "Totals for the past 30 days, 90 days, 6 months, year and all time, and the activity per year.");
        ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, perYear, 20, true, true, 8, fade, summary);
        indexReport.endSection();

        if (!isEmpty(perMonth)) {
            indexReport.startSection("Per Month", "Activity per month, past " + (CommitsReportGenerator.PAST_MONTHS / 12) + " years.");
            CommitsReportGenerator.addPerMonthDiagram(indexReport, analysis, perMonth);
            indexReport.endSection();
        }
        if (!isEmpty(perWeek)) {
            indexReport.startCollapsibleSection("Per Week", "Activity per week, past " + (CommitsReportGenerator.PAST_WEEKS / 52) + " years.");
            CommitsReportGenerator.addPerWeekDiagram(indexReport, analysis, perWeek);
            indexReport.endCollapsibleSection();
        }
        if (!isEmpty(perDay)) {
            indexReport.startCollapsibleSection("Per Day", "Activity per day, past year.");
            CommitsReportGenerator.addPerDayDiagram(indexReport, analysis, perDay);
            indexReport.endCollapsibleSection();
        }
    }

    // The extensions a scope's per-extension chart lists (its analyzed files' extensions); null for
    // Unscoped and All, which list every extension found in their history.
    private static List<String> scopeExtensions(CodeAnalysisResults analysisResults, String scope) {
        switch (scope) {
            case "main": return analysisResults.getMainAspectAnalysisResults().getExtensions();
            case "test": return analysisResults.getTestAspectAnalysisResults().getExtensions();
            case "build": return analysisResults.getBuildAndDeployAspectAnalysisResults().getExtensions();
            case "generated": return analysisResults.getGeneratedAspectAnalysisResults().getExtensions();
            case "other": return analysisResults.getOtherAspectAnalysisResults().getExtensions();
            default: return null;
        }
    }

    // Commits and contributors per year for each file extension, over the history of the scope's files.
    private static void addPerFileExtensionSection(RichTextReport indexReport, CodeAnalysisResults analysisResults, String scopeLabel,
                                                   java.util.function.Predicate<String> lowercasePathFilter, List<String> extensions) {
        List<HistoryPerExtension> historyPerExtensionPerYear = analysisResults.getFilesHistoryAnalysisResults().getHistoryPerExtensionPerYear(lowercasePathFilter);
        if (historyPerExtensionPerYear.isEmpty()) {
            return;
        }
        String which = "All".equals(scopeLabel) ? "all files in the history" : HtmlEscapeUtils.escape(scopeLabel.toLowerCase()) + " files";
        indexReport.startSection("Per File Extension", "Commits and contributors per year for each file extension, " + which + ".");

        indexReport.startTable();
        indexReport.startTableRow();
        indexReport.addTableCell(ReportFileExporter.getIconSvg("commits") + "<div style='font-size: 80%'>commits</div>", "border: none; text-align: center");
        indexReport.startTableCell("border: none");
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
        indexReport.endSection();
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
    private static boolean isEmpty(List<ContributionTimeSlot> slots) {
        return slots == null || slots.isEmpty();
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
