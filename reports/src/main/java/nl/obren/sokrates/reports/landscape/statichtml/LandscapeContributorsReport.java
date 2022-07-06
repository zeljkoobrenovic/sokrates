package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.core.SummaryUtils;
import nl.obren.sokrates.reports.landscape.utils.ContributorPerExtensionHelper;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositoryInfo;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LandscapeContributorsReport {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private RichTextReport report;
    private Set<String> contributorsLinkedFromTables;
    private boolean recent = false;

    public LandscapeContributorsReport(LandscapeAnalysisResults landscapeAnalysisResults, RichTextReport report, Set<String> contributorsLinkedFromTables) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.report = report;
        this.contributorsLinkedFromTables = contributorsLinkedFromTables;
    }

    public static String getContributorUrlFromTemplate(String contributorId, String template) {
        String idVariable = "${contributorid}";
        if (StringUtils.isNotBlank(template) && template.contains(idVariable)) {
            return template.replace(idVariable, contributorId.replaceAll("[@].*", ""));
        }

        return null;
    }

    public void saveContributorsTable(List<ContributorRepositories> contributors, int totalCommits, boolean recent) {
        this.recent = recent;
        report.startTable("width: 100%");
        if (recent) {
            report.addTableHeaderLeft("Contributor", "# commits<br>30 days", "# commits<br>90 days", "# commits<br>past year", "# commits<br>all time", "first", "latest", "repositories", "main", "details");
        } else {
            report.addTableHeaderLeft("Contributor", "# commits<br>all time", "# commits<br>past year", "# commits<br>90 days", "# commits<br>30 days", "first", "latest", "repositories", "main", "details");
        }
        int counter[] = {0};

        int limit = landscapeAnalysisResults.getConfiguration().getContributorsListLimit();
        contributors.stream().limit(limit).forEach(contributor -> {
            contributorsLinkedFromTables.add(contributor.getContributor().getEmail());
            addContributor(totalCommits, counter, contributor);
        });
        report.endTable();

        if (limit < contributors.size()) {
            report.addParagraph("The list is limited to " + limit + " items (out of " + contributors.size() + ").",
                    "color:grey; font-size: 90%; margin-top: 16px; margin-left: 11px");
        }
    }

    private void addContributor(int totalCommits, int[] counter, ContributorRepositories contributor) {
        String color = contributor.getContributor().getCommitsCount90Days() > 0 ? "grey" : "lightgrey";
        report.startTableRow(contributor.getContributor().getCommitsCount30Days() > 0 ? "font-weight: bold;"
                : "color: " + color);
        counter[0] += 1;
        String avatarHtml = "";
        String avatarUrl = this.getAvatarUrl(contributor.getContributor().getEmail(), this.landscapeAnalysisResults.getConfiguration().getContributorAvatarLinkTemplate());
        if (avatarUrl != null) {
            avatarHtml = "<div style='vertical-align: middle; display: inline-block; width: 48px; margin-top: 2px;'>" +
                    "<img style='border-radius: 50%; height: 40px; width: 40px; margin-right: 10px;' src='" + avatarUrl + "' " +
                    "onerror=\"this.onerror=null;this.src='" + DataImageUtils.DEVELOPER + "';\">" +
                    "</div>";
        } else {
            avatarHtml = "<div style='vertical-align: middle; display: inline-block; width: 48px; margin-top: 2px;'>" +
                    "<img style='border-radius: 50%; height: 40px; width: 40px; margin-right: 10px;' src='" + DataImageUtils.DEVELOPER + "'>" +
                    "</div>";
        }
        String link = this.getContributorUrl(contributor.getContributor().getEmail());
        String contributorBody = avatarHtml + StringEscapeUtils.escapeHtml4(contributor.getContributor().getEmail());
        report.addTableCellWithTitle("<a target='_blank' style='color: " + color + "; text-decoration: none' href='" + link + "'>" + contributorBody + "</a>",
                "vertical-align: middle;", "" + counter[0]);
        int commitsCountAllTime = contributor.getContributor().getCommitsCount();
        int commitsCount30Days = contributor.getContributor().getCommitsCount30Days();
        if (recent) {
            double percentage = 100.0 * commitsCount30Days / totalCommits;
            String percText = " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)";
            report.addTableCell(FormattingUtils.formatCount(commitsCount30Days) + percText, "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount90Days()), "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount365Days()), "vertical-align: middle;");
            report.addTableCell(commitsCountAllTime + "", "vertical-align: middle;");
        } else {
            double percentage = 100.0 * commitsCountAllTime / totalCommits;
            String percText = " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)";
            report.addTableCell(commitsCountAllTime + percText, "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount365Days()), "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount90Days()), "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(commitsCount30Days), "vertical-align: middle;");
        }
        report.addTableCell(contributor.getContributor().getFirstCommitDate(), "vertical-align: middle;");
        report.addTableCell(contributor.getContributor().getLatestCommitDate(), "vertical-align: middle;");
        StringBuilder repositoryInfo = new StringBuilder();
        report.startTableCell();
        if (recent) {
            List<ContributorRepositoryInfo> recentRepositories = contributor.getRepositories()
                    .stream()
                    .filter(p -> p.getCommits30Days() > 0)
                    .collect(Collectors.toCollection(ArrayList::new));
            int repositoriesCount = recentRepositories.size();
            report.startShowMoreBlock(repositoriesCount + (repositoriesCount == 1 ? " repository" : " repositories"));
            recentRepositories.forEach(contributorRepositoryInfo -> {
                String repositoryName = contributorRepositoryInfo.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName();
                int commits = contributorRepositoryInfo.getCommits30Days();
                if (repositoryInfo.length() > 0) {
                    repositoryInfo.append("<br/>");
                }
                repositoryInfo.append(repositoryName + " <span style='color: grey'>(" + commits + (commits == 1 ? " commit" : " commit") + ")</span>");
            });
        } else {
            int repositoriesCount = contributor.getRepositories().size();
            report.startShowMoreBlock(repositoriesCount + (repositoriesCount == 1 ? " repository" : " repositories"));
            contributor.getRepositories().forEach(contributorRepositoryInfo -> {
                String repositoryName = contributorRepositoryInfo.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName();
                int commits = contributorRepositoryInfo.getCommitsCount();
                if (repositoryInfo.length() > 0) {
                    repositoryInfo.append("<br/>");
                }
                repositoryInfo.append(repositoryName + " <span style='color: grey'>(" + commits + (commits == 1 ? " commit" : " commit") + ")</span>");
            });
        }
        report.addHtmlContent(repositoryInfo.toString());
        report.endTableCell();
        String biggestExtension = new ContributorPerExtensionHelper().getBiggestExtension(landscapeAnalysisResults.getConfiguration(), contributor);
        String icon;
        if (biggestExtension == null) {
            icon = "";
        } else {
            icon = DataImageUtils.getLangDataImageDiv30(biggestExtension);
        }
        report.addTableCell(icon, "text-align: center; width: 32px; max-width: 32px");
        report.addTableCell("<a target='_blank' href='" + link + "'  title='volume details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "text-align: center");
        report.endTableRow();
    }
    private String getDetailsIcon() {
        return SummaryUtils.getIconSvg("details", 22, 22);
    }


    private String getContributorUrl(String email) {
        return "contributors/" + LandscapeIndividualContributorsReports.getContributorIndividualReportFileName(email);
    }

    public static String getAvatarUrl(String contributorId, String linkTemplate) {
        return getContributorUrlFromTemplate(contributorId, linkTemplate);
    }

    public boolean isRecent() {
        return recent;
    }

    public void setRecent(boolean recent) {
        this.recent = recent;
    }
}
