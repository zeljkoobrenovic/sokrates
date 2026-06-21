/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    // The summary windows shown as leading columns of the activity table (label + day span; <=0 = all
    // time). Order is the column order, left to right.
    static final int[] SUMMARY_WINDOW_DAYS = {30, 90, 0};
    static final String[] SUMMARY_WINDOW_LABELS = {"30 days", "90 days", "all time"};

    // Per-metric totals for one window (one cell each in a metric row's leading summary columns).
    public static class WindowTotals {
        public int added, deleted, fileUpdates, commits, contributors;
    }

    // The leading-summary data for one scope: one WindowTotals per SUMMARY_WINDOW_DAYS entry. Rendered as
    // the leading columns of the activity table by addContributorsPerTimeSlot when passed in.
    public static class ActivitySummary {
        public final WindowTotals[] windows;
        public ActivitySummary(WindowTotals[] windows) {
            this.windows = windows;
        }
    }

    /**
     * Builds the leading-summary totals for a scope (commits/file-updates/churn from the scope's
     * day-level time slots, distinct contributors from per-scope commit dates), one column per
     * SUMMARY_WINDOW_DAYS window. {@code scope == null} means all scopes (uses the all-scope day slots
     * and each contributor's flat commit dates). A window of <=0 days means all time.
     */
    public static ActivitySummary buildActivitySummary(ContributorsAnalysisResults contributorsAnalysisResults, String scope) {
        List<ContributionTimeSlot> perDay = scope == null
                ? contributorsAnalysisResults.getContributorsPerDay()
                : contributorsAnalysisResults.getContributorsPerDayByScope().get(scope);

        WindowTotals[] windows = new WindowTotals[SUMMARY_WINDOW_DAYS.length];
        for (int w = 0; w < SUMMARY_WINDOW_DAYS.length; w++) {
            int days = SUMMARY_WINDOW_DAYS[w];
            WindowTotals t = new WindowTotals();
            if (perDay != null) {
                for (ContributionTimeSlot slot : perDay) {
                    if (days <= 0 || nl.obren.sokrates.sourcecode.filehistory.DateUtils.isCommittedLessThanDaysAgo(slot.getTimeSlot(), days)) {
                        t.commits += slot.getCommitsCount();
                        t.fileUpdates += slot.getFileUpdatesCount();
                        t.added += slot.getLinesAdded();
                        t.deleted += slot.getLinesDeleted();
                    }
                }
            }
            // Distinct non-bot contributors active in this scope+window.
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
            t.contributors = people.size();
            windows[w] = t;
        }
        return new ActivitySummary(windows);
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
     * @param summary when non-null, the metric rows (churn/file-updates/commits/contributors) get
     *                leading summary columns (30 days / 90 days / all time) as real table cells aligned to
     *                each row — replacing pixel-margin-aligned cards. Null keeps the plain chart.
     */
    public static void addContributorsPerTimeSlot(RichTextReport report, List<ContributionTimeSlot> contributorsPerTimeSlot, int limit, boolean showTimeSlot, boolean showContributors, int padding, boolean fade, ActivitySummary summary) {
        Collections.sort(contributorsPerTimeSlot, (a, b) -> b.getTimeSlot().compareTo(a.getTimeSlot()));

        if (contributorsPerTimeSlot.size() > 0) {
            if (contributorsPerTimeSlot.size() > limit) {
                contributorsPerTimeSlot = contributorsPerTimeSlot.subList(0, limit);
            }

            int maxContributors = contributorsPerTimeSlot.stream().mapToInt(c -> c.getContributorsCount()).max().orElse(1);
            int maxCommits = contributorsPerTimeSlot.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);
            int maxFileUpdatesCount = contributorsPerTimeSlot.stream().mapToInt(c -> c.getFileUpdatesCount()).max().orElse(1);
            // Churn is drawn as a diverging chart: additions above a zero baseline, deletions below it.
            // Both sides share one scale (the largest single-side value across slots) so an addition and
            // a deletion of equal size draw equal bar lengths. The row is only emitted when there is
            // churn data at all (older history files have none).
            int maxChurn = contributorsPerTimeSlot.stream()
                    .mapToInt(c -> Math.max(c.getLinesAdded(), c.getLinesDeleted())).max().orElse(0);
            boolean hasChurn = maxChurn > 0;

            report.startDiv("overflow-y: auto; font-size: 90%");
            report.startTable();

            // Leading summary columns header (30 days / 90 days / all time), spanning the summary cells
            // that each metric row prepends. Only when a summary is supplied.
            if (summary != null) {
                report.startTableRow();
                report.addTableCell("", "border: none;"); // above the metric icon column
                for (String label : SUMMARY_WINDOW_LABELS) {
                    report.addTableCell(label, "border: none; text-align: center; vertical-align: bottom; font-size: 70%; color: grey; padding: 2px 6px;");
                }
                // A spacer cell above the time-series area (kept narrow; charts start after the summary).
                report.addTableCell("", "border: none;");
                report.endTableRow();
            }

            if (hasChurn) {
                addChurnRow(report, contributorsPerTimeSlot, maxChurn, showTimeSlot, padding, fade, summary);
            }

            // With leading summary columns the metric icons centre vertically to line up with the
            // centred summary cells; without them (e.g. the Commits report charts) keep the icons at the
            // bottom so they sit on the baseline the bars grow from.
            String iconVAlign = summary != null ? "middle" : "bottom";

            report.startTableRow();
            report.addTableCell(getIconSvg("change", 64), "border: none; vertical-align: " + iconVAlign + ";" + (fade ? "opacity: 0.4" : ""));
            addSummaryCells(report, summary, SummaryMetric.FILE_UPDATES, fade);
            String styleFileUpdatesCount;
            if (showTimeSlot) {
                styleFileUpdatesCount = "border: none; padding: " + padding + "px; width: 10px; text-align: center; vertical-align: bottom; font-size: 80%";
            } else {
                styleFileUpdatesCount = "border: none; padding: " + padding + "px; vertical-align: bottom; font-size: 80%";
            }
            for (ContributionTimeSlot timeSlot : contributorsPerTimeSlot) {
                report.startTableCell(styleFileUpdatesCount);
                if (timeSlot != null) {
                    int count = timeSlot.getFileUpdatesCount();
                    if (showTimeSlot) {
                        report.addParagraph(FormattingUtils.getSmallTextForNumber(count) + "", "margin: 0px; font-size: 90%" + (count == 0 ? "; color: #d0d0d0" : ""));
                    } else {
                        report.addParagraph("&nbsp;", "margin: 0px; font-size: 90%");
                    }
                    String title = timeSlot.getTimeSlot() + ": " + count + "\n\n";
                    RiskDistributionStats stats = timeSlot.getFileUpdatesCountStats();
                    title += stats.getDescription();

                    Palette palette = Palette.getRiskPalette();

                    int heightVeryHigh = 1 + (int) (64.0 * stats.getVeryHighRiskValue() / maxFileUpdatesCount);
                    report.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + palette.nextColor() + "; height:" + heightVeryHigh + "px'></div>");

                    int heightHigh = 1 + (int) (64.0 * stats.getHighRiskValue() / maxFileUpdatesCount);
                    report.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + palette.nextColor() + "; height:" + heightHigh + "px'></div>");

                    int heightMedium = 1 + (int) (64.0 * stats.getMediumRiskValue() / maxFileUpdatesCount);
                    report.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + palette.nextColor() + "; height:" + heightMedium + "px'></div>");

                    int heightLow = 1 + (int) (64.0 * stats.getLowRiskValue() / maxFileUpdatesCount);
                    report.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + palette.nextColor() + "; height:" + heightLow + "px'></div>");

                    int heightNegligible = 1 + (int) (64.0 * stats.getNegligibleRiskValue() / maxFileUpdatesCount);
                    report.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + palette.nextColor() + "; height:" + heightNegligible + "px'></div>");
                } else {
                    report.addHtmlContent("<div style='width: 100%; background-color: #d0d0d0; height:1px'></div>");
                }
                report.endTableCell();
            }
            report.endTableRow();

            report.startTableRow();
            report.addTableCell(getIconSvg("commits", 64), "border: none; vertical-align: " + iconVAlign + ";" + (fade ? "opacity: 0.4" : ""));
            addSummaryCells(report, summary, SummaryMetric.COMMITS, fade);
            String style;
            if (showTimeSlot) {
                style = "border: none; padding: " + padding + "px; width: 10px; text-align: center; vertical-align: bottom; font-size: 80%";
            } else {
                style = "border: none; padding: " + padding + "px; vertical-align: bottom; font-size: 80%";
            }
            for (ContributionTimeSlot timeSlot : contributorsPerTimeSlot) {
                report.startTableCell(style);
                if (timeSlot != null) {
                    int count = timeSlot.getCommitsCount();
                    if (showTimeSlot) {
                        report.addParagraph(FormattingUtils.getSmallTextForNumber(count) + "", "margin: 0px; font-size: 90%" + (count == 0 ? "; color: #d0d0d0" : ""));
                    } else {
                        report.addParagraph("&nbsp;", "margin: 0px; font-size: 90%");
                    }
                    int height = 1 + (int) (64.0 * count / maxCommits);
                    String title = timeSlot.getTimeSlot() + ": " + count;
                    report.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: darkgrey; height:" + height + "px'></div>");
                } else {
                    report.addHtmlContent("<div style='width: 100%; background-color: #d0d0d0; height:1px'></div>");
                }
                report.endTableCell();
            }
            report.endTableRow();

            if (showContributors) {
                report.startTableRow();
                report.addTableCell(getIconSvg("contributors", 64), "border: none; vertical-align: " + iconVAlign + ";" + (fade ? "opacity: 0.4" : ""));
                addSummaryCells(report, summary, SummaryMetric.CONTRIBUTORS, fade);
                for (ContributionTimeSlot timeSlot : contributorsPerTimeSlot) {
                    report.startTableCell(style);
                    if (timeSlot != null) {
                        int count = timeSlot.getContributorsCount();
                        if (showTimeSlot) {
                            report.addParagraph(FormattingUtils.getSmallTextForNumber(count) + "", "margin: 0px; font-size: 90%" + (count == 0 ? "; color: #d0d0d0" : ""));
                        } else {
                            report.addParagraph("&nbsp;", "margin: 0px; font-size: 90%");
                        }
                        int height = 1 + (int) (64.0 * count / maxContributors);
                        String title = timeSlot.getTimeSlot() + ": " + count;
                        report.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
                    } else {
                        report.addHtmlContent("<div style='width: 100%; background-color: #d0d0d0; height:1px'></div>");
                    }
                    report.endTableCell();
                }
                report.endTableRow();
            }

            if (showTimeSlot) {
                report.startTableRow();
                report.addTableCell("", "border: none; ");
                // Blank cells under the leading summary columns so the time-axis labels stay aligned.
                if (summary != null) {
                    for (int s = 0; s < SUMMARY_WINDOW_DAYS.length; s++) {
                        report.addTableCell("", "border: none;");
                    }
                }
                for (ContributionTimeSlot timeSlot : contributorsPerTimeSlot) {
                    if (timeSlot == null) {
                        continue;
                    }
                    String slotString = timeSlot.getTimeSlot().replaceAll("\\-", "<br>");
                    if (timeSlot.getCommitsCount() > 0 || timeSlot.getContributorsCount() > 0) {
                        report.addTableCell(slotString + "", "border: none; padding: " + padding + "px; width: 10px; text-align: center; vertical-align: top; font-size: 80%");
                    } else {
                        report.addTableCell(slotString + "", "border: none; padding: " + padding + "px; width: 10px; text-align: center; vertical-align: top; font-size: 80%; color: #c0c0c0");
                    }
                }
                report.endTableRow();
            }

            report.endTable();
            report.endDiv();
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
        report.addTableCell(getIconSvg("lines_churn", 64), "border: none; vertical-align: middle;" + (fade ? "opacity: 0.4" : ""));
        addSummaryCells(report, summary, SummaryMetric.CHURN, fade);
        String style;
        if (showTimeSlot) {
            style = "border: none; padding: " + padding + "px; width: 10px; text-align: center; vertical-align: middle; font-size: 80%";
        } else {
            style = "border: none; padding: " + padding + "px; vertical-align: middle; font-size: 80%";
        }
        for (ContributionTimeSlot timeSlot : contributorsPerTimeSlot) {
            report.startTableCell(style);
            if (timeSlot != null) {
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
                report.addHtmlContent("<div title='" + title + "' style='height: " + (CHURN_HALF_HEIGHT + labelHeight)
                        + "px; display: flex; flex-direction: column; justify-content: flex-end; align-items: center'>");
                if (showTimeSlot) {
                    report.addHtmlContent("<div style='height: " + labelHeight + "px; font-size: 70%; line-height: " + labelHeight + "px; color: #2e7d32'>" + addedLabel + "</div>");
                }
                report.addHtmlContent("<div style='width: 100%; background-color: #2e7d32; height:" + heightAdded + "px'></div>");
                report.addHtmlContent("</div>");
                // Zero baseline — a single shared horizontal line at the centre of the cell.
                report.addHtmlContent("<div style='width: 100%; height: 1px; background-color: #999999'></div>");
                // Bottom half: a fixed-height, top-anchored column holding [bar][label] so the bar's head
                // always touches the baseline above and the -deleted count sits just under it.
                report.addHtmlContent("<div title='" + title + "' style='height: " + (CHURN_HALF_HEIGHT + labelHeight)
                        + "px; display: flex; flex-direction: column; justify-content: flex-start; align-items: center'>");
                report.addHtmlContent("<div style='width: 100%; background-color: #c62828; height:" + heightDeleted + "px'></div>");
                if (showTimeSlot) {
                    report.addHtmlContent("<div style='height: " + labelHeight + "px; font-size: 70%; line-height: " + labelHeight + "px; color: #c62828'>" + deletedLabel + "</div>");
                }
                report.addHtmlContent("</div>");
            } else {
                report.addHtmlContent("<div style='width: 100%; height: 1px; background-color: #999999'></div>");
            }
            report.endTableCell();
        }
        report.endTableRow();
    }

    // Which metric a leading summary cell shows.
    private enum SummaryMetric { CHURN, FILE_UPDATES, COMMITS, CONTRIBUTORS }

    // Emits the leading summary cells (one per SUMMARY_WINDOW_DAYS window) for a metric row, right after
    // that row's icon cell. No-op when summary is null (plain chart). The cell shows the window total for
    // the metric, styled to sit beside the row's chart at the same vertical rhythm.
    // Opacity per summary window, parallel to SUMMARY_WINDOW_DAYS: the 30-day window is the primary
    // signal (full opacity); 90-day and all-time are progressively dimmed so the recent window stands out.
    private static final double[] SUMMARY_WINDOW_OPACITY = {1.0, 0.3, 0.2};

    private static void addSummaryCells(RichTextReport report, ActivitySummary summary, SummaryMetric metric, boolean fade) {
        if (summary == null) {
            return;
        }
        for (int w = 0; w < summary.windows.length; w++) {
            WindowTotals t = summary.windows[w];
            // Per-window dimming; when there is no recent activity at all (fade) dim a touch further.
            double opacity = w < SUMMARY_WINDOW_OPACITY.length ? SUMMARY_WINDOW_OPACITY[w] : 0.2;
            if (fade) {
                opacity *= 0.5;
            }
            // Data is centred both horizontally and vertically in the cell.
            String cellStyle = "border: none; text-align: center; vertical-align: middle;"
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
                default:
                    value = numberCell(t.contributors);
                    break;
            }
            report.addTableCell(value, cellStyle);
        }
    }

    private static String numberCell(int count) {
        String color = count == 0 ? "#c0c0c0" : "#333333";
        return "<span style='font-size: 150%; color: " + color + ";'>" + FormattingUtils.getSmallTextForNumber(count) + "</span>";
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
        if (contributor30Days.size() > 0) {
            indexReport.addParagraph("Past 30 days (" + contributor30Days.size() + "):", "font-size: 80%");
            contributor30Days.forEach(contributor -> {
                addContributor(indexReport, max, total, contributor);
            });
        } else {
            indexReport.addParagraph("No " + type.toLowerCase() + " in past 30 days.", "font-size: 80%");
        }
        indexReport.addHorizontalLine();
        if (contributor90Days.size() > 0) {
            indexReport.addParagraph("Past 31 to 90 days (" + contributor90Days.size() + "):", "font-size: 80%");
            contributor90Days.forEach(contributor -> {
                addContributor(indexReport, max, total, contributor);
            });
        } else {
            indexReport.addParagraph("No " + type.toLowerCase() + " in past 31 to 90 days.", "font-size: 80%");
        }
        indexReport.addHorizontalLine();
        if (contributor180Days.size() > 0) {
            indexReport.addParagraph("Past 91 to 180 days (" + contributor180Days.size() + "):", "font-size: 80%");
            contributor180Days.forEach(contributor -> {
                addContributor(indexReport, max, total, contributor);
            });
        } else {
            indexReport.addParagraph("No " + type.toLowerCase() + " in past 91 to 180 days.", "font-size: 80%");
        }
        indexReport.addLevel2Header("Historical " + type + " (" + historicalCount + ")", "margin-top: 40px");
        indexReport.addParagraph("Last " + type.toLowerCase() + " more than 6 months ago", "color: grey");
        contributors.stream().limit(MAX_CONTRIBUTOR_LIST_SIZE).filter(c -> !c.isActive()).forEach(contributor -> {
            addContributor(indexReport, max, total, contributor);
        });
        indexReport.endDiv();
        indexReport.endSection();
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
        String info = StringEscapeUtils.escapeHtml4(contributor.getEmail()
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
        indexReport.addHtmlContent(contributor.getEmail());
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
