package nl.obren.sokrates.reports.landscape.statichtml.projects;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.ReportConstants;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.generators.statichtml.ControlsReportGenerator;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.ProjectTag;
import nl.obren.sokrates.sourcecode.landscape.ProjectTagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class LandscapeProjectsReport {
    private static final Log LOG = LogFactory.getLog(LandscapeProjectsReport.class);

    private LandscapeAnalysisResults landscapeAnalysisResults;
    private int limit = 1000;
    private TagMap customTagsMap;
    private String link;
    private String linkLabel;
    private File reportsFolder;
    private List<ProjectTagGroup> tagGroups = new ArrayList<>();

    private String type = "";

    public LandscapeProjectsReport(LandscapeAnalysisResults landscapeAnalysisResults, int limit, TagMap customTagsMap) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.limit = limit;
        this.customTagsMap = customTagsMap;
        this.type = "long report";
    }

    public LandscapeProjectsReport(LandscapeAnalysisResults landscapeAnalysisResults, int limit, String link, String linkLabel, TagMap customTagsMap) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.limit = limit;
        this.link = link;
        this.linkLabel = linkLabel;
        this.customTagsMap = customTagsMap;
        this.type = "short report";
    }

    public void saveProjectsReport(RichTextReport report, File reportsFolder, List<ProjectAnalysisResults> projectsAnalysisResults, List<ProjectTagGroup> tagGroups) {
        ProcessingStopwatch.start("reporting/repositories/" + type + "/preparing");
        this.reportsFolder = reportsFolder;
        this.tagGroups = tagGroups;
        report.startTabGroup();
        boolean showCommits = landscapeAnalysisResults.getCommitsCount() > 0;
        if (showCommits) {
            report.addTab("commitsTrend", "Commits Trend", true);
            report.addTab("contributorsTrend", "Contributors Trend", false);
        }
        report.addTab("repositories", "Size & Details", !showCommits);
        if (showCommits) {
            report.addTab("history", "History", false);
        }
        report.addTab("metrics", "Metrics", false);
        if (showCommits) {
            report.addTab("correlations", "Correlations", false);
        }
        report.addTab("features", "Features of Interest", false);
        report.endTabGroup();
        ProcessingStopwatch.end("reporting/repositories/" + type + "/preparing");

        ProcessingStopwatch.start("reporting/repositories/" + type + "/size");
        addProjectsBySize(report, projectsAnalysisResults);
        ProcessingStopwatch.end("reporting/repositories/" + type + "/size");
        if (showCommits) {
            ProcessingStopwatch.start("reporting/repositories/" + type + "/commits");
            addCommitBasedLists(report, projectsAnalysisResults);
            ProcessingStopwatch.end("reporting/repositories/" + type + "/commits");
            ProcessingStopwatch.start("reporting/repositories/" + type + "/history");
            addHistory(report, projectsAnalysisResults);
            ProcessingStopwatch.end("reporting/repositories/" + type + "/history");
        }

        report.startTabContentSection("metrics", false);
        ProcessingStopwatch.start("reporting/repositories/" + type + "/metrics");
        addMetrics(report, projectsAnalysisResults);
        ProcessingStopwatch.end("reporting/repositories/" + type + "/metrics");
        report.endTabContentSection();

        report.startTabContentSection("features", false);
        ProcessingStopwatch.start("reporting/repositories/" + type + "/features of interest");
        addFeaturesOfInterest(report);
        ProcessingStopwatch.end("reporting/repositories/" + type + "/features of interest");
        report.endTabContentSection();
    }

    private void addFeaturesOfInterest(RichTextReport report) {
        List<ProjectAnalysisResults> projectAnalysisResults = landscapeAnalysisResults.getProjectAnalysisResults();

        FeaturesOfInterestAggregator aggregator = new FeaturesOfInterestAggregator(projectAnalysisResults);
        aggregator.aggregateFeaturesOfInterest(limit);

        List<List<ProjectConcernData>> concerns = aggregator.getConcerns();
        List<List<ProjectConcernData>> projects = aggregator.getProjects();

        if (concerns.size() == 0) {
            report.addParagraph("No features of interest found in repositories.");
            return;
        }

        report.startTable();

        report.startTableRow();
        report.addTableCell("", "border: none");
        report.addTableCell("", "border: none");
        concerns.stream().filter(concern -> concern.size() > 0).forEach(concern -> {
            report.startTableCellColSpan(2, "");
            report.addContentInDiv(concern.get(0).getConcern().getName() + " (" + concern.size() + ")", "text-align: center");
            report.endTableCell();
        });
        report.addTableCell("", "border: none");
        report.endTableRow();

        report.startTableRow();
        report.addTableCell("", "border-left: none; border-top: none");
        report.addTableCell("Repositories (" + aggregator.getProjectsMap().size() + ")", "");
        concerns.stream().filter(concern -> concern.size() > 0).forEach(concern -> {
            int concernsCount = concern.stream().mapToInt(c -> c.getConcern().getNumberOfRegexLineMatches()).reduce((a, b) -> a + b).orElse(0);
            report.addTableCell(concernsCount + " matches", "font-size: 70%; text-align: center;");
            int filesCount = concern.stream().mapToInt(c -> c.getConcern().getFilesCount()).reduce((a, b) -> a + b).orElse(0);
            report.addTableCell(filesCount + " files", "font-size: 70%; text-align: center;");
        });
        report.addTableCell("Details", "font-size: 70%");
        report.endTableRow();

        projects.forEach(project -> {
            report.startTableRow();
            ProjectConcernData projectConcertData = project.get(0);
            String logoLink = projectConcertData.getProject().getAnalysisResults().getMetadata().getLogoLink();
            report.addTableCell(getImageWithLink(projectConcertData.getProject(), logoLink), "text-align: center");
            String projectName = projectConcertData.getProjectName();
            report.addTableCell("<a href='" + this.getFeaturesProjectReportUrl(projectConcertData.getProject()) + "' target='_blank'>"
                    + "" + projectName + "</a>", "");
            concerns.stream().filter(concern -> concern.size() > 0).forEach(concern -> {
                String key = projectName + "::" + concern.get(0).getConcern().getName();
                int instancesCount = aggregator.getProjectsConcernMap().containsKey(key) ? aggregator.getProjectsConcernMap().get(key).getConcern().getNumberOfRegexLineMatches() : 0;
                report.addTableCell("" + (instancesCount > 0 ? instancesCount : "-"), "text-align: center;");
                int filesCount = aggregator.getProjectsConcernMap().containsKey(key) ? aggregator.getProjectsConcernMap().get(key).getConcern().getFilesCount() : 0;
                report.addTableCell("" + (filesCount > 0 ? filesCount : "-"), "text-align: center;");
            });
            report.addTableCell("<a href='" + this.getFeaturesProjectReportUrl(projectConcertData.getProject()) + "' target='_blank'>"
                    + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 38) + "</div></a>", "text-align: center");
            report.endTableRow();
        });

        report.endTable();
        if (limit < aggregator.getProjectsMap().size()) {
            addShowMoreFooter(report, aggregator.getProjectsMap().size());
        }
    }

    public void addMetrics(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount());
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount180Days()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount180Days());
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days());
        addMetricsTable(report, projectsAnalysisResults);
    }

    public void addHistory(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startTabContentSection("history", false);
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays()
                        - a.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays());
        addSummaryGraphHistory(report, projectsAnalysisResults);
        addHistory(report, projectsAnalysisResults, "Commits", "blue", (slot) -> slot.getCommitsCount());
        report.endTabContentSection();
    }

    public void addCommitBasedLists(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startTabContentSection("commitsTrend", true);
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days());
        addSummaryGraphCommits(report, projectsAnalysisResults);
        addCommitsTrend(report, projectsAnalysisResults, "Commits", "blue", (slot) -> slot.getCommitsCount());
        report.endTabContentSection();
        report.startTabContentSection("contributorsTrend", false);
        Collections.sort(projectsAnalysisResults,
                (a, b) -> (int) b.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count()
                        - (int) a.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count());
        addSummaryGraphContributors(report, projectsAnalysisResults);
        addCommitsTrend(report, projectsAnalysisResults, "Contributors", "darkred", (slot) -> slot.getContributorsCount());
        report.endTabContentSection();
        report.startTabContentSection("correlations", false);

        CorrelationDiagramGenerator<ProjectAnalysisResults> correlationDiagramGenerator = new CorrelationDiagramGenerator<>(report, projectsAnalysisResults);

        correlationDiagramGenerator.addCorrelations("Recent Contributors vs. Commits (30 days)", "commits (30d)", "recent contributors (30d)",
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Recent Contributors vs. Repository Main LOC", "main LOC", "recent contributors (30d)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Recent Commits (30 days) vs. Repository Main LOC", "main LOC", "commits (30d)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Age in Years vs. Repository Main LOC", "main LOC", "age (years)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> Math.round(10 * p.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays() / 365.0) / 10,
                p -> p.getAnalysisResults().getMetadata().getName());

        report.endTabContentSection();
    }

    public void addSummaryGraphCommits(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(projectsAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .max().orElse(1), 1);
        int sum = projectsAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .sum();
        int maxHeight = 64;
        int cummulative[] = {0};
        int index[] = {0};
        boolean breakPointReached[] = {false};
        int projectsCount = projectsAnalysisResults.size();
        projectsAnalysisResults.stream().limit(landscapeAnalysisResults.getConfiguration().getProjectsListLimit()).forEach(projectAnalysis -> {
            int commits30d = projectAnalysis.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days();
            cummulative[0] += commits30d;
            index[0] += 1;
            int height = (int) (1 + maxHeight * (double) commits30d / max);
            String name = projectAnalysis.getAnalysisResults().getMetadata().getName();
            double percentage = RichTextRenderingUtils.getPercentage(sum, cummulative[0]);
            double percentageProjects = RichTextRenderingUtils.getPercentage(projectsCount, index[0]);
            String color = commits30d > 0 ? (!breakPointReached[0] && percentage >= 50 ? "blue" : "blue; opacity: 0.5") : "lightgrey; opacity: 0.5;";
            if (percentage >= 50) {
                breakPointReached[0] = true;
            }
            report.addContentInDivWithTooltip("",
                    name + ": " + commits30d + " lines of code (main)\n" +
                            "cumulative: top " + index[0] + " repositories (" + percentageProjects + "%) = "
                            + cummulative[0] + " commits in past 30 days (" + percentage + "%)",
                    "margin: 0; padding: 0; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 8px; height: " + height + "px");
        });
        report.endDiv();
        report.startDiv("font-size: 80%; margin-bottom: 6px;");
        report.addNewTabLink("bubble chart", "visuals/bubble_chart_projects_commits.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("tree map", "visuals/tree_map_projects_commits.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("animated history (all time)", "visuals/racing_charts_commits_projects.html?tickDuration=1200");
        report.addHtmlContent(" | ");
        report.addNewTabLink("animated history (12 months window)", "visuals/racing_charts_commits_window_projects.html?tickDuration=1200");
        report.addHtmlContent(" | ");
        report.addNewTabLink("data", "data/projects.txt");
        report.endDiv();
    }

    public void addSummaryGraphHistory(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(projectsAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays())
                .max().orElse(1), 1);
        int maxHeight = 64;
        projectsAnalysisResults.stream().limit(landscapeAnalysisResults.getConfiguration().getProjectsListLimit()).forEach(projectAnalysis -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = projectAnalysis.getAnalysisResults().getFilesHistoryAnalysisResults();
            int ageInDays = filesHistoryAnalysisResults.getAgeInDays();
            int height = (int) (1 + maxHeight * (double) ageInDays / max);
            String color = ageInDays > 0 ? "darkgreen" : "lightgrey";
            int projectAgeYears = (int) Math.round(filesHistoryAnalysisResults.getAgeInDays() / 365.0);
            String age = projectAgeYears == 0 ? "<1y" : projectAgeYears + "y";
            report.addContentInDivWithTooltip("",
                    projectAnalysis.getAnalysisResults().getMetadata().getName() + ": " + ageInDays + " days (" + age + ")",
                    "margin: 0; padding: 0; opacity: 0.5; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 8px; height: " + height + "px");
        });
        report.endDiv();
        report.startDiv("font-size: 80%; margin-bottom: 6px;");
        report.addNewTabLink("bubble chart", "visuals/bubble_chart_projects_age.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("tree map", "visuals/tree_map_projects_age.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("data", "data/projects.txt");
        report.endDiv();
    }

    public void addSummaryGraphContributors(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(projectsAnalysisResults.stream()
                .mapToInt(p -> (int) p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count())
                .max().orElse(1), 1);
        int maxHeight = 64;
        projectsAnalysisResults.stream().limit(landscapeAnalysisResults.getConfiguration().getProjectsListLimit()).forEach(projectAnalysis -> {
            int contributors30d = (int) projectAnalysis.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
            int height = (int) (1 + maxHeight * (double) contributors30d / max);
            String color = contributors30d > 0 ? "darkred" : "lightgrey";
            report.addContentInDivWithTooltip("",
                    projectAnalysis.getAnalysisResults().getMetadata().getName() + ": " + contributors30d + " contributors (30 days)",
                    "margin: 0; padding: 0; opacity: 0.5; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 8px; height: " + height + "px");
        });
        report.endDiv();
        report.startDiv("font-size: 80%; margin-bottom: 6px;");
        report.addNewTabLink("bubble chart", "visuals/bubble_chart_projects_contributors.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("tree map", "visuals/tree_map_projects_contributors.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("animated contributors per month (12 months average)", "visuals/racing_charts_contributors_per_month.html?tickDuration=1200");
        report.addHtmlContent(" | ");
        report.addNewTabLink("data", "data/projects.txt");
        report.endDiv();
    }

    public void addSummaryGraphMainLoc(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(projectsAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .max().orElse(1), 1);
        int sum = projectsAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .sum();
        int cummulative[] = {0};
        int index[] = {0};
        int maxHeight = 64;
        boolean breakPointReached[] = {false};
        int projectsCount = projectsAnalysisResults.size();
        projectsAnalysisResults.stream().limit(landscapeAnalysisResults.getConfiguration().getProjectsListLimit()).forEach(projectAnalysis -> {
            int mainLoc = projectAnalysis.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            cummulative[0] += mainLoc;
            index[0] += 1;
            int height = (int) (1 + maxHeight * (double) mainLoc / max);
            String name = projectAnalysis.getAnalysisResults().getMetadata().getName();
            double percentage = RichTextRenderingUtils.getPercentage(sum, cummulative[0]);
            double percentageProjects = RichTextRenderingUtils.getPercentage(projectsCount, index[0]);
            String color = mainLoc > 0 ? (!breakPointReached[0] && percentage >= 50 ? "blue" : "skyblue") : "lightgrey";
            if (percentage >= 50) {
                breakPointReached[0] = true;
            }
            report.addContentInDivWithTooltip("",
                    name + ": " + mainLoc + " lines of code (main)\n" +
                            "cumulative: top " + index[0] + " repositories (" + percentageProjects + "%) = "
                            + cummulative[0] + " LOC (" + percentage + "%)",
                    "margin: 0; padding: 0; opacity: 0.9; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 8px; height: " + height + "px");
        });
        report.endDiv();
        report.startDiv("font-size: 80%; margin-bottom: 6px;");
        report.addNewTabLink("bubble chart", "visuals/bubble_chart_projects_loc.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("tree map", "visuals/tree_map_projects_loc.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("data", "data/projects.txt");
        report.endDiv();
    }

    private boolean showTags() {
        for (ProjectTagGroup tagGroup : tagGroups) {
            if (tagGroup.getProjectTags().size() > 0) return true;
        }
        return false;
    }

    public void addProjectsBySize(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()
                        - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());

        boolean showCommits = landscapeAnalysisResults.getCommitsCount() > 0;
        report.startTabContentSection("repositories", !showCommits);
        addSummaryGraphMainLoc(report, projectsAnalysisResults);
        report.startTable("width: 100%");
        int thresholdContributors = landscapeAnalysisResults.getConfiguration().getProjectThresholdContributors();
        List<String> headers = new ArrayList<>(Arrays.asList("", "Repository" + (thresholdContributors > 1 ? "<br/>(" + thresholdContributors + "+&nbsp;contributors)" : ""),
                "Main<br/>Language", "LOC<br/>(main)*",
                "LOC<br/>(test)", "LOC<br/>(other)",
                "Age", "Latest<br>Commit Date",
                "Contributors<br>(30d)", "Rookies<br>(30d)", "Commits<br>(30d)"));
        headers.add("Report");
        if (showTags()) {
            headers.add("Tags");
        }
        report.addTableHeader(headers.toArray(String[]::new));

        projectsAnalysisResults.stream().limit(limit).forEach(projectAnalysis -> {
            addProjectRow(report, projectAnalysis);
        });


        report.endTable();
        if (limit < projectsAnalysisResults.size()) {
            addShowMoreFooter(report, projectsAnalysisResults.size());
        }
        report.endTabContentSection();
    }

    private void addShowMoreFooter(RichTextReport report, int totalSize) {
        report.startDiv("color:grey; font-size: 90%; margin-top: 16px;");
        report.addParagraph("The list is limited to " + limit +
                " items (out of " + totalSize + ").", "margin-left: 11px");
        if (link != null && linkLabel != null) {
            report.startDiv("margin-left: 10px; margin-bottom: 12px;");
            report.addNewTabLink(link, linkLabel);
            report.endDiv();
        }
        report.endDiv();
    }

    private void addCommitsTrend(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults, String label, String color, Counter counter) {
        report.startTable();
        report.addTableHeader("", "Repository", "Commits<br>(30d)" + (label.equalsIgnoreCase("Commits") ? "*" : ""),
                "Contributors<br>(30d)" + (label.equalsIgnoreCase("Contributors") ? "*" : ""), "Rookies<br>(30d)", label + " per Week (past year)", "Details");
        int maxCommits[] = {1};
        int pastWeeks = 52;
        projectsAnalysisResults.forEach(projectAnalysis -> {
            List<ContributionTimeSlot> contributorsPerWeek = LandscapeReportGenerator.getContributionWeeks(projectAnalysis.getAnalysisResults().getContributorsAnalysisResults().getContributorsPerWeek(), pastWeeks, landscapeAnalysisResults.getLatestCommitDate());
            contributorsPerWeek.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());
            if (contributorsPerWeek.size() > pastWeeks) {
                contributorsPerWeek = contributorsPerWeek.subList(0, pastWeeks);
            }
            contributorsPerWeek.forEach(c -> maxCommits[0] = Math.max(counter.getCount(c), maxCommits[0]));
        });
        projectsAnalysisResults.stream().limit(limit).forEach(projectAnalysis -> {
            report.startTableRow();
            String name = projectAnalysis.getAnalysisResults().getMetadata().getName();
            String logoLink = projectAnalysis.getAnalysisResults().getMetadata().getLogoLink();
            report.addTableCell(getImageWithLink(projectAnalysis, logoLink), "text-align: center");
            report.addTableCell("<a href='" + this.getProjectReportUrl(projectAnalysis) + "' target='_blank'>"
                    + "<div>" + name + "</div></a>", "vertical-align: middle; min-width: 400px; max-width: 400px");
            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysis.getAnalysisResults().getContributorsAnalysisResults();
            report.addTableCell(contributorsAnalysisResults.getCommitsCount30Days() + "", "text-align: center");
            List<Contributor> contributors = contributorsAnalysisResults.getContributors();
            int recentContributorsCount = (int) contributors.stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
            int rookiesCount = (int) contributors.stream().filter(c -> c.isRookie(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
            report.addTableCell(recentContributorsCount + "", "text-align: center");
            report.addTableCell(rookiesCount + "", "text-align: center");
            List<ContributionTimeSlot> contributorsPerWeek = LandscapeReportGenerator.getContributionWeeks(contributorsAnalysisResults.getContributorsPerWeek(), pastWeeks, landscapeAnalysisResults.getLatestCommitDate());
            contributorsPerWeek.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());
            if (contributorsPerWeek.size() > pastWeeks) {
                contributorsPerWeek = contributorsPerWeek.subList(0, pastWeeks);
            }
            report.startTableCell("white-space: nowrap; overflow-x: hidden;");
            contributorsPerWeek.forEach(contributionTimeSlot -> {
                int h = (int) (2 + 24.0 * counter.getCount(contributionTimeSlot) / maxCommits[0]);
                report.addContentInDivWithTooltip("", +counter.getCount(contributionTimeSlot) + " in the week of " + contributionTimeSlot.getTimeSlot(),
                        "margin: 0; margin-right: -4px; padding: 0; display: inline-block; vertical-align: bottom; width: 12px; " +
                                "background-color: " + (counter.getCount(contributionTimeSlot) == 0 ? "grey" : color) + "; " +
                                "opacity: 0.5; height: " + (h + "px"));
            });
            report.endTableCell();
            report.addTableCell("<a href='" + this.getProjectReportUrl(projectAnalysis) + "' target='_blank'>"
                    + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 38) + "</div></a>", "text-align: center");
            report.endTableRow();
        });

        report.endTable();

        if (limit < projectsAnalysisResults.size()) {
            addShowMoreFooter(report, projectsAnalysisResults.size());
        }

    }

    private String getImageWithLink(ProjectAnalysisResults projectAnalysis, String logoLink) {
        String prefix = landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix();
        return "<a href='" + this.getProjectReportUrl(projectAnalysis) + "' target='_blank'>" +
                (StringUtils.isNotBlank(logoLink)
                        ? ("<img src='" + getLogoLink(prefix + projectAnalysis.getSokratesProjectLink()
                        .getHtmlReportsRoot().replace("/index.html", ""), logoLink) + "' " +
                        "style='width: 20px' " +
                        "onerror=\"this.onerror=null;this.src='" + ReportConstants.SOKRATES_SVG_ICON_SMALL_BASE64 + "'\">")
                        : ReportConstants.SOKRATES_SVG_ICON_SMALL) +
                "</a>";
    }

    private String getLogoLink(String projectLinkPrefix, String link) {
        return link.startsWith("/") || link.contains("://") || link.startsWith("data:image")
                ? link
                : StringUtils.appendIfMissing(projectLinkPrefix, "/") + link;
    }

    private void addMetricsTable(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startTable();
        List<String> headers = new ArrayList<>();
        headers.addAll(Arrays.asList(new String[]{"", "Repository", "Main<br>Lang", "Duplication", "File Size",
                "Unit Size", "Conditional<br>Complexity", "Newness", "Freshness", "Update<br>Frequency"}));
        if (showControls()) {
            headers.add("Controls");
        }

        report.addTableHeader(headers.toArray(new String[headers.size()]));

        boolean startedInactiveSection90Days[] = {false};
        boolean startedInactiveSection180Days[] = {false};

        projectsAnalysisResults.stream().filter(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() > 0).limit(limit).forEach(projectAnalysis -> {
            int commits90Days = projectAnalysis.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days();
            int commits180Days = projectAnalysis.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount180Days();
            if (commits90Days == 0 && commits180Days > 0 && !startedInactiveSection90Days[0]) {
                startedInactiveSection90Days[0] = true;
                report.startTableRow();
                report.addMultiColumnTableCell("<h3 style='margin: 0; margin-top: 14px; margin-bottom: 6px;'>Repository not active in past 90 days</h3>", 11);
                report.endTableRow();
            }
            if (commits180Days == 0 && !startedInactiveSection180Days[0]) {
                startedInactiveSection180Days[0] = true;
                report.startTableRow();
                report.addMultiColumnTableCell("<h3 style='margin: 0; margin-top: 14px; margin-bottom: 6px;'>Repository not active in past 180 days</h3>", 11);
                report.endTableRow();
            }
            report.startTableRow(commits90Days > 0 ? "" : "opacity: 0.7");
            CodeAnalysisResults projectAnalysisResults = projectAnalysis.getAnalysisResults();
            String name = projectAnalysisResults.getMetadata().getName();
            String logoLink = projectAnalysisResults.getMetadata().getLogoLink();

            report.addTableCell(getImageWithLink(projectAnalysis, logoLink), "text-align: center");

            AspectAnalysisResults main = projectAnalysis.getAnalysisResults().getMainAspectAnalysisResults();
            String locText = FormattingUtils.formatCount(main.getLinesOfCode());
            String commits90DaysText = commits90Days > 0 ? ", <b>" + FormattingUtils.formatCount(commits90Days) + "</b> commits (90d)" : "";
            report.addTableCell("<a href='" + this.getProjectReportUrl(projectAnalysis) + "' target='_blank'>"
                            + "<div>" + name + "</div><div style='color: black; font-size: 80%'><b>" + locText + "</b> LOC (main)" + commits90DaysText + "</div></a>",
                    "vertical-align: middle; max-width: 400px");

            List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
            StringBuilder locSummary = new StringBuilder();
            if (linesOfCodePerExtension.size() > 0) {
                locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
            } else {
                locSummary.append("-");
            }
            String lang = locSummary.toString().replace("> = ", ">");
            report.startTableCell("text-align: left; max-width: 32px;");
            report.startDiv("min-width: 130px; white-space: nowrap; overflow: hidden; filter: grayscale(100%);");
            report.addHtmlContent(DataImageUtils.getLangDataImageDiv30(lang));
            report.endDiv();
            report.endTableCell();


            report.addTableCell("<a href='" + this.getDuplicationProjectReportUrl(projectAnalysis) + "' target='_blank'>" +
                    getDuplicationVisual(projectAnalysisResults.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getFileSizeProjectReportUrl(projectAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(projectAnalysisResults.getFilesAnalysisResults().getOverallFileSizeDistribution(), Palette.getRiskPalette()) +
                    "</a>", "text-align: center");

            report.addTableCell("<a href='" + this.getUnitSizeProjectReportUrl(projectAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(projectAnalysisResults.getUnitsAnalysisResults().getUnitSizeRiskDistribution(), Palette.getRiskPalette()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getConditionalComplexityReportUrl(projectAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(projectAnalysisResults.getUnitsAnalysisResults().getConditionalComplexityRiskDistribution(), Palette.getRiskPalette()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getFileAgeProjectReportUrl(projectAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(projectAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution(), Palette.getAgePalette()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getFileAgeProjectReportUrl(projectAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(projectAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution(), Palette.getFreshnessPalette()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getFileChangeFrequencyProjectReportUrl(projectAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(projectAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution(), Palette.getHeatPalette()) +
                    "</a>", "text-align: center");

            if (showControls()) {
                report.startTableCell("text-align: center; font-size: 90%");
                report.addHtmlContent("<a target='_blank' href='" + this.getControlsProjectReportUrl(projectAnalysis) + "'>");
                addControls(report, projectAnalysisResults);
                report.addHtmlContent("</a>");
                report.endTableCell();
            }
            report.addTableCell("<a href='" + this.getProjectReportUrl(projectAnalysis) + "' target='_blank'>"
                    + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 38) + "</div></a>", "text-align: center");
            report.endTableRow();
        });

        report.endTable();

        if (limit < projectsAnalysisResults.size()) {
            addShowMoreFooter(report, projectsAnalysisResults.size());
        }

    }

    private void addHistory(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults, String label, String color, Counter counter) {
        report.startTable();
        report.addTableHeader("", "Repository",
                "Age", label + " per Year", "Contributors", "Commits", "Freshness", "Details");
        int pastYears = landscapeAnalysisResults.getConfiguration().getProjectsHistoryLimit();
        int maxCommits[] = {1};
        projectsAnalysisResults.forEach(projectAnalysis -> {
            List<ContributionTimeSlot> contributorsPerYear = LandscapeReportGenerator.getContributionYears(projectAnalysis.getAnalysisResults().getContributorsAnalysisResults().getContributorsPerYear(), pastYears, landscapeAnalysisResults.getLatestCommitDate());
            contributorsPerYear.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());
            if (contributorsPerYear.size() > pastYears) {
                contributorsPerYear = contributorsPerYear.subList(0, pastYears);
            }
            contributorsPerYear.forEach(c -> maxCommits[0] = Math.max(counter.getCount(c), maxCommits[0]));
        });

        projectsAnalysisResults.stream().limit(limit).forEach(projectAnalysis -> {
            report.startTableRow();
            CodeAnalysisResults projectAnalysisResults = projectAnalysis.getAnalysisResults();
            String name = projectAnalysisResults.getMetadata().getName();
            String logoLink = projectAnalysisResults.getMetadata().getLogoLink();

            report.addTableCell(getImageWithLink(projectAnalysis, logoLink), "text-align: center");

            report.addTableCell("<a href='" + this.getProjectReportUrl(projectAnalysis) + "' target='_blank'>"
                    + "<div>" + name + "</div></a>", "vertical-align: middle; min-width: 400px; max-width: 400px");

            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysisResults.getContributorsAnalysisResults();
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = projectAnalysisResults.getFilesHistoryAnalysisResults();
            int projectAgeYears = (int) Math.round(filesHistoryAnalysisResults.getAgeInDays() / 365.0);
            String age = projectAgeYears == 0 ? "<1y" : projectAgeYears + "y";
            report.addTableCell(age, "text-align: center; font-size: 90%");

            List<ContributionTimeSlot> contributorsPerYear = LandscapeReportGenerator.getContributionYears(contributorsAnalysisResults.getContributorsPerYear(), pastYears, landscapeAnalysisResults.getLatestCommitDate());
            contributorsPerYear.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());
            if (contributorsPerYear.size() > pastYears) {
                contributorsPerYear = contributorsPerYear.subList(0, pastYears);
            }
            report.startTableCell("white-space: nowrap; overflow-x: hidden;");
            String firstDate = filesHistoryAnalysisResults.getFirstDate();
            String firstYear = firstDate.length() >= 4 ? firstDate.substring(0, 4) : "";
            boolean reachedFirstYear[] = {false};
            contributorsPerYear.forEach(contributionTimeSlot -> {
                int count = counter.getCount(contributionTimeSlot);
                if (count > 0) {
                    int h = (int) (6 + 24.0 * count / maxCommits[0]);
                    report.addContentInDivWithTooltip("", +count + " in " + contributionTimeSlot.getTimeSlot(),
                            "margin: 0; margin-right: -4px; padding: 0; display: inline-block; vertical-align: bottom; width: 16px; " +
                                    "background-color: " + color + "; " +
                                    "opacity: 0.5; height: " + (h + "px"));
                } else {
                    if (contributionTimeSlot.getTimeSlot().substring(0, 4).compareTo(firstYear) >= 0) {
                        report.addContentInDivWithTooltip("", +count + " in the week of " + contributionTimeSlot.getTimeSlot(),
                                "margin: 0; margin-right: -4px; padding: 0; display: inline-block; vertical-align: bottom; width: 16px; " +
                                        "background-color: lightgrey; " +
                                        "opacity: 0.5; height: 5px;");
                    } else if (!reachedFirstYear[0]) {
                        reachedFirstYear[0] = true;
                        report.addContentInDiv(firstDate, "color: grey; vertical-align: top; margin-left: 6px; display: inline-block; font-size: 70%");
                    }
                }
            });
            report.endTableCell();
            report.addTableCell(contributorsAnalysisResults.getContributors().size() + "", "text-align: center; font-size: 90%");
            report.addTableCell(contributorsAnalysisResults.getCommitsCount() + "", "text-align: center; font-size: 90%");
            report.addTableCell(getRiskProfileVisual(projectAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution(), Palette.getFreshnessPalette()));
            report.addTableCell("<a href='" + this.getProjectReportUrl(projectAnalysis) + "' target='_blank'>"
                    + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 38) + "</div></a>", "text-align: center");
            report.endTableRow();
        });

        report.endTable();

        if (limit < projectsAnalysisResults.size()) {
            addShowMoreFooter(report, projectsAnalysisResults.size());
        }

    }

    private String getDuplicationVisual(Number duplicationPercentage) {
        if (duplicationPercentage.doubleValue() == 0) {
            return "<span style='color: grey; font-size: 70%'>not measured</span>";
        }
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(100);
        chart.setBarHeight(20);
        chart.setMaxBarWidth(100);
        chart.setBarStartXOffset(2);
        chart.setActiveColor("crimson");
        chart.setBackgroundColor("#9DC034");
        chart.setBackgroundStyle("");
        return chart.getPercentageSvg(duplicationPercentage.doubleValue(), "", "");
    }


    private String getRiskProfileVisual(RiskDistributionStats distributionStats, Palette palette) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(100);
        chart.setBarHeight(20);
        chart.setMaxBarWidth(100);
        chart.setBarStartXOffset(0);

        if (distributionStats != null) {
            List<Integer> values = Arrays.asList(
                    distributionStats.getVeryHighRiskValue(),
                    distributionStats.getHighRiskValue(),
                    distributionStats.getMediumRiskValue(),
                    distributionStats.getLowRiskValue(),
                    distributionStats.getNegligibleRiskValue());

            return chart.getStackedBarSvg(values, palette, "", "");
        } else {
            return "";
        }
    }


    private boolean showControls() {
        return landscapeAnalysisResults.getConfiguration().isShowProjectControls();
    }

    private void addProjectRow(RichTextReport report, ProjectAnalysisResults projectAnalysis) {
        CodeAnalysisResults analysisResults = projectAnalysis.getAnalysisResults();
        Metadata metadata = analysisResults.getMetadata();
        String logoLink = metadata.getLogoLink();

        String latestCommitDate = projectAnalysis.getAnalysisResults().getContributorsAnalysisResults().getLatestCommitDate();
        report.startTableRow(DateUtils.isCommittedLessThanDaysAgo(latestCommitDate, 90) ? ""
                : (DateUtils.isCommittedLessThanDaysAgo(latestCommitDate, 180) ? "color:#b0b0b0" : "color:#c3c3c3"));
        report.addTableCell(getImageWithLink(projectAnalysis, logoLink), "text-align: center");
        report.addTableCell("<a href='" + this.getProjectReportUrl(projectAnalysis) + "' target='_blank'>"
                + "<div>" + metadata.getName() + "</div></a>", "vertical-align: middle; min-width: 400px; max-width: 400px");
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
        List<Contributor> contributors = analysisResults.getContributorsAnalysisResults().getContributors()
                .stream().filter(c -> c.getCommitsCount() >= thresholdCommits).collect(Collectors.toCollection(ArrayList::new));

        int recentContributorsCount = (int) contributors.stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
        int rookiesCount = (int) contributors.stream().filter(c -> c.isRookie(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();

        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        StringBuilder locSummary = new StringBuilder();
        if (linesOfCodePerExtension.size() > 0) {
            locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
        } else {
            locSummary.append("-");
        }
        String lang = locSummary.toString().replace("> = ", ">");
        report.startTableCell("text-align: left");
        report.startDiv("min-width: 130px; white-space: nowrap; overflow: hidden");
        report.addHtmlContent(DataImageUtils.getLangDataImageDiv30(lang));
        report.addContentInDiv(lang, "vertical-align: middle; display: inline-block;margin-top: 5px");
        report.endDiv();
        report.endTableCell();
        report.addTableCell(FormattingUtils.formatCount(main.getLinesOfCode(), "-"), "text-align: center; font-size: 90%");

        report.addTableCell(FormattingUtils.formatCount(test.getLinesOfCode(), "-"), "text-align: center; font-size: 90%");
        report.addTableCell(FormattingUtils.formatCount(generated.getLinesOfCode() + build.getLinesOfCode() + other.getLinesOfCode(), "-"), "text-align: center; font-size: 90%");
        int projectAgeYears = (int) Math.round(analysisResults.getFilesHistoryAnalysisResults().getAgeInDays() / 365.0);
        String age = projectAgeYears == 0 ? "<1y" : projectAgeYears + "y";
        report.addTableCell(age, "text-align: center; font-size: 90%");
        report.addTableCell(latestCommitDate, "text-align: center; font-size: 90%");
        report.addTableCell(FormattingUtils.formatCount(recentContributorsCount, "-"), "text-align: center; font-size: 90%");
        report.addTableCell(FormattingUtils.formatCount(rookiesCount, "-"), "text-align: center; font-size: 90%");
        report.addTableCell(FormattingUtils.formatCount(analysisResults.getContributorsAnalysisResults().getCommitsCount30Days(), "-"), "text-align: center; font-size: 90%");
        String projectReportUrl = getProjectReportUrl(projectAnalysis);
        report.addTableCell("<a href='" + projectReportUrl + "' target='_blank'>"
                + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 40) + "</div></a>", "text-align: center; font-size: 90%");
        if (showTags()) {
            report.addTableCell(getTags(projectAnalysis), "color: black");
        }
        report.endTableRow();
    }

    private void addControls(RichTextReport report, CodeAnalysisResults analysisResults) {
        analysisResults.getControlResults().getGoalsAnalysisResults().forEach(goalsAnalysisResults -> {
            goalsAnalysisResults.getControlStatuses().forEach(status -> {
                String style = "display: inline-block; border: 2px; border-radius: 50%; height: 12px; width: 12px; background-color: " + ControlsReportGenerator.getColor(status.getStatus());
                MetricRangeControl control = status.getControl();
                String tooltip = control.getDescription() + "\n"
                        + control.getDesiredRange().getTextDescription() + "\n\n"
                        + "" + status.getMetric().getValue();
                report.addContentInDivWithTooltip(" ", tooltip, style);
            });
        });
    }

    private String getProjectReportFolderUrl(ProjectAnalysisResults projectAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/";
    }

    private String getProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "index.html";
    }

    private String getDuplicationProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "Duplication.html";
    }

    private String getFileSizeProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "FileSize.html";
    }

    private String getUnitSizeProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "UnitSize.html";
    }

    private String getConditionalComplexityReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "ConditionalComplexity.html";
    }

    private String getFileAgeProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "FileAge.html";
    }

    private String getFileChangeFrequencyProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "FileChangeFrequency.html";
    }

    private String getFeaturesProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "FeaturesOfInterest.html";
    }

    private String getControlsProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return getProjectReportFolderUrl(projectAnalysis) + "Controls.html";
    }

    private String getTags(ProjectAnalysisResults project) {
        StringBuilder tagsHtml = new StringBuilder();

        customTagsMap.getProjectTags(project).forEach(tag -> {
            tagsHtml.append("<div style='margin: 2px; padding: 5px; display: inline-block; background-color: " + getTabColor(tag) + "; font-size: 70%; border-radius: 10px'>");
            tagsHtml.append(tag.getTag());
            tagsHtml.append("</div>");
        });

        return tagsHtml.toString();
    }

    private String getTabColor(ProjectTag tag) {
        return tag.getGroup() != null && StringUtils.isNotBlank(tag.getGroup().getColor()) ? tag.getGroup().getColor() : "#99badd";
    }

}
