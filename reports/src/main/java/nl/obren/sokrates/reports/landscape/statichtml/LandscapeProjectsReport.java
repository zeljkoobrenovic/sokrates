package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.ReportConstants;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.generators.statichtml.ControlsReportGenerator;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.ProjectTag;
import nl.obren.sokrates.sourcecode.landscape.ProjectTagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class LandscapeProjectsReport {
    private static final Log LOG = LogFactory.getLog(LandscapeProjectsReport.class);

    private LandscapeAnalysisResults landscapeAnalysisResults;
    private int limit = 1000;
    private String link;
    private String linkLabel;
    private Map<String, TagStats> tagStatsMap = new HashMap<>();
    private File reportsFolder;

    public LandscapeProjectsReport(LandscapeAnalysisResults landscapeAnalysisResults, int limit) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.limit = limit;
    }

    public LandscapeProjectsReport(LandscapeAnalysisResults landscapeAnalysisResults, int limit, String link, String linkLabel) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.limit = limit;
        this.link = link;
        this.linkLabel = linkLabel;
    }

    public void saveProjectsReport(RichTextReport report, File reportsFolder, List<ProjectAnalysisResults> projectsAnalysisResults) {
        this.reportsFolder = reportsFolder;
        report.startTabGroup();
        boolean showCommits = landscapeAnalysisResults.getCommitsCount() > 0;
        if (showCommits) {
            report.addTab("commitsTrend", "Commits Trend", true);
            report.addTab("contributorsTrend", "Contributors Trend", false);
        }
        report.addTab("projects", "Size & Details", !showCommits);
        if (showCommits) {
            report.addTab("history", "History", false);
        }
        report.addTab("metrics", "Metrics", false);
        if (showCommits) {
            report.addTab("correlations", "Correlations", false);
        }
        if (showTags()) {
            report.addTab("tags", "Tags", false);
        }
        report.addTab("features", "Features of Interest", false);
        report.endTabGroup();
        addProjectsBySize(report, projectsAnalysisResults);
        if (showCommits) {
            addCommitBasedLists(report, projectsAnalysisResults);
            addHistory(report, projectsAnalysisResults);
        }
        if (showTags()) {
            report.startTabContentSection("tags", false);
            addTagStats(report);
            report.endTabContentSection();
        }
        report.startTabContentSection("metrics", false);
        addMetrics(report, projectsAnalysisResults);
        report.endTabContentSection();

        report.startTabContentSection("features", false);
        addFeaturesOfInterest(report);
        report.endTabContentSection();
    }

    private void addFeaturesOfInterest(RichTextReport report) {
        List<ProjectAnalysisResults> projectAnalysisResults = landscapeAnalysisResults.getProjectAnalysisResults();

        FeaturesOfInterestAggregator aggregator = new FeaturesOfInterestAggregator(projectAnalysisResults);
        aggregator.aggregateFeaturesOfInterest(limit);

        List<List<ProjectConcernData>> concerns = aggregator.getConcerns();
        List<List<ProjectConcernData>> projects = aggregator.getProjects();

        if (concerns.size() == 0) {
            report.addParagraph("No features of interest found in projects.");
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
        report.addTableCell("Projects (" + aggregator.getProjectsMap().size() + ")", "");
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

        correlationDiagramGenerator.addCorrelations("Recent Contributors vs. Project Main LOC", "main LOC", "recent contributors (30d)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Recent Commits (30 days) vs. Project Main LOC", "main LOC", "commits (30d)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days(),
                p -> p.getAnalysisResults().getMetadata().getName());

        correlationDiagramGenerator.addCorrelations("Age in Years vs. Project Main LOC", "main LOC", "age (years)",
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
                            "cumulative: top " + index[0] + " projects (" + percentageProjects + "%) = "
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
                            "cumulative: top " + index[0] + " projects (" + percentageProjects + "%) = "
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

    public void addProjectsBySize(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()
                        - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());

        boolean showCommits = landscapeAnalysisResults.getCommitsCount() > 0;
        report.startTabContentSection("projects", !showCommits);
        addSummaryGraphMainLoc(report, projectsAnalysisResults);
        report.startTable("width: 100%");
        int thresholdContributors = landscapeAnalysisResults.getConfiguration().getProjectThresholdContributors();
        List<String> headers = new ArrayList<>(Arrays.asList("", "Project" + (thresholdContributors > 1 ? "<br/>(" + thresholdContributors + "+&nbsp;contributors)" : ""),
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

        updateTagMap(projectsAnalysisResults);

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
        report.addTableHeader("", "Project", "Commits<br>(30d)" + (label.equalsIgnoreCase("Commits") ? "*" : ""),
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
        headers.addAll(Arrays.asList(new String[]{"", "Project", "Main<br>Lang", "Duplication", "File Size",
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
                report.addMultiColumnTableCell("<h3 style='margin: 0; margin-top: 14px; margin-bottom: 6px;'>Project not active in past 90 days</h3>", 11);
                report.endTableRow();
            }
            if (commits180Days == 0 && !startedInactiveSection180Days[0]) {
                startedInactiveSection180Days[0] = true;
                report.startTableRow();
                report.addMultiColumnTableCell("<h3 style='margin: 0; margin-top: 14px; margin-bottom: 6px;'>Project not active in past 180 days</h3>", 11);
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
        report.addTableHeader("", "Project",
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

    private void addTagStats(RichTextReport report) {
        renderTagDependencies();

        report.startDiv("margin: 12px; font-size: 90%");
        report.addHtmlContent("all tag dependencies: ");
        report.addNewTabLink("3D graph (via projects)", "visuals/tags_graph_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (via projects)", "visuals/tags_graph.svg");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (excluding projects)", "visuals/tags_graph_direct.svg");
        report.endDiv();

        report.startTable();
        report.addTableHeader("Tag", "# projects", "LOC<br>(main)", "LOC<br>(test)", "LOC<br>(active)", "LOC<br>(new)", "# commits<br>(30 days)", "# contributors<br>(30 days)");
        int index[] = {0};
        this.landscapeAnalysisResults.getConfiguration().getProjectTagGroups().forEach(tagGroup -> {
            index[0] += 1;
            report.startTableRow();
            report.startMultiColumnTableCell(8, "background-color: " + tagGroup.getColor());
            report.startDiv("margin-top: 8px");
            report.addHtmlContent(tagGroup.getName());
            if (StringUtils.isNotBlank(tagGroup.getDescription())) {
                report.addHtmlContent("<span style='color: grey;'>(" + tagGroup.getDescription() + "</span>)");
            }
            report.startDiv("margin: 5px; font-size: 80%");
            report.addHtmlContent("tag dependencies: ");
            report.addNewTabLink("3D graph (via projects)", "visuals/tags_graph_" + index[0] + "_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (via projects)", "visuals/tags_graph_" + index[0] + ".svg");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (excluding projects)", "visuals/tags_graph_" + index[0] + "_direct.svg");
            report.endDiv();

            report.endTableCell();
            report.endTableRow();
            tagGroup.getProjectTags().stream()
                    .sorted((a, b) -> (tagStatsMap.get(a.getTag()) != null && tagStatsMap.get(b.getTag()) != null) ? tagStatsMap.get(b.getTag()).getProjectsAnalysisResults().size() - tagStatsMap.get(a.getTag()).getProjectsAnalysisResults().size() : 0)
                    .forEach(projectTag -> {
                        String tagName = projectTag.getTag();
                        addTagRow(report, tagName, projectTag, tagGroup.getColor());
                    });
        });
        if (tagStatsMap.containsKey("")) {
            report.addMultiColumnTableCell("&nbsp;", 8);
            addTagRow(report, "", new ProjectTag(), "lightgrey");
        }
        report.endTable();


        visualizeTagProjects(report);
    }

    private void renderTagDependencies() {
        int index[] = {0};
        this.landscapeAnalysisResults.getConfiguration().getProjectTagGroups().forEach(tagGroup -> {
            index[0] += 1;
            String prefix = "tags_graph_" + index[0];
            List<ProjectTag> groupTags = tagGroup.getProjectTags();
            exportTagGraphs(prefix, groupTags);
        });

        List<ProjectTag> allTags = new ArrayList<>();
        this.landscapeAnalysisResults.getConfiguration().getProjectTagGroups().forEach(tagGroup -> {
            allTags.addAll(tagGroup.getProjectTags());
        });
        String prefix = "tags_graph";
        exportTagGraphs(prefix, allTags);
    }

    private void exportTagGraphs(String prefix, List<ProjectTag> groupTags) {
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, Set<String>> projectTagsMap = new HashMap<>();
        groupTags.stream().filter(tag -> tagStatsMap.get(tag.getTag()) != null)
                .forEach(tag -> {
                    TagStats stats = tagStatsMap.get(tag.getTag());
                    stats.getProjectsAnalysisResults().forEach(project -> {
                        String name = project.getAnalysisResults().getMetadata().getName();
                        dependencies.add(new ComponentDependency("[" + name + "]", tag.getTag()));
                        if (!projectTagsMap.containsKey(name)) {
                            projectTagsMap.put(name, new HashSet<>());
                        }
                        projectTagsMap.get(name).add(tag.getTag());
                    });
                });
        new Force3DGraphExporter().export3DForceGraph(dependencies, reportsFolder, prefix);

        List<ComponentDependency> directDependencies = new ArrayList<>();
        Map<String, ComponentDependency> directDependenciesMap = new HashMap<>();
        projectTagsMap.values().forEach(projectTags -> {
            projectTags.forEach(tag1 -> {
                projectTags.stream().filter(tag2 -> !tag1.equals(tag2)).forEach(tag2 -> {
                    String key1 = tag1 + "::" + tag2;
                    String key2 = tag2 + "::" + tag1;
                    if (directDependenciesMap.containsKey(key1)) {
                        directDependenciesMap.get(key1).increment(1);
                    } else if (directDependenciesMap.containsKey(key2)) {
                        directDependenciesMap.get(key2).increment(1);
                    } else {
                        ComponentDependency directDependency = new ComponentDependency(
                                tag1 + " (" + tagStatsMap.get(tag1).getProjectsAnalysisResults().size() + ")",
                                tag2 + " (" + tagStatsMap.get(tag2).getProjectsAnalysisResults().size() + ")");
                        directDependencies.add(directDependency);
                        directDependenciesMap.put(key1, directDependency);
                    }
                });
            });
        });

        directDependencies.forEach(d -> d.setCount(d.getCount() / 2));

        GraphvizDependencyRenderer graphvizDependencyRenderer = new GraphvizDependencyRenderer();
        graphvizDependencyRenderer.setMaxNumberOfDependencies(1000);
        graphvizDependencyRenderer.setTypeGraph();
        graphvizDependencyRenderer.setOrientation("RL");
        List<String> keys = tagStatsMap.keySet().stream().filter(t -> tagStatsMap.get(t) != null).collect(Collectors.toList());
        String graphvizContent = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(keys), dependencies);
        String graphvizContentDirect = graphvizDependencyRenderer.getGraphvizContent(new ArrayList<>(), directDependencies);
        try {
            FileUtils.write(new File(reportsFolder, "visuals/" + prefix + ".svg"), GraphvizUtil.getSvgFromDot(graphvizContent), StandardCharsets.UTF_8);
            FileUtils.write(new File(reportsFolder, "visuals/" + prefix + "_direct.svg"), GraphvizUtil.getSvgFromDot(graphvizContentDirect), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.info(e);
        }
    }

    private void visualizeTagProjects(RichTextReport report) {
        report.startDiv("margin: 6px; margin-top: 44px;");
        report.startShowMoreBlock("show visuals...");
        int maxLoc = this.landscapeAnalysisResults.getProjectAnalysisResults().stream()
                .map(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .reduce((a, b) -> Math.max(a, b)).get();
        int maxCommits = this.landscapeAnalysisResults.getProjectAnalysisResults().stream()
                .map(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .reduce((a, b) -> Math.max(a, b)).get();
        this.landscapeAnalysisResults.getConfiguration().getProjectTagGroups().forEach(tagGroup -> {
            tagGroup.getProjectTags().forEach(tag -> {
                String tagName = tag.getTag();
                visualizeTag(report, maxLoc, maxCommits, tagName);
            });
        });
        if (tagStatsMap.containsKey("")) {
            visualizeTag(report, maxLoc, maxCommits, "");
        }
        report.endShowMoreBlock();
        report.endDiv();
    }

    private void visualizeTag(RichTextReport report, int maxLoc, int maxCommits, String tagName) {
        TagStats stats = tagStatsMap.get(tagName);
        if (stats == null) {
            return;
        }

        report.startDiv("margin: 18px;");
        report.addContentInDiv("<b>" + (tagName.isBlank() ? "Untagged" : tagName) + "</b> (" + stats.getProjectsAnalysisResults().size() + ")", "margin-bottom: 5px");
        List<ProjectAnalysisResults> projects = stats.getProjectsAnalysisResults();
        projects.sort((a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days() - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days());
        projects.forEach(project -> {
            int loc = project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            int barSize = 3 + (int) Math.round(Math.sqrt(4900 * ((double) loc / maxLoc)));
            int commitsCount30Days = project.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days();
            double opacity = commitsCount30Days > 0 ? 0.4 + 0.6 * commitsCount30Days / maxCommits : 0.0;
            report.startNewTabLink(getProjectReportUrl(project), "");
            report.startDivWithLabel(tooltip(project),
                    "border: 1px solid grey; border-radius: 50%;" +
                            "display: inline-block; " +
                            "padding: 0;" +
                            "vertical-align: middle; " +
                            "overflow: none; " +
                            "width: " + (barSize + 2) + "px; " +
                            "height: " + (barSize + 2) + "px; ");
            report.startDiv(" margin: 0;border-radius: 50%;" +
                    "opacity: " + opacity + ";" +
                    "background-color: " + getTabColor(stats.getTag()) + "; " +
                    "border: 1px solid lightgrey; cursor: pointer;" +
                    "width: " + barSize + "px; " +
                    "height: " + barSize + "px; ");
            report.endDiv();
            report.endDiv();
            report.endNewTabLink();
        });
        report.endDiv();
    }

    private String tooltip(ProjectAnalysisResults project) {
        CodeAnalysisResults analysis = project.getAnalysisResults();
        return analysis.getMetadata().getName() + "\n\n" +
                analysis.getContributorsAnalysisResults().getCommitsCount30Days() + " commits (30 days)" + "\n" +
                analysis.getContributorsAnalysisResults().getContributors()
                        .stream().filter(contributor -> contributor.getCommitsCount30Days() > 0).count() + " contributors (30 days)" + "\n" +
                FormattingUtils.formatCount(analysis.getMainAspectAnalysisResults().getLinesOfCode()) + " LOC";
    }

    private void addTagRow(RichTextReport report, String tagName, ProjectTag tag, String color) {
        TagStats stats = tagStatsMap.get(tagName);
        if (stats == null) {
            return;
        }
        report.startTableRow("text-align: center");
        report.startTableCell();
        if (StringUtils.isNotBlank(tagName)) {
            String tooltip = "";

            if (tag.getPatterns().size() > 0) {
                tooltip += "includes projects with names like:\n  - " + tag.getPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
            }
            if (tag.getExcludePatterns().size() > 0) {
                tooltip += "excludes projects with names like:\n  - " + tag.getExcludePatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
            }
            if (tag.getPathPatterns().size() > 0) {
                tooltip += "includes projects with at least one file matching:\n  - " + tag.getPathPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
            }
            if (tag.getExcludePathPatterns().size() > 0) {
                tooltip += "excludes projects with at least one file matching:\n  - " + tag.getExcludePathPatterns().stream().collect(Collectors.joining("\n  - ")) + "\n";
            }
            if (tag.getMainExtensions().size() > 0) {
                tooltip += "includes projects with main extensions:\n  - " + tag.getMainExtensions().stream().collect(Collectors.joining("\n  - ")) + "\n";
            }

            report.addContentInDivWithTooltip(tagName,
                    tooltip,
                    "cursor: help; padding: 4px; border-radius: 6px; background-color: " + color);
        } else {
            report.addContentInDiv("Untagged");
        }
        report.endTableCell();
        if (stats != null) {
            List<ProjectAnalysisResults> projectsAnalysisResults = new ArrayList<>(stats.getProjectsAnalysisResults());
            projectsAnalysisResults.sort((a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
            int count = projectsAnalysisResults.size();
            report.startTableCell("text-align: left;");
            report.startShowMoreBlock("<b>" + count + "</b>" + (count == 1 ? " project" : " projects"));
            projectsAnalysisResults.forEach(project -> {
                CodeAnalysisResults projectAnalysisResults = project.getAnalysisResults();
                String projectReportUrl = getProjectReportUrl(project);
                report.addContentInDiv(
                        "<a href='" + projectReportUrl + "' target='_blank' style='margin-left: 10px'>" + projectAnalysisResults.getMetadata().getName() + "</a> "
                                + "<span color='lightgrey'>(<b>"
                                + FormattingUtils.formatCount(projectAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode(), "-") + "</b> LOC)</span>");
            });
            report.endShowMoreBlock();
            report.endTableCell();
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                    .sum()), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode())
                    .sum(), "-"), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(LandscapeAnalysisResults.getLoc1YearActive(projectsAnalysisResults), "-"), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(LandscapeAnalysisResults.getLocNew(projectsAnalysisResults), "-"), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                    .sum(), "-"), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(getRecentContributorCount(projectsAnalysisResults), "-"), "text-align: center");
        } else {
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
            report.addTableCell("");
        }

        report.endTableRow();
    }

    private int getRecentContributorCount(List<ProjectAnalysisResults> projectsAnalysisResults) {
        Set<String> ids = new HashSet<>();
        projectsAnalysisResults.forEach(project -> {
            project.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream()
                    .filter(c -> c.getCommitsCount30Days() > 0).forEach(c -> ids.add(c.getEmail()));
        });
        return ids.size();
    }

    private void addProjectRow(RichTextReport report, ProjectAnalysisResults projectAnalysis) {
        CodeAnalysisResults analysisResults = projectAnalysis.getAnalysisResults();
        Metadata metadata = analysisResults.getMetadata();
        String logoLink = metadata.getLogoLink();

        String latestCommitDate = projectAnalysis.getAnalysisResults().getContributorsAnalysisResults().getLatestCommitDate();
        DateUtils.isCommittedLessThanDaysAgo(latestCommitDate, 90);
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

    private boolean showTags() {
        for (ProjectTagGroup tagGroup : landscapeAnalysisResults.getConfiguration().getProjectTagGroups()) {
            if (tagGroup.getProjectTags().size() > 0) return true;
        }
        return false;
    }

    private String getTags(ProjectAnalysisResults project) {
        List<NumericMetric> linesOfCodePerExtension = project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension();
        linesOfCodePerExtension.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
        String mainTech = linesOfCodePerExtension.size() > 0 ? linesOfCodePerExtension.get(0).getName().replaceAll(".*[.]", "") : "";
        List<ProjectTag> tags = new ArrayList<>();
        this.landscapeAnalysisResults.getConfiguration().getProjectTagGroups().forEach(tagGroup -> tags.addAll(tagGroup.getProjectTags()));

        StringBuilder tagsHtml = new StringBuilder();

        boolean tagged[] = {false};

        tags.forEach(tag -> {
            if (isTagged(project, mainTech, tag)) {
                tagsHtml.append("<div style='margin: 2px; padding: 5px; display: inline-block; background-color: " + getTabColor(tag) + "; font-size: 70%; border-radius: 10px'>");
                tagsHtml.append(tag.getTag());
                tagsHtml.append("</div>");

                tagged[0] = true;
            }
        });

        return tagsHtml.toString();
    }

    private void updateTagMap(List<ProjectAnalysisResults> projects) {
        projects.forEach(project -> {
            List<NumericMetric> linesOfCodePerExtension = project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension();
            linesOfCodePerExtension.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
            String mainTech = linesOfCodePerExtension.size() > 0 ? linesOfCodePerExtension.get(0).getName().replaceAll(".*[.]", "") : "";
            List<ProjectTag> tags = new ArrayList<>();
            this.landscapeAnalysisResults.getConfiguration().getProjectTagGroups().forEach(tagGroup -> tags.addAll(tagGroup.getProjectTags()));

            boolean tagged[] = {false};

            tags.forEach(tag -> {
                if (isTagged(project, mainTech, tag)) {
                    if (!tagStatsMap.containsKey(tag.getTag())) {
                        tagStatsMap.put(tag.getTag(), new TagStats(tag));
                    }
                    tagStatsMap.get(tag.getTag()).getProjectsAnalysisResults().add(project);
                    tagged[0] = true;
                }
            });

            if (!tagged[0]) {
                if (!tagStatsMap.containsKey("")) {
                    tagStatsMap.put("", new TagStats(new ProjectTag()));
                }
                tagStatsMap.get("").getProjectsAnalysisResults().add(project);
            }

        });
    }

    private boolean isTagged(ProjectAnalysisResults project, String mainTech, ProjectTag tag) {
        String name = project.getAnalysisResults().getMetadata().getName();
        return !tag.excludesMainTechnology(mainTech) &&
                ((tag.matchesName(name) && !tag.excludeName(name)) || tag.matchesMainTechnology(mainTech) || tag.matchesPath(project.getFiles()));
    }

    private String getTabColor(ProjectTag tag) {
        return tag.getGroup() != null && StringUtils.isNotBlank(tag.getGroup().getColor()) ? tag.getGroup().getColor() : "#99badd";
    }

}
