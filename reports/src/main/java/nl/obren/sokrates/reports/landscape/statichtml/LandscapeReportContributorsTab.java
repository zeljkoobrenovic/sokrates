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
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private File folder;
    private File reportsFolder;
    // Contributor/rookie time-slot maps, indexed by scope (main/test/build/generated/other/unscoped),
    // plus the all-scope key ALL_SCOPE. Each inner map is timeSlot -> distinct contributor emails. The
    // scope tabs in the activity diagrams select among these via currentScope; ALL_SCOPE backs the
    // "All" tab (and is the only one populated for analyses generated before per-scope contributor data
    // existed). The leaf getters read the currentScope's inner map.
    static final String ALL_SCOPE = "*";
    private final Map<String, Map<String, List<String>>> contributorsPerWeekMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> rookiesPerWeekMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> contributorsPerDayMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> rookiesPerDayMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> contributorsPerMonthMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> rookiesPerMonthMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> contributorsPerYearMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> rookiesPerYearMapByScope = new LinkedHashMap<>();
    // Per-scope first/last commit date per contributor email, derived from commitDatesByScope, so the
    // "first/last contribution" rows stay consistent with the selected scope. email -> "yyyy-MM-dd".
    private final Map<String, Map<String, String>> firstCommitDateByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> lastCommitDateByScope = new LinkedHashMap<>();
    // The scope whose maps the leaf getters currently read. Set before rendering each scope panel; the
    // panels render sequentially (addScopeToggle invokes each Runnable in turn), so a single mutable
    // field is safe. Defaults to ALL_SCOPE (the only scope when there is no scope toggle).
    private String currentScope = ALL_SCOPE;
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

        this.landscapeAnalysisResults = landscapeAnalysisResults;

        populateTimeSlotMaps();
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

        // Racing charts are scope-independent and write files; emit them once before the scope panels.
        exportMonthlyRacingCharts();

        // Scope selector wrapping the time-based activity diagrams (Year/Month/Week/Day). The churn and
        // commits rows filter by scope from the landscape per-scope aggregates; the contributor-count and
        // first/last rows filter via the per-scope time-slot maps (currentScope). The per-extension
        // section below is extension-based (not scopeable), so it stays outside the toggle. When no
        // per-scope contributor data exists (older analyses) only the "All" panel is shown.
        java.util.LinkedHashMap<String, Runnable> scopePanels = new java.util.LinkedHashMap<>();
        java.util.List<String> availableScopes = getAvailableContributorScopes();
        availableScopes.forEach(scope -> {
            String label = nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils.SCOPE_LABELS.get(scope);
            scopePanels.put(label, () -> renderActivityDiagramsForScope(scope, commitsMaxYears, significantContributorMinCommitDaysPerYear));
        });
        scopePanels.put("All", () -> renderActivityDiagramsForScope(ALL_SCOPE, commitsMaxYears, significantContributorMinCommitDaysPerYear));

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
    // currentScope so the contributor-row getters read that scope's maps, then restores ALL_SCOPE.
    private void renderActivityDiagramsForScope(String scope, int commitsMaxYears, int significantContributorMinCommitDaysPerYear) {
        String previousScope = currentScope;
        currentScope = scope;
        try {
            landscapeReport.startSubSection("Overall Activity Per Year", "Past " + commitsMaxYears + " years");
            addContributorsPerYear(true);
            landscapeReport.startDetailsBlock("significant contributions per year (" + significantContributorMinCommitDaysPerYear + "+ commit days per year)...");
            addContributorsPerYear();
            landscapeReport.endDetailsBlock();
            landscapeReport.endSection();

            landscapeReport.startDetailsBlock("Activity per month...");
            landscapeReport.startSubSection("Activity Per Month", "Past two years");
            addContributorsPerMonth();
            landscapeReport.endSection();
            landscapeReport.endDetailsBlock();

            landscapeReport.startDetailsBlock("Activity per week...");
            landscapeReport.startSubSection("Activity Per Week", "Past two years");
            addContributorsPerWeek();
            landscapeReport.endSection();
            landscapeReport.endDetailsBlock();

            landscapeReport.startDetailsBlock("Activity per day...");
            landscapeReport.startSubSection("Activity Per Day", "Past six months");
            addContributorsPerDay();
            landscapeReport.endSection();
            landscapeReport.endDetailsBlock();
        } finally {
            currentScope = previousScope;
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
            collectLinkedContributors(recentContributors, contributorsLinkedFromTables);
            collectLinkedContributors(contributors, contributorsLinkedFromTables);
            collectLinkedContributors(bots, contributorsLinkedFromTables);

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

            // Map contributorId -> languages they committed to in the last 30 days, built from the
            // SAME per-extension commit history the Overview "Contributors Per File Extension"
            // badges count, so includesLang:<lang> in the report matches those badge counts exactly.
            Map<String, List<String>> recentLangsByContributor = buildRecentLangsByContributor();

            Map<String, List<ContributorReportExport>> groups = new LinkedHashMap<>();
            groups.put("recent", toExports(recentContributors, configuration, peopleConfig, tagRules, recentLangsByContributor));
            groups.put("all", toExports(contributors, configuration, peopleConfig, tagRules, recentLangsByContributor));
            groups.put("bots", toExports(bots, configuration, peopleConfig, tagRules, recentLangsByContributor));

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
                                                    PeopleConfig peopleConfig, List<ContributorTag> tagRules,
                                                    Map<String, List<String>> recentLangsByContributor) {
        // Export every contributor (no list-limit cap): the client-rendered report pages the
        // display itself (show-more), and search needs the full set. Sorted by commit recency.
        return list.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount())
                .sorted((a, b) -> b.getContributor().getCommitsCount365Days() - a.getContributor().getCommitsCount365Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount90Days() - a.getContributor().getCommitsCount90Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .map(cr -> new ContributorReportExport(cr, configuration, peopleConfig, teamsConfig, tagRules,
                        recentLangsByContributor.get(cr.getContributor().getEmail().toLowerCase())))
                .collect(Collectors.toList());
    }

    // Records the top-N contributors (capped at getContributorsListLimit, sorted by recency like
    // the former table) into the linked set, so the matching individual per-person reports are
    // generated. This preserves the selection that the removed server-rendered contributor tables
    // used to make, without rendering any HTML.
    private void collectLinkedContributors(List<ContributorRepositories> contributors, Set<String> linked) {
        int limit = landscapeAnalysisResults.getConfiguration().getContributorsListLimit();
        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount())
                .sorted((a, b) -> b.getContributor().getCommitsCount365Days() - a.getContributor().getCommitsCount365Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount180Days() - a.getContributor().getCommitsCount180Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount90Days() - a.getContributor().getCommitsCount90Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .limit(limit)
                .forEach(contributor -> linked.add(contributor.getContributor().getEmail()));
    }

    // For each contributor id (lowercased, matching the report rows' email key), the set of
    // languages (lowercased extensions) they committed to in the last 30 days — inverted from the
    // landscape's per-extension committers30Days, the exact data the Overview badges count.
    private Map<String, List<String>> buildRecentLangsByContributor() {
        Map<String, List<String>> map = new HashMap<>();
        landscapeAnalysisResults.getContributorsPerExtension().forEach(commitsPerExtension -> {
            String lang = commitsPerExtension.getExtension().replace("*.", "").trim().toLowerCase();
            if (lang.isEmpty()) {
                return;
            }
            commitsPerExtension.getCommitters30Days().forEach(committerId -> {
                String key = committerId.toLowerCase();
                List<String> langs = map.computeIfAbsent(key, k -> new ArrayList<>());
                if (!langs.contains(lang)) {
                    langs.add(lang);
                }
            });
        });
        return map;
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
        // Open this report (contributors/teams) pre-filtered to people who have committed to the
        // clicked language; the query is in the URL fragment so the embedded-data page stays cached.
        String link = StringUtils.isNotBlank(lang)
                ? type.plural() + "-report.html?tab=recent#includesLang:" + lang.trim().toLowerCase()
                : null;
        InfoBlocks.addLangInfoBlockExtra(landscapeReport, value, lang, description, extra, link);
    }

    private void addSmallInfoBlock(String value, String subtitle, String color, String link) {
        InfoBlocks.addSmallInfoBlock(landscapeReport, value, subtitle, color, link);
    }

    private void addActivityTrendCard(String value, String subtitle, String icon) {
        InfoBlocks.addActivityTrendCard(landscapeReport, value, subtitle, icon);
    }

    // The churn/commits ContributionTimeSlot list for the current scope: the all-scope landscape
    // aggregate for ALL_SCOPE, otherwise the per-scope landscape aggregate (empty list when that scope
    // carries no data). Used by the activity charts; the contributor-count rows read the per-scope maps
    // via currentScope independently.
    private List<ContributionTimeSlot> scopedYear() {
        if (ALL_SCOPE.equals(currentScope)) return landscapeAnalysisResults.getContributorsPerYear();
        return landscapeAnalysisResults.getContributorsPerYearByScope().getOrDefault(currentScope, new ArrayList<>());
    }

    private List<ContributionTimeSlot> scopedMonth() {
        if (ALL_SCOPE.equals(currentScope)) return landscapeAnalysisResults.getContributorsPerMonth();
        return landscapeAnalysisResults.getContributorsPerMonthByScope().getOrDefault(currentScope, new ArrayList<>());
    }

    private List<ContributionTimeSlot> scopedWeek() {
        if (ALL_SCOPE.equals(currentScope)) return landscapeAnalysisResults.getContributorsPerWeek();
        return landscapeAnalysisResults.getContributorsPerWeekByScope().getOrDefault(currentScope, new ArrayList<>());
    }

    private List<ContributionTimeSlot> scopedDay() {
        if (ALL_SCOPE.equals(currentScope)) return landscapeAnalysisResults.getContributorsPerDay();
        return landscapeAnalysisResults.getContributorsPerDayByScope().getOrDefault(currentScope, new ArrayList<>());
    }

    private void addContributorsPerYear(boolean showContributorsCount) {
        List<ContributionTimeSlot> contributorsPerYear = scopedYear();
        if (contributorsPerYear.size() > 0) {
            int limit = landscapeAnalysisResults.getConfiguration().getCommitsMaxYears();
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(0, limit);
            }

            int maxCommits = contributorsPerYear.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);

            landscapeReport.startDiv("overflow-y: none;");
            landscapeReport.startTable();

            String style = "border: none; text-align: center; vertical-align: bottom; font-size: 80%; height: 100px";
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);

            // Churn row first, above commits.
            addChurnPerYearRow(contributorsPerYear, style);

            landscapeReport.startTableRow();
            landscapeReport.startTableCell("border: none; height: 130px; vertical-align: bottom;");
            int commitsCount = scopedTotalCommits();
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

            if (showContributorsCount) {
                int maxContributors[] = {1};
                contributorsPerYear.forEach(year -> {
                    int count = getContributorsCountPerYear(year.getTimeSlot());
                    maxContributors[0] = Math.max(maxContributors[0], count);
                });
                landscapeReport.startTableRow();
                landscapeReport.startTableCell("border: none; height: 100px; vertical-align: bottom;");
                int contributorsCount = scopedTotalContributors();
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
        List<ContributionTimeSlot> contributorsPerWeek = getContributionWeeks(scopedWeek(),
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
        List<ContributionTimeSlot> contributorsPerDay = getContributionDays(scopedDay(),
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

    // Writes the per-month racing bar charts to disk. Scope-independent (always all-scope) and emits
    // files, so it runs once from addContributionTrends rather than inside each scope panel's render.
    private void exportMonthlyRacingCharts() {
        new RacingRepositoriesBarChartsExporter(landscapeAnalysisResults, landscapeAnalysisResults.getContributorsPerRepositoryAndMonth(), "repositories").exportRacingChart(reportsFolder);
        new RacingRepositoriesBarChartsExporter(landscapeAnalysisResults, landscapeAnalysisResults.getContributorsCommits(), "contributors").exportRacingChart(reportsFolder);
    }

    private void addContributorsPerMonth() {
        int limit = 24;
        List<ContributionTimeSlot> monthlyContributions = scopedMonth();
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

    // Reads an inner (timeSlot -> emails) map for the current scope, falling back to an empty map when
    // the current scope has no entries (e.g. a scope with no contributors).
    private Map<String, List<String>> scoped(Map<String, Map<String, List<String>>> byScope) {
        Map<String, List<String>> map = byScope.get(currentScope);
        return map != null ? map : Collections.emptyMap();
    }

    // Total commits for the current scope: the landscape all-scope total for ALL_SCOPE, otherwise the
    // sum of the scope's per-year commit counts. Drives the "commits" trend card so it tracks the tab.
    private int scopedTotalCommits() {
        if (ALL_SCOPE.equals(currentScope)) {
            return landscapeAnalysisResults.getCommitsCount();
        }
        return scopedYear().stream().mapToInt(ContributionTimeSlot::getCommitsCount).sum();
    }

    // Distinct contributors for the current scope: the full contributor list for ALL_SCOPE, otherwise
    // the union of emails across the scope's per-year map. Drives the "contributors" trend card.
    private int scopedTotalContributors() {
        if (ALL_SCOPE.equals(currentScope)) {
            return contributors.size();
        }
        Set<String> emails = new HashSet<>();
        scoped(contributorsPerYearMapByScope).values().forEach(emails::addAll);
        return emails.size();
    }

    private int getContributorsCountPerYear(String year) {
        Map<String, List<String>> map = scoped(contributorsPerYearMapByScope);
        return map.containsKey(year) ? map.get(year).size() : 0;
    }

    private void populateTimeSlotMaps() {
        // Always build the all-scope maps from each contributor's flat commit dates.
        contributors.forEach(cr -> populateTimeSlotMapsForScope(cr, ALL_SCOPE, cr.getContributor().getCommitDates()));

        // Build per-scope maps from each contributor's per-scope commit dates (empty for older
        // analyses, so those scopes simply stay absent and the toggle falls back to "All" only).
        contributors.forEach(cr -> {
            Map<String, List<String>> byScope = cr.getContributor().getCommitDatesByScope();
            if (byScope != null) {
                byScope.forEach((scope, dates) -> populateTimeSlotMapsForScope(cr, scope, dates));
            }
        });
    }

    // Returns the scope keys (besides ALL_SCOPE) that any contributor carries data for, in the canonical
    // SCOPE_LABELS order. Empty when no per-scope contributor data is present (older analyses).
    java.util.List<String> getAvailableContributorScopes() {
        java.util.Set<String> present = contributorsPerYearMapByScope.keySet();
        java.util.List<String> ordered = new ArrayList<>();
        nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils.SCOPE_LABELS.keySet().forEach(scope -> {
            if (present.contains(scope)) {
                ordered.add(scope);
            }
        });
        return ordered;
    }

    private void populateTimeSlotMapsForScope(ContributorRepositories contributorRepositories, String scope, List<String> commitDates) {
        if (commitDates == null || commitDates.isEmpty()) {
            return;
        }
        Map<String, List<String>> perDay = contributorsPerDayMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> rookiesDay = rookiesPerDayMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> perWeek = contributorsPerWeekMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> rookiesWeek = rookiesPerWeekMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> perMonth = contributorsPerMonthMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> rookiesMonth = rookiesPerMonthMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> perYear = contributorsPerYearMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> rookiesYear = rookiesPerYearMapByScope.computeIfAbsent(scope, k -> new HashMap<>());

        commitDates.forEach(day -> {
            String week = DateUtils.getWeekMonday(day);
            String month = DateUtils.getMonth(day);
            String year = DateUtils.getYear(day);

            updateTimeSlotMap(contributorRepositories, perDay, rookiesDay, day, day);
            updateTimeSlotMap(contributorRepositories, perWeek, rookiesWeek, week, week);
            updateTimeSlotMap(contributorRepositories, perMonth, rookiesMonth, month, month + "-01");
            updateTimeSlotMap(contributorRepositories, perYear, rookiesYear, year, year + "-01-01");
        });

        // Track this contributor's first/last commit day within the scope (min/max of its dates).
        String email = contributorRepositories.getContributor().getEmail();
        String min = commitDates.get(0);
        String max = commitDates.get(0);
        for (String d : commitDates) {
            if (d.compareTo(min) < 0) min = d;
            if (d.compareTo(max) > 0) max = d;
        }
        firstCommitDateByScope.computeIfAbsent(scope, k -> new HashMap<>()).merge(email, min, (a, b) -> a.compareTo(b) <= 0 ? a : b);
        lastCommitDateByScope.computeIfAbsent(scope, k -> new HashMap<>()).merge(email, max, (a, b) -> a.compareTo(b) >= 0 ? a : b);
    }

    private List<String> getSignificantContributorsPerYear(List<ContributorRepositories> contributorRepositories, String year, boolean rookiesOnly, int thresholdCommitDays) {
        if (rookiesOnly) {
            return getLastContributorsPerYear(year, true);
        }
        // Count this year's commit DAYS within the current scope (per-scope dates when scoped, the flat
        // commit dates for the all-scope tab) so "significant" contributors are scope-consistent.
        return contributorRepositories.stream()
                .filter(c -> scopedCommitDates(c.getContributor()).stream().filter(d -> d.startsWith(year)).count() >= thresholdCommitDays)
                .map(c -> c.getContributor().getEmail())
                .collect(Collectors.toList());
    }

    // This contributor's commit dates for the current scope: the flat list for ALL_SCOPE, otherwise the
    // scope's entry from commitDatesByScope (empty if the contributor never touched that scope).
    private List<String> scopedCommitDates(Contributor contributor) {
        if (ALL_SCOPE.equals(currentScope)) {
            return contributor.getCommitDates();
        }
        List<String> dates = contributor.getCommitDatesByScope().get(currentScope);
        return dates != null ? dates : Collections.emptyList();
    }

    // First/last commit date for an email within the current scope (the all-scope contributor dates for
    // ALL_SCOPE, otherwise the per-scope derived map). Empty string when the contributor has no activity
    // in the scope, which the callers treat as "no match".
    private String scopedFirstCommitDate(Contributor contributor) {
        if (ALL_SCOPE.equals(currentScope)) {
            return contributor.getFirstCommitDate();
        }
        Map<String, String> map = firstCommitDateByScope.get(currentScope);
        return map != null ? map.getOrDefault(contributor.getEmail(), "") : "";
    }

    private String scopedLastCommitDate(Contributor contributor) {
        if (ALL_SCOPE.equals(currentScope)) {
            return contributor.getLatestCommitDate();
        }
        Map<String, String> map = lastCommitDateByScope.get(currentScope);
        return map != null ? map.getOrDefault(contributor.getEmail(), "") : "";
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
        Map<String, List<String>> map = scoped(rookiesOnly ? rookiesPerWeekMapByScope : contributorsPerWeekMapByScope);
        return map.containsKey(week) ? map.get(week) : new ArrayList<>();
    }

    private List<String> getContributorsPerDay(String day, boolean rookiesOnly) {
        Map<String, List<String>> map = scoped(rookiesOnly ? rookiesPerDayMapByScope : contributorsPerDayMapByScope);
        return map.containsKey(day) ? map.get(day) : new ArrayList<>();
    }

    private List<String> getLastContributorsPerWeek(String week, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> {
                    String f = scopedFirstCommitDate(c.getContributor());
                    String l = scopedLastCommitDate(c.getContributor());
                    return !f.isEmpty() && !DateUtils.getWeekMonday(f).equals(DateUtils.getWeekMonday(l));
                })
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    String date = first ? scopedFirstCommitDate(contributor) : scopedLastCommitDate(contributor);
                    if (!date.isEmpty() && DateUtils.getWeekMonday(date).equals(week)) {
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
                .filter(c -> {
                    String f = scopedFirstCommitDate(c.getContributor());
                    String l = scopedLastCommitDate(c.getContributor());
                    return !f.isEmpty() && !f.equals(l);
                })
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    String date = first ? scopedFirstCommitDate(contributor) : scopedLastCommitDate(contributor);
                    if (!date.isEmpty() && date.equals(day)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    private List<String> getContributorsPerMonth(String month, boolean rookiesOnly) {
        Map<String, List<String>> map = scoped(rookiesOnly ? rookiesPerMonthMapByScope : contributorsPerMonthMapByScope);
        return map.containsKey(month) ? map.get(month) : new ArrayList<>();
    }

    private List<String> getLastContributorsPerYear(String year, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> {
                    String f = scopedFirstCommitDate(c.getContributor());
                    String l = scopedLastCommitDate(c.getContributor());
                    return !f.isEmpty() && !DateUtils.getYear(l).equals(DateUtils.getYear(f));
                })
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    String date = first ? scopedFirstCommitDate(contributor) : scopedLastCommitDate(contributor);
                    if (!date.isEmpty() && DateUtils.getYear(date).equals(year)) {
                        String email = contributor.getEmail();
                        // only look at contributors with at least N commit days per year (within the scope)
                        if (scopedCommitDates(contributor).size() >= landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear()) {
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
                .filter(c -> {
                    String f = scopedFirstCommitDate(c.getContributor());
                    String l = scopedLastCommitDate(c.getContributor());
                    return !f.isEmpty() && !DateUtils.getMonth(l).equals(DateUtils.getMonth(f));
                })
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    String date = first ? scopedFirstCommitDate(contributor) : scopedLastCommitDate(contributor);
                    if (!date.isEmpty() && DateUtils.getMonth(date).equals(month)) {
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

    public List<RichTextReport> getIndividualReports() {
        return individualReports;
    }

    public List<RichTextReport> getBotReports() {
        return botReports;
    }
}
