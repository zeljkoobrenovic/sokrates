package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjectInfo;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjects;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LandscapeContributorsReport {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private RichTextReport report;
    private boolean recent = false;

    public LandscapeContributorsReport(LandscapeAnalysisResults landscapeAnalysisResults, RichTextReport report) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.report = report;
    }

    public void saveContributorsTable(List<ContributorProjects> contributors, int totalCommits, boolean recent) {
        this.recent = recent;
        report.startTable("width: 100%");
        if (recent) {
            report.addTableHeaderLeft("Contributor", "# commits<br>30 days", "# commits<br>90 days", "# commits<br>all time", "first", "latest", "projects");
        } else {
            report.addTableHeaderLeft("Contributor", "# commits<br>all time", "# commits<br>90 days", "# commits<br>30 days", "first", "latest", "projects");
        }
        int counter[] = {0};

        contributors.forEach(contributor -> {
            addContributor(totalCommits, counter, contributor);
        });
        report.endTable();
    }


    private void addContributor(int totalCommits, int[] counter, ContributorProjects contributor) {
        String color = contributor.getContributor().getCommitsCount90Days() > 0 ? "grey" : "lightgrey";
        report.startTableRow(contributor.getContributor().getCommitsCount30Days() > 0 ? "font-weight: bold;"
                : "color: " + color);
        counter[0] += 1;
        String avatarHtml = "";
        String avatarUrl = this.getAvatarUrl(contributor.getContributor().getEmail());
        if (avatarUrl != null) {
            avatarHtml = "<div style='vertical-align: middle; display: inline-block; width: 48px; margin-top: 2px;'><img style='border-radius: 50%; height: 40px; width: 40px; margin-right: 10px;' src='" + avatarUrl + "'></div>";
        }
        String link = this.getContributorUrl(contributor.getContributor().getEmail());
        String contributorBody = avatarHtml + StringEscapeUtils.escapeHtml4(contributor.getContributor().getEmail());
        if (link != null) {
            report.addTableCellWithTitle("<a target='_blank' style='color: " + color + "; text-decoration: none' href='" + link + "'>" + contributorBody + "</a>",
                    "vertical-align: middle;", "" + counter[0]);
        } else {
            report.addTableCellWithTitle(contributorBody, "vertical-align: middle;", "" + counter[0]);
        }
        int commitsCountAllTime = contributor.getContributor().getCommitsCount();
        int commitsCount30Days = contributor.getContributor().getCommitsCount30Days();
        if (recent) {
            double percentage = 100.0 * commitsCount30Days / totalCommits;
            String percText = " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)";
            report.addTableCell(FormattingUtils.formatCount(commitsCount30Days) + percText, "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount90Days()), "vertical-align: middle;");
            report.addTableCell(commitsCountAllTime + "", "vertical-align: middle;");
        } else {
            double percentage = 100.0 * commitsCountAllTime / totalCommits;
            String percText = " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)";
            report.addTableCell(commitsCountAllTime + percText, "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount90Days()), "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(commitsCount30Days), "vertical-align: middle;");
        }
        report.addTableCell(contributor.getContributor().getFirstCommitDate(), "vertical-align: middle;");
        report.addTableCell(contributor.getContributor().getLatestCommitDate(), "vertical-align: middle;");
        StringBuilder projectInfo = new StringBuilder();
        report.startTableCell();
        if (recent) {
            List<ContributorProjectInfo> recentProjects = contributor.getProjects()
                    .stream()
                    .filter(p -> p.getCommits30Days() > 0)
                    .collect(Collectors.toCollection(ArrayList::new));
            int projectsCount = recentProjects.size();
            report.startShowMoreBlock(projectsCount + (projectsCount == 1 ? " project" : " projects"));
            recentProjects.forEach(contributorProjectInfo -> {
                String projectName = contributorProjectInfo.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName();
                int commits = contributorProjectInfo.getCommits30Days();
                if (projectInfo.length() > 0) {
                    projectInfo.append("<br/>");
                }
                projectInfo.append(projectName + " <span style='color: grey'>(" + commits + (commits == 1 ? " commit" : " commit") + ")</span>");
            });
        } else {
            int projectsCount = contributor.getProjects().size();
            report.startShowMoreBlock(projectsCount + (projectsCount == 1 ? " project" : " projects"));
            contributor.getProjects().forEach(contributorProjectInfo -> {
                String projectName = contributorProjectInfo.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName();
                int commits = contributorProjectInfo.getCommitsCount();
                if (projectInfo.length() > 0) {
                    projectInfo.append("<br/>");
                }
                projectInfo.append(projectName + " <span style='color: grey'>(" + commits + (commits == 1 ? " commit" : " commit") + ")</span>");
            });
        }
        report.addHtmlContent(projectInfo.toString());
        report.endTableCell();
        report.endTableRow();
    }

    private String getContributorUrl(String contributorId) {
        return getUrl(contributorId, this.landscapeAnalysisResults.getConfiguration().getContributorLinkTemplate());
    }

    private String getAvatarUrl(String contributorId) {
        return getUrl(contributorId, this.landscapeAnalysisResults.getConfiguration().getContributorAvatarLinkTemplate());
    }

    private String getUrl(String contributorId, String template) {
        String idVariable = "${contributorid}";
        if (StringUtils.isNotBlank(template) && template.contains(idVariable)) {
            return template.replace(idVariable, contributorId.replaceAll("[@].*", ""));
        }

        return null;
    }

    public boolean isRecent() {
        return recent;
    }

    public void setRecent(boolean recent) {
        this.recent = recent;
    }
}
