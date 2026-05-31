package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeContributorsReport;
import nl.obren.sokrates.reports.landscape.utils.ContributorPerExtensionHelper;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.ContributorTag;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositoryInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON-serializable view of one contributor (or team / bot) for the client-rendered
 * landscape contributors report ({@code contributors-report.html}). Carries the same
 * data the old server-rendered contributors table showed, per row. Distinct from
 * {@link ContributorExport}, which backs the {@code data/contributors.json} export.
 */
public class ContributorReportExport {
    private String email;
    private String mainLang;
    private String avatarUrl;
    private List<String> tags = new ArrayList<>();
    private String team;
    private int membersCount;
    private int commitsCount;
    private int commitsCount30Days;
    private int commitsCount90Days;
    private int commitsCount365Days;
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private int repositoriesCount;
    private int repositoriesCount30Days;
    private String reportUrl = "";

    public ContributorReportExport() {
    }

    public ContributorReportExport(ContributorRepositories cr, LandscapeConfiguration configuration,
                                   PeopleConfig peopleConfig, TeamsConfig teamsConfig, List<ContributorTag> tagRules) {
        Contributor c = cr.getContributor();
        email = c.getEmail();

        String biggestExtension = new ContributorPerExtensionHelper().getBiggestExtension(configuration, cr, peopleConfig);
        mainLang = biggestExtension != null ? biggestExtension.replace("*.", "").trim().toLowerCase() : "";

        // Avatar: explicit per-person image, else the configured avatar URL template (may be null).
        PersonConfig personConfig = peopleConfig != null ? peopleConfig.getPersonByName(email) : null;
        if (personConfig != null && StringUtils.isNotBlank(personConfig.getImage())) {
            avatarUrl = personConfig.getImage();
        } else {
            avatarUrl = LandscapeContributorsReport.getAvatarUrl(email, configuration.getContributorAvatarLinkTemplate());
        }

        if (tagRules != null) {
            tagRules.forEach(tagRule -> {
                if (RegexUtils.matchesAnyPattern(email, tagRule.getPatterns())) {
                    tags.add(tagRule.getName());
                }
            });
        }

        if (teamsConfig != null) {
            for (TeamConfig teamConfig : teamsConfig.getTeams()) {
                if (RegexUtils.matchesAnyPattern(email, teamConfig.getEmailPatterns())) {
                    team = teamConfig.getName();
                    break;
                }
            }
        }

        membersCount = cr.getMembers() != null ? cr.getMembers().size() : 0;

        commitsCount = c.getCommitsCount();
        commitsCount30Days = c.getCommitsCount30Days();
        commitsCount90Days = c.getCommitsCount90Days();
        commitsCount365Days = c.getCommitsCount365Days();
        firstCommitDate = c.getFirstCommitDate() != null ? c.getFirstCommitDate() : "";
        latestCommitDate = c.getLatestCommitDate() != null ? c.getLatestCommitDate() : "";

        List<ContributorRepositoryInfo> repositories = cr.getRepositories();
        repositoriesCount = repositories != null ? repositories.size() : 0;
        repositoriesCount30Days = repositories == null ? 0
                : (int) repositories.stream().filter(p -> p.getCommits30Days() > 0).count();

        reportUrl = LandscapeContributorsReport.getContributorUrl(email);
    }

    public String getEmail() {
        return email;
    }

    public String getMainLang() {
        return mainLang;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getTeam() {
        return team;
    }

    public int getMembersCount() {
        return membersCount;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public int getCommitsCount90Days() {
        return commitsCount90Days;
    }

    public int getCommitsCount365Days() {
        return commitsCount365Days;
    }

    public String getFirstCommitDate() {
        return firstCommitDate;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public int getRepositoriesCount() {
        return repositoriesCount;
    }

    public int getRepositoriesCount30Days() {
        return repositoriesCount30Days;
    }

    public String getReportUrl() {
        return reportUrl;
    }
}
