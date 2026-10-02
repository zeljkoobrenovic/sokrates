package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.reports.core.ReportConstants;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.landscape.data.ContributorReportExport;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

/**
 * The activity charts of the landscape contributors tab: commits, contributors, churn and first/last contributions
 * per year, month, week and day (read from the tab's {@link ContributorTimeSlots} for the selected scope), plus the
 * monthly racing charts. Moved out of {@link LandscapeReportContributorsTab}, which owns the tab's layout.
 */
class ContributorActivityCharts {
    private final RichTextReport landscapeReport;
    private final LandscapeAnalysisResults landscapeAnalysisResults;
    private final List<ContributorRepositories> contributors;
    private final ContributorTimeSlots timeSlots;
    private final File reportsFolder;

    ContributorActivityCharts(RichTextReport landscapeReport, LandscapeAnalysisResults landscapeAnalysisResults, List<ContributorRepositories> contributors,
                              ContributorTimeSlots timeSlots, File reportsFolder) {
        this.landscapeReport = landscapeReport;
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.contributors = contributors;
        this.timeSlots = timeSlots;
        this.reportsFolder = reportsFolder;
    }

    private void addActivityTrendCard(String value, String subtitle, String icon) {
        InfoBlocks.addActivityTrendCard(landscapeReport, value, subtitle, icon);
    }

    void addContributorsPerYear(boolean showContributorsCount) {
        List<ContributionTimeSlot> contributorsPerYear = timeSlots.scopedYear();
        if (contributorsPerYear.size() > 0) {
            int limit = landscapeAnalysisResults.getConfiguration().getCommitsMaxYears();
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(0, limit);
            }

            landscapeReport.startDiv("overflow-y: auto;");
            landscapeReport.startTable();

            String style = "border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px";
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);

