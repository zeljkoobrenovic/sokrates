package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.ProjectTag;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class LandscapeProjectsReport {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private Map<String, TagStats> tagStatsMap = new HashMap<>();

    public LandscapeProjectsReport(LandscapeAnalysisResults landscapeAnalysisResults) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
    }

    public void saveProjectsReport(RichTextReport report, List<ProjectAnalysisResults> projectsAnalysisResults) {
        if (showTags()) {
            report.startTabGroup();
            report.addTab("projects", "Projects", true);
            report.addTab("tags", "Tags", false);
            report.endTabGroup();
        }
        if (showTags()) {
            report.startTabContentSection("projects", true);
        }
        report.startTable("width: 100%");
        int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
        int thresholdContributors = landscapeAnalysisResults.getConfiguration().getProjectThresholdContributors();
        List<String> headers = new ArrayList<>(Arrays.asList("", "Project" + (thresholdContributors > 1 ? "<br/>(" + thresholdContributors + "+&nbsp;contributors)" : ""),
                "Main<br/>Language", "LOC<br/>(main)",
                "LOC<br/>(test)", "LOC<br/>(other)",
                "Age", "Contributors" + (thresholdCommits > 1 ? "<br/>(" + thresholdCommits + "+&nbsp;commits)" : ""),
                "Recent<br>Contributors<br>(30d)", "Rookies", "Commits<br>this year", "Report"));
        if (showTags()) {
            headers.add(2, "Tags");
        }
        report.addTableHeader(headers.toArray(String[]::new));
        Collections.sort(projectsAnalysisResults,
                (a, b) -> b.getAnalysisResults().getContributorsAnalysisResults().getCommitsThisYear()
                        - a.getAnalysisResults().getContributorsAnalysisResults().getCommitsThisYear());
        projectsAnalysisResults.forEach(projectAnalysis -> {
            addProjectRow(report, projectAnalysis);
        });
        report.endTable();
        if (showTags()) {
            report.endTabContentSection();
            report.startTabContentSection("tags", false);
            addTabStats(report);
            report.endTabContentSection();
        }
    }

    private void addTabStats(RichTextReport report) {
        ArrayList<TagStats> tagStats = new ArrayList<>(this.tagStatsMap.values());
        tagStats.sort((a, b) -> b.getProjectsAnalysisResults().size() - a.getProjectsAnalysisResults().size());

        report.startTable();
        report.addTableHeader("Tag", "# projects", "LOC<br>(main)", "LOC<br>(test)", "# commits<br>(this year)", "# contributors<br>(30 days)");

        this.landscapeAnalysisResults.getConfiguration().getProjectTags().forEach(projectTag -> {
            String tagName = projectTag.getTag();
            addTagRow(report, tagName, projectTag.getPatterns());
        });
        if (tagStatsMap.containsKey("")) {
            addTagRow(report, "", new ArrayList<>());
        }

        report.endTable();
    }

    private void addTagRow(RichTextReport report, String tagName, List<String> patterns) {
        TagStats stats = tagStatsMap.get(tagName);
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
                        "<a href='" + projectReportUrl + "' target='_blank'>" + projectAnalysisResults.getMetadata().getName() + "</a> "
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
            report.addTableCell(FormattingUtils.formatCount(projectsAnalysisResults
                    .stream()
                    .mapToInt(p -> p.getAnalysisResults().getContributorsAnalysisResults().getCommitsThisYear())
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

        report.startTableRow();
        report.addTableCell(StringUtils.isNotBlank(logoLink) ? "<img src='" + logoLink + "' style='width: 20px'>" : "", "text-align: center");
        report.addTableCell(metadata.getName());
        if (showTags()) {
            report.addTableCell(getTags(projectAnalysis));
        }
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        int thresholdCommits = landscapeAnalysisResults.getConfiguration().getContributorThresholdCommits();
        List<Contributor> contributors = analysisResults.getContributorsAnalysisResults().getContributors()
                .stream().filter(c -> c.getCommitsCount() >= thresholdCommits).collect(Collectors.toCollection(ArrayList::new));

        int contributorsCount = contributors.size();
        int recentContributorsCount = (int) contributors.stream().filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
        int rookiesCount = (int) contributors.stream().filter(c -> c.isRookie(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();

        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        StringBuilder locSummary = new StringBuilder();
        if (linesOfCodePerExtension.size() > 0) {
            locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
        } else {
            locSummary.append("-");
        }
        report.addTableCell(locSummary.toString().replace("> = ", ">"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(main.getLinesOfCode(), "-"), "text-align: center");

        report.addTableCell(FormattingUtils.formatCount(test.getLinesOfCode(), "-"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(generated.getLinesOfCode() + build.getLinesOfCode() + other.getLinesOfCode(), "-"), "text-align: center");
        int projectAgeYears = (int) Math.round(analysisResults.getFilesHistoryAnalysisResults().getAgeInDays() / 365.0);
        String age = projectAgeYears == 0 ? "<1y" : projectAgeYears + "y";
        report.addTableCell(age, "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(contributorsCount, "-"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(recentContributorsCount, "-"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(rookiesCount, "-"), "text-align: center");
        report.addTableCell(FormattingUtils.formatCount(analysisResults.getContributorsAnalysisResults().getCommitsThisYear(), "-"), "text-align: center");
        String projectReportUrl = getProjectReportUrl(projectAnalysis);
        report.addTableCell("<a href='" + projectReportUrl + "' target='_blank'>"
                + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 40) + "</div></a>", "text-align: center");
        report.endTableRow();
    }

    private String getProjectReportUrl(ProjectAnalysisResults projectAnalysis) {
        return landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/index.html";
    }

    private boolean showTags() {
        return landscapeAnalysisResults.getConfiguration().getProjectTags().size() > 0;
    }

    private String getTags(ProjectAnalysisResults project) {
        String name = project.getAnalysisResults().getMetadata().getName();
        List<ProjectTag> tags = this.landscapeAnalysisResults.getConfiguration().getProjectTags();

        StringBuilder tagsHtml = new StringBuilder();

        boolean tagged[] = {false};

        tags.forEach(tag -> {
            if (tag.matches(name)) {
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
        return StringUtils.isNotBlank(tag.getColor()) ? tag.getColor() : "lightgrey";
    }


}
