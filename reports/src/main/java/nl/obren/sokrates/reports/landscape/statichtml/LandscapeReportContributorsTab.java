/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
import nl.obren.sokrates.reports.core.ReportConstants;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

public class LandscapeReportContributorsTab {

    private List<RichTextReport> individualReports = new ArrayList<>();
    private List<RichTextReport> botReports = new ArrayList<>();

    enum Type {
        CONTRIBUTORS("contributor", "contributors", true),
        TEAMS("team", "teams", false);
        private final String singular;
        private final String plural;
        private final boolean showBots;

        Type(String singular, String plural, boolean showBots) {
            this.singular = singular;
            this.plural = plural;
            this.showBots = showBots;
        }

        public String singular() {
            return singular;
        }

        boolean showBots() {
            return showBots;
        }

        public String plural() {
            return plural;
        }
    }

    private static final Log LOG = LogFactory.getLog(LandscapeReportContributorsTab.class);
    public static final String PEOPLE_COLOR = "#ADD8E6";
    private final List<ContributorRepositories> contributors;
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private File folder;
    private File reportsFolder;
    private ContributorTimeSlots timeSlots;
    private RichTextReport landscapeReport;
    private ContributorActivityCharts charts;
    private ContributorsPerExtensionSection perExtensionSection;
    private ContributorReportPages reportPages;
    private final Type type;
    private final TeamsConfig teamsConfig;

    public LandscapeReportContributorsTab(LandscapeAnalysisResults landscapeAnalysisResults, List<ContributorRepositories> contributors, RichTextReport landscapeReport, File folder, File reportsFolder, Type type, TeamsConfig teamsConfig) {
        this.contributors = contributors;
        this.folder = folder;
        this.reportsFolder = reportsFolder;
        this.landscapeReport = landscapeReport;
        this.type = type;
        this.teamsConfig = teamsConfig;

        this.landscapeAnalysisResults = landscapeAnalysisResults;

        this.timeSlots = new ContributorTimeSlots(contributors, landscapeAnalysisResults);
        timeSlots.populateTimeSlotMaps();
    }