            // Churn row first, above commits.
            addChurnPerYearRow(contributorsPerYear, style);
            addCommitsPerYearRow(contributorsPerYear, style, thisYear);
            if (showContributorsCount) {
                addContributorsCountPerYearRow(contributorsPerYear, style, thisYear);
            }
            addYearLabelsRow(contributorsPerYear, thisYear);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    /** The commits per year as bars (the current year dark), led by the total commits trend card. */
    private void addCommitsPerYearRow(List<ContributionTimeSlot> contributorsPerYear, String style, int thisYear) {
        int maxCommits = contributorsPerYear.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);
        landscapeReport.startTableRow();
        landscapeReport.startTableCell("border: none; height: 130px; vertical-align: bottom;");
        int commitsCount = timeSlots.scopedTotalCommits();
        if (commitsCount > 0) {
            addActivityTrendCard(FormattingUtils.getSmallTextForNumber(commitsCount), "commits", "commits");
        }
        landscapeReport.endTableCell();
        contributorsPerYear.forEach(year -> {
            landscapeReport.startTableCell(style);
            int count = year.getCommitsCount();
            String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
            landscapeReport.addParagraph(count + "", "margin: 2px; color: " + color);
            int height = 1 + (int) (64.0 * count / maxCommits);
            String bgColor = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "lightgrey";
            landscapeReport.addHtmlContent("<div style='width: 100%; background-color: " + bgColor + "; height:" + height + "px'></div>");
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
    }

    /** The distinct contributors per year as sky-blue bars, led by the total contributors trend card. */
    private void addContributorsCountPerYearRow(List<ContributionTimeSlot> contributorsPerYear, String style, int thisYear) {
        int maxContributors[] = {1};
        contributorsPerYear.forEach(year -> {
            int count = timeSlots.getContributorsCountPerYear(year.getTimeSlot());
            maxContributors[0] = Math.max(maxContributors[0], count);
        });
        landscapeReport.startTableRow();
        landscapeReport.startTableCell("border: none; height: 100px; vertical-align: bottom;");
        int contributorsCount = timeSlots.scopedTotalContributors();
        if (contributorsCount > 0) {
            addActivityTrendCard(FormattingUtils.getSmallTextForNumber(contributorsCount), "contributors", "contributors");
        }
        landscapeReport.endTableCell();
        contributorsPerYear.forEach(year -> {
            landscapeReport.startTableCell(style);
            int count = timeSlots.getContributorsCountPerYear(year.getTimeSlot());
            String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
            landscapeReport.addParagraph(count + "", "margin: 2px; color: " + color + ";");
            int height = 1 + (int) (64.0 * count / maxContributors[0]);
            landscapeReport.addHtmlContent("<div style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
    }

    /** The year labels, with the latest commit's month and day under its year. */
    private void addYearLabelsRow(List<ContributionTimeSlot> contributorsPerYear, int thisYear) {
        landscapeReport.startTableRow();
        landscapeReport.addTableCell("", "border: none; ");
        var ref = new Object() {
            String latestCommitDate = landscapeAnalysisResults.getLatestCommitDate();
        };
        if (ref.latestCommitDate.length() > 5) {
            ref.latestCommitDate = ref.latestCommitDate.substring(5);
        }
        contributorsPerYear.forEach(year -> {
            String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
            landscapeReport.startTableCell("vertical-align: top; border: none; text-align: center; font-size: 90%; color: " + color);
            landscapeReport.addHtmlContent(year.getTimeSlot());
            if (landscapeAnalysisResults.getLatestCommitDate().startsWith(year.getTimeSlot() + "-")) {
                landscapeReport.addContentInDiv(ref.latestCommitDate, "text-align: center; color: grey; font-size: 9px");
            }
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
    }

    void addContributorsPerWeek() {
        int limit = 104;
        List<ContributionTimeSlot> contributorsPerWeek = LandscapeReportContributorsTab.getContributionWeeks(timeSlots.scopedWeek(),
                limit, landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerWeek.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerWeek.size() > 0) {
            if (contributorsPerWeek.size() > limit) {
                contributorsPerWeek = contributorsPerWeek.subList(0, limit);
            }

            landscapeReport.startDiv("overflow: auto");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerWeek.size() >= 4 ? 4 : contributorsPerWeek.size();

            addChartRows(contributorsPerWeek, "weeks", minMaxWindow,
                    (timeSlot, rookiesOnly) -> timeSlots.getContributorsPerWeek(timeSlot, rookiesOnly),
                    (timeSlot, rookiesOnly) -> timeSlots.getLastContributorsPerWeek(timeSlot, true),
                    (timeSlot, rookiesOnly) -> timeSlots.getLastContributorsPerWeek(timeSlot, false), 14);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    void addContributorsPerDay() {
        int limit = 180;
        List<ContributionTimeSlot> contributorsPerDay = LandscapeReportContributorsTab.getContributionDays(timeSlots.scopedDay(),
                limit, landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerDay.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerDay.size() > 0) {
            if (contributorsPerDay.size() > limit) {
                contributorsPerDay = contributorsPerDay.subList(0, limit);
            }

            landscapeReport.startDiv("overflow: auto");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerDay.size() >= 4 ? 4 : contributorsPerDay.size();

            addChartRows(contributorsPerDay, "days", minMaxWindow,
                    (timeSlot, rookiesOnly) -> timeSlots.getContributorsPerDay(timeSlot, rookiesOnly),
                    (timeSlot, rookiesOnly) -> timeSlots.getLastContributorsPerDay(timeSlot, true),
                    (timeSlot, rookiesOnly) -> timeSlots.getLastContributorsPerDay(timeSlot, false), 14);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    // Writes the per-month racing bar charts to disk. Scope-independent (always all-scope) and emits
    // files, so it runs once from addContributionTrends rather than inside each scope panel's render.
    void exportMonthlyRacingCharts() {
        new RacingRepositoriesBarChartsExporter(landscapeAnalysisResults, landscapeAnalysisResults.getContributorsPerRepositoryAndMonth(), "repositories").exportRacingChart(reportsFolder);
        new RacingRepositoriesBarChartsExporter(landscapeAnalysisResults, landscapeAnalysisResults.getContributorsCommits(), "contributors").exportRacingChart(reportsFolder);
    }

    void addContributorsPerMonth() {
        int limit = 24;
        List<ContributionTimeSlot> monthlyContributions = timeSlots.scopedMonth();
        List<ContributionTimeSlot> contributorsPerMonth = LandscapeReportContributorsTab.getContributionMonths(monthlyContributions,
                limit, landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerMonth.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerMonth.size() > 0) {
            if (contributorsPerMonth.size() > limit) {
                contributorsPerMonth = contributorsPerMonth.subList(0, limit);
            }

            landscapeReport.startDiv("overflow: auto");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerMonth.size() >= 3 ? 3 : contributorsPerMonth.size();

            addChartRows(contributorsPerMonth, "months", minMaxWindow, (timeSlot, rookiesOnly) -> timeSlots.getContributorsPerMonth(timeSlot, rookiesOnly),
                    (timeSlot, rookiesOnly) -> timeSlots.getLastContributorsPerMonth(timeSlot, true),
                    (timeSlot, rookiesOnly) -> timeSlots.getLastContributorsPerMonth(timeSlot, false), 40);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    void addContributorsPerYear() {
        List<ContributionTimeSlot> yearlyContributions = landscapeAnalysisResults.getContributorsPerYear();
        List<ContributionTimeSlot> contributorsPerYear = LandscapeReportContributorsTab.getContributionYears(yearlyContributions,
                landscapeAnalysisResults.getConfiguration().getCommitsMaxYears(), landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerYear.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerYear.size() > 0) {
            landscapeReport.startDiv("overflow: auto");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerYear.size() >= 3 ? 3 : contributorsPerYear.size();

            addChartRows(contributorsPerYear, "years", minMaxWindow, (timeSlot, rookiesOnly) -> timeSlots.getSignificantContributorsPerYear(contributors, timeSlot, rookiesOnly, landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear()),
                    (timeSlot, rookiesOnly) -> timeSlots.getLastContributorsPerYear(timeSlot, true),
                    (timeSlot, rookiesOnly) -> timeSlots.getLastContributorsPerYear(timeSlot, false), 64);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addChartRows(List<ContributionTimeSlot> contributorsPerWeek, String unit, int minMaxWindow, ContributorsExtractor contributorsExtractor, ContributorsExtractor firstContributorsExtractor, ContributorsExtractor lastContributorsExtractor, int barWidth) {
        addTickMarksPerWeekRow(contributorsPerWeek, barWidth);
        addChurnPerTimeUnitRow(contributorsPerWeek, barWidth);
        addCommitsPerWeekRow(contributorsPerWeek, minMaxWindow, barWidth);
        addContributorsPerWeekRow(contributorsPerWeek, contributorsExtractor);
        int maxContributors = contributorsPerWeek.stream().mapToInt(c -> contributorsExtractor.getContributors(c.getTimeSlot(), false).size()).max().orElse(1);
        addContributorsPerTimeUnitRow(contributorsPerWeek, firstContributorsExtractor, maxContributors, true, "bottom");
        addContributorsPerTimeUnitRow(contributorsPerWeek, lastContributorsExtractor, maxContributors, false, "top");
    }

    private void addContributorsPerWeekRow(List<ContributionTimeSlot> contributorsPerWeek, ContributorsExtractor contributorsExtractor) {
        landscapeReport.startTableRow();
        int max = 1;
        for (ContributionTimeSlot contributionTimeSlot : contributorsPerWeek) {
            max = Math.max(contributorsExtractor.getContributors(contributionTimeSlot.getTimeSlot(), false).size(), max);
        }
        int maxContributors = max;
        landscapeReport.addTableCell("<b>Contributors</b>" +
                "<div style='font-size: 80%; margin-left: 8px'><div style='color: green'>rookies</div><div style='color: #588BAE'>veterans</div></div>", "border: none");
        contributorsPerWeek.forEach(week -> {
            landscapeReport.startTableCell("max-width: 20px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px");
            List<String> extractedContributors = contributorsExtractor.getContributors(week.getTimeSlot(), false);
            List<String> rookies = contributorsExtractor.getContributors(week.getTimeSlot(), true);
            int count = extractedContributors.size();
            int rookiesCount = rookies.size();
            int height = 2 + (int) (64.0 * count / maxContributors);
            int heightRookies = 1 + (int) (64.0 * rookiesCount / maxContributors);
            String title = "period " + week.getTimeSlot() + " = " + count + " extractedContributors (" + rookiesCount + " rookies):\n\n" +
                    HtmlEscapeUtils.escape(extractedContributors.subList(0, extractedContributors.size() < 200 ? extractedContributors.size() : 200).stream().collect(Collectors.joining(", ")));
            String yearString = week.getTimeSlot().split("[-]")[0];

            String color = "darkgrey";

            if (StringUtils.isNumeric(yearString)) {
                int year = Integer.parseInt(yearString);
                color = year % 2 == 0 ? "#89CFF0" : "#588BAE";
            }

            landscapeReport.addHtmlContent("<div>");
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; color: grey; font-size: 80%; margin: 1px'>" + count + "</div>");
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: green; height:" + (heightRookies) + "px; margin: 1px'></div>");
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + (height - heightRookies) + "px; margin: 1px'></div>");
            landscapeReport.addHtmlContent("</div>");
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
    }

    private void addContributorsPerTimeUnitRow(List<ContributionTimeSlot> contributorsPerWeek, ContributorsExtractor contributorsExtractor, int maxContributors, boolean first, final String valign) {
        landscapeReport.startTableRow();
        landscapeReport.addTableCell("<b>" + (first ? "First" : "Last") + " Contribution</b>" +
                "<div style='color: grey; font-size: 80%; margin-left: 8px; margin-top: 4px;'>"
                + "</div>", "border: none; vertical-align: " + (first ? "bottom" : "top"));
        boolean firstItem[] = {true};
        contributorsPerWeek.forEach(timeUnit -> {
            addTimeUnitCell(timeUnit, contributorsExtractor, maxContributors, first, valign, firstItem[0]);
            firstItem[0] = false;
        });
        landscapeReport.endTableRow();
    }

    /** One time unit's bar (height by contributor count, colour by year parity) with the count above (first) or below (last) it. */
    private void addTimeUnitCell(ContributionTimeSlot timeUnit, ContributorsExtractor contributorsExtractor, int maxContributors, boolean first, String valign, boolean firstItem) {
        landscapeReport.startTableCell("max-width: 20px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: " + valign + "; font-size: 80%; height: 100px");
        List<String> extractedContributors = contributorsExtractor.getContributors(timeUnit.getTimeSlot(), true);
        int count = extractedContributors.size();
        int height = 4 + (int) (64.0 * count / maxContributors);
        String title = "timeUnit of " + timeUnit.getTimeSlot() + " = " + count + " extractedContributors:\n\n" +
                HtmlEscapeUtils.escape(extractedContributors.subList(0, extractedContributors.size() < 200 ? extractedContributors.size() : 200).stream().collect(Collectors.joining(", ")));
        String yearString = timeUnit.getTimeSlot().split("[-]")[0];

        String color = "lightgrey";
        if (count > 0 && StringUtils.isNumeric(yearString)) {
            color = timeUnitBarColor(Integer.parseInt(yearString), first, firstItem);
        } else {
            height = 1;
        }

        String countLabel = "<div title='" + title + "' style='width: 100%; color: grey; font-size: 80%; margin: 1px'>" + count + "</div>";
        if (first && count > 0) {
            landscapeReport.addHtmlContent(countLabel);
        }
        landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + height + "px; margin: 1px'></div>");
        if (!first && count > 0) {
            landscapeReport.addHtmlContent(countLabel);
        }
        landscapeReport.endTableCell();
    }

    /** Greens for first contributions, reds for last ones (the first slot of the last-contribution row greyed), alternating by year. */
    private static String timeUnitBarColor(int year, boolean first, boolean firstItem) {
        if (first) {
            return year % 2 == 0 ? "limegreen" : "darkgreen";
        }
        if (firstItem) {
            return "rgba(220,220,220,100)";
        }
        return year % 2 == 0 ? "crimson" : "rgba(100,0,0,100)";
    }

    private void addTickMarksPerWeekRow(List<ContributionTimeSlot> contributorsPerWeek, int barWidth) {
        landscapeReport.startTableRow();
        landscapeReport.addTableCell("", "border: none");

        for (int i = 0; i < contributorsPerWeek.size(); i++) {
            ContributionTimeSlot week = contributorsPerWeek.get(i);

            String yearString = week.getTimeSlot().split("[-]")[0];

            String color = "darkgrey";

            if (StringUtils.isNumeric(yearString)) {
                int year = Integer.parseInt(yearString);
                color = year % 2 == 0 ? "#c9c9c9" : "#656565";
            }
            String[] splitNow = week.getTimeSlot().split("-");
            String textNow = splitNow.length < 2 ? splitNow[0] : splitNow[0] + "<br>" + splitNow[1];

            int colspan = 1;

            while (true) {
                String nextTimeSlot = contributorsPerWeek.size() > i + 1 ? contributorsPerWeek.get(i + 1).getTimeSlot() : "";
                String[] splitNext = nextTimeSlot.split("-");
                String textNext = splitNext.length < 2 ? "" : splitNext[0] + "<br>" + splitNext[1];
                if (contributorsPerWeek.size() <= i + 1 || !textNow.equalsIgnoreCase(textNext)) {
                    break;
                }
                colspan++;
                i++;
            }
            landscapeReport.startTableCellColSpan(colspan, "width: "
                    + barWidth + "px; min-width: "
                    + barWidth + "px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 16px");
            landscapeReport.addHtmlContent("<div style='width: 100%; margin: 1px; font-size: 80%; color: '" + color + ">"
                    + textNow + "</div>");
            landscapeReport.endTableCell();
        }
        landscapeReport.endTableRow();
    }

    private void addCommitsPerWeekRow(List<ContributionTimeSlot> contributorsPerWeek, int minMaxWindow, int barWidth) {
        landscapeReport.startTableRow();
        int maxCommits = contributorsPerWeek.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);
        int maxCommits4Weeks = contributorsPerWeek.subList(0, minMaxWindow).stream().mapToInt(c -> c.getCommitsCount()).max().orElse(0);
        int minCommits4Weeks = contributorsPerWeek.subList(0, minMaxWindow).stream().mapToInt(c -> c.getCommitsCount()).min().orElse(0);
        landscapeReport.addTableCell("<b>Commits</b>" +
                "<div style='color: grey; font-size: 80%; margin-left: 8px; margin-top: 4px;'>"
                + "min (" + minMaxWindow + " weeks): " + minCommits4Weeks
                + "<br>max (" + minMaxWindow + " weeks): " + maxCommits4Weeks + "</div>", "border: none");
        contributorsPerWeek.forEach(week -> {
            landscapeReport.startTableCell("width: " + barWidth + "px; min-width: " + barWidth + "px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px");
            int count = week.getCommitsCount();
            int height = 1 + (int) (64.0 * count / maxCommits);
            String title = "week of " + week.getTimeSlot() + " = " + count + " commits";
            String yearString = week.getTimeSlot().split("[-]")[0];

            String color = "darkgrey";

            if (StringUtils.isNumeric(yearString)) {
                int year = Integer.parseInt(yearString);
                color = year % 2 == 0 ? "#c9c9c9" : "#656565";
            }

            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; color: grey; font-size: 70%; margin: 0px'>" + count + "</div>");
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + height + "px; margin: 1px'></div>");
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
    }

    private static final String CHURN_ADDED_COLOR = "#2e7d32";
    private static final String CHURN_DELETED_COLOR = "#c62828";
    private static final int CHURN_HALF_HEIGHT = 48;
    private static final int CHURN_LABEL_HEIGHT = 14;

    // A row of diverging line-churn bars per time slot: additions grow UP (green) from a centred zero
    // baseline with the +added count just above the bar, deletions grow DOWN (red) below it with the
    // -deleted count just under. Both sides share one scale (the largest single-side value) so equal
    // magnitudes draw equal lengths. Mirrors the per-repository churn chart. Labels use the compact
    // K/M format. No-op when there is no churn data (older analyses).
    private void addChurnPerTimeUnitRow(List<ContributionTimeSlot> slots, int barWidth) {
        int maxChurn = slots.stream().mapToInt(c -> Math.max(c.getLinesAdded(), c.getLinesDeleted())).max().orElse(0);
        if (maxChurn <= 0) {
            return;
        }
        landscapeReport.startTableRow();
        landscapeReport.addTableCell("<b>Line churn</b>" +
                "<div style='font-size: 80%; margin-left: 8px'><div style='color: " + CHURN_ADDED_COLOR + "'>added</div><div style='color: " + CHURN_DELETED_COLOR + "'>deleted</div></div>", "border: none");
        slots.forEach(slot -> {
            landscapeReport.startTableCell("width: " + barWidth + "px; min-width: " + barWidth + "px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: middle; font-size: 80%");
            int added = slot.getLinesAdded();
            int deleted = slot.getLinesDeleted();
            int heightAdded = added > 0 ? 1 + (int) ((CHURN_HALF_HEIGHT - 1) * added / (double) maxChurn) : 0;
            int heightDeleted = deleted > 0 ? 1 + (int) ((CHURN_HALF_HEIGHT - 1) * deleted / (double) maxChurn) : 0;
            String title = slot.getTimeSlot() + ": +" + added + " / -" + deleted + " lines";
            addDivergingChurnCell(added, deleted, heightAdded, heightDeleted, title);
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
    }

    // Emits one diverging churn cell body (top half = +added label over an up-bar resting on the
    // baseline; a 1px baseline; bottom half = a down-bar hanging from the baseline over the -deleted
    // label). Fixed half/label heights keep the baseline at a constant position across slots so it
    // never collapses or disappears. Shared by both landscape churn charts.
    private void addDivergingChurnCell(int added, int deleted, int heightAdded, int heightDeleted, String title) {
        String addedLabel = added > 0 ? "+" + FormattingUtils.getSmallTextForNumber(added) : "&nbsp;";
        String deletedLabel = deleted > 0 ? "-" + FormattingUtils.getSmallTextForNumber(deleted) : "&nbsp;";
        landscapeReport.addHtmlContent("<div title='" + title + "' style='height: " + (CHURN_HALF_HEIGHT + CHURN_LABEL_HEIGHT)
                + "px; display: flex; flex-direction: column; justify-content: flex-end; align-items: center'>");
        landscapeReport.addHtmlContent("<div style='height: " + CHURN_LABEL_HEIGHT + "px; font-size: 70%; line-height: " + CHURN_LABEL_HEIGHT + "px; white-space: nowrap; color: " + CHURN_ADDED_COLOR + "'>" + addedLabel + "</div>");
        landscapeReport.addHtmlContent("<div style='width: 100%; background-color: " + CHURN_ADDED_COLOR + "; opacity: 0.7; height:" + heightAdded + "px'></div>");
        landscapeReport.addHtmlContent("</div>");
        landscapeReport.addHtmlContent("<div style='width: 100%; height: 1px; background-color: #999999'></div>");
        landscapeReport.addHtmlContent("<div title='" + title + "' style='height: " + (CHURN_HALF_HEIGHT + CHURN_LABEL_HEIGHT)
                + "px; display: flex; flex-direction: column; justify-content: flex-start; align-items: center'>");
        landscapeReport.addHtmlContent("<div style='width: 100%; background-color: " + CHURN_DELETED_COLOR + "; opacity: 0.7; height:" + heightDeleted + "px'></div>");
        landscapeReport.addHtmlContent("<div style='height: " + CHURN_LABEL_HEIGHT + "px; font-size: 70%; line-height: " + CHURN_LABEL_HEIGHT + "px; white-space: nowrap; color: " + CHURN_DELETED_COLOR + "'>" + deletedLabel + "</div>");
        landscapeReport.addHtmlContent("</div>");
    }

    // Per-year churn row for the "Overall Activity Per Year" chart: a "line churn" trend card + a
    // diverging +added(green, up)/-deleted(red, down) bar per year around a shared zero baseline,
    // abbreviated labels. Mirrors the per-repository churn chart. No-op without churn data.
    private void addChurnPerYearRow(List<ContributionTimeSlot> contributorsPerYear, String style) {
        int maxChurn = contributorsPerYear.stream().mapToInt(y -> Math.max(y.getLinesAdded(), y.getLinesDeleted())).max().orElse(0);
        if (maxChurn <= 0) {
            return;
        }
        // Diverging bars are centred on the baseline, so this row is middle-aligned.
        String churnStyle = style.replace("vertical-align: bottom", "vertical-align: middle");
        landscapeReport.startTableRow();
        landscapeReport.startTableCell("border: none; height: 130px; vertical-align: middle;");
        int totalAdded = contributorsPerYear.stream().mapToInt(ContributionTimeSlot::getLinesAdded).sum();
        int totalDeleted = contributorsPerYear.stream().mapToInt(ContributionTimeSlot::getLinesDeleted).sum();
        addActivityTrendCard("<span style='font-size: 15px;'>"
                        + "<span style='color: " + CHURN_ADDED_COLOR + ";'>+" + FormattingUtils.getSmallTextForNumber(totalAdded) + "</span>"
                        + "<br><span style='color: " + CHURN_DELETED_COLOR + ";'>-" + FormattingUtils.getSmallTextForNumber(totalDeleted) + "</span></span>",
                "line churn", "lines_churn");
        landscapeReport.endTableCell();
        contributorsPerYear.forEach(year -> {
            landscapeReport.startTableCell(churnStyle);
            int added = year.getLinesAdded();
            int deleted = year.getLinesDeleted();
            int heightAdded = added > 0 ? 1 + (int) ((CHURN_HALF_HEIGHT - 1) * added / (double) maxChurn) : 0;
            int heightDeleted = deleted > 0 ? 1 + (int) ((CHURN_HALF_HEIGHT - 1) * deleted / (double) maxChurn) : 0;
            String title = year.getTimeSlot() + ": +" + added + " / -" + deleted + " lines";
            addDivergingChurnCell(added, deleted, heightAdded, heightDeleted, title);
            landscapeReport.endTableCell();
        });
        landscapeReport.endTableRow();
    }

}
