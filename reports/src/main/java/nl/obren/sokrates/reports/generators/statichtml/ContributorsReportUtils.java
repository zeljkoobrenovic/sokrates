/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class ContributorsReportUtils {

    public static final int MAX_CONTRIBUTOR_LIST_SIZE = 500;

    // Scope keys (as used in the per-scope time-slot maps) paired with their display labels, in the
    // order the scope tabs appear. Keeps the two report sites that render the tabs in sync.
    public static final java.util.LinkedHashMap<String, String> SCOPE_LABELS = new java.util.LinkedHashMap<>();
    static {
        SCOPE_LABELS.put("main", "Main");
        SCOPE_LABELS.put("test", "Test");
        SCOPE_LABELS.put("build", "Build");
        SCOPE_LABELS.put("generated", "Generated");
        SCOPE_LABELS.put("other", "Other");
        // Residual tab (last): commits to files in no scope — deleted/renamed-away or excluded from
        // every aspect. Makes the scope tabs sum to "All" (key from GitContributorsUtil.UNSCOPED).
        SCOPE_LABELS.put("unscoped", "Unscoped");
    }

    // The default summary windows (day span; <=0 = all time) used by the Overview tab. Order is the
    // column/tooltip order, left to right.
    static final int[] SUMMARY_WINDOW_DAYS = {30, 90, 0};
    // The wider window set used by the Activity tab and the Commits report (all shown as columns).
    public static final int[] ACTIVITY_WINDOW_DAYS = {30, 90, 180, 365, 0};

    // Human label for a window span (<=0 = all time).
    static String windowLabel(int days) {
        if (days <= 0) return "all time";
        if (days == 180) return "6 months";
        if (days == 365) return "1 year";
        return days + " days";
    }

    // Per-metric totals for one window (one cell each in a metric row's leading summary columns).
    public static class WindowTotals {
        public int added, deleted, fileUpdates, commits, contributors;
        // Commits with an AI-agent co-author (from commit trailers); 0 without the trailers sidecar.
        public int aiCommits;
    }

    // The leading-summary data for one scope: one WindowTotals per SUMMARY_WINDOW_DAYS entry. Rendered as
    // the leading columns of the activity table by addContributorsPerTimeSlot when passed in.
    public static class ActivitySummary {
        public final WindowTotals[] windows;
        // Window spans parallel to windows (<=0 = all time); labels come from windowLabel.
        public final int[] days;
        // How many leading windows are rendered as real columns (the rest are tooltip-only).
        public final int columns;
        public ActivitySummary(WindowTotals[] windows) {
            this(windows, SUMMARY_WINDOW_DAYS, 1);
        }
        public ActivitySummary(WindowTotals[] windows, int[] days, int columns) {
            this.windows = windows;
            this.days = days;
            this.columns = Math.min(columns, windows.length);
        }
        String label(int w) {
            return windowLabel(w < days.length ? days[w] : 0);
        }
    }

    /**
     * Builds the leading-summary totals for a scope (commits/file-updates/churn from the scope's
     * day-level time slots, distinct contributors from per-scope commit dates), one column per
     * SUMMARY_WINDOW_DAYS window. {@code scope == null} means all scopes (uses the all-scope day slots
     * and each contributor's flat commit dates). A window of <=0 days means all time.
     */
    public static ActivitySummary buildActivitySummary(ContributorsAnalysisResults contributorsAnalysisResults, String scope) {
        return buildActivitySummary(contributorsAnalysisResults, scope, SUMMARY_WINDOW_DAYS, 1);
    }

    /**
     * Same as above with explicit windows; the first {@code columns} windows are rendered as leading
     * columns of the activity table, all of them go into the icon tooltips.
     */
    public static ActivitySummary buildActivitySummary(ContributorsAnalysisResults contributorsAnalysisResults, String scope, int[] windowDays, int columns) {
        List<ContributionTimeSlot> perDay = scope == null
                ? contributorsAnalysisResults.getContributorsPerDay()
                : contributorsAnalysisResults.getContributorsPerDayByScope().get(scope);

        WindowTotals[] windows = new WindowTotals[windowDays.length];
        for (int w = 0; w < windowDays.length; w++) {
            int days = windowDays[w];
            WindowTotals t = windowTotals(perDay, days);
            t.contributors = activeContributorsCount(contributorsAnalysisResults, scope, days);
            windows[w] = t;
        }
        return new ActivitySummary(windows, windowDays, columns);
    }

    /** Commits, AI co-authored commits, file updates and churn of the per-day slots within the window (days <= 0 = all time). */
    private static WindowTotals windowTotals(List<ContributionTimeSlot> perDay, int days) {
        WindowTotals t = new WindowTotals();
        if (perDay != null) {
            for (ContributionTimeSlot slot : perDay) {
                if (days <= 0 || nl.obren.sokrates.sourcecode.filehistory.DateUtils.isCommittedLessThanDaysAgo(slot.getTimeSlot(), days)) {
                    t.commits += slot.getCommitsCount();
                    t.aiCommits += slot.getAiCoAuthoredCommitsCount();
                    t.fileUpdates += slot.getFileUpdatesCount();
                    t.added += slot.getLinesAdded();
                    t.deleted += slot.getLinesDeleted();
                }
            }
        }
        return t;
    }

    /** Distinct non-bot contributors with a commit in the scope within the window (days <= 0 = all time). */
    private static int activeContributorsCount(ContributorsAnalysisResults contributorsAnalysisResults, String scope, int days) {
        java.util.Set<String> people = new java.util.HashSet<>();
        for (Contributor c : contributorsAnalysisResults.getContributors()) {
            if (c.isBot()) {
                continue;
            }
            List<String> dates = scope == null ? c.getCommitDates() : c.getCommitDatesByScope().get(scope);
            if (dates == null) {
                continue;
            }
            boolean active = days <= 0
                    ? !dates.isEmpty()
                    : dates.stream().anyMatch(d -> nl.obren.sokrates.sourcecode.filehistory.DateUtils.isCommittedLessThanDaysAgo(d, days));
            if (active) {
                people.add(c.getEmail());
            }
        }
        return people.size();
    }

    /**
     * Renders a small tab-like scope selector (e.g. "Main" / ... / "All") above a set of activity diagrams,
     * with one show/hide panel per scope. Each entry's {@link Runnable} renders that scope's body into
     * the report. Self-contained: it emits its own buttons + panels + a scoped inline switch script, so
     * it does NOT use the global tab machinery (openTab toggles every .tabcontent on the page, which
     * would break when nested inside an existing tab). The first scope is shown by default.
     *
     * @param groupId a page-unique id so multiple selectors don't collide
     * @param scopePanels ordered map of scope label -> body renderer
     */
    public static void addScopeToggle(RichTextReport report, String groupId, Map<String, Runnable> scopePanels) {
        if (scopePanels.isEmpty()) {
            return;
        }
        // If only one scope is available (e.g. no main classification), skip the selector chrome and
        // just render that scope's body inline.
        if (scopePanels.size() == 1) {
            scopePanels.values().iterator().next().run();
            return;
        }

        report.addHtmlContent("<div style='margin: 18px 10px'>");
        int[] i = {0};
        scopePanels.keySet().forEach(label -> {
            String safeLabel = label.replaceAll("[^A-Za-z0-9]", "_");
            boolean active = i[0] == 0;
            String bg = active ? "black" : "#eeeeee";
            String color = active ? "white" : "#333333";
            report.addHtmlContent("<button id='" + groupId + "_btn_" + safeLabel + "'"
                    + " onclick=\"showActivityScope('" + groupId + "', '" + safeLabel + "')\""
                    + " style='background-color: " + bg + "; color: " + color
                    + "; padding: 3px 12px; margin-right: 4px; cursor: pointer; border-radius: 999px; font-size: 80%; border: none'>"
                    + label + "</button>");
            i[0]++;
        });
        report.addHtmlContent("</div>");

        i[0] = 0;
        scopePanels.forEach((label, renderer) -> {
            String safeLabel = label.replaceAll("[^A-Za-z0-9]", "_");
            boolean active = i[0] == 0;
            report.addHtmlContent("<div id='" + groupId + "_panel_" + safeLabel + "' class='" + groupId + "_panel'"
                    + " style='display: " + (active ? "block" : "none") + ";'>");
            renderer.run();
            report.addHtmlContent("</div>");
            i[0]++;
        });

        // Scoped switch: only touches this group's own panels/buttons (by id prefix), so it composes
        // with the global tab machinery and with other scope selectors on the same page.
        report.addHtmlContent("<script>\n"
                + "function showActivityScope(groupId, scope) {\n"
                + "  var panels = document.getElementsByClassName(groupId + '_panel');\n"
                + "  for (var i = 0; i < panels.length; i++) { panels[i].style.display = 'none'; }\n"
                + "  var panel = document.getElementById(groupId + '_panel_' + scope);\n"
                + "  if (panel) { panel.style.display = 'block'; }\n"
                + "  var btns = document.querySelectorAll('[id^=\"' + groupId + '_btn_\"]');\n"
                + "  for (var j = 0; j < btns.length; j++) {\n"
                + "    btns[j].style.backgroundColor = '#eeeeee'; btns[j].style.color = '#333333';\n"
                + "  }\n"
                + "  var btn = document.getElementById(groupId + '_btn_' + scope);\n"
                + "  if (btn) { btn.style.backgroundColor = 'black'; btn.style.color = 'white'; }\n"
                + "}\n"
                + "</script>");
    }

    public static void addContributorsSection(CodeAnalysisResults analysisResults, RichTextReport report) {
        ContributorsAnalysisResults contributorsAnalysisResults = analysisResults.getContributorsAnalysisResults();
        List<Contributor> contributors = contributorsAnalysisResults.getContributors();
        List<Contributor> people = contributors.stream().filter(c -> !c.isBot()).collect(Collectors.toList());
        List<Contributor> bots = contributors.stream().filter(c -> c.isBot()).collect(Collectors.toList());

        if (people.size() > 0) {
            addContributors(report, people, "Contributors");
        }

        if (bots.size() > 0) {
            addContributors(report, bots, "Bots");
        }
    }

    private static ContributionTimeSlot findSlot(List<ContributionTimeSlot> slots, int year) {
        for (ContributionTimeSlot slot : slots) {
            if (slot.getTimeSlot().endsWith(year + "")) return slot;
        }
        return null;
    }

    public static void addContributorsPerTimeSlot(RichTextReport report, List<ContributionTimeSlot> contributorsPerTimeSlot, int limit, boolean showTimeSlot, boolean showContributors, int padding, boolean fade) {
        addContributorsPerTimeSlot(report, contributorsPerTimeSlot, limit, showTimeSlot, showContributors, padding, fade, null);
    }

    /**
     * @param summary when non-null, the metric rows (churn/file-updates/commits/contributors) get their
     *                window totals (30 days / 90 days / all time) in the icon tooltip and the icon links to the
     *                detailed report. Null keeps the plain chart (plain icon, description-only tooltip).
     */
    public static void addContributorsPerTimeSlot(RichTextReport report, List<ContributionTimeSlot> contributorsPerTimeSlot, int limit, boolean showTimeSlot, boolean showContributors, int padding, boolean fade, ActivitySummary summary) {
        Collections.sort(contributorsPerTimeSlot, (a, b) -> b.getTimeSlot().compareTo(a.getTimeSlot()));
        if (contributorsPerTimeSlot.isEmpty()) {
            return;
        }
        List<ContributionTimeSlot> slots = contributorsPerTimeSlot.size() > limit ? contributorsPerTimeSlot.subList(0, limit) : contributorsPerTimeSlot;

        int maxContributors = maxOf(slots, ContributionTimeSlot::getContributorsCount, 1);
        int maxCommits = maxOf(slots, ContributionTimeSlot::getCommitsCount, 1);
        // The AI co-authored commits row (from Co-authored-by trailers) is only emitted when any
        // slot has one — histories extracted without the trailers sidecar look exactly as before.
        boolean hasAiCommits = slots.stream().anyMatch(c -> c != null && c.getAiCoAuthoredCommitsCount() > 0);
        int maxFileUpdatesCount = maxOf(slots, ContributionTimeSlot::getFileUpdatesCount, 1);
        // Churn is drawn as a diverging chart: additions above a zero baseline, deletions below it.
        // Both sides share one scale (the largest single-side value across slots) so an addition and
        // a deletion of equal size draw equal bar lengths. The row is only emitted when there is
        // churn data at all (older history files have none).
        int maxChurn = maxOf(slots, c -> Math.max(c.getLinesAdded(), c.getLinesDeleted()), 0);

        report.startDiv("overflow-y: auto; font-size: 90%");
        report.startTable();

        // Leading summary column headers ("30 days", …) above the window totals each metric row
        // prepends (summary.columns of them). Only when a summary is supplied.
        if (summary != null) {
            addTimeSlotLabelsRow(report, slots, padding, summary, true);
        }
        if (maxChurn > 0) {
            addChurnRow(report, slots, maxChurn, showTimeSlot, padding, fade, summary);
        }

        // With leading summary columns the metric icons centre vertically to line up with the
        // centred summary cells; without them (e.g. the Commits report charts) keep the icons at the
        // bottom so they sit on the baseline the bars grow from.
        String iconVAlign = summary != null ? "middle" : "bottom";
        String cellStyle = barCellStyle(showTimeSlot, padding);

        addBarRow(report, slots, cellStyle, showTimeSlot, "change", "number of files changed per commit", SummaryMetric.FILE_UPDATES, iconVAlign, fade, summary,
                ContributionTimeSlot::getFileUpdatesCount, (timeSlot, count) -> addFileUpdatesBars(report, timeSlot, count, maxFileUpdatesCount));
        addBarRow(report, slots, cellStyle, showTimeSlot, "commits", "number of commits", SummaryMetric.COMMITS, iconVAlign, fade, summary,
                ContributionTimeSlot::getCommitsCount, (timeSlot, count) -> addBar(report, timeSlot.getTimeSlot() + ": " + count, "darkgrey", count, maxCommits));
        if (hasAiCommits) {
            // Same scale as the commits row above, so the bar reads as the AI share of commits.
            addBarRow(report, slots, cellStyle, showTimeSlot, "bot", "number of commits with an AI coding agent co-author (from commit trailers such as Co-authored-by)", SummaryMetric.AI_COMMITS, iconVAlign, fade, summary,
                    ContributionTimeSlot::getAiCoAuthoredCommitsCount, (timeSlot, count) -> addBar(report, aiCommitsTitle(timeSlot, count), "#b39ddb", count, maxCommits));
        }
        if (showContributors) {
            addBarRow(report, slots, cellStyle, showTimeSlot, "contributors", "number of contributors", SummaryMetric.CONTRIBUTORS, iconVAlign, fade, summary,
                    ContributionTimeSlot::getContributorsCount, (timeSlot, count) -> addBar(report, timeSlot.getTimeSlot() + ": " + count, "skyblue", count, maxContributors));
        }
        if (showTimeSlot) {
            addTimeSlotLabelsRow(report, slots, padding, summary, false);
        }

        report.endTable();
        report.endDiv();
    }

    private static int maxOf(List<ContributionTimeSlot> slots, java.util.function.ToIntFunction<ContributionTimeSlot> value, int orElse) {
        return slots.stream().mapToInt(value).max().orElse(orElse);
    }

    private static String barCellStyle(boolean showTimeSlot, int padding) {
        return showTimeSlot
                ? "border: none; padding: " + padding + "px; width: 10px; text-align: center; vertical-align: bottom; font-size: 80%"
                : "border: none; padding: " + padding + "px; vertical-align: bottom; font-size: 80%";
    }

    /**
     * One metric row of the activity table: the metric icon, the summary cell, then one cell per
     * time slot with the count (when the time axis is shown) and the bars {@code bars} draws for it;
     * a null slot (a gap in the history) gets a thin grey line.
     */
    private static void addBarRow(RichTextReport report, List<ContributionTimeSlot> slots, String cellStyle, boolean showTimeSlot,
                                  String icon, String description, SummaryMetric metric, String iconVAlign, boolean fade, ActivitySummary summary,
                                  ToIntFunction<ContributionTimeSlot> value, ObjIntConsumer<ContributionTimeSlot> bars) {
        report.startTableRow();
        addMetricIconCell(report, icon, iconVAlign, fade, description, summary, metric);
        addSummaryCell(report, summary, metric, fade);
        for (ContributionTimeSlot timeSlot : slots) {
            report.startTableCell(cellStyle);
            if (timeSlot != null) {
                int count = value.applyAsInt(timeSlot);
                if (showTimeSlot) {
                    report.addParagraph(FormattingUtils.getSmallTextForNumber(count) + "", "margin: 0px; font-size: 90%" + (count == 0 ? "; color: #d0d0d0" : ""));
                } else {
                    report.addParagraph("&nbsp;", "margin: 0px; font-size: 90%");
                }
                bars.accept(timeSlot, count);
            } else {
                report.addHtmlContent("<div style='width: 100%; background-color: #d0d0d0; height:1px'></div>");
            }
            report.endTableCell();
        }
        report.endTableRow();
    }

    /** A bar of {@code count} out of {@code max} (1 to 65 px high). */
    private static void addBar(RichTextReport report, String title, String color, int count, int max) {
        int height = 1 + (int) (64.0 * count / max);
        report.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + height + "px'></div>");
    }

    /** The file-updates bar is stacked: one segment per risk band of the files-per-commit distribution, in the risk palette. */
    private static void addFileUpdatesBars(RichTextReport report, ContributionTimeSlot timeSlot, int count, int maxFileUpdatesCount) {
        RiskDistributionStats stats = timeSlot.getFileUpdatesCountStats();
        String title = timeSlot.getTimeSlot() + ": " + count + "\n\n" + stats.getDescription();
        Palette palette = Palette.getRiskPalette();
        for (int bandValue : new int[]{stats.getVeryHighRiskValue(), stats.getHighRiskValue(), stats.getMediumRiskValue(), stats.getLowRiskValue(), stats.getNegligibleRiskValue()}) {
            addBar(report, title, palette.nextColor(), bandValue, maxFileUpdatesCount);
        }
    }

    private static String aiCommitsTitle(ContributionTimeSlot timeSlot, int count) {
        return timeSlot.getTimeSlot() + ": " + count + " of " + timeSlot.getCommitsCount() + " commits"
                + (timeSlot.getCommitsCount() > 0 ? " (" + (100 * count / timeSlot.getCommitsCount()) + "%)" : "");
    }

    /**
     * The time-axis labels: as the header row above the summary column titles (labels sit at the
     * bottom of their cells) or as the footer row under the bars (labels at the top, blank cells
     * under the summary columns keep them aligned). Slots without activity are greyed.
     */
    private static void addTimeSlotLabelsRow(RichTextReport report, List<ContributionTimeSlot> slots, int padding, ActivitySummary summary, boolean header) {
        report.startTableRow();
        report.addTableCell("", header ? "border: none;" : "border: none; ");
        if (summary != null) {
            addSummaryColumnCells(report, summary, header);
        }
        String style = "border: none; padding: " + padding + "px; " + (header ? "padding-bottom: 0; " : "")
                + "width: 10px; text-align: center; vertical-align: " + (header ? "bottom" : "top") + "; font-size: 80%";
        for (ContributionTimeSlot timeSlot : slots) {
            if (timeSlot == null) {
                continue;
            }
            String slotString = timeSlot.getTimeSlot().replaceAll("\\-", "<br>");
            boolean active = timeSlot.getCommitsCount() > 0 || timeSlot.getContributorsCount() > 0;
            report.addTableCell(slotString + "", style + (active ? "" : "; color: #c0c0c0"));
        }
        report.endTableRow();
    }

    /** One cell per summary window: its label in the header row, empty in the footer row. */
    private static void addSummaryColumnCells(RichTextReport report, ActivitySummary summary, boolean header) {
        for (int w = 0; w < summary.columns; w++) {
            if (header) {
                report.addTableCell(summary.label(w), "border: none; text-align: center; vertical-align: bottom; font-size: 70%; color: grey; padding: 2px 6px;");
            } else {
                report.addTableCell("", "border: none;");
            }
        }
    }

    // Max bar length (px) for each side of the diverging churn chart. The two halves (additions above,
    // deletions below the zero baseline) plus their labels together roughly match the height of the
    // other activity rows.
    private static final int CHURN_HALF_HEIGHT = 32;

    // Renders the lines-changed (churn) graph row as a diverging chart: one column per time slot with
    // additions drawn as a green bar growing UP from a centred zero baseline and deletions as a red bar
    // growing DOWN below it. The +added count sits directly ABOVE its bar and the -deleted count directly
    // UNDER its bar (both labels hug the baseline next to their bar, not the cell edge). Additions and
    // deletions share one scale so equal magnitudes draw equal lengths. Only called when there is churn
    // data. Sits directly above the file-updates row.
    private static void addChurnRow(RichTextReport report, List<ContributionTimeSlot> contributorsPerTimeSlot,
                                    int maxChurn, boolean showTimeSlot, int padding, boolean fade, ActivitySummary summary) {
        report.startTableRow();
        addMetricIconCell(report, "lines_churn", "middle", fade, "line churn", summary, SummaryMetric.CHURN);
        addSummaryCell(report, summary, SummaryMetric.CHURN, fade);
        String style;
        if (showTimeSlot) {
            style = "border: none; padding: " + padding + "px; width: 10px; text-align: center; vertical-align: middle; font-size: 80%";
        } else {
            style = "border: none; padding: " + padding + "px; vertical-align: middle; font-size: 80%";
        }
        for (ContributionTimeSlot timeSlot : contributorsPerTimeSlot) {
            report.startTableCell(style);
            if (timeSlot != null) {
                addChurnCell(report, timeSlot, maxChurn, showTimeSlot);
            } else {
                report.addHtmlContent("<div style='width: 100%; height: 1px; background-color: #999999'></div>");
            }
            report.endTableCell();
        }
        report.endTableRow();
    }

    /** A diverging bar: added lines up from a shared zero baseline, deleted lines down, with optional count labels. */
    private static void addChurnCell(RichTextReport report, ContributionTimeSlot timeSlot, int maxChurn, boolean showTimeSlot) {
        int added = timeSlot.getLinesAdded();
        int deleted = timeSlot.getLinesDeleted();
        String title = timeSlot.getTimeSlot() + ": +" + added + " / -" + deleted + " lines";

        // Bars share one scale (max single-side value); a present-but-tiny bar still shows 1px.
        int heightAdded = added > 0 ? 1 + (int) ((CHURN_HALF_HEIGHT - 1) * added / (double) maxChurn) : 0;
        int heightDeleted = deleted > 0 ? 1 + (int) ((CHURN_HALF_HEIGHT - 1) * deleted / (double) maxChurn) : 0;

        String addedLabel = showTimeSlot && added > 0 ? "+" + FormattingUtils.getSmallTextForNumber(added) : "&nbsp;";
        String deletedLabel = showTimeSlot && deleted > 0 ? "-" + FormattingUtils.getSmallTextForNumber(deleted) : "&nbsp;";
        // Label strip height is reserved even when empty so the zero baseline stays put across
        // slots (otherwise short/empty bars let the two halves collapse together and the line
        // appears to vanish).
        int labelHeight = showTimeSlot ? 12 : 0;

        // Top half: a fixed-height, bottom-anchored column holding [label][bar] so the +added
        // count sits just above its bar and the bar's foot always rests on the baseline below.
        addChurnHalf(report, title, labelHeight, "flex-end", true, showTimeSlot, "#2e7d32", addedLabel, heightAdded);
        // Zero baseline — a single shared horizontal line at the centre of the cell.
        report.addHtmlContent("<div style='width: 100%; height: 1px; background-color: #999999'></div>");
        // Bottom half: a fixed-height, top-anchored column holding [bar][label] so the bar's head
        // always touches the baseline above and the -deleted count sits just under it.
        addChurnHalf(report, title, labelHeight, "flex-start", false, showTimeSlot, "#c62828", deletedLabel, heightDeleted);
    }

    /** One half of the diverging churn bar: a fixed-height flex column with the bar and, when labels are shown, its count label above or below it. */
    private static void addChurnHalf(RichTextReport report, String title, int labelHeight, String justify, boolean labelAboveBar, boolean showLabel,
                                     String color, String label, int barHeight) {
        report.addHtmlContent("<div title='" + title + "' style='height: " + (CHURN_HALF_HEIGHT + labelHeight)
                + "px; display: flex; flex-direction: column; justify-content: " + justify + "; align-items: center'>");
        String labelHtml = "<div style='height: " + labelHeight + "px; font-size: 70%; line-height: " + labelHeight + "px; color: " + color + "'>" + label + "</div>";
        if (showLabel && labelAboveBar) {
            report.addHtmlContent(labelHtml);
        }
        report.addHtmlContent("<div style='width: 100%; background-color: " + color + "; height:" + barHeight + "px'></div>");
        if (showLabel && !labelAboveBar) {
            report.addHtmlContent(labelHtml);
        }
        report.addHtmlContent("</div>");
    }

    // Which metric a leading summary cell shows.
    enum SummaryMetric { CHURN, FILE_UPDATES, COMMITS, AI_COMMITS, CONTRIBUTORS }

    // Report page (relative to the per-repository html/ folder) each activity metric links to.
    private static final Map<SummaryMetric, String> SUMMARY_METRIC_REPORT = Map.of(
            SummaryMetric.CHURN, "FileChurn.html",
            SummaryMetric.FILE_UPDATES, "FileChurn.html",
            SummaryMetric.COMMITS, "Commits.html",
            SummaryMetric.AI_COMMITS, "Commits.html",
            SummaryMetric.CONTRIBUTORS, "Contributors.html");

    // Emits a metric row's leading icon cell. When a summary is supplied, the window totals
    // (30 days / 90 days / all time) go into the icon's hover tooltip (only the 30-day total is also
    // rendered as a leading column, see addSummaryCell) and the icon becomes a link to the metric's
    // detailed report.
    private static void addMetricIconCell(RichTextReport report, String icon, String vAlign, boolean fade, String description,
                                          ActivitySummary summary, SummaryMetric metric) {
        String style = "border: none; vertical-align: " + vAlign + ";" + (fade ? "opacity: 0.4" : "");
        String iconSvg = getIconSvg(icon, 64);
        if (summary == null) {
            report.addTableCellWithTitle(iconSvg, style, description);
            return;
        }
        String title = summaryTooltip(description, summary, metric);
        String link = SUMMARY_METRIC_REPORT.get(metric);
        report.addTableCellWithTitle("<a href='" + link + "' target='_blank' style='text-decoration: none'>" + iconSvg + "</a>", style, title);
    }

    // Emits the leading summary cells (the first summary.columns windows — just the 30-day total on the
    // Overview, all windows on the Activity tab) for a metric row, right after that row's icon cell.
    // No-op when summary is null (plain chart). Every window is also in the icon tooltip.
    private static void addSummaryCell(RichTextReport report, ActivitySummary summary, SummaryMetric metric, boolean fade) {
        if (summary == null) {
            return;
        }
        for (int w = 0; w < summary.columns; w++) {
            addSummaryWindowCell(report, summary.windows[w], metric, fade, w == 0);
        }
    }

    private static void addSummaryWindowCell(RichTextReport report, WindowTotals t, SummaryMetric metric, boolean fade, boolean primary) {
        // The first (30-day) window is the primary signal; later windows are dimmed a little.
        double opacity = (primary ? 1.0 : 0.6) * (fade ? 0.5 : 1.0);
        String cellStyle = "border: none; border-left: 1px solid #ccc; border-right: 1px solid #ccc; text-align: center; vertical-align: middle;"
                + " padding: 2px 6px; min-width: 48px; opacity: " + opacity + ";";
        String value;
        switch (metric) {
            case CHURN:
                value = "<span style='color: #2e7d32;'>+" + FormattingUtils.getSmallTextForNumber(t.added) + "</span>"
                        + "<br><span style='color: #c62828;'>-" + FormattingUtils.getSmallTextForNumber(t.deleted) + "</span>";
                break;
            case FILE_UPDATES:
                value = numberCell(t.fileUpdates);
                break;
            case COMMITS:
                value = numberCell(t.commits);
                break;
            case AI_COMMITS:
                value = numberCell(t.aiCommits);
                break;
            default:
                value = numberCell(t.contributors);
                break;
        }
        report.addTableCell(value, cellStyle);
    }

    private static String numberCell(int count) {
        String color = count == 0 ? "#c0c0c0" : "#333333";
        return "<span style='font-size: 150%; color: " + color + ";'>" + FormattingUtils.getSmallTextForNumber(count) + "</span>";
    }

    // Tooltip text: the description followed by one "<window>: <total>" line per summary window
    // (churn shows "+added / -deleted"). Package-visible for tests.
    static String summaryTooltip(String description, ActivitySummary summary, SummaryMetric metric) {
        StringBuilder title = new StringBuilder(description).append("\n");
        for (int w = 0; w < summary.windows.length; w++) {
            WindowTotals t = summary.windows[w];
            String value;
            switch (metric) {
                case CHURN:
                    value = "+" + FormattingUtils.formatCount(t.added) + " / -" + FormattingUtils.formatCount(t.deleted) + " lines";
                    break;
                case FILE_UPDATES:
                    value = FormattingUtils.formatCount(t.fileUpdates);
                    break;
                case COMMITS:
                    value = FormattingUtils.formatCount(t.commits);
                    break;
                case AI_COMMITS:
                    value = FormattingUtils.formatCount(t.aiCommits) + " of " + FormattingUtils.formatCount(t.commits)
                            + (t.commits > 0 ? " (" + (100 * t.aiCommits / t.commits) + "%)" : "");
                    break;
                default:
                    value = FormattingUtils.formatCount(t.contributors);
                    break;
            }
            title.append("\n").append(summary.label(w)).append(": ").append(value);
        }
        return title.toString();
    }

    public static void addContributors(RichTextReport indexReport, List<Contributor> contributors, String type) {
        indexReport.addLineBreak();
        indexReport.startSubSection(type, "");
        Collections.sort(contributors, (a, b) -> b.getCommitsCount() - a.getCommitsCount());
        int max = contributors.get(0).getCommitsCount();
        int total = contributors.stream().mapToInt(c -> c.getCommitsCount()).sum();
        long activeCount = contributors.stream().filter(c -> c.isActive()).count();
        long rookiesCount = contributors.stream().filter(c -> c.isRookie()).count();
        long veteransCount = activeCount - rookiesCount;
        long historicalCount = contributors.size() - activeCount;
        indexReport.startDiv("");
        indexReport.addLevel2Header("Recent " + type + " (" + activeCount + ")");
        indexReport.addParagraph("Committed in past 6 months (a rookie = the first commit in past year)", "color: grey");
        List<Contributor> contributor30Days = contributors.stream().filter(c -> c.isActive(30)).collect(Collectors.toList());
        List<Contributor> contributor90Days = contributors.stream().filter(c -> c.isActive(90) && !c.isActive(30)).collect(Collectors.toList());
        List<Contributor> contributor180Days = contributors.stream().filter(c -> c.isActive(180) && !c.isActive(90)).collect(Collectors.toList());
        addActivityWindow(indexReport, type, "Past 30 days", "in past 30 days", contributor30Days, max, total);
        indexReport.addHorizontalLine();
        addActivityWindow(indexReport, type, "Past 31 to 90 days", "in past 31 to 90 days", contributor90Days, max, total);
        indexReport.addHorizontalLine();
        addActivityWindow(indexReport, type, "Past 91 to 180 days", "in past 91 to 180 days", contributor180Days, max, total);
        indexReport.addLevel2Header("Historical " + type + " (" + historicalCount + ")", "margin-top: 40px");
        indexReport.addParagraph("Last " + type.toLowerCase() + " more than 6 months ago", "color: grey");
        contributors.stream().limit(MAX_CONTRIBUTOR_LIST_SIZE).filter(c -> !c.isActive()).forEach(contributor -> {
            addContributor(indexReport, max, total, contributor);
        });
        indexReport.endDiv();
        indexReport.endSection();
    }

    /** The contributors of one activity window with its heading, or a "No <type> ..." line when empty. */
    private static void addActivityWindow(RichTextReport indexReport, String type, String label, String emptyLabel, List<Contributor> window, int max, int total) {
        if (window.size() > 0) {
            indexReport.addParagraph(label + " (" + window.size() + "):", "font-size: 80%");
            window.forEach(contributor -> {
                addContributor(indexReport, max, total, contributor);
            });
        } else {
            indexReport.addParagraph("No " + type.toLowerCase() + " " + emptyLabel + ".", "font-size: 80%");
        }
    }

    public static void addContributor(RichTextReport indexReport, int max, int total, Contributor contributor) {
        int commitsCount = contributor.getCommitsCount();
        // max/total can be 0 when every contributor has 0 counted commits; avoid NaN/Infinity styles.
        double opacity = max > 0 ? 0.2 + 0.8 * commitsCount / max : 1.0;
        double percentage = total > 0 ? 100.0 * commitsCount / total : 0.0;
        String churnInfo = "";
        if (contributor.getLinesAdded() > 0 || contributor.getLinesDeleted() > 0) {
            churnInfo = ", +" + contributor.getLinesAdded() + "/-" + contributor.getLinesDeleted() + " lines";
        }
        String info = HtmlEscapeUtils.escape(contributor.getEmail()
                + " " + commitsCount
                + " commits (" + FormattingUtils.getFormattedPercentage(percentage) + "%)" + churnInfo + ","
                + " between " + contributor.getFirstCommitDate() + " and " + contributor.getLatestCommitDate());

        if (contributor.isRookie()) {
            indexReport.addHtmlContent("<div style='margin: 4px; box-shadow: rgba(9, 30, 66, 0.25) 0px 4px 8px -2px, rgba(9, 30, 66, 0.08) 0px 0px 0px 1px; text-align: center; border-bottom:2px solid green; display: inline-block;opacity:" + opacity + "' title='" + info + "'>");
        } else {
            indexReport.addHtmlContent("<div style='margin: 4px; box-shadow: rgba(9, 30, 66, 0.25) 0px 4px 8px -2px, rgba(9, 30, 66, 0.08) 0px 0px 0px 1px; text-align: center; display: inline-block;opacity:" + opacity + "' title='" + info + "'>");
        }
        String icon = contributor.isBot() ? "bot" : "contributor";
        indexReport.addHtmlContent(getIconSvg(icon, 64));
        indexReport.addHtmlContent("<div style='padding: 4px; font-size: 10px; width: 64px; overflow: hidden; max-height: 22px; min-height: 22px;'>");
        indexReport.addText(contributor.getEmail());
        indexReport.addHtmlContent("</div>");
        indexReport.addHtmlContent("</div>");
    }

    public static String getIconSvg(String icon) {
        return getIconSvg(icon, 40);
    }

    public static String getIconSvg(String icon, int size) {
        String svg = HtmlTemplateUtils.getResource("/icons/" + icon + ".svg");
        svg = svg.replaceAll("height='.*?'", "height='" + size + "px'");
        svg = svg.replaceAll("width='.*?'", "width='" + size + "px'");
        return svg;
    }

}
