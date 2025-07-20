package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.core.SummaryUtils;
import nl.obren.sokrates.reports.landscape.utils.ContributorPerExtensionHelper;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.landscape.ContributorTag;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamConfig;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositoryInfo;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;

public class LandscapeContributorsReport {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private RichTextReport report;
    private Set<String> contributorsLinkedFromTables;
    private boolean recent = false;

    private Map<String, List<String>> contributorTagMap = new HashMap<>();
    private Map<String, List<String>> tagMap = new HashMap<>();
    private List<String> tags = new ArrayList<>();

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

    private void tag(List<ContributorRepositories> contributors) {
        contributorTagMap.clear();
        tagMap.clear();
        tags.clear();

        List<ContributorTag> tagRules = landscapeAnalysisResults.getConfiguration().getTagContributors();

        tagRules.forEach(tagRule -> {
            tagMap.put(tagRule.getName(), new ArrayList<>());
        });

        contributors.forEach(contributor -> {
            String email = contributor.getContributor().getEmail();
            contributorTagMap.put(email, new ArrayList<>());
            tagRules.forEach(tagRule -> {
                if (RegexUtils.matchesAnyPattern(email, tagRule.getPatterns())) {
                    String tag = tagRule.getName();
                    contributorTagMap.get(email).add(tag);
                    tagMap.get(tag).add(email);
                }
            });
        });


        tagMap.keySet().stream().filter(key -> tagMap.get(key).size() > 0)
                .sorted((a, b) -> tagMap.get(b).size() - tagMap.get(a).size())
                .forEach(key -> {
                    tags.add(key);
                });
    }


