package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspectUtils;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import nl.obren.sokrates.sourcecode.landscape.utils.EmailTransformations;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.*;
import java.util.stream.Collectors;
import nl.obren.sokrates.sourcecode.landscape.TeamConfig;
import java.util.function.Predicate;

/**
 * Builds the landscape's contributor and team lists from the repositories' contributor data: contributors are
 * merged across repositories by email (after the configured email transformations and config-people.json),
 * teams are assembled from config-teams.json. Moved out of {@link LandscapeAnalysisResults}, which caches the
 * results and applies the commit threshold, the bot split and anonymization.
 */
class LandscapeContributorsAggregator {
    private final LandscapeConfiguration configuration;
    private final PeopleConfig peopleConfig;
    private final TeamsConfig teamsConfig;
    private final Predicate<String> isBot;

    LandscapeContributorsAggregator(LandscapeConfiguration configuration, PeopleConfig peopleConfig, TeamsConfig teamsConfig, Predicate<String> isBot) {
        this.configuration = configuration;
        this.peopleConfig = peopleConfig;
        this.teamsConfig = teamsConfig;
        this.isBot = isBot;
    }

    /** Every contributor of the repositories, merged across repositories by their (people-config-canonical) email, most commits first. */
    List<ContributorRepositories> contributors(List<RepositoryAnalysisResults> repositories) {
        List<ContributorRepositories> list = new ArrayList<>();
        Map<String, ContributorRepositories> map = new HashMap<>();

        repositories.forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            contributorsAnalysisResults.getContributors().forEach(contributor -> {
                String originalEmail = contributor.getEmail().toLowerCase();
                String originalUserName = contributor.getUserName();
                String contributorId = originalEmail;
                if (GitHistoryUtils.shouldIgnore(contributorId, configuration.getIgnoreContributors())) {
                    return;
                }
                contributorId = EmailTransformations.transformEmail(contributorId, originalUserName, configuration.getTransformContributorEmails(), peopleConfig);
                if (GitHistoryUtils.shouldIgnore(contributorId, configuration.getIgnoreContributors())) {
                    return;
                }

                if (StringUtils.isBlank(contributorId)) {
                    return;
                }

                if (map.containsKey(contributorId)) {
                    mergeInto(map.get(contributorId), contributor, repositoryAnalysisResults);
                } else {
                    ContributorRepositories newContributorWithRepositories = newContributorRepositories(contributor, contributorId, originalEmail, originalUserName, repositoryAnalysisResults);
                    map.put(contributorId, newContributorWithRepositories);
                    list.add(newContributorWithRepositories);
                }
            });
        });

        Collections.sort(list, (a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount());

        return list;
    }

    /** Adds one repository's contributor record to an already known contributor: counts, dates, churn and the repository entry. */
    private static void mergeInto(ContributorRepositories existingContributor, Contributor contributor, RepositoryAnalysisResults repositoryAnalysisResults) {
        int repositoryCommits = contributor.getCommitsCount();
        List<String> commitDates = contributor.getCommitDates();
        Map<String, Integer> commitsPerDate = contributor.getCommitsPerDate();
        int repositoryCommits30Days = contributor.getCommitsCount30Days();
        int repositoryCommits90Days = contributor.getCommitsCount90Days();
        int repositoryCommits180Days = contributor.getCommitsCount180Days();
        int repositoryCommits365Days = contributor.getCommitsCount365Days();

        String latestCommitDate = contributor.getLatestCommitDate();
        String firstCommitDate = contributor.getFirstCommitDate();

        Contributor contributorInfo = existingContributor.getContributor();

        contributorInfo.setCommitsCount(contributorInfo.getCommitsCount() + repositoryCommits);
        contributorInfo.setCommitsCount30Days(contributorInfo.getCommitsCount30Days() + repositoryCommits30Days);
        contributorInfo.setCommitsCount90Days(contributorInfo.getCommitsCount90Days() + repositoryCommits90Days);
        contributorInfo.setCommitsCount180Days(contributorInfo.getCommitsCount180Days() + repositoryCommits180Days);
        contributorInfo.setCommitsCount365Days(contributorInfo.getCommitsCount365Days() + repositoryCommits365Days);

        contributorInfo.addActiveYears(contributor.getActiveYears());
        contributorInfo.addCommitDates(contributor.getCommitDates());
        contributorInfo.addCommitDatesByScope(contributor.getCommitDatesByScope());
        contributorInfo.addCommitsPerDate(commitsPerDate);
        contributorInfo.addLinesPerDate(contributor.getLinesAddedPerDate(), contributor.getLinesDeletedPerDate());
        contributorInfo.addChurn(contributor.getLinesAdded(), contributor.getLinesDeleted(),
                contributor.getLinesAdded30Days(), contributor.getLinesDeleted30Days(),
                contributor.getLinesAdded90Days(), contributor.getLinesDeleted90Days(),
                contributor.getLinesAdded180Days(), contributor.getLinesDeleted180Days(),
                contributor.getLinesAdded365Days(), contributor.getLinesDeleted365Days());

        ContributorRepositoryInfo repoInfo = existingContributor.addRepository(repositoryAnalysisResults, firstCommitDate, latestCommitDate,
                repositoryCommits, repositoryCommits30Days, repositoryCommits90Days,
                repositoryCommits180Days, repositoryCommits365Days,
                new ArrayList<>(commitDates), new LinkedHashMap<>(commitsPerDate));
        repoInfo.addChurn(contributor.getLinesAdded(), contributor.getLinesDeleted(),
                contributor.getLinesAdded30Days(), contributor.getLinesDeleted30Days(),
                contributor.getLinesAdded90Days(), contributor.getLinesDeleted90Days(),
                contributor.getLinesAdded365Days(), contributor.getLinesDeleted365Days());
        repoInfo.addChurnPerDate(contributor.getLinesAddedPerDate(), contributor.getLinesDeletedPerDate());

        if (firstCommitDate.compareTo(contributorInfo.getFirstCommitDate()) < 0) {
            contributorInfo.setFirstCommitDate(firstCommitDate);
        }
        if (latestCommitDate.compareTo(contributorInfo.getLatestCommitDate()) > 0) {
            contributorInfo.setLatestCommitDate(latestCommitDate);
        }
    }

    /** A new landscape contributor from one repository's contributor record, with that repository as its first entry. */
    private ContributorRepositories newContributorRepositories(Contributor contributor, String contributorId, String originalEmail, String originalUserName,
                                                               RepositoryAnalysisResults repositoryAnalysisResults) {
        int repositoryCommits = contributor.getCommitsCount();
        List<String> commitDates = contributor.getCommitDates();
        Map<String, Integer> commitsPerDate = contributor.getCommitsPerDate();
        int repositoryCommits30Days = contributor.getCommitsCount30Days();
        int repositoryCommits90Days = contributor.getCommitsCount90Days();
        int repositoryCommits180Days = contributor.getCommitsCount180Days();
        int repositoryCommits365Days = contributor.getCommitsCount365Days();

        Contributor newContributor = new Contributor();

        newContributor.setEmail(contributorId);
        // If a configured person (matched by email patterns OR userName patterns) defines
        // a userName, it overrides the commit-derived userName; otherwise keep the one
        // from commits. Match on the ORIGINAL email + userName — contributorId may have
        // been collapsed by transformEmail to the person's canonical email or (for entries
        // with a blank email) to the display name, neither of which is an emailPattern, so
        // matching on it would miss the entry and wrongly keep the commit userName.
        String configuredUserName = peopleConfig != null
                ? peopleConfig.getPerson(originalEmail, originalUserName).getUserName() : "";
        newContributor.setUserName(StringUtils.isNotBlank(configuredUserName)
                ? configuredUserName : contributor.getUserName());
        newContributor.setCommitsCount(repositoryCommits);
        newContributor.setCommitsCount30Days(repositoryCommits30Days);
        newContributor.setCommitsCount90Days(repositoryCommits90Days);
        newContributor.setCommitsCount180Days(repositoryCommits180Days);
        newContributor.setCommitsCount365Days(repositoryCommits365Days);
        newContributor.setFirstCommitDate(contributor.getFirstCommitDate());
        newContributor.setLatestCommitDate(contributor.getLatestCommitDate());
        newContributor.setActiveYears(new ArrayList<>(contributor.getActiveYears()));
        newContributor.setCommitDates(new ArrayList<>(contributor.getCommitDates()));
        newContributor.addCommitDatesByScope(contributor.getCommitDatesByScope());
        newContributor.setCommitsPerDate(new LinkedHashMap<>(commitsPerDate));
        newContributor.setLinesAddedPerDate(new LinkedHashMap<>(contributor.getLinesAddedPerDate()));
        newContributor.setLinesDeletedPerDate(new LinkedHashMap<>(contributor.getLinesDeletedPerDate()));
        newContributor.setLinesAdded(contributor.getLinesAdded());
        newContributor.setLinesDeleted(contributor.getLinesDeleted());
        newContributor.setLinesAdded30Days(contributor.getLinesAdded30Days());
        newContributor.setLinesDeleted30Days(contributor.getLinesDeleted30Days());
        newContributor.setLinesAdded90Days(contributor.getLinesAdded90Days());
        newContributor.setLinesDeleted90Days(contributor.getLinesDeleted90Days());
        newContributor.setLinesAdded180Days(contributor.getLinesAdded180Days());
        newContributor.setLinesDeleted180Days(contributor.getLinesDeleted180Days());
        newContributor.setLinesAdded365Days(contributor.getLinesAdded365Days());
        newContributor.setLinesDeleted365Days(contributor.getLinesDeleted365Days());

        ContributorRepositories newContributorWithRepositories = new ContributorRepositories(newContributor);

        ContributorRepositoryInfo newRepoInfo = newContributorWithRepositories.addRepository(repositoryAnalysisResults, newContributor.getFirstCommitDate(),
                newContributor.getLatestCommitDate(),
                repositoryCommits, repositoryCommits30Days, repositoryCommits90Days,
                repositoryCommits180Days, repositoryCommits365Days,
                new ArrayList<>(commitDates), new LinkedHashMap<>(commitsPerDate));
        newRepoInfo.addChurn(contributor.getLinesAdded(), contributor.getLinesDeleted(),
                contributor.getLinesAdded30Days(), contributor.getLinesDeleted30Days(),
                contributor.getLinesAdded90Days(), contributor.getLinesDeleted90Days(),
                contributor.getLinesAdded365Days(), contributor.getLinesDeleted365Days());
        newRepoInfo.addChurnPerDate(contributor.getLinesAddedPerDate(), contributor.getLinesDeletedPerDate());
        return newContributorWithRepositories;
    }

    /** The configured teams built from the contributors (each in the first matching team), plus the "Undefined Team" of active unmatched ones; the contributors themselves when no team is configured. */
    List<ContributorRepositories> teams(List<ContributorRepositories> allContributors) {
        final List<ContributorRepositories> contributors = new ArrayList<>(allContributors);
        if (teamsConfig.getTeams() == null || teamsConfig.getTeams().size() == 0) {
            return contributors;
        }

        final List<ContributorRepositories> teams = new ArrayList<>();
        final Map<String, ContributorRepositories> map = new HashMap<>();

        ContributorRepositories remainder = new ContributorRepositories(new Contributor("Undefined Team"));

        while (contributors.size() > 0) {
            ContributorRepositories contributor = contributors.remove(0);
            if (isBot.test(contributor.getContributor().getEmail())) continue;

            boolean added = addToConfiguredTeam(contributor, teams, map);

            // The "Undefined Team" (remainder) collects contributors not matched by any configured
            // team, but only ACTIVE ones (last commit within Contributor.ACTIVITY_THRESHOLD_DAYS,
            // 180 days) — long-dormant unmatched contributors are left out. Configured teams above
            // keep all their members regardless of activity.
            if (!added && contributor.getContributor().isActive()
                    && !remainder.getMembers().contains(contributor)) {
                addMember(remainder, contributor);
            }
        }

        if (remainder.getMembers().size() > 0 && remainder.getRepositories().size() > 0) {
            teams.add(remainder);
        }

        return teams;
    }

    /** Adds the contributor to the first configured team matching their email or userName (created on first use); false when none matches. */
    private boolean addToConfiguredTeam(ContributorRepositories contributor, List<ContributorRepositories> teams, Map<String, ContributorRepositories> map) {
        String email = contributor.getContributor().getEmail();
        // email and userName are already the config-people.json-canonical values (getAllContributors
        // applied the transformation). Team matching therefore runs against the post-people-config
        // email/userName, falling back to the original commit values where no person config applied.
        String userName = contributor.getContributor().getUserName();
        for (TeamConfig teamConfig : teamsConfig.getTeams()) {
            String name = teamConfig.getName();
            if (teamConfig.matches(email, userName)) {
                ContributorRepositories team = map.get(name);
                if (team == null) {
                    team = new ContributorRepositories(new Contributor(name));
                    map.put(name, team);
                    teams.add(team);
                }
                addMember(team, contributor);
                return true;
            }
        }
        return false;
    }

    private void addMember(ContributorRepositories team, ContributorRepositories contributor) {
        team.getMembers().add(contributor);
        contributor.getRepositories().forEach(repo -> {
            addRepoToTeam(repo, team);
        });
    }

    private void addRepoToTeam(ContributorRepositoryInfo repo, ContributorRepositories team) {
        Contributor teamData = team.getContributor();
        teamData.setCommitsCount(teamData.getCommitsCount() + repo.getCommitsCount());
        teamData.setCommitsCount30Days(teamData.getCommitsCount30Days() + repo.getCommits30Days());
        teamData.setCommitsCount90Days(teamData.getCommitsCount90Days() + repo.getCommits90Days());
        teamData.setCommitsCount180Days(teamData.getCommitsCount180Days() + repo.getCommits180Days());
        teamData.setCommitsCount365Days(teamData.getCommitsCount365Days() + repo.getCommits365Days());
        if (StringUtils.isBlank(teamData.getFirstCommitDate()) || repo.getFirstCommitDate().compareTo(teamData.getFirstCommitDate()) < 0) {
            teamData.setFirstCommitDate(repo.getFirstCommitDate());
        }
        if (StringUtils.isBlank(teamData.getLatestCommitDate()) || repo.getLatestCommitDate().compareTo(teamData.getLatestCommitDate()) > 0) {
            teamData.setLatestCommitDate(repo.getLatestCommitDate());
        }
        ContributorRepositoryInfo teamRepoInfo = team.addRepository(repo.getRepositoryAnalysisResults(), repo.getFirstCommitDate(), repo.getLatestCommitDate(),
                repo.getCommitsCount(), repo.getCommits30Days(), repo.getCommits90Days(),
                repo.getCommits180Days(), repo.getCommits365Days(), repo.getCommitDates(), repo.getCommitsPerDate());
        teamRepoInfo.addChurn(repo.getChurnAdded(), repo.getChurnDeleted(),
                repo.getChurnAdded30Days(), repo.getChurnDeleted30Days(),
                repo.getChurnAdded90Days(), repo.getChurnDeleted90Days(),
                repo.getChurnAdded365Days(), repo.getChurnDeleted365Days());
        teamRepoInfo.addChurnPerDate(repo.getChurnAddedPerDate(), repo.getChurnDeletedPerDate());
    }
}