    void addContributorsTabs(String tabId) {
        int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount(contributors);
        landscapeReport.startTabContentSection(tabId, false);
        ProcessingStopwatch.start("reporting/summary");
        LOG.info("Adding big contributors summary...");
        addBigContributorsSummary();

        List<ContributorRepositories> recentContributors = landscapeAnalysisResults.getRecentContributors(contributors);
        addContributorsListsSection(recentContributorsCount, landscapeAnalysisResults.getLatestCommitDate(), recentContributors);

        if (recentContributorsCount > 0) {
            perExtensionSection().addContributorsPerExtension(true);
        }
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesContributorsAtStart());
        LOG.info("Adding contributors...");
        addContributors();
        if (isContributorReport()) {
            perExtensionSection().addContributorsPerExtension();
        }

        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesContributors());
        ProcessingStopwatch.end("reporting/summary");
        landscapeReport.endTabContentSection();
    }

    // The contribution trends (per year / month / week / day) live in their own top-level
    // "Activity" tab (they used to close the Contributors tab under a "Contribution Trends"
    // header). Called on the contributors instance only (teams have no trends).
    void addActivityTab(String tabId) {
        landscapeReport.startTabContentSection(tabId, false);
        LOG.info("Adding trends...");
        ProcessingStopwatch.start("reporting/activity trends");
        addContributionTrends();
        ProcessingStopwatch.end("reporting/activity trends");
        landscapeReport.endTabContentSection();
    }

    public static List<ContributionTimeSlot> getContributionDays(List<ContributionTimeSlot> contributorsPerDayOriginal, int pastDays, String lastCommitDate) {
        List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>(contributorsPerDayOriginal);
        List<String> slots = contributorsPerDay.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastDays(pastDays, lastCommitDate);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerDay.add(new ContributionTimeSlot(pastDate, Thresholds.defaultCommitFilesCountThresholds()));
            }
        });
        return contributorsPerDay;
    }

    public static List<ContributionTimeSlot> getContributionWeeks(List<ContributionTimeSlot> contributorsPerWeekOriginal, int pastWeeks, String lastCommitDate) {
        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>(contributorsPerWeekOriginal);
        List<String> slots = contributorsPerWeek.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastWeeks(pastWeeks, lastCommitDate);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate, Thresholds.defaultCommitFilesCountThresholds()));
            }
        });
        return contributorsPerWeek;
    }

    public static List<ContributionTimeSlot> getContributionYears(List<ContributionTimeSlot> contributorsPerWeekOriginal, int pastYears, String lastCommitDate) {
        List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>(contributorsPerWeekOriginal);
        List<String> slots = contributorsPerWeek.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastYears(pastYears, lastCommitDate);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerWeek.add(new ContributionTimeSlot(pastDate, Thresholds.defaultCommitFilesCountThresholds()));
            }
        });
        return contributorsPerWeek;
    }

    public static List<ContributionTimeSlot> getContributionMonths(List<ContributionTimeSlot> contributorsPerMonthOriginal, int pastMonths, String lastCommitDate) {
        List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>(contributorsPerMonthOriginal);
        List<String> slots = contributorsPerMonth.stream().map(slot -> slot.getTimeSlot()).collect(Collectors.toCollection(ArrayList::new));
        List<String> pastDates = DateUtils.getPastMonths(pastMonths, lastCommitDate);
        pastDates.forEach(pastDate -> {
            if (!slots.contains(pastDate)) {
                contributorsPerMonth.add(new ContributionTimeSlot(pastDate, Thresholds.defaultCommitFilesCountThresholds()));
            }
        });
        return contributorsPerMonth;
    }

    private void addBigContributorsSummary() {
        long contributorsCount = contributors.size();
        int mainLocActive = landscapeAnalysisResults.getMainLoc1YearActive();
        int mainLocNew = landscapeAnalysisResults.getMainLocNew();
        if (contributorsCount > 0) {
            int recentContributorsCount = landscapeAnalysisResults.getRecentContributorsCount(contributors);
            int locPerRecentContributor = 0;
            int locNewPerRecentContributor = 0;
            if (recentContributorsCount > 0) {
                locPerRecentContributor = (int) Math.round((double) mainLocActive / recentContributorsCount);
                locNewPerRecentContributor = (int) Math.round((double) mainLocNew / recentContributorsCount);
            }
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(recentContributorsCount), "recent " + type.plural(),
                    "(past 30 days)", getExtraPeopleInfo(contributors, contributorsCount) + "\n" + FormattingUtils.formatCount(locPerRecentContributor) + " active lines of code per recent " + type.singular());
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getRecentContributorsCount3Months(contributors)), "3m " + type.plural(),
                    "(past 90 days)", getExtraPeopleInfo(contributors, contributorsCount));
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getRecentContributorsCount6Months(contributors)), "6m " + type.plural(),
                    "(past 180 days)", getExtraPeopleInfo(contributors, contributorsCount));
            int rookiesContributorsCount = landscapeAnalysisResults.getRookiesContributorsCount(contributors);
            addPeopleInfoBlock(FormattingUtils.getSmallTextForNumber(rookiesContributorsCount),
                    ("rookie " + type.plural()),
                    "(started in past year)", "active contributors with the first commit in past year");
            addWorkloadInfoBlock(FormattingUtils.getSmallTextForNumber(locPerRecentContributor), type.singular() + " load",
                    "(active LOC/" + type.singular() + ")", "active lines of code per recent " + type.singular() + "\n\n" + FormattingUtils.getPlainTextForNumber(locNewPerRecentContributor) + " new LOC/recent " + type.singular());
            List<ComponentDependency> peopleDependencies = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 30);
            peopleDependencies.sort((a, b) -> b.getCount() - a.getCount());
        }
    }

    private void addContributionTrends() {
        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int commitsMaxYears = configuration.getCommitsMaxYears();
        int significantContributorMinCommitDaysPerYear = configuration.getSignificantContributorMinCommitDaysPerYear();

        landscapeReport.startDiv("margin: 12px");
        landscapeReport.addParagraph("latest commit date: <b>" + landscapeAnalysisResults.getLatestCommitDate() + "</b>", "color: grey");

        // Racing charts are scope-independent and write files; emit them once before the scope panels.
        charts().exportMonthlyRacingCharts();

        // Scope selector wrapping the time-based activity diagrams (Year/Month/Week/Day). The churn and
        // commits rows filter by scope from the landscape per-scope aggregates; the contributor-count and
        // first/last rows filter via the per-scope time-slot maps (timeSlots.getCurrentScope()). The per-extension
        // section below is extension-based (not scopeable), so it stays outside the toggle. When no
        // per-scope contributor data exists (older analyses) only the "All" panel is shown.
        java.util.LinkedHashMap<String, Runnable> scopePanels = new java.util.LinkedHashMap<>();
        java.util.List<String> availableScopes = getAvailableContributorScopes();
        availableScopes.forEach(scope -> {
            String label = nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils.SCOPE_LABELS.get(scope);
            scopePanels.put(label, () -> renderActivityDiagramsForScope(scope, commitsMaxYears, significantContributorMinCommitDaysPerYear));
        });
        scopePanels.put("All", () -> renderActivityDiagramsForScope(ContributorTimeSlots.ALL_SCOPE, commitsMaxYears, significantContributorMinCommitDaysPerYear));

        landscapeReport.startDiv("padding: 5px; border: 1px dashed #ccc; margin-bottom: 20px");
        nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils.addScopeToggle(landscapeReport, "landscape_activity_scope", scopePanels);
        landscapeReport.endDiv();

        landscapeReport.startSubSection("Activity Per Year &amp; File Extension", "commits");
        landscapeReport.startDiv("max-height: 600px; overflow-y: auto;");
        landscapeReport.startDiv("margin-bottom: 16px; vertical-align: middle;");
        landscapeReport.addContentInDiv(ReportConstants.ANIMATION_SVG_ICON, "display: inline-block; vertical-align: middle; margin: 4px;");
        landscapeReport.addHtmlContent("animated commit history: ");
        landscapeReport.addNewTabLink("all time cumulative", "visuals/racing_charts_extensions_commits.html?tickDuration=600");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("12 months window", "visuals/racing_charts_extensions_commits_window.html?tickDuration=600");
        landscapeReport.endDiv();
        List<NumericMetric> linesOfCodePerExtensionMain = LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, landscapeAnalysisResults.getMainLinesOfCodePerExtension());
        List<String> extensions = linesOfCodePerExtensionMain.stream().map(loc -> loc.getName().replaceAll(".*[.]", "").trim()).collect(Collectors.toList());
        List<HistoryPerExtension> yearlyCommitHistoryPerExtension = landscapeAnalysisResults.getYearlyCommitHistoryPerExtension();
        HistoryPerLanguageGenerator.getInstanceCommits(yearlyCommitHistoryPerExtension, extensions).addHistoryPerLanguage(landscapeReport);
        new RacingLanguagesBarChartsExporter(landscapeAnalysisResults, yearlyCommitHistoryPerExtension, extensions).exportRacingChart(reportsFolder);
        landscapeReport.endDiv();
        landscapeReport.endSection();

        landscapeReport.endDiv();
    }

    // Renders the four time-based activity diagrams (Year/Month/Week/Day) for a single scope. Sets
    // timeSlots.getCurrentScope() so the contributor-row getters read that scope's maps, then restores ContributorTimeSlots.ALL_SCOPE.
    private void renderActivityDiagramsForScope(String scope, int commitsMaxYears, int significantContributorMinCommitDaysPerYear) {
        String previousScope = timeSlots.getCurrentScope();
        timeSlots.setCurrentScope(scope);
        try {
            landscapeReport.startSubSection("Overall Activity Per Year", "Past " + commitsMaxYears + " years");
            charts().addContributorsPerYear(true);
            landscapeReport.startDetailsBlock("significant contributions per year (" + significantContributorMinCommitDaysPerYear + "+ commit days per year)...");
            charts().addContributorsPerYear();
            landscapeReport.endDetailsBlock();
            landscapeReport.endSection();

            landscapeReport.startDetailsBlock("Activity per month...");
            landscapeReport.startSubSection("Activity Per Month", "Past two years");
            charts().addContributorsPerMonth();
            landscapeReport.endSection();
            landscapeReport.endDetailsBlock();

            landscapeReport.startDetailsBlock("Activity per week...");
            landscapeReport.startSubSection("Activity Per Week", "Past two years");
            charts().addContributorsPerWeek();
            landscapeReport.endSection();
            landscapeReport.endDetailsBlock();

            landscapeReport.startDetailsBlock("Activity per day...");
            landscapeReport.startSubSection("Activity Per Day", "Past six months");
            charts().addContributorsPerDay();
            landscapeReport.endSection();
            landscapeReport.endDetailsBlock();
        } finally {
            timeSlots.setCurrentScope(previousScope);
        }
    }

    private void addIFrames(List<WebFrameLink> iframes) {
        if (iframes.size() > 0) {
            iframes.forEach(iframe -> {
                addIFrame(iframe);
            });
        }
    }

    private void addIFrame(WebFrameLink iframe) {
        if (StringUtils.isNotBlank(iframe.getTitle())) {
            String title;
            if (StringUtils.isNotBlank(iframe.getMoreInfoLink())) {
                title = "<a href='" + iframe.getMoreInfoLink() + "' target='_blank' style='text-decoration: none'>" + iframe.getTitle() + "</a>";
                title += "&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON;
            } else {
                title = iframe.getTitle();
            }
            landscapeReport.startSubSectionNoMargins(title, "");
        }
        String style = StringUtils.defaultIfBlank(iframe.getStyle(), "width: 100%; height: 200px; border: 1px solid lightgrey;");
        landscapeReport.addHtmlContent("<iframe src='" + iframe.getSrc()
                + "' frameborder='0' style='" + style + "'"
                + (iframe.getScrolling() ? "" : " scrolling='no' ")
                + "></iframe>");
        if (StringUtils.isNotBlank(iframe.getTitle())) {
            landscapeReport.endSection();
        }
    }

    private void addContributors() {
        ProcessingStopwatch.start("reporting/contributors");
        int contributorsCount = landscapeAnalysisResults.getContributorsCount(contributors);

        if (contributorsCount > 0) {
            ProcessingStopwatch.start("reporting/contributors/preparing");

            List<ContributorRepositories> bots = landscapeAnalysisResults.getBots();
            Collections.sort(bots, (a, b) -> b.getContributor().getCommitsCount180Days() - a.getContributor().getCommitsCount180Days());
            Collections.sort(bots, (a, b) -> b.getContributor().getCommitsCount90Days() - a.getContributor().getCommitsCount90Days());
            Collections.sort(bots, (a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days());
            List<ContributorRepositories> recentContributors = landscapeAnalysisResults.getRecentContributors(contributors);
            Collections.sort(recentContributors, (a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days());
            final String[] latestCommit = {""};
            contributors.forEach(c -> {
                if (c.getContributor().getLatestCommitDate().compareTo(latestCommit[0]) > 0) {
                    latestCommit[0] = c.getContributor().getLatestCommitDate();
                }
            });

            ProcessingStopwatch.end("reporting/contributors/table");

            ProcessingStopwatch.start("reporting/contributors/saving tables");
            // The old per-tab server-rendered HTML tables (contributors.html / contributors-recent.html
            // / bots.html / teams.html) are no longer written — the searchable client-rendered
            // contributors-report.html below replaces them. We still compute which contributors are
            // referenced (capped at getContributorsListLimit), since that set gates which individual
            // per-person reports get generated.
            Set<String> contributorsLinkedFromTables = new HashSet<>();
            reportPages().collectLinkedContributors(recentContributors, contributorsLinkedFromTables);
            reportPages().collectLinkedContributors(contributors, contributorsLinkedFromTables);
            reportPages().collectLinkedContributors(bots, contributorsLinkedFromTables);

            // Client-rendered, searchable/sortable contributors report (recent / all / bots tabs).
            reportPages().saveContributorsReportPage(recentContributors, contributors, bots);

            ProcessingStopwatch.end("reporting/contributors/saving tables");

            ProcessingStopwatch.start("reporting/contributors/individual reports");
            List<ContributorRepositories> linkedContributors = contributors.stream()
                    .filter(c -> contributorsLinkedFromTables.contains(c.getContributor().getEmail()))
                    .collect(Collectors.toList());
            LOG.info("Saving individual reports for " + linkedContributors.size() + " contributor(s) linked from tables (out of " + contributors.size() + ")");
            List<ContributorRepositories> linkedBots = bots.stream()
                    .filter(c -> contributorsLinkedFromTables.contains(c.getContributor().getEmail()))
                    .collect(Collectors.toList());
            LOG.info("Saving bot reports for " + linkedBots.size() + " contributor(s) linked from tables (out of " + linkedBots.size() + ")");
            // Teams go to team-report.html (their own embedded archive); contributors and bots go
            // to contributor-report.html (shared archive). The TEAMS tab passes teams as its
            // "contributors" list, so isTeam is driven by this tab's type. Bots only exist for the
            // contributors tab.
            boolean isTeam = type == Type.TEAMS;
            individualReports = new LandscapeIndividualContributorsReports(landscapeAnalysisResults, reportsFolder).getIndividualReports(linkedContributors, isTeam);
            botReports = new LandscapeIndividualContributorsReports(landscapeAnalysisResults, reportsFolder).getIndividualReports(linkedBots, false);
            ProcessingStopwatch.end("reporting/contributors/individual reports");
            ProcessingStopwatch.end("reporting/contributors/preparing");
        }
        ProcessingStopwatch.end("reporting/contributors");
    }

    private void addContributorsListsSection(int recentContributorsCount, String latestCommit, List<ContributorRepositories> recentContributors) {
        landscapeReport.startSubSectionNoMargins("<a href='" + type.plural() + "-report.html' target='_blank' style='text-decoration: none'>" +
                        "" + StringUtils.capitalize(type.plural()) + "</a>&nbsp;&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON,
                "latest commit " + latestCommit);

        landscapeReport.addHtmlContent("<iframe src='" + type.plural() + "-report.html?tab=recent' frameborder=0 style='height: 650px; width: 100%; margin-bottom: 0px; padding: 0;'></iframe>");

        landscapeReport.startDetailsBlock("recently active " + StringUtils.lowerCase(type.plural()) + " stats...");

        addRecentContributorLinks();

        DescriptiveStatistics stats = new DescriptiveStatistics();
        recentContributors.forEach(c -> stats.addValue(c.getContributor().getCommitsCount30Days()));
        double max = Math.max(stats.getMax(), 1);
        double sum = Math.max(stats.getSum(), 1);

        ProcessingStopwatch.start("reporting/contributors/table");
        String barsHtml = recentCommitsBarsHtml(recentContributors, recentContributorsCount, max, sum);

        if (isContributorReport()) {
            addCommitsDistribution(recentContributors, recentContributorsCount, stats, max);
        }

        landscapeReport.addParagraph("contributors sorted by recent commits:", "font-size: 70%; margin-top: 12px;");
        landscapeReport.startDiv("white-space: nowrap; width: 100%; overflow-x: scroll;");
        landscapeReport.addHtmlContent(barsHtml);
        landscapeReport.endDiv();

        landscapeReport.endDetailsBlock();

        landscapeReport.endSection();
    }

    /** One bar per recent contributor (at most contributorsListLimit), sized by 30-day commits; the bar crossing 50% of all commits is blue, rookies get a green foot. */
    private String recentCommitsBarsHtml(List<ContributorRepositories> recentContributors, int recentContributorsCount, double max, double sum) {
        int cumulativeCount[] = {0};
        double prevCumulativePercentage[] = {0};
        int index[] = {0};

        StringBuilder barsHtml = new StringBuilder();

        recentContributors.stream().limit(landscapeAnalysisResults.getConfiguration().getContributorsListLimit()).forEach(c -> {
            index[0] += 1;
            Contributor contributor = c.getContributor();
            int count = contributor.getCommitsCount30Days();
            int height = (int) (Math.round(64 * count / max)) + 1;
            cumulativeCount[0] += count;
            // Use floating-point divisors (10.0 / 100.0): Math.round returns a long, so dividing by an
            // int here truncated the intended decimals (e.g. 53.7% rendered as 53%). Matches the
            // correct pattern used for the distribution percentages below.
            double cumulativePercentage = Math.round(1000.0 * cumulativeCount[0] / sum) / 10.0;
            double contributorPercentage = Math.round(10000.0 * index[0] / recentContributorsCount) / 100.0;
            String tooltip = HtmlEscapeUtils.escape(contributor.getEmail())
                    + "\n - commits (30d): " + count
                    + "\n - cumulative commits (top " + index[0] + "): " + cumulativeCount[0]
                    + "\n - cumulative percentage (top " + contributorPercentage + "% " + "): " + cumulativePercentage + "%";
            String color = (prevCumulativePercentage[0] < 50 && cumulativePercentage >= 50) ? "blue" : "skyblue";
            String style = "cursor: help; margin-right: 1px; vertical-align: bottom; width: 8px; background-color: " + color + "; display: inline-block; height: " + height + "px";

            if (contributor.isRookie()) {
                style += "; border-bottom: 4px solid green;";
            } else {
                style += "; border-bottom: 4px solid " + color + ";";
            }

            barsHtml.append("<div title='" + tooltip + "' style='" + style + "'></div>");
            prevCumulativePercentage[0] = cumulativePercentage;
        });
        return barsHtml.toString();
    }

    /** How many contributors made 1, 2, ... max commits (the median count in blue), plus the p90..p10 percentiles. */
    private void addCommitsDistribution(List<ContributorRepositories> recentContributors, int recentContributorsCount, DescriptiveStatistics stats, double max) {
        StringBuilder distHtml = new StringBuilder();

        long most = 1;

        for (int i = 1; i <= max; i++) {
            final int d = i;
            most = Math.max(most, recentContributors.stream().filter(c -> c.getContributor().getCommitsCount30Days() == d).count());
        }

        for (int i = 1; i <= max; i++) {
            final int d = i;
            long count = recentContributors.stream().filter(c -> c.getContributor().getCommitsCount30Days() == d).count();
            long height = count > 0 ? (int) (80.0 * count / most) + 5 : 0;
            double median = stats.getPercentile(50);
            String color = d == median ? "blue" : "#990000";
            String style = "cursor: help; margin-right: 1px; vertical-align: bottom; width: 4px; background-color: " + color + "; display: inline-block; height: " + height + "px";
            String title = count + " contributor(s) (" + (Math.round(10000.0 * count / recentContributorsCount) / 100.0) + "%) with " + d + " commit(s)";
            distHtml.append("<div title='" + title + "' style='" + style + "'></div>");
        }

        landscapeReport.startDiv("white-space: nowrap; width: 100%; overflow-x: scroll;");
        landscapeReport.addParagraph("commits distribution:", "font-size: 70%;");
        landscapeReport.addHtmlContent(distHtml.toString());
        landscapeReport.endDiv();
        landscapeReport.startDiv("color: grey; font-size: 70%");
        landscapeReport.addHtmlContent("commits per contributor | ");
        for (int p = 90; p >= 10; p -= 10) {
            double percentile = stats.getPercentile(p);
            landscapeReport.addHtmlContent("p(" + p + ") = " + (int) Math.round(percentile) + "; ");
        }
        landscapeReport.endDiv();
    }

    private void addContributorLinks() {
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + type.plural() + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + type.plural() + ".html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('" + type.plural() + ".txt')\">txt</a>");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('" + type.plural() + ".json')\">json</a>");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private void addRecentContributorLinks() {
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_" + type.plural() + "_30_days.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_" + type.plural() + "_30_days.html");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
    }

    private String getExtraPeopleInfo(List<ContributorRepositories> contributors, long contributorsCount) {
        String info = "";

        int recentContributorsCount6Months = landscapeAnalysisResults.getRecentContributorsCount6Months(contributors);
        int recentContributorsCount3Months = landscapeAnalysisResults.getRecentContributorsCount3Months(contributors);
        info += FormattingUtils.getPlainTextForNumber(landscapeAnalysisResults.getRecentContributorsCount(contributors)) + " contributors (30 days)\n";
        info += FormattingUtils.getPlainTextForNumber(recentContributorsCount3Months) + " contributors (3 months)\n";
        info += FormattingUtils.getPlainTextForNumber(recentContributorsCount6Months) + " contributors (6 months)\n";

        LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
        int thresholdCommits = configuration.getContributorThresholdCommits();
        info += FormattingUtils.getPlainTextForNumber((int) contributorsCount) + " contributors (all time)\n";
        info += "\nOnly the contributors with " + (thresholdCommits > 1 ? "(" + thresholdCommits + "+&nbsp;commits)" : "") + " included";

        return info;
    }

    private void addPeopleInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        addPeopleInfoBlockWithColor(mainValue, subtitle, description, tooltip, PEOPLE_COLOR);
    }

    private void addWorkloadInfoBlock(String mainValue, String subtitle, String description, String tooltip) {
        addWorkloadInfoBlockWithColor(mainValue, subtitle, description, tooltip, "orange");
    }

    private void addPeopleInfoBlockWithColor(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: #707070; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip, isContributorReport() ? "contributors" : "teams");
    }

    private void addWorkloadInfoBlockWithColor(String mainValue, String subtitle, String description, String tooltip, String color) {
        if (StringUtils.isNotBlank(description)) {
            subtitle += "<br/><span style='color: grey; font-size: 80%'>" + description + "</span>";
        }
        addInfoBlockWithColor(mainValue, subtitle, color, tooltip, "workload");
    }

    private void addInfoBlockWithColor(String mainValue, String subtitle, String color, String tooltip, String icon) {
        InfoBlocks.addInfoBlockWithColor(landscapeReport, mainValue, subtitle, color, tooltip, icon);
    }

    private void addSmallInfoBlock(String value, String subtitle, String color, String link) {
        InfoBlocks.addSmallInfoBlock(landscapeReport, value, subtitle, color, link);
    }

    private ContributorReportPages reportPages() {
        if (reportPages == null) {
            reportPages = new ContributorReportPages(landscapeAnalysisResults, contributors, reportsFolder, type, teamsConfig);
        }
        return reportPages;
    }

    private ContributorsPerExtensionSection perExtensionSection() {
        if (perExtensionSection == null) {
            perExtensionSection = new ContributorsPerExtensionSection(landscapeReport, landscapeAnalysisResults, contributors, reportsFolder, type, teamsConfig);
        }
        return perExtensionSection;
    }

    private ContributorActivityCharts charts() {
        if (charts == null) {
            charts = new ContributorActivityCharts(landscapeReport, landscapeAnalysisResults, contributors, timeSlots, reportsFolder);
        }
        return charts;
    }

    // Returns the scope keys (besides ContributorTimeSlots.ALL_SCOPE) that any contributor carries data for, in the canonical
    // SCOPE_LABELS order. Empty when no per-scope contributor data is present (older analyses).
    java.util.List<String> getAvailableContributorScopes() {
        java.util.Set<String> present = timeSlots.scopesWithData();
        java.util.List<String> ordered = new ArrayList<>();
        nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils.SCOPE_LABELS.keySet().forEach(scope -> {
            if (present.contains(scope)) {
                ordered.add(scope);
            }
        });
        return ordered;
    }

    private boolean isContributorReport() {
        return type == Type.CONTRIBUTORS;
    }

    public List<RichTextReport> getIndividualReports() {
        return individualReports;
    }

    public List<RichTextReport> getBotReports() {
        return botReports;
    }
}
