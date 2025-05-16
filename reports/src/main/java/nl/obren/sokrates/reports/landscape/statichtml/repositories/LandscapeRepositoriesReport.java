package nl.obren.sokrates.reports.landscape.statichtml.repositories;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.ReportConstants;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.generators.statichtml.ControlsReportGenerator;
import nl.obren.sokrates.reports.landscape.data.LandscapeDataExport;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator;
import nl.obren.sokrates.reports.landscape.utils.CorrelationDiagramGenerator;
import nl.obren.sokrates.reports.landscape.utils.Counter;
import nl.obren.sokrates.reports.landscape.utils.FeaturesOfInterestAggregator;
import nl.obren.sokrates.reports.landscape.utils.RepositoryConcernData;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.RepositoryTag;
import nl.obren.sokrates.sourcecode.landscape.TagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class LandscapeRepositoriesReport {
    private static final Log LOG = LogFactory.getLog(LandscapeRepositoriesReport.class);

    private LandscapeAnalysisResults landscapeAnalysisResults;
    private int limit = 1000;
    private TagMap customTagsMap;
    private String link;
    private String linkLabel;
    private File reportsFolder;
    private List<TagGroup> tagGroups = new ArrayList<>();

    private String type = "";

    public LandscapeRepositoriesReport(LandscapeAnalysisResults landscapeAnalysisResults, int limit, TagMap customTagsMap) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.limit = limit;
        this.customTagsMap = customTagsMap;
        this.type = "long report";
    }

    public LandscapeRepositoriesReport(LandscapeAnalysisResults landscapeAnalysisResults, int limit, String link, String linkLabel, TagMap customTagsMap) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.limit = limit;
        this.link = link;
        this.linkLabel = linkLabel;
        this.customTagsMap = customTagsMap;
        this.type = "short report";
    }

    public void saveRepositoriesReport(RichTextReport report, File reportsFolder, List<RepositoryAnalysisResults> repositoryAnalysisResults, List<TagGroup> tagGroups) {
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
            report.addTab("newest", "Creation", false);
        }
        report.addTab("metrics", "Metrics", false);
        if (showCommits) {
            report.addTab("correlations", "Correlations", false);
        }
        report.addTab("features", "Features of Interest", false);
        report.addLinkInTab("Explorer...", "repositories-explorer.html");
        report.addLinkInTab("Files...", "files-explorer.html");
        report.endTabGroup();
        ProcessingStopwatch.end("reporting/repositories/" + type + "/preparing");

        ProcessingStopwatch.start("reporting/repositories/" + type + "/size");
        addRepositoriesDetails(report, repositoryAnalysisResults);
        ProcessingStopwatch.end("reporting/repositories/" + type + "/size");
        if (showCommits) {
            ProcessingStopwatch.start("reporting/repositories/" + type + "/commits");
            addCommitBasedLists(report, repositoryAnalysisResults);
            ProcessingStopwatch.end("reporting/repositories/" + type + "/commits");
            ProcessingStopwatch.start("reporting/repositories/" + type + "/history");
            addHistory(report, repositoryAnalysisResults);
            addNewest(report, repositoryAnalysisResults);
            ProcessingStopwatch.end("reporting/repositories/" + type + "/history");
        }

        report.startTabContentSection("metrics", false);
        ProcessingStopwatch.start("reporting/repositories/" + type + "/metrics");
        addMetrics(report, repositoryAnalysisResults);
        ProcessingStopwatch.end("reporting/repositories/" + type + "/metrics");
        report.endTabContentSection();

        report.startTabContentSection("features", false);
        ProcessingStopwatch.start("reporting/repositories/" + type + "/features of interest");
        addFeaturesOfInterest(report);
        ProcessingStopwatch.end("reporting/repositories/" + type + "/features of interest");
        report.endTabContentSection();
    }

    private void addFeaturesOfInterest(RichTextReport report) {
        List<RepositoryAnalysisResults> repositoryAnalysisResults = landscapeAnalysisResults.getRepositoryAnalysisResults();

        FeaturesOfInterestAggregator aggregator = new FeaturesOfInterestAggregator(repositoryAnalysisResults);
        aggregator.aggregateFeaturesOfInterest(limit);

        List<List<RepositoryConcernData>> concerns = aggregator.getConcerns();
        List<List<RepositoryConcernData>> repositories = aggregator.getRepositories();

        if (concerns.size() == 0) {
            report.addParagraph("No features of interest found in repositories.");
            return;
        }

        report.startTable();

        report.startTableRow("white-space: nowrap");
        report.addTableCell("", "border: none");
        report.addTableCell("", "border: none");
        concerns.stream().filter(concern -> concern.size() > 0).forEach(concern -> {
            report.startTableCellColSpan(2, "");
            report.addContentInDiv(concern.get(0).getConcern().getName() + " (" + concern.size() + ")", "text-align: center");
            report.endTableCell();
        });
        report.addTableCell("", "border: none");
        report.endTableRow();

        report.startTableRow("white-space: nowrap");
        report.addTableCell("", "border-left: none; border-top: none");
        report.addTableCell("Repositories (" + aggregator.getRepositoriesMap().size() + ")", "");
        concerns.stream().filter(concern -> concern.size() > 0).forEach(concern -> {
            int concernsCount = concern.stream().mapToInt(c -> c.getConcern().getNumberOfRegexLineMatches()).reduce((a, b) -> a + b).orElse(0);
            report.addTableCell(concernsCount + " matches", "font-size: 70%; text-align: center;");
            int filesCount = concern.stream().mapToInt(c -> c.getConcern().getFilesCount()).reduce((a, b) -> a + b).orElse(0);
            report.addTableCell(filesCount + " files", "font-size: 70%; text-align: center;");
        });
        report.addTableCell("Details", "font-size: 70%");
        report.endTableRow();

        repositories.forEach(repository -> {
            report.startTableRow("white-space: nowrap");
            RepositoryConcernData repositoryConcernData = repository.get(0);
            addLangTableCell(report, repository.get(0).getRepository().getAnalysisResults().getMainAspectAnalysisResults());
            String repositoryName = repositoryConcernData.getRepositoryName();
            String name = getRepositoryDisplayHtml(repositoryName);

            report.addTableCell("<a href='" + this.getFeaturesReportUrl(repositoryConcernData.getRepository()) + "' target='_blank'>"
                    + "" + name + "</a>", "");
            concerns.stream().filter(concern -> concern.size() > 0).forEach(concern -> {
                String key = repositoryName + "::" + concern.get(0).getConcern().getName();
                int instancesCount = aggregator.getRepositoriesConcernMap().containsKey(key) ? aggregator.getRepositoriesConcernMap().get(key).getConcern().getNumberOfRegexLineMatches() : 0;
                report.addTableCell("" + (instancesCount > 0 ? instancesCount : "-"), "text-align: center;");
                int filesCount = aggregator.getRepositoriesConcernMap().containsKey(key) ? aggregator.getRepositoriesConcernMap().get(key).getConcern().getFilesCount() : 0;
                report.addTableCell("" + (filesCount > 0 ? filesCount : "-"), "text-align: center;");
            });
            report.addTableCell("<a href='" + this.getFeaturesReportUrl(repositoryConcernData.getRepository()) + "' target='_blank'>"
                    + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 38) + "</div></a>", "text-align: center");
            report.endTableRow();
        });

        report.endTable();
        if (limit < aggregator.getRepositoriesMap().size()) {
            addShowMoreFooter(report, aggregator.getRepositoriesMap().size());
        }
    }

    public void addMetrics(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        Collections.sort(repositoryAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount());
        Collections.sort(repositoryAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount180Days()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount180Days());
        Collections.sort(repositoryAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days());
        addMetricsTable(report, repositoryAnalysisResults);
    }

    public void addHistory(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        report.startTabContentSection("history", false);
        List<RepositoryAnalysisResults> sorted = new ArrayList<>(repositoryAnalysisResults);
        Collections.sort(sorted,
                (a, b) -> b.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays()
                        - a.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays());
        addSummaryGraphHistory(report, sorted);
        addHistory(report, sorted, "Commits", "blue", (slot) -> slot.getCommitsCount());
        report.endTabContentSection();
    }

    public void addNewest(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        report.startTabContentSection("newest", false);
        List<RepositoryAnalysisResults> sorted = new ArrayList<>(repositoryAnalysisResults);
        Collections.sort(sorted,
                (a, b) -> a.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays()
                        - b.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays());
        addSummaryGraphNewReposPerYear(report, sorted);
        addHistory(report, sorted, "Commits", "blue", (slot) -> slot.getCommitsCount());
        report.endTabContentSection();
    }

    public void addCommitBasedLists(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        report.startTabContentSection("commitsTrend", true);
        Collections.sort(repositoryAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days());
        addSummaryGraphCommits(report, repositoryAnalysisResults);
        addCommitsTrend(report, repositoryAnalysisResults, "Commits", "blue", (slot) -> slot.getCommitsCount());
        report.endTabContentSection();
        report.startTabContentSection("contributorsTrend", false);
        Collections.sort(repositoryAnalysisResults,
                (a, b) -> (int) b.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count()
                        - (int) a.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count());
        addSummaryGraphContributors(report, repositoryAnalysisResults);
        addCommitsTrend(report, repositoryAnalysisResults, "Contributors", "darkred", (slot) -> slot.getContributorsCount());
        report.endTabContentSection();
    }

    public void addSummaryGraphCommits(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(repositoryAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .max().orElse(1), 1);
        int sum = repositoryAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .sum();
        int maxHeight = 64;
        int cummulative[] = {0};
        int index[] = {0};
        boolean breakPointReached[] = {false};
        int repositoriesCount = repositoryAnalysisResults.size();
        repositoryAnalysisResults.stream().limit(landscapeAnalysisResults.getConfiguration().getRepositoriesListLimit()).forEach(repositoryAnalysis -> {
            int commits30d = repositoryAnalysis.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days();
            cummulative[0] += commits30d;
            index[0] += 1;
            int height = (int) (1 + maxHeight * (double) commits30d / max);
            String name = getRepositoryDisplayHtml(repositoryAnalysis.getAnalysisResults().getMetadata().getName());
            double percentage = RichTextRenderingUtils.getPercentage(sum, cummulative[0]);
            double percentageRepositories = RichTextRenderingUtils.getPercentage(repositoriesCount, index[0]);
            String color = commits30d > 0 ? (!breakPointReached[0] && percentage >= 50 ? "blue" : "blue; opacity: 0.5") : "lightgrey; opacity: 0.5;";
            if (percentage >= 50) {
                breakPointReached[0] = true;
            }
            report.addContentInDivWithTooltip("",
                    name + ": " + commits30d + " lines of code (main)\n" +
                            "cumulative: top " + index[0] + " repositories (" + percentageRepositories + "%) = "
                            + cummulative[0] + " commits in past 30 days (" + percentage + "%)",
                    "margin: 0; padding: 0; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 8px; height: " + height + "px");
        });
        report.endDiv();
        report.startDiv("font-size: 80%; margin-bottom: 6px;");
        report.addNewTabLink("bubble chart", "visuals/bubble_chart_repositories_commits.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("tree map", "visuals/tree_map_repositories_commits.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("animated history (all time)", "visuals/racing_charts_commits_repositories.html?tickDuration=600");
        report.addHtmlContent(" | ");
        report.addNewTabLink("animated history (12 months window)", "visuals/racing_charts_commits_window_repositories.html?tickDuration=600");
        report.addHtmlContent(" | ");
        report.addNewTabLink("data", "data/" + LandscapeDataExport.REPOSITORIES_DATA_FILE_NAME);
        report.endDiv();
    }

    public void addSummaryGraphHistory(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(repositoryAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays())
                .max().orElse(1), 1);
        int maxHeight = 64;
        repositoryAnalysisResults.stream().limit(landscapeAnalysisResults.getConfiguration().getRepositoriesListLimit()).forEach(repositoryAnalysis -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = repositoryAnalysis.getAnalysisResults().getFilesHistoryAnalysisResults();
            int ageInDays = filesHistoryAnalysisResults.getAgeInDays();
            int height = (int) (1 + maxHeight * (double) ageInDays / max);
            String color = ageInDays > 0 ? "darkgreen" : "lightgrey";
            int repositoryAgeYears = (int) Math.round(filesHistoryAnalysisResults.getAgeInDays() / 365.0);
            String age = repositoryAgeYears == 0 ? "<1y" : repositoryAgeYears + "y";
            report.addContentInDivWithTooltip("",
                    repositoryAnalysis.getAnalysisResults().getMetadata().getName() + ": " + ageInDays + " days (" + age + ")",
                    "margin: 0; padding: 0; opacity: 0.5; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 8px; height: " + height + "px");
        });
        report.endDiv();
        report.startDiv("font-size: 80%; margin-bottom: 6px;");
        report.addNewTabLink("bubble chart", "visuals/bubble_chart_repositories_age.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("tree map", "visuals/tree_map_repositories_age.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("data", "data/" + LandscapeDataExport.REPOSITORIES_DATA_FILE_NAME);
        report.endDiv();
    }

    public void addSummaryGraphNewReposPerYear(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        report.addContentInDiv("repository creation per year:", "font-size: 80%; color: grey; margin-bottom: 5px;");

        int yearNow = Calendar.getInstance().get(Calendar.YEAR);
        List<List<RepositoryAnalysisResults>> perYear = new ArrayList<>();

        int goingBack = 20;
        for (int i = 0; i < goingBack; i++) {
            int year = yearNow - i;
            ArrayList<RepositoryAnalysisResults> yearRepos = new ArrayList<>();
            perYear.add(yearRepos);

            repositoryAnalysisResults.forEach(analysis -> {
                String firstDate = analysis.getAnalysisResults().getFilesHistoryAnalysisResults().getFirstDate();
                if (firstDate != null && firstDate.startsWith(year + "")) {
                    yearRepos.add(analysis);
                }
            });
        }

        int max = Math.max(perYear.stream().mapToInt(p -> p.size()).max().orElse(1), 1);

        int maxHeight = 64;

        for (int i = 0; i < goingBack; i++) {
            int year = yearNow - i;
            List<RepositoryAnalysisResults> yearRepos = perYear.get(i);

            int height = (int) (1 + maxHeight * (double) yearRepos.size() / max);
            String color = "skyblue";
            String info = year + ": " + yearRepos.size() + " repos";

            info += "\n\n";

            info += yearRepos.stream().map(r -> r.getAnalysisResults().getMetadata().getName()).collect(Collectors.joining("\n"));

            report.startDiv("display: inline-block");

            report.addContentInDivWithTooltip(yearRepos.size() > 0 ? "+" + yearRepos.size() + "" : "", "",
                    "font-size: 80%; text-align: center");
            report.addContentInDivWithTooltip("", info,
                    "margin: 0; padding: 0; opacity: 0.5; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 44px; height: " + height + "px");
            report.addContentInDivWithTooltip(year + "", "",
                    "font-size: 70%; text-align: center; color: grey");

            report.endDiv();
        }
        report.endDiv();
    }

    public void addSummaryGraphContributors(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(repositoryAnalysisResults.stream()
                .mapToInt(p -> (int) p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count())
                .max().orElse(1), 1);
        int maxHeight = 64;
        repositoryAnalysisResults.stream().limit(landscapeAnalysisResults.getConfiguration().getRepositoriesListLimit()).forEach(repositoryAnalysis -> {
            int contributors30d = (int) repositoryAnalysis.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
            int height = (int) (1 + maxHeight * (double) contributors30d / max);
            String color = contributors30d > 0 ? "darkred" : "lightgrey";
            report.addContentInDivWithTooltip("",
                    repositoryAnalysis.getAnalysisResults().getMetadata().getName() + ": " + contributors30d + " contributors (30 days)",
                    "margin: 0; padding: 0; opacity: 0.5; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 8px; height: " + height + "px");
        });
        report.endDiv();
        report.startDiv("font-size: 80%; margin-bottom: 6px;");
        report.addNewTabLink("bubble chart", "visuals/bubble_chart_repositories_contributors.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("tree map", "visuals/tree_map_repositories_contributors.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("data", "data/" + LandscapeDataExport.REPOSITORIES_DATA_FILE_NAME);
        report.endDiv();
    }

    public void addSummaryGraphMainLoc(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(repositoryAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .max().orElse(1), 1);
        int sum = repositoryAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .sum();
        int cumulative[] = {0};
        int index[] = {0};
        int maxHeight = 64;
        boolean breakPointReached[] = {false};
        int repositoriesCount = repositoryAnalysisResults.size();
        repositoryAnalysisResults.stream().limit(landscapeAnalysisResults.getConfiguration().getRepositoriesListLimit()).forEach(repositoryAnalysis -> {
            int mainLoc = repositoryAnalysis.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            cumulative[0] += mainLoc;
            index[0] += 1;
            int height = (int) (1 + maxHeight * (double) mainLoc / max);
            String name = getRepositoryDisplayHtml(repositoryAnalysis.getAnalysisResults().getMetadata().getName());
            double percentage = RichTextRenderingUtils.getPercentage(sum, cumulative[0]);
            double percentageRepositories = RichTextRenderingUtils.getPercentage(repositoriesCount, index[0]);
            String color = mainLoc > 0 ? (!breakPointReached[0] && percentage >= 50 ? "blue" : "skyblue") : "lightgrey";
            if (percentage >= 50) {
                breakPointReached[0] = true;
            }
            report.addContentInDivWithTooltip("",
                    name + ": " + mainLoc + " lines of code (main)\n" +
                            "cumulative: top " + index[0] + " repositories (" + percentageRepositories + "%) = "
                            + cumulative[0] + " LOC (" + percentage + "%)",
                    "margin: 0; padding: 0; opacity: 0.9; margin-right: 1px; background-color: " + color + "; display: inline-block; width: 8px; height: " + height + "px");
        });
        report.endDiv();
        report.startDiv("font-size: 80%; margin-bottom: 6px;");
        report.addNewTabLink("bubble chart", "visuals/bubble_chart_repositories_loc.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("tree map", "visuals/tree_map_repositories_loc.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("data", "data/" + LandscapeDataExport.REPOSITORIES_DATA_FILE_NAME);
        report.endDiv();
    }


    private boolean showTags() {
        for (TagGroup tagGroup : tagGroups) {
            if (tagGroup.getRepositoryTags().size() > 0) return true;
        }
        return false;
    }

    public void addRepositoriesDetails(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        Collections.sort(repositoryAnalysisResults,
                (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()
                        - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());

        boolean showCommits = landscapeAnalysisResults.getCommitsCount() > 0;
        report.startTabContentSection("repositories", !showCommits);
        addSummaryGraphMainLoc(report, repositoryAnalysisResults);
        report.startTable("width: 100%");
        int thresholdContributors = landscapeAnalysisResults.getConfiguration().getRepositoryThresholdContributors();
        List<String> headers = new ArrayList<>(Arrays.asList("", "Repository" + (thresholdContributors > 1 ? "<br/>(" + thresholdContributors + "+&nbsp;contributors)" : ""),
                "LOC<br/>(main)*",
                "LOC<br/>(test)", "LOC<br/>(other)",
                "Age", "Latest<br>Commit Date",
                "Contributors<br>(30d)", "Rookies<br>(30d)", "Commits<br>(30d)"));
        headers.add("Report");
        if (showTags()) {
            headers.add("Tags");
        }
        report.addTableHeader(headers.toArray(String[]::new));

        repositoryAnalysisResults.stream().limit(limit).forEach(repositoryAnalysis -> {
            addRepositoryRow(report, repositoryAnalysis);
        });


        report.endTable();
        if (limit < repositoryAnalysisResults.size()) {
            addShowMoreFooter(report, repositoryAnalysisResults.size());
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

    private void addCommitsTrend(RichTextReport report, List<RepositoryAnalysisResults> repositoryAnalysisResults, String label, String color, Counter counter) {
        report.startTable();
        report.addTableHeader("", "Repository", "Commits<br>(30d)" + (label.equalsIgnoreCase("Commits") ? "*" : ""),
                "Contributors<br>(30d)" + (label.equalsIgnoreCase("Contributors") ? "*" : ""), "Rookies<br>(30d)", label + " per Week (past year)", "Details");
        int maxCommits[] = {1};
        int pastWeeks = 52;
        repositoryAnalysisResults.forEach(repositoryAnalysis -> {
            List<ContributionTimeSlot> contributorsPerWeek = LandscapeReportGenerator.getContributionWeeks(repositoryAnalysis.getAnalysisResults().getContributorsAnalysisResults().getContributorsPerWeek(), pastWeeks, landscapeAnalysisResults.getLatestCommitDate());
            contributorsPerWeek.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());
            if (contributorsPerWeek.size() > pastWeeks) {
                contributorsPerWeek = contributorsPerWeek.subList(0, pastWeeks);
            }
            contributorsPerWeek.forEach(c -> maxCommits[0] = Math.max(counter.getCount(c), maxCommits[0]));
        });
        repositoryAnalysisResults.stream().limit(limit).forEach(repositoryAnalysis -> {
            report.startTableRow("white-space: nowrap");
            String name = getRepositoryDisplayHtml(repositoryAnalysis.getAnalysisResults().getMetadata().getName());
            addLangTableCell(report, repositoryAnalysis.getAnalysisResults().getMainAspectAnalysisResults());
            report.addTableCell("<a href='" + this.getRepositoryReportUrl(repositoryAnalysis) + "' target='_blank'>"
                    + "<div>" + name + "</div></a>", "overflow: hidden; white-space: nowrap; vertical-align: middle; min-width: 400px; max-width: 400px");
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysis.getAnalysisResults().getContributorsAnalysisResults();
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
            report.addTableCell("<a href='" + this.getRepositoryReportUrl(repositoryAnalysis) + "' target='_blank'>"
                    + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 38) + "</div></a>", "text-align: center");
            report.endTableRow();
        });

        report.endTable();

        if (limit < repositoryAnalysisResults.size()) {
            addShowMoreFooter(report, repositoryAnalysisResults.size());
        }

    }

    private String getImageWithLink(RepositoryAnalysisResults repositoryAnalysis, String logoLink) {
        String prefix = landscapeAnalysisResults.getConfiguration().getRepositoryReportsUrlPrefix();
        return "<a href='" + this.getRepositoryReportUrl(repositoryAnalysis) + "' target='_blank'>" +
                (StringUtils.isNotBlank(logoLink)
                        ? ("<img src='" + getLogoLink(prefix + repositoryAnalysis.getSokratesRepositoryLink()
                        .getHtmlReportsRoot().replace("/index.html", ""), logoLink) + "' " +
                        "style='width: 20px' " +
                        "onerror=\"this.onerror=null;this.src='" + ReportConstants.SOKRATES_SVG_ICON_SMALL_BASE64 + "'\">")
                        : ReportConstants.SOKRATES_SVG_ICON_SMALL) +
                "</a>";
    }

    private String getLogoLink(String repositoryLinkPrefix, String link) {
        return link.startsWith("/") || link.contains("://") || link.startsWith("data:image")
                ? link
                : StringUtils.appendIfMissing(repositoryLinkPrefix, "/") + link;
    }

    private void addMetricsTable(RichTextReport report, List<RepositoryAnalysisResults> repositoriesAnalysisResults) {
        report.startTable();
        List<String> headers = new ArrayList<>();
        headers.addAll(Arrays.asList(new String[]{"Main<br>Lang", "Repository", "Duplication", "File Size",
                "Unit Size", "Conditional<br>Complexity", "Newness", "Freshness", "Update<br>Frequency"}));
        if (showControls()) {
            headers.add("Controls");
        }

        report.addTableHeader(headers.toArray(new String[headers.size()]));

        boolean startedInactiveSection90Days[] = {false};
        boolean startedInactiveSection180Days[] = {false};

        repositoriesAnalysisResults.stream().filter(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() > 0).limit(limit).forEach(repositoryAnalysis -> {
            int commits90Days = repositoryAnalysis.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days();
            int commits180Days = repositoryAnalysis.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount180Days();
            if (commits90Days == 0 && commits180Days > 0 && !startedInactiveSection90Days[0]) {
                startedInactiveSection90Days[0] = true;
                report.startTableRow("white-space: nowrap");
                report.addMultiColumnTableCell("<h3 style='margin: 0; margin-top: 14px; margin-bottom: 6px;'>Repository not active in past 90 days</h3>", 11);
                report.endTableRow();
            }
            if (commits180Days == 0 && !startedInactiveSection180Days[0]) {
                startedInactiveSection180Days[0] = true;
                report.startTableRow("white-space: nowrap");
                report.addMultiColumnTableCell("<h3 style='margin: 0; margin-top: 14px; margin-bottom: 6px;'>Repository not active in past 180 days</h3>", 11);
                report.endTableRow();
            }
            report.startTableRow("white-space: nowrap" + (commits90Days > 0 ? "" : "; opacity: 0.7"));
            CodeAnalysisResults repositoryAnalysisResults = repositoryAnalysis.getAnalysisResults();
            String name = getRepositoryDisplayHtml(repositoryAnalysis.getAnalysisResults().getMetadata().getName());

            AspectAnalysisResults main = repositoryAnalysis.getAnalysisResults().getMainAspectAnalysisResults();

            addLangTableCell(report, main);

            String locText = FormattingUtils.formatCount(main.getLinesOfCode());
            String commits90DaysText = commits90Days > 0 ? ", <b>" + FormattingUtils.formatCount(commits90Days) + "</b> commits (90d)" : "";
            report.addTableCell("<a href='" + this.getRepositoryReportUrl(repositoryAnalysis) + "' target='_blank'>"
                            + "<div>" + name + "</div><div style='color: black; font-size: 80%'><b>" + locText + "</b> LOC (main)" + commits90DaysText + "</div></a>",
                    "overflow: hidden; white-space: nowrap; vertical-align: middle; max-width: 400px");

            report.addTableCell("<a href='" + this.getDuplicationReportUrl(repositoryAnalysis) + "' target='_blank'>" +
                    getDuplicationVisual(repositoryAnalysisResults.skipDuplicationAnalysis(), repositoryAnalysisResults.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getFileSizeReportUrl(repositoryAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(repositoryAnalysisResults.getFilesAnalysisResults().getOverallFileSizeDistribution(), Palette.getRiskPalette()) +
                    "</a>", "text-align: center");

            report.addTableCell("<a href='" + this.getUnitSizeReportUrl(repositoryAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(repositoryAnalysisResults.getUnitsAnalysisResults().getUnitSizeRiskDistribution(), Palette.getRiskPalette()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getConditionalComplexityReportUrl(repositoryAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(repositoryAnalysisResults.getUnitsAnalysisResults().getConditionalComplexityRiskDistribution(), Palette.getRiskPalette()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getFileAgeReportUrl(repositoryAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(repositoryAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution(), Palette.getAgePalette()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getFileAgeReportUrl(repositoryAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(repositoryAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution(), Palette.getFreshnessPalette()) +
                    "</a>", "text-align: center");
            report.addTableCell("<a href='" + this.getFileChangeFrequencyReportUrl(repositoryAnalysis) + "' target='_blank'>" +
                    getRiskProfileVisual(repositoryAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution(), Palette.getHeatPalette()) +
                    "</a>", "text-align: center");

            if (showControls()) {
                report.startTableCell("text-align: center; font-size: 90%");
                report.addHtmlContent("<a target='_blank' href='" + this.getControlsReportUrl(repositoryAnalysis) + "'>");
                addControls(report, repositoryAnalysisResults);
                report.addHtmlContent("</a>");
                report.endTableCell();
            }
            report.addTableCell("<a href='" + this.getRepositoryReportUrl(repositoryAnalysis) + "' target='_blank'>"
                    + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 38) + "</div></a>", "text-align: center");
            report.endTableRow();
        });

        report.endTable();

        if (limit < repositoriesAnalysisResults.size()) {
            addShowMoreFooter(report, repositoriesAnalysisResults.size());
        }

    }

    private static void addLangTableCell(RichTextReport report, AspectAnalysisResults main) {
        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        StringBuilder locSummary = new StringBuilder();
        if (linesOfCodePerExtension.size() > 0) {
            locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
        } else {
            locSummary.append("-");
        }
        String lang = locSummary.toString().replace("> = ", ">");
        report.startTableCell("text-align: left; max-width: 38px;");
        report.startDiv("white-space: nowrap; overflow: hidden");
        report.addHtmlContent(DataImageUtils.getLangDataImageDiv36(lang));
        report.endDiv();
        report.endTableCell();
    }

    private void addHistory(RichTextReport report, List<RepositoryAnalysisResults> repositoriesAnalysisResults, String label, String color, Counter counter) {
        report.startTable();
        report.addTableHeader("", "Repository",
                "Age", label + " per Year", "Contributors", "Commits", "Freshness", "Details");
        int pastYears = landscapeAnalysisResults.getConfiguration().getRepositoriesHistoryLimit();
        int maxCommits[] = {1};
        repositoriesAnalysisResults.forEach(repositoryAnalysis -> {
            List<ContributionTimeSlot> contributorsPerYear = LandscapeReportGenerator.getContributionYears(repositoryAnalysis.getAnalysisResults().getContributorsAnalysisResults().getContributorsPerYear(), pastYears, landscapeAnalysisResults.getLatestCommitDate());
            contributorsPerYear.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());
            if (contributorsPerYear.size() > pastYears) {
                contributorsPerYear = contributorsPerYear.subList(0, pastYears);
            }
            contributorsPerYear.forEach(c -> maxCommits[0] = Math.max(counter.getCount(c), maxCommits[0]));
        });

        int listCount[] = {0};
        repositoriesAnalysisResults.forEach(repositoryAnalysis -> {
            if (repositoryAnalysis.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays() == 0) {
                return;
            }
            listCount[0]++;
            if (listCount[0] > limit) return;
            report.startTableRow("white-space: nowrap");
            CodeAnalysisResults repositoryAnalysisAnalysisResults = repositoryAnalysis.getAnalysisResults();
            String name = getRepositoryDisplayHtml(repositoryAnalysisAnalysisResults.getMetadata().getName());

            addLangTableCell(report, repositoryAnalysis.getAnalysisResults().getMainAspectAnalysisResults());

            report.addTableCell("<a href='" + this.getRepositoryReportUrl(repositoryAnalysis) + "' target='_blank'>"
                    + "<div>" + name + "</div></a>", "overflow: hidden; white-space: nowrap; vertical-align: middle; min-width: 400px; max-width: 400px");

            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisAnalysisResults.getContributorsAnalysisResults();
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = repositoryAnalysisAnalysisResults.getFilesHistoryAnalysisResults();
            int ageInDays = filesHistoryAnalysisResults.getAgeInDays();
            int repositoryAgeYears = (int) Math.round(ageInDays / 365.0);
            String age = repositoryAgeYears == 0 ? ageInDays + "d" : repositoryAgeYears + "y";
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
            report.addTableCell(getRiskProfileVisual(repositoryAnalysisAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution(), Palette.getFreshnessPalette()));
            report.addTableCell("<a href='" + this.getRepositoryReportUrl(repositoryAnalysis) + "' target='_blank'>"
                    + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 38) + "</div></a>", "text-align: center");
            report.endTableRow();
        });

        report.endTable();

        if (limit < repositoriesAnalysisResults.size()) {
            addShowMoreFooter(report, repositoriesAnalysisResults.size());
        }

    }

    private static String getRepositoryDisplayHtml(String name) {
        if (name.contains("/")) {
            int index = name.indexOf("/");
            String context = name.substring(0, index).trim();
            String remainder = name.substring(index + 1).trim();

            name = "<div style='font-size: 90%; color: grey'>" + context + "</div>" + "<div>" + remainder + "</div>";
        }
        return name;
    }

    private String getDuplicationVisual(boolean skipDuplication, Number duplicationPercentage) {
        if (skipDuplication) {
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
        return landscapeAnalysisResults.getConfiguration().isShowRepositoryControls();
    }

    private void addRepositoryRow(RichTextReport report, RepositoryAnalysisResults repositoryAnalysis) {
        CodeAnalysisResults analysisResults = repositoryAnalysis.getAnalysisResults();
        Metadata metadata = analysisResults.getMetadata();

        String latestCommitDate = repositoryAnalysis.getAnalysisResults().getContributorsAnalysisResults().getLatestCommitDate();
        report.startTableRow("white-space: nowrap;" + (DateUtils.isCommittedLessThanDaysAgo(latestCommitDate, 90) ? ""
                : (DateUtils.isCommittedLessThanDaysAgo(latestCommitDate, 180) ? "color:#b0b0b0" : "color:#c3c3c3")));
        addLangTableCell(report, repositoryAnalysis.getAnalysisResults().getMainAspectAnalysisResults());
        String name = getRepositoryDisplayHtml(metadata.getName());
        report.addTableCell("<a href='" + this.getRepositoryReportUrl(repositoryAnalysis) + "' target='_blank'>"
                + "<div>" + name + "</div></a>", "overflow: hidden; white-space: nowrap; vertical-align: middle; min-width: 400px; max-width: 400px");
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

        report.addTableCell(FormattingUtils.formatCount(main.getLinesOfCode(), "-"), "text-align: center; font-size: 90%");

        report.addTableCell(FormattingUtils.formatCount(test.getLinesOfCode(), "-"), "text-align: center; font-size: 90%");
        report.addTableCell(FormattingUtils.formatCount(generated.getLinesOfCode() + build.getLinesOfCode() + other.getLinesOfCode(), "-"), "text-align: center; font-size: 90%");
        int repositoryAgeYears = (int) Math.round(analysisResults.getFilesHistoryAnalysisResults().getAgeInDays() / 365.0);
        String age = repositoryAgeYears == 0 ? "<1y" : repositoryAgeYears + "y";
        report.addTableCell(age, "text-align: center; font-size: 90%");
        report.addTableCell(latestCommitDate, "text-align: center; font-size: 90%");
        report.addTableCell(FormattingUtils.formatCount(recentContributorsCount, "-"), "text-align: center; font-size: 90%");
        report.addTableCell(FormattingUtils.formatCount(rookiesCount, "-"), "text-align: center; font-size: 90%");
        report.addTableCell(FormattingUtils.formatCount(analysisResults.getContributorsAnalysisResults().getCommitsCount30Days(), "-"), "text-align: center; font-size: 90%");
        String repositoryReportUrl = getRepositoryReportUrl(repositoryAnalysis);
        report.addTableCell("<a href='" + repositoryReportUrl + "' target='_blank'>"
                + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 40) + "</div></a>", "text-align: center; font-size: 90%");
        if (showTags()) {
            report.addTableCell(getTags(repositoryAnalysis), "color: black");
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

    private String getRepositoryReportFolderUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getRepositoryReportsUrlPrefix() + repositoryAnalysis.getSokratesRepositoryLink().getHtmlReportsRoot() + "/";
    }

    private String getRepositoryReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "index.html";
    }

    private String getDuplicationReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "Duplication.html";
    }

    private String getFileSizeReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "FileSize.html";
    }

    private String getUnitSizeReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "UnitSize.html";
    }

    private String getConditionalComplexityReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "ConditionalComplexity.html";
    }

    private String getFileAgeReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "FileAge.html";
    }

    private String getFileChangeFrequencyReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "FileChangeFrequency.html";
    }

    private String getFeaturesReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "FeaturesOfInterest.html";
    }

    private String getControlsReportUrl(RepositoryAnalysisResults repositoryAnalysis) {
        return getRepositoryReportFolderUrl(repositoryAnalysis) + "Controls.html";
    }

    private String getTags(RepositoryAnalysisResults repository) {
        StringBuilder tagsHtml = new StringBuilder();

        customTagsMap.getRepositoryTags(repository).forEach(tag -> {
            tagsHtml.append("<div style='margin: 2px; padding: 5px; display: inline-block; background-color: " + getTabColor(tag) + "; font-size: 70%; border-radius: 10px'>");
            tagsHtml.append(tag.getTag());
            tagsHtml.append("</div>");
        });

        return tagsHtml.toString();
    }

    private String getTabColor(RepositoryTag tag) {
        return tag.getGroup() != null && StringUtils.isNotBlank(tag.getGroup().getColor()) ? tag.getGroup().getColor() : "#99badd";
    }

}
