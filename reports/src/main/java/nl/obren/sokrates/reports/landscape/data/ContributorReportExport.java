package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeContributorsReport;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeIndividualContributorsReports;
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
    // Optional display name (git author name) — shown next to the email like the per-repository
    // Contributors report does. May be blank.
    private String userName = "";
    private String mainLang;
    // Every language (lowercased extension) the contributor has committed to, ordered by recent
    // activity desc — backs the includesLang:<lang> filter in the report.
    private List<String> langs = new ArrayList<>();
    private String avatarUrl;
    private List<String> tags = new ArrayList<>();
    private String team;
    private int membersCount;
    private int commitsCount;
    private int commitsCount30Days;
    private int commitsCount90Days;
    private int commitsCount365Days;
    // Lines added/deleted across all of this contributor's commits (and the 30-day window); 0 for
    // history files without churn columns.
    private int linesAdded;
    private int linesDeleted;
    private int linesAdded30Days;
    private int linesDeleted30Days;
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private int repositoriesCount;
    private int repositoriesCount30Days;
    private String reportUrl = "";

    public ContributorReportExport() {
    }

    public ContributorReportExport(ContributorRepositories cr, LandscapeConfiguration configuration,
                                   PeopleConfig peopleConfig, TeamsConfig teamsConfig, List<ContributorTag> tagRules) {
        this(cr, configuration, peopleConfig, teamsConfig, tagRules, null);
    }

    /**
     * @param recentLangs the languages this contributor committed to in the last 30 days, computed
     *                    from the SAME per-extension commit history the Overview "Contributors Per
     *                    File Extension" badges count (so includesLang:&lt;lang&gt; matches that
     *                    population exactly). When null, falls back to the per-extension helper.
     */
    public ContributorReportExport(ContributorRepositories cr, LandscapeConfiguration configuration,
                                   PeopleConfig peopleConfig, TeamsConfig teamsConfig, List<ContributorTag> tagRules,
                                   List<String> recentLangs) {
        Contributor c = cr.getContributor();
        email = c.getEmail();
        userName = StringUtils.defaultString(c.getUserName());

        ContributorPerExtensionHelper extensionHelper = new ContributorPerExtensionHelper();
        // The contributor's main language is the most active extension (per-extension helper).
        mainLang = extensionHelper.getBiggestExtensionLanguage(configuration, cr, peopleConfig);
        // includesLang: filters on the recent (30-day) commit-based language set so the report
        // matches the Overview badge counts; fall back to the helper's languages if not provided.
        langs = recentLangs != null ? recentLangs
                : extensionHelper.getLanguages(configuration, cr, peopleConfig);

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
        // A team has members; a contributor does not. Route on this directly instead of the
        // team-email set, which shares one key namespace with contributors and could mis-route a
        // contributor whose safe-email key collides with a team name.
        boolean isTeam = membersCount > 0;

        commitsCount = c.getCommitsCount();
        commitsCount30Days = c.getCommitsCount30Days();
        commitsCount90Days = c.getCommitsCount90Days();
        commitsCount365Days = c.getCommitsCount365Days();
        linesAdded = c.getLinesAdded();
        linesDeleted = c.getLinesDeleted();
        linesAdded30Days = c.getLinesAdded30Days();
        linesDeleted30Days = c.getLinesDeleted30Days();
        firstCommitDate = c.getFirstCommitDate() != null ? c.getFirstCommitDate() : "";
        latestCommitDate = c.getLatestCommitDate() != null ? c.getLatestCommitDate() : "";

        List<ContributorRepositoryInfo> repositories = cr.getRepositories();
        repositoriesCount = repositories != null ? repositories.size() : 0;
        repositoriesCount30Days = repositories == null ? 0
                : (int) repositories.stream().filter(p -> p.getCommits30Days() > 0).count();

        reportUrl = LandscapeIndividualContributorsReports.getContributorReportUrl(email, isTeam);
    }

    public String getEmail() {
        return email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMainLang() {
        return mainLang;
    }

    public List<String> getLangs() {
        return langs;
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

    public int getLinesAdded() {
        return linesAdded;
    }

    public int getLinesDeleted() {
        return linesDeleted;
    }

    public int getLinesAdded30Days() {
        return linesAdded30Days;
    }

    public int getLinesDeleted30Days() {
        return linesDeleted30Days;
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
