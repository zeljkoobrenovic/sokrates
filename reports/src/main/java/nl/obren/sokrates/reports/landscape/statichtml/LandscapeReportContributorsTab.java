/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
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

        public String plural() {
            return plural;
        }
    }

    private static final Log LOG = LogFactory.getLog(LandscapeReportContributorsTab.class);
    public static final String PEOPLE_COLOR = "#ADD8E6";
    private final List<ContributorRepositories> contributors;
    private RichTextReport landscapeRecentContributorsReport = new RichTextReport("", "${type}-recent.html");
    private RichTextReport landscapeContributorsReport = new RichTextReport("", "${type}.html");
    private RichTextReport landscapeBotsReport = new RichTextReport("", "bots.html");
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private File folder;
    private File reportsFolder;
    private Map<String, List<String>> contributorsPerWeekMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerWeekMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerDayMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerDayMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerMonthMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerMonthMap = new HashMap<>();
    private Map<String, List<String>> contributorsPerYearMap = new HashMap<>();
    private Map<String, List<String>> rookiesPerYearMap = new HashMap<>();
    private RichTextReport landscapeReport;
    private final Type type;
    private final TeamsConfig teamsConfig;

    public LandscapeReportContributorsTab(LandscapeAnalysisResults landscapeAnalysisResults, List<ContributorRepositories> contributors, RichTextReport landscapeReport, File folder, File reportsFolder, Type type, TeamsConfig teamsConfig) {
        this.contributors = contributors;
        this.folder = folder;
        this.reportsFolder = reportsFolder;
        this.landscapeReport = landscapeReport;
        this.type = type;
        this.teamsConfig = teamsConfig;

        landscapeRecentContributorsReport.setFileName(type.plural() + "-recent.html");
        landscapeContributorsReport.setFileName(type.plural() + ".html");

        this.landscapeAnalysisResults = landscapeAnalysisResults;

        landscapeRecentContributorsReport.setEmbedded(true);
        landscapeContributorsReport.setEmbedded(true);
        landscapeBotsReport.setEmbedded(true);

        populateTimeSlotMaps();

        landscapeContributorsReport.setEmbedded(true);
        landscapeBotsReport.setEmbedded(true);
        landscapeRecentContributorsReport.setEmbedded(true);
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
            addContributorsPerExtension(true);
        }
        addIFrames(landscapeAnalysisResults.getConfiguration().getiFramesContributorsAtStart());
        LOG.info("Adding contributors...");
        addContributors();
        if (isContributorReport()) {
            addContributorsPerExtension();
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

        landscapeReport.startSubSection("Overall Activity Per Year", "Past " + commitsMaxYears + " years");
        addContributorsPerYear(true);
        landscapeReport.startDetailsBlock("significant contributions per year (" + significantContributorMinCommitDaysPerYear + "+ commit days per year)...");
        addContributorsPerYear();
        landscapeReport.endDetailsBlock();
        landscapeReport.endSection();

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


        LOG.info("Adding contributors per extension...");


        landscapeReport.startSubSection("Activity Per Month", "Past two years");
        addContributorsPerMonth();
        landscapeReport.endSection();

        landscapeReport.startSubSection("Activity Per Week", "Past two years");
        addContributorsPerWeek();
        landscapeReport.endSection();

        landscapeReport.startSubSection("Per Day", "Past six months");
        addContributorsPerDay();
        landscapeReport.endSection();

        landscapeReport.endDiv();
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

    private void addContributorsPerExtension(boolean linkCharts) {
        landscapeReport.startSubSection(StringUtils.capitalize(type.plural()) + " Per File Extension", "past 30 days");
        if (linkCharts) {
            landscapeReport.startDiv("");
            landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_extensions_" + type.plural() + "_30d.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("tree map", "visuals/tree_map_extensions_" + type.plural() + "_30d.html");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.endDiv();
        }

        landscapeReport.startDiv("");
        List<String> mainExtensions = getMainExtensions();
        List<CommitsPerExtension> contributorsPerExtension = landscapeAnalysisResults.getContributorsPerExtension()
                .stream().filter(c -> mainExtensions.contains(c.getExtension())).collect(Collectors.toList());
        Collections.sort(contributorsPerExtension, (a, b) -> b.getCommitters30Days().size() - a.getCommitters30Days().size());
        boolean tooLong = contributorsPerExtension.size() > 25;
        List<CommitsPerExtension> contributorsPerExtensionDisplay = tooLong ? contributorsPerExtension.subList(0, 25) : contributorsPerExtension;
        List<CommitsPerExtension> linesOfCodePerExtensionHide = tooLong ? contributorsPerExtension.subList(25, contributorsPerExtension.size()) : new ArrayList<>();

        ExtractStringListValue<CommitsPerExtension> valueFunction;

        if (isContributorReport()) {
            valueFunction = (e) -> e.getCommitters30Days();
        } else {
            valueFunction = (e) -> e.getTeams30Days(teamsConfig);
        }

        contributorsPerExtensionDisplay.stream()
                .filter(e -> e.getCommitters30Days().size() > 0)
                .sorted((a, b) -> b.getCommitsCount30Days() - a.getCommitsCount30Days())
                .sorted((a, b) -> valueFunction.getValue(b).size() - valueFunction.getValue(a).size())
                .forEach(extension -> {
                    addLangInfo(extension, valueFunction, extension.getCommitsCount30Days(), getSvgIcon());
                });

        if (linesOfCodePerExtensionHide.stream().filter(e -> e.getCommitters30Days().size() > 0).count() > 0) {
            landscapeReport.startShowMoreBlockDisappear("", "show all...");
            linesOfCodePerExtensionHide.stream().filter(e -> e.getCommitters30Days().size() > 0).forEach(extension -> {
                addLangInfo(extension, valueFunction, extension.getCommitsCount30Days(), getSvgIcon());
            });
            landscapeReport.endShowMoreBlockDisappear();
        }
        landscapeReport.endDiv();

        addContributorDependencies(contributorsPerExtension);
        landscapeReport.endSection();
    }

    private String getSvgIcon() {
        return isContributorReport() ? DEVELOPER_SVG_ICON : TEAM_SVG_ICON;
    }

    private void addContributorDependencies(List<CommitsPerExtension> contributorsPerExtension) {
        Map<String, List<String>> contrExtMap = new HashMap<>();
        Set<String> extensionsNames = new HashSet<>();
        contributorsPerExtension.stream().filter(e -> e.getCommitters30Days().size() > 0).forEach(commitsPerExtension -> {
            String extensionDisplayLabel = commitsPerExtension.getExtension() + " (" + commitsPerExtension.getCommitters30Days().size() + ")";
            extensionsNames.add(extensionDisplayLabel);
            commitsPerExtension.getCommitters30Days().forEach(contributor -> {
                if (contrExtMap.containsKey(contributor)) {
                    contrExtMap.get(contributor).add(extensionDisplayLabel);
                } else {
                    contrExtMap.put(contributor, new ArrayList<>(Arrays.asList(extensionDisplayLabel)));
                }
            });
        });
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependencyMap = new HashMap<>();

        List<String> mainExtensions = getMainExtensions();
        contrExtMap.values().stream().filter(v -> v.size() > 1).forEach(extensions -> {
            extensions.stream().filter(extension1 -> mainExtensions.contains(extension1.replaceAll("\\(.*\\)", "").trim())).forEach(extension1 -> {
                extensions.stream().filter(extension2 -> mainExtensions.contains(extension2.replaceAll("\\(.*\\)", "").trim())).filter(extension2 -> !extension1.equalsIgnoreCase(extension2)).forEach(extension2 -> {
                    String key1 = extension1 + "::" + extension2;
                    String key2 = extension2 + "::" + extension1;

                    if (dependencyMap.containsKey(key1)) {
                        dependencyMap.get(key1).increment(1);
                    } else if (dependencyMap.containsKey(key2)) {
                        dependencyMap.get(key2).increment(1);
                    } else {
                        ComponentDependency dependency = new ComponentDependency(extension1, extension2);
                        dependencyMap.put(key1, dependency);
                        dependencies.add(dependency);
                    }
                });
            });
        });

        dependencies.forEach(dependency -> dependency.setCount(dependency.getCount() / 2));

        GraphvizDependencyRenderer renderer = new GraphvizDependencyRenderer();
        renderer.setMaxNumberOfDependencies(100);
        renderer.setDefaultNodeFillColor("deepskyblue2");
        renderer.setTypeGraph();
        String graphvizContent = renderer.getMermaidContent(new ArrayList<>(extensionsNames), dependencies);

        if (isContributorReport()) {
            new Force3DGraphExporter().export2D3DForceGraph(dependencies, reportsFolder, "extension_dependencies_30d");

            landscapeReport.startDetailsBlock("extension dependencies...");

            landscapeReport.addGraphvizFigure("extension_dependencies_30d", "Extension dependencies", graphvizContent);
            addDownloadLinks("extension_dependencies_30d");
            landscapeReport.addLineBreak();
            landscapeReport.addNewTabLink(" - show extension dependencies as 2D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/extension_dependencies_30d_force_2d.html");
            landscapeReport.addNewTabLink(" - show extension dependencies as 3D force graph&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON, "visuals/extension_dependencies_30d_force_3d.html");

            landscapeReport.endDetailsBlock();
        }
    }

    private List<String> getMainExtensions() {
        return landscapeAnalysisResults.getMainLinesOfCodePerExtension().stream()
                .map(l -> l.getName().replace("*.", "").trim()).collect(Collectors.toList());
    }

    private void addLangInfo(CommitsPerExtension extension, ExtractStringListValue<CommitsPerExtension> extractor, int commitsCount, String suffix) {
        int size = extractor.getValue(extension).size();
        String smallTextForNumber = FormattingUtils.getSmallTextForNumber(size) + suffix;
        addLangInfoBlockExtra(smallTextForNumber, extension.getExtension().replace("*.", "").trim(),
                size + " " + (size == 1 ? "contributor" : "contributors (" + commitsCount + " commits)") + ":\n" +
                        extractor.getValue(extension).stream().limit(100)
                                .collect(Collectors.joining(", ")), FormattingUtils.getSmallTextForNumber(commitsCount) + " commits");
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
            int totalCommits = contributors.stream().mapToInt(c -> c.getContributor().getCommitsCount()).sum();
            int botCommits = bots.stream().mapToInt(c -> c.getContributor().getCommitsCount()).sum();
            int totalRecentCommits = recentContributors.stream().mapToInt(c -> c.getContributor().getCommitsCount30Days()).sum();
            final String[] latestCommit = {""};
            contributors.forEach(c -> {
                if (c.getContributor().getLatestCommitDate().compareTo(latestCommit[0]) > 0) {
                    latestCommit[0] = c.getContributor().getLatestCommitDate();
                }
            });

            ProcessingStopwatch.end("reporting/contributors/table");

            ProcessingStopwatch.start("reporting/contributors/saving tables");
            Set<String> contributorsLinkedFromTables = new HashSet<>();
            new LandscapeContributorsReport(landscapeAnalysisResults, landscapeRecentContributorsReport, contributorsLinkedFromTables)
                    .saveContributorsTable(recentContributors, totalRecentCommits, true);
            new LandscapeContributorsReport(landscapeAnalysisResults, landscapeContributorsReport, contributorsLinkedFromTables)
                    .saveContributorsTable(contributors, totalCommits, false);
            new LandscapeContributorsReport(landscapeAnalysisResults, landscapeBotsReport, contributorsLinkedFromTables)
                    .saveContributorsTable(bots, botCommits, false);

            // Client-rendered, searchable/sortable contributors report (recent / all / bots tabs).
            saveContributorsReportPage(recentContributors, contributors, bots);

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
        }
        ProcessingStopwatch.end("reporting/contributors");
    }

    /**
     * Renders the client-rendered, searchable/sortable contributors report ({@code &lt;type&gt;-report.html})
     * with Recent / All time / Bots tabs, replacing the separate static contributor tables.
     */
    private void saveContributorsReportPage(List<ContributorRepositories> recentContributors,
                                            List<ContributorRepositories> contributors,
                                            List<ContributorRepositories> bots) {
        try {
            LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
            PeopleConfig peopleConfig = landscapeAnalysisResults.getPeopleConfig();
            List<ContributorTag> tagRules = configuration.getTagContributors();

            Map<String, List<ContributorReportExport>> groups = new LinkedHashMap<>();
            groups.put("recent", toExports(recentContributors, configuration, peopleConfig, tagRules));
            groups.put("all", toExports(contributors, configuration, peopleConfig, tagRules));
            groups.put("bots", toExports(bots, configuration, peopleConfig, tagRules));

            // Language icons for every distinct main language across the three lists.
            List<String> langs = new ArrayList<>();
            groups.values().forEach(list -> list.forEach(e -> langs.add(e.getMainLang())));
            String langIcons = DataImageUtils.getLangDataImageMapJson(langs);

            JsonGenerator jsonGenerator = new JsonGenerator();
            Map<String, Object> optionsData = new LinkedHashMap<>();
            optionsData.put("showBots", type.showBots && !bots.isEmpty());
            optionsData.put("avatarTeam", DataImageUtils.TEAM);
            optionsData.put("avatarDeveloper", DataImageUtils.DEVELOPER);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("langIcons", langIcons);
            placeholders.put("options", jsonGenerator.generateCompressed(optionsData));

            String html = new ExplorerTemplate().render("contributors-report.html", groups, placeholders);
            FileUtils.write(new File(reportsFolder, type.plural() + "-report.html"), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private List<ContributorReportExport> toExports(List<ContributorRepositories> list, LandscapeConfiguration configuration,
                                                    PeopleConfig peopleConfig, List<ContributorTag> tagRules) {
        // Export every contributor (no list-limit cap): the client-rendered report pages the
        // display itself (show-more), and search needs the full set. Sorted by commit recency.
        return list.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount())
                .sorted((a, b) -> b.getContributor().getCommitsCount365Days() - a.getContributor().getCommitsCount365Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount90Days() - a.getContributor().getCommitsCount90Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .map(cr -> new ContributorReportExport(cr, configuration, peopleConfig, teamsConfig, tagRules))
                .collect(Collectors.toList());
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

        int cumulativeCount[] = {0};
        double prevCumulativePercentage[] = {0};
        int index[] = {0};

        StringBuilder barsHtml = new StringBuilder();
        ProcessingStopwatch.end("reporting/contributors/preparing");

        ProcessingStopwatch.start("reporting/contributors/table");
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
            String tooltip = contributor.getEmail()
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

        if (isContributorReport()) {
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

        landscapeReport.addParagraph("contributors sorted by recent commits:", "font-size: 70%; margin-top: 12px;");
        landscapeReport.startDiv("white-space: nowrap; width: 100%; overflow-x: scroll;");
        landscapeReport.addHtmlContent(barsHtml.toString());
        landscapeReport.endDiv();

        landscapeReport.endDetailsBlock();

        landscapeReport.endSection();
    }

    private void addContributorsPerExtension() {
        int commitsCount = landscapeAnalysisResults.getCommitsCount();
        if (commitsCount > 0) {
            List<CommitsPerExtension> perExtension = landscapeAnalysisResults.getContributorsPerExtension();

            if (perExtension.size() > 0) {
                int count = perExtension.size();
                int limit = 100;
                if (perExtension.size() > limit) {
                    perExtension = perExtension.subList(0, limit);
                }
                landscapeReport.startSubSection("Commits & File Extensions (" + count + ")", "");

                landscapeReport.startDetailsBlock("extension stats...");

                landscapeReport.startTable("");
                landscapeReport.addTableHeader("", "Extension",
                        "# contributors<br>30 days", "# commits<br>30 days", "# files<br>30 days",
                        "# contributors<br>90 days", "# commits<br>90 days", "# files<br>90 days",
                        "# contributors", "# commits", "# files");

                perExtension.forEach(commitsPerExtension -> {
                    addCommitExtension(commitsPerExtension);
                });
                landscapeReport.endTable();
                if (perExtension.size() < count) {
                    landscapeReport.addParagraph("Showing top " + limit + " items (out of " + count + ").");
                }

                landscapeReport.endDetailsBlock();

                landscapeReport.endSection();
            }
        }
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

    private void addCommitExtension(CommitsPerExtension commitsPerExtension) {
        landscapeReport.startTableRow(commitsPerExtension.getCommitters30Days().size() > 0 ? "font-weight: bold;"
                : "color: " + (commitsPerExtension.getCommitters90Days().size() > 0 ? "grey" : "lightgrey"));
        String extension = commitsPerExtension.getExtension();
        landscapeReport.addTableCell("" + DataImageUtils.getLangDataImageDiv42(extension), "text-align: center;");
        landscapeReport.addTableCell("" + extension, "text-align: center; max-width: 100px; width: 100px");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters30Days().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount30Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount30Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters90Days().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount90Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount90Days(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitters().size(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getCommitsCount(), "text-align: center;");
        landscapeReport.addTableCell("" + commitsPerExtension.getFilesCount(), "text-align: center;");
        landscapeReport.endTableCell();
        landscapeReport.endTableRow();
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

    private void addLangInfoBlockExtra(String value, String lang, String description, String extra) {
        InfoBlocks.addLangInfoBlockExtra(landscapeReport, value, lang, description, extra);
    }

    private void addSmallInfoBlock(String value, String subtitle, String color, String link) {
        InfoBlocks.addSmallInfoBlock(landscapeReport, value, subtitle, color, link);
    }

    private void addActivityTrendCard(String value, String subtitle, String icon) {
        InfoBlocks.addActivityTrendCard(landscapeReport, value, subtitle, icon);
    }

    private void addContributorsPerYear(boolean showContributorsCount) {
        List<ContributionTimeSlot> contributorsPerYear = landscapeAnalysisResults.getContributorsPerYear();
        if (contributorsPerYear.size() > 0) {
            int limit = landscapeAnalysisResults.getConfiguration().getCommitsMaxYears();
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(0, limit);
            }

            int maxCommits = contributorsPerYear.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);

            landscapeReport.startDiv("overflow-y: none;");
            landscapeReport.startTable();

            landscapeReport.startTableRow();
            landscapeReport.startTableCell("border: none; height: 130px; vertical-align: bottom;");
            int commitsCount = landscapeAnalysisResults.getCommitsCount();
            if (commitsCount > 0) {
                addActivityTrendCard(FormattingUtils.getSmallTextForNumber(commitsCount), "commits", "commits");
            }
            landscapeReport.endTableCell();
            String style = "border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px";
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);
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

            if (showContributorsCount) {
                int maxContributors[] = {1};
                contributorsPerYear.forEach(year -> {
                    int count = getContributorsCountPerYear(year.getTimeSlot());
                    maxContributors[0] = Math.max(maxContributors[0], count);
                });
                landscapeReport.startTableRow();
                landscapeReport.startTableCell("border: none; height: 130px; vertical-align: bottom;");
                int contributorsCount = contributors.size();
                if (contributorsCount > 0) {
                    addActivityTrendCard(FormattingUtils.getSmallTextForNumber(contributorsCount), "contributors", "contributors");
                }
                landscapeReport.endTableCell();
                contributorsPerYear.forEach(year -> {
                    landscapeReport.startTableCell(style);
                    int count = getContributorsCountPerYear(year.getTimeSlot());
                    String color = year.getTimeSlot().equals(thisYear + "") ? "#343434" : "#989898";
                    landscapeReport.addParagraph(count + "", "margin: 2px; color: " + color + ";");
                    int height = 1 + (int) (64.0 * count / maxContributors[0]);
                    landscapeReport.addHtmlContent("<div style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
                    landscapeReport.endTableCell();
                });
                landscapeReport.endTableRow();
            }

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

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addContributorsPerWeek() {
        int limit = 104;
        List<ContributionTimeSlot> contributorsPerWeek = getContributionWeeks(landscapeAnalysisResults.getContributorsPerWeek(),
                limit, landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerWeek.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerWeek.size() > 0) {
            if (contributorsPerWeek.size() > limit) {
                contributorsPerWeek = contributorsPerWeek.subList(0, limit);
            }

            landscapeReport.startDiv("overflow: hidden");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerWeek.size() >= 4 ? 4 : contributorsPerWeek.size();

            addChartRows(contributorsPerWeek, "weeks", minMaxWindow,
                    (timeSlot, rookiesOnly) -> getContributorsPerWeek(timeSlot, rookiesOnly),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerWeek(timeSlot, true),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerWeek(timeSlot, false), 14);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addContributorsPerDay() {
        int limit = 180;
        List<ContributionTimeSlot> contributorsPerDay = getContributionDays(landscapeAnalysisResults.getContributorsPerDay(),
                limit, landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerDay.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerDay.size() > 0) {
            if (contributorsPerDay.size() > limit) {
                contributorsPerDay = contributorsPerDay.subList(0, limit);
            }

            landscapeReport.startDiv("overflow: hidden");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerDay.size() >= 4 ? 4 : contributorsPerDay.size();

            addChartRows(contributorsPerDay, "days", minMaxWindow,
                    (timeSlot, rookiesOnly) -> getContributorsPerDay(timeSlot, rookiesOnly),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerDay(timeSlot, true),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerDay(timeSlot, false), 14);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addContributorsPerMonth() {
        int limit = 24;
        List<ContributionTimeSlot> monthlyContributions = landscapeAnalysisResults.getContributorsPerMonth();
        new RacingRepositoriesBarChartsExporter(landscapeAnalysisResults, landscapeAnalysisResults.getContributorsPerRepositoryAndMonth(), "repositories").exportRacingChart(reportsFolder);
        new RacingRepositoriesBarChartsExporter(landscapeAnalysisResults, landscapeAnalysisResults.getContributorsCommits(), "contributors").exportRacingChart(reportsFolder);
        List<ContributionTimeSlot> contributorsPerMonth = getContributionMonths(monthlyContributions,
                limit, landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerMonth.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerMonth.size() > 0) {
            if (contributorsPerMonth.size() > limit) {
                contributorsPerMonth = contributorsPerMonth.subList(0, limit);
            }

            landscapeReport.startDiv("overflow: hidden");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerMonth.size() >= 3 ? 3 : contributorsPerMonth.size();

            addChartRows(contributorsPerMonth, "months", minMaxWindow, (timeSlot, rookiesOnly) -> getContributorsPerMonth(timeSlot, rookiesOnly),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerMonth(timeSlot, true),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerMonth(timeSlot, false), 40);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addContributorsPerYear() {
        List<ContributionTimeSlot> yearlyContributions = landscapeAnalysisResults.getContributorsPerYear();
        List<ContributionTimeSlot> contributorsPerYear = getContributionYears(yearlyContributions,
                landscapeAnalysisResults.getConfiguration().getCommitsMaxYears(), landscapeAnalysisResults.getLatestCommitDate());

        contributorsPerYear.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        if (contributorsPerYear.size() > 0) {
            landscapeReport.startDiv("overflow: hidden");
            landscapeReport.startTable();

            int minMaxWindow = contributorsPerYear.size() >= 3 ? 3 : contributorsPerYear.size();

            addChartRows(contributorsPerYear, "years", minMaxWindow, (timeSlot, rookiesOnly) -> getSignificantContributorsPerYear(contributors, timeSlot, rookiesOnly, landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear()),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerYear(timeSlot, true),
                    (timeSlot, rookiesOnly) -> getLastContributorsPerYear(timeSlot, false), 64);

            landscapeReport.endTable();
            landscapeReport.endDiv();

            landscapeReport.addLineBreak();
        }
    }

    private void addChartRows(List<ContributionTimeSlot> contributorsPerWeek, String unit, int minMaxWindow, ContributorsExtractor contributorsExtractor, ContributorsExtractor firstContributorsExtractor, ContributorsExtractor lastContributorsExtractor, int barWidth) {
        addTickMarksPerWeekRow(contributorsPerWeek, barWidth);
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
                    extractedContributors.subList(0, extractedContributors.size() < 200 ? extractedContributors.size() : 200).stream().collect(Collectors.joining(", "));
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
            landscapeReport.startTableCell("max-width: 20px; padding: 0; margin: 1px; border: none; text-align: center; vertical-align: " + valign + "; font-size: 80%; height: 100px");
            List<String> extractedContributors = contributorsExtractor.getContributors(timeUnit.getTimeSlot(), true);
            int count = extractedContributors.size();
            int height = 4 + (int) (64.0 * count / maxContributors);
            String title = "timeUnit of " + timeUnit.getTimeSlot() + " = " + count + " extractedContributors:\n\n" +
                    extractedContributors.subList(0, extractedContributors.size() < 200 ? extractedContributors.size() : 200).stream().collect(Collectors.joining(", "));
            String yearString = timeUnit.getTimeSlot().split("[-]")[0];

            String color = "lightgrey";

            if (count > 0 && StringUtils.isNumeric(yearString)) {
                int year = Integer.parseInt(yearString);
                if (first) {
                    color = year % 2 == 0 ? "limegreen" : "darkgreen";
                } else {
                    if (firstItem[0]) {
                        color = "rgba(220,220,220,100)";
                    } else {
                        color = year % 2 == 0 ? "crimson" : "rgba(100,0,0,100)";
                    }
                }
            } else {
                height = 1;
            }

            if (first && count > 0) {
                landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; color: grey; font-size: 80%; margin: 1px'>" + count + "</div>");
            }
            landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; background-color: " + color + "; height:" + height + "px; margin: 1px'></div>");
            if (!first && count > 0) {
                landscapeReport.addHtmlContent("<div title='" + title + "' style='width: 100%; color: grey; font-size: 80%; margin: 1px'>" + count + "</div>");
            }
            landscapeReport.endTableCell();
            firstItem[0] = false;
        });
        landscapeReport.endTableRow();
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

    private int getContributorsCountPerYear(String year) {
        return this.contributorsPerYearMap.containsKey(year) ? contributorsPerYearMap.get(year).size() : 0;
    }

    private void populateTimeSlotMaps() {
        contributors.forEach(contributorRepositories -> {
            List<String> commitDates = contributorRepositories.getContributor().getCommitDates();
            commitDates.forEach(day -> {
                String week = DateUtils.getWeekMonday(day);
                String month = DateUtils.getMonth(day);
                String year = DateUtils.getYear(day);

                updateTimeSlotMap(contributorRepositories, contributorsPerDayMap, rookiesPerDayMap, day, day);
                updateTimeSlotMap(contributorRepositories, contributorsPerWeekMap, rookiesPerWeekMap, week, week);
                updateTimeSlotMap(contributorRepositories, contributorsPerMonthMap, rookiesPerMonthMap, month, month + "-01");
                updateTimeSlotMap(contributorRepositories, contributorsPerYearMap, rookiesPerYearMap, year, year + "-01-01");
            });
        });

    }

    private List<String> getSignificantContributorsPerYear(List<ContributorRepositories> contributorRepositories, String year, boolean rookiesOnly, int thresholdCommitDays) {
        if (rookiesOnly) {
            return getLastContributorsPerYear(year, true);
        }
        return contributorRepositories.stream()
                .filter(c -> c.getContributor().getCommitDates().stream().filter(d -> d.startsWith(year)).count() >= thresholdCommitDays)
                .map(c -> c.getContributor().getEmail())
                .collect(Collectors.toList());
    }

    private void updateTimeSlotMap(ContributorRepositories contributorRepositories,
                                   Map<String, List<String>> map, Map<String, List<String>> rookiesMap, String key, String rookieDate) {
        boolean rookie = contributorRepositories.getContributor().isRookieAtDate(rookieDate);

        String email = contributorRepositories.getContributor().getEmail();
        if (map.containsKey(key)) {
            if (!map.get(key).contains(email)) {
                map.get(key).add(email);
            }
        } else {
            map.put(key, new ArrayList<>(Arrays.asList(email)));
        }
        if (rookie) {
            if (rookiesMap.containsKey(key)) {
                if (!rookiesMap.get(key).contains(email)) {
                    rookiesMap.get(key).add(email);
                }
            } else {
                rookiesMap.put(key, new ArrayList<>(Arrays.asList(email)));
            }
        }
    }

    private List<String> getContributorsPerWeek(String week, boolean rookiesOnly) {
        Map<String, List<String>> map = rookiesOnly ? rookiesPerWeekMap : contributorsPerWeekMap;
        return map.containsKey(week) ? map.get(week) : new ArrayList<>();
    }

    private List<String> getContributorsPerDay(String day, boolean rookiesOnly) {
        Map<String, List<String>> map = rookiesOnly ? rookiesPerDayMap : contributorsPerDayMap;
        return map.containsKey(day) ? map.get(day) : new ArrayList<>();
    }

    private List<String> getLastContributorsPerWeek(String week, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> !DateUtils.getWeekMonday(c.getContributor().getFirstCommitDate()).equals(DateUtils.getWeekMonday(c.getContributor().getLatestCommitDate())))
                // .filter(c -> c.getContributor().getCommitDates().size() >= landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear())
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    if (DateUtils.getWeekMonday(first ? contributor.getFirstCommitDate() : contributor.getLatestCommitDate()).equals(week)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    private List<String> getLastContributorsPerDay(String day, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> !c.getContributor().getFirstCommitDate().equals(c.getContributor().getLatestCommitDate()))
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    if ((first ? contributor.getFirstCommitDate() : contributor.getLatestCommitDate()).equals(day)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    private List<String> getContributorsPerMonth(String month, boolean rookiesOnly) {
        Map<String, List<String>> map = rookiesOnly ? rookiesPerMonthMap : contributorsPerMonthMap;
        return map.containsKey(month) ? map.get(month) : new ArrayList<>();
    }

    private List<String> getLastContributorsPerYear(String year, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> !DateUtils.getYear(c.getContributor().getLatestCommitDate()).equals(DateUtils.getYear(c.getContributor().getFirstCommitDate())))
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    if (DateUtils.getYear(first ? contributor.getFirstCommitDate() : contributor.getLatestCommitDate()).equals(year)) {
                        String email = contributor.getEmail();
                        // only look at contributors with at least 10 commits days per year
                        if (contributor.getCommitDates().size() >= landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear()) {
                            emails.put(email, email);
                        }
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    private List<String> getLastContributorsPerMonth(String month, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> !DateUtils.getMonth(c.getContributor().getLatestCommitDate()).equals(DateUtils.getMonth(c.getContributor().getFirstCommitDate())))
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    if (DateUtils.getMonth(first ? contributor.getFirstCommitDate() : contributor.getLatestCommitDate()).equals(month)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    private boolean isContributorReport() {
        return type == Type.CONTRIBUTORS;
    }

    private void addDownloadLinks(String graphId) {
        landscapeReport.startDiv("");
        landscapeReport.addHtmlContent("Download: ");
        landscapeReport.addHtmlContent("<a href=\"#\" onclick=\"return downloadMermaid('" + graphId + "');\">Mermaid (.mmd)</a>");
        landscapeReport.addHtmlContent(" ");
        landscapeReport.addNewTabLink("(open online Mermaid editor)", "https://obren.io/tools/mermaid/");
        landscapeReport.endDiv();
    }

    public RichTextReport getLandscapeContributorsReport() {
        return landscapeContributorsReport;
    }

    public RichTextReport getLandscapeRecentContributorsReport() {
        return landscapeRecentContributorsReport;
    }

    public RichTextReport getLandscapeBotsReport() {
        return landscapeBotsReport;
    }

    public List<RichTextReport> getIndividualReports() {
        return individualReports;
    }

    public List<RichTextReport> getBotReports() {
        return botReports;
    }
}