    public void saveContributorsTable(List<ContributorRepositories> contributors, int totalCommits, boolean recent) {
        this.tag(contributors);

        if (tags.size() > 0) {
            report.startDiv("font-size: 80%; ");
            report.addHtmlContent("tags: ");
            tags.forEach(tag -> {
                report.addContentInDiv(tag + " (" + tagMap.get(tag).size() + ")", "background-color: skyblue; border-radius: 10px; display: inline-block; padding: 5px 5px 7px 5px; margin: 5px;");
            });
            report.endDiv();
        }

        this.recent = recent;
        report.startTable("width: 100%");
        if (recent) {
            report.addTableHeaderLeft("", "contributor", "# commits<br>30 days", "# commits<br>90 days", "# commits<br>past year", "# commits<br>all time", "first", "latest", "repositories", "details");
        } else {
            report.addTableHeaderLeft("", "contributor", "# commits<br>all time", "# commits<br>past year", "# commits<br>90 days", "# commits<br>30 days", "first", "latest", "repositories", "details");
        }
        int counter[] = {0};

        int limit = landscapeAnalysisResults.getConfiguration().getContributorsListLimit();
        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount())
                .sorted((a, b) -> b.getContributor().getCommitsCount365Days() - a.getContributor().getCommitsCount365Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount180Days() - a.getContributor().getCommitsCount180Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount90Days() - a.getContributor().getCommitsCount90Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .limit(limit).forEach(contributor -> {
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
        String color = contributor.getContributor().getCommitsCount90Days() > 0 ? "black" : "lightgrey";
        report.startTableRow(contributor.getContributor().getCommitsCount30Days() > 0 ? "font-weight: bold;"
                : "color: " + color);
        counter[0] += 1;
        PeopleConfig peopleConfig = landscapeAnalysisResults.getPeopleConfig();
        String biggestExtension = new ContributorPerExtensionHelper().getBiggestExtension(landscapeAnalysisResults.getConfiguration(), contributor, peopleConfig);
        String icon;
        if (biggestExtension == null) {
            icon = "";
        } else {
            icon = DataImageUtils.getLangDataImageDiv30(biggestExtension);
        }
        report.addTableCell(icon, "text-align: center; width: 32px; max-width: 32px");
        String avatarHtml = "";

        String contributorId = contributor.getContributor().getEmail();
        PersonConfig personConfig = peopleConfig != null ? peopleConfig.getPersonByName(contributorId) : null;

        String avatarUrl;
        if (personConfig != null && StringUtils.isNotBlank(personConfig.getImage())) {
            avatarUrl = personConfig.getImage();
        } else {
            avatarUrl = this.getAvatarUrl(contributorId, this.landscapeAnalysisResults.getConfiguration().getContributorAvatarLinkTemplate());
        }
        String defaultAvatar = contributor.getMembers().size() > 0 ? DataImageUtils.TEAM : DataImageUtils.DEVELOPER;
        if (avatarUrl != null) {
            avatarHtml = "<div style='vertical-align: middle; display: inline-block; width: 48px; margin-top: 2px;'>" +
                    "<img style='border-radius: 50%; height: 38px; width: 38px; margin-right: 10px;' src='" + avatarUrl + "' " +
                    "onerror=\"this.onerror=null;this.src='" + defaultAvatar + "';\">" +
                    "</div>";
        } else {
            avatarHtml = "<div style='vertical-align: middle; display: inline-block; width: 48px; margin-top: 2px;'>" +
                    "<img style='border-radius: 50%; height: 38px; width: 38px; margin-right: 10px;' src='" + defaultAvatar + "'>" +
                    "</div>";
        }
        String link = this.getContributorUrl(contributorId);
        StringBuilder contributorBody = new StringBuilder(avatarHtml + StringEscapeUtils.escapeHtml4(contributorId));

        if (contributor.getMembers().size() > 0) {
            contributorBody.append(" (" + FormattingUtils.formatCount(contributor.getMembers().size()) + ")");
        }

        contributorTagMap.get(contributorId).forEach(tag -> {
            contributorBody.append("<div style='vertical-align: top; font-size: 60%; background-color: skyblue; border-radius: 7px; display: inline-block; padding: 3px 3px 5px 3px; margin: 5px;'>" + tag + "</div>");
        });
        String team = getTeam(contributorId);
        String body = contributorBody.toString();
        if (team != null) {
            body = "<div><div style='vertical-align: top; font-size: 60%; background-color: lightyellow; border-radius: 12px; display: inline-block; padding: 3px; margin: 5px; color: black'>" + team + "</div><div style='margin-top: -15px; margin-bottom: -9px;'>" + body + "</div></div>";
        }

        report.addTableCellWithTitle("<a target='_blank' style='color: " + color + "; text-decoration: none' href='" + link + "'>" + body + "</a>",
                "vertical-align: middle; white-space: nowrap; overflow: hidden;", "" + counter[0]);
        int commitsCountAllTime = contributor.getContributor().getCommitsCount();
        int commitsCount30Days = contributor.getContributor().getCommitsCount30Days();
        if (recent) {
            double percentage = 100.0 * commitsCount30Days / totalCommits;
            String percText = " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)";
            report.addTableCell(FormattingUtils.formatCount(commitsCount30Days) + percText, "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount90Days()), "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount365Days()), "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(commitsCountAllTime) + "", "vertical-align: middle;");
        } else {
            double percentage = 100.0 * commitsCountAllTime / totalCommits;
            String percText = " (" + FormattingUtils.getFormattedPercentage(percentage) + "%)";
            report.addTableCell(FormattingUtils.formatCount(commitsCountAllTime) + percText, "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount365Days()), "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(contributor.getContributor().getCommitsCount90Days()), "vertical-align: middle;");
            report.addTableCell(FormattingUtils.formatCount(commitsCount30Days), "vertical-align: middle;");
        }
        report.addTableCell(contributor.getContributor().getFirstCommitDate(), "vertical-align: middle; white-space: nowrap;");
        report.addTableCell(contributor.getContributor().getLatestCommitDate(), "vertical-align: middle; white-space: nowrap;");
        StringBuilder repositoryInfo = new StringBuilder();
        report.startTableCell();
        if (recent) {
            List<ContributorRepositoryInfo> recentRepositories = contributor.getRepositories()
                    .stream()
                    .filter(p -> p.getCommits30Days() > 0)
                    .collect(Collectors.toCollection(ArrayList::new));
            int repositoriesCount = recentRepositories.size();
            report.startShowMoreBlock(repositoriesCount + (repositoriesCount == 1 ? "&nbsp;repo" : "&nbsp;repos"));
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
        report.addTableCell("<a target='_blank' href='" + link + "'  title='volume details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "text-align: center");
        report.endTableRow();
    }

    private String getDetailsIcon() {
        return SummaryUtils.getIconSvg("details", 22, 22);
    }


    public static String getContributorUrl(String email) {
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

    private String getTeam(String contributorId) {
        for (TeamConfig teamConfig : landscapeAnalysisResults.getTeamsConfig().getTeams()) {
            if (RegexUtils.matchesAnyPattern(contributorId, teamConfig.getEmailPatterns())) {
                return teamConfig.getName();
            }
        }

        return null;
    }
}
