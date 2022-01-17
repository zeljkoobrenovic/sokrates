package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.generators.statichtml.ControlsReportGenerator;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.ProjectTag;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

interface Counter {
    int getCount(ContributionTimeSlot slot);
}

public class LandscapeProjectsReport {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private Map<String, TagStats> tagStatsMap = new HashMap<>();

    public LandscapeProjectsReport(LandscapeAnalysisResults landscapeAnalysisResults) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
    }

    public void saveProjectsReport(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startTabGroup();
        report.addTab("projects", "Projects", true);
        boolean showCommits = landscapeAnalysisResults.getCommitsCount() > 0;
        if (showCommits) {
            report.addTab("commitsTrend", "Commits Trend", false);
            report.addTab("contributorsTrend", "Contributors Trend", false);
            report.addTab("correlations", "Correlations", false);
        }
        if (showTags()) {
            report.addTab("tags", "Tags", false);
        }
        report.endTabGroup();
        addProjectsBySize(report, projectsAnalysisResults);
        if (showCommits) {
            addCommitBasedLists(report, projectsAnalysisResults);
        }
        if (showTags()) {
            report.startTabContentSection("tags", false);
            addTagStats(report);
            report.endTabContentSection();
        }
        report.endTabContentSection();
    }

    public void addCommitBasedLists(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startTabContentSection("commitsTrend", false);
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

        CorrelationDiagramGenerator correlationDiagramGenerator = new CorrelationDiagramGenerator(report, projectsAnalysisResults);

        correlationDiagramGenerator.addCorrelations("Recent Contributors vs. Commits (30 days)", "commits (30d)", "recent contributors (30d)",
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count());

        correlationDiagramGenerator.addCorrelations("Recent Contributors vs. Project Main LOC", "main LOC", "recent contributors (30d)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count());

        correlationDiagramGenerator.addCorrelations("Recent Commits (30 days) vs. Project Main LOC", "main LOC", "commits (30d)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days());

        correlationDiagramGenerator.addCorrelations("Age in Days vs. Project Main LOC", "main LOC", "age (days)",
                p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode(),
                p -> p.getAnalysisResults().getFilesHistoryAnalysisResults().getAgeInDays());

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
        projectsAnalysisResults.forEach(projectAnalysis -> {
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
        report.addNewTabLink("data", "data/projects.txt");
        report.endDiv();
    }

    public void addSummaryGraphContributors(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        report.startDiv("white-space: nowrap; overflow-x: scroll; width: 100%");
        int max = Math.max(projectsAnalysisResults.stream()
                .mapToInt(p -> (int) p.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count())
                .max().orElse(1), 1);
        int maxHeight = 64;
        projectsAnalysisResults.forEach(projectAnalysis -> {
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
        projectsAnalysisResults.forEach(projectAnalysis -> {
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
        report.startTabContentSection("projects", true);
        addSummaryGraphMainLoc(report, projectsAnalysisResults);
        report.startTable("width: 100%");
        int thresholdContributors = landscapeAnalysisResults.getConfiguration().getProjectThresholdContributors();
        List<String> headers = new ArrayList<>(Arrays.asList("", "Project" + (thresholdContributors > 1 ? "<br/>(" + thresholdContributors + "+&nbsp;contributors)" : ""),
                "Main<br/>Language", "LOC<br/>(main)*",
                "LOC<br/>(test)", "LOC<br/>(other)",
                "Age", "Latest<br>Commit Date",
                "Contributors<br>(30d)", "Rookies<br>(30d)", "Commits<br>(30d)"));
        if (showTags()) {
            headers.add(3, "Tags");
        }
        if (showControls()) {
            headers.add("Controls");
        }
        headers.add("Report");
        report.addTableHeader(headers.toArray(String[]::new));
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode()
                        - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());

        int limit = landscapeAnalysisResults.getConfiguration().getProjectsListLimit();
        projectsAnalysisResults.stream().limit(limit).forEach(projectAnalysis -> {
            addProjectRow(report, projectAnalysis);
        });

        report.endTable();
        if (limit < projectsAnalysisResults.size()) {
            report.addParagraph("The list is limited to " + limit + " items (out of " + projectsAnalysisResults.size() + ").",
                    "color:grey; font-size: 90%; margin-top: 16px; margin-left: 11px");
        }
        report.endTabContentSection();
    }

    private void addCommitsTrend(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults, String label, String color, Counter counter) {
        report.startTable();
        report.addTableHeader("Project", "Commits<br>(30d)" + (label.equalsIgnoreCase("Commits") ? "*" : ""),
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
        int limit = landscapeAnalysisResults.getConfiguration().getProjectsListLimit();
        projectsAnalysisResults.stream().limit(limit).forEach(projectAnalysis -> {
            report.startTableRow();
            report.addTableCell(projectAnalysis.getAnalysisResults().getMetadata().getName(), "min-width: 400px; max-width: 400px");
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
            report.addParagraph("The list is limited to " + limit + " items (out of " + projectsAnalysisResults.size() + ")." +
                    "color:grey; font-size: 90%");
        }

    }

    private boolean showControls() {
        return landscapeAnalysisResults.getConfiguration().isShowProjectControls();
    }

    private void addTagStats(RichTextReport report) {
        report.startTable();
        report.addTableHeader("Tag", "# projects", "LOC<br>(main)", "LOC<br>(test)", "LOC<br>(active)", "LOC<br>(new)", "# commits<br>(30 days)", "# contributors<br>(30 days)");

        this.landscapeAnalysisResults.getConfiguration().getProjectTags().forEach(projectTag -> {
            String tagName = projectTag.getTag();
            addTagRow(report, tagName, projectTag.getPatterns());
        });
        if (tagStatsMap.containsKey("")) {
            addTagRow(report, "", new ArrayList<>());
        }
        report.endTable();

        visualizeTagProjects(report);
    }

    private void visualizeTagProjects(RichTextReport report) {
        report.startDiv("");
        int maxLoc = this.landscapeAnalysisResults.getProjectAnalysisResults().stream()
                .map(p -> p.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode())
                .reduce((a, b) -> Math.max(a, b)).get();
        int maxCommits = this.landscapeAnalysisResults.getProjectAnalysisResults().stream()
                .map(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                .reduce((a, b) -> Math.max(a, b)).get();
        this.landscapeAnalysisResults.getConfiguration().getProjectTags().forEach(tag -> {
            String tagName = tag.getTag();
            visualizeTag(report, maxLoc, maxCommits, tagName);
        });
        if (tagStatsMap.containsKey("")) {
            visualizeTag(report, maxLoc, maxCommits, "");
        }
        report.endTable();
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

    private void addTagRow(RichTextReport report, String tagName, List<String> patterns) {
        TagStats stats = tagStatsMap.get(tagName);
        if (stats == null) {
            return;
        }
        report.startTableRow("text-align: center");
        report.startTableCell();
        if (StringUtils.isNotBlank(tagName)) {
            report.addContentInDivWithTooltip(tagName,
                    patterns.stream().collect(Collectors.joining(", ")),
                    "padding: 4px; border-radius: 6px; background-color: " + getTabColor(stats.getTag()));
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
                                + FormattingUtils.formatCount(projectAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode()) + "</b> LOC)</span>");
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
                    .sum()), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(LandscapeAnalysisResults.getLocActive(projectsAnalysisResults), "text-align: center"));
            report.addTableCell(FormattingUtils.formatCount(LandscapeAnalysisResults.getLocNew(projectsAnalysisResults), "text-align: center"));
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days())
                    .sum()), "text-align: center");
            report.addTableCell(FormattingUtils.formatCount(getRecentContributorCount(projectsAnalysisResults)), "text-align: center");
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
        report.addTableCell(StringUtils.isNotBlank(logoLink) ? "<img src='" + logoLink + "' style='width: 20px'>" : "", "text-align: center");
        report.addTableCell(metadata.getName());
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
        if (showTags()) {
            report.addTableCell(getTags(projectAnalysis), "color: black");
        }
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
        if (showControls()) {
            report.startTableCell("text-align: center; font-size: 90%");
            addControls(report, analysisResults);
            report.endTableCell();
        }
        report.addTableCell("<a href='" + projectReportUrl + "' target='_blank'>"
                + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 40) + "</div></a>", "text-align: center; font-size: 90%");
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

    private String getProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/index.html";
    }

    private String getCommitsProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/Commits.html";
    }

    private boolean showTags() {
        return landscapeAnalysisResults.getConfiguration().getProjectTags().size() > 0;
    }

    private String getTags(ProjectAnalysisResults project) {
        String name = project.getAnalysisResults().getMetadata().getName();
        List<NumericMetric> linesOfCodePerExtension = project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension();
        linesOfCodePerExtension.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
        String mainTech = linesOfCodePerExtension.size() > 0 ? linesOfCodePerExtension.get(0).getName().replaceAll(".*[.]", "") : "";
        List<ProjectTag> tags = this.landscapeAnalysisResults.getConfiguration().getProjectTags();

        StringBuilder tagsHtml = new StringBuilder();

        boolean tagged[] = {false};

        tags.forEach(tag -> {
            if (!tag.exclude(name) && !tag.excludesMainTechnology(mainTech) && (tag.matches(name) || tag.matchesMainTechnology(mainTech))) {
                tagsHtml.append("<div style='margin: 2px; padding: 5px; display: inline-block; background-color: " + getTabColor(tag) + "; font-size: 70%; border-radius: 10px'>");
                tagsHtml.append(tag.getTag());
                tagsHtml.append("</div>");

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

        return tagsHtml.toString();
    }

    private String getTabColor(ProjectTag tag) {
        return StringUtils.isNotBlank(tag.getColor()) ? tag.getColor() : "deepskyblue";
    }

}
