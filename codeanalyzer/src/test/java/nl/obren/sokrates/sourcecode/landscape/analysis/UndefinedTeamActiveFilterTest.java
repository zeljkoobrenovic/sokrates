/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The "Undefined Team" (the remainder bucket in {@link LandscapeAnalysisResults#getTeams()} that
 * collects contributors not matched by any configured team) must include only ACTIVE contributors
 * — those whose last commit is within {@code Contributor.ACTIVITY_THRESHOLD_DAYS} (180 days).
 * Configured teams are unaffected and keep all their members regardless of activity.
 */
class UndefinedTeamActiveFilterTest {

    private static final String UNDEFINED_TEAM = "Undefined Team";

    private static String daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    // A single repository whose contributors all committed on their given latest dates.
    private RepositoryAnalysisResults repositoryWith(Contributor... contributors) {
        ContributorsAnalysisResults car = new ContributorsAnalysisResults();
        car.setContributors(List.of(contributors));
        CodeAnalysisResults analysis = new CodeAnalysisResults();
        analysis.getMetadata().setName("repo-1");
        analysis.getMainAspectAnalysisResults().setLinesOfCode(100);
        analysis.setContributorsAnalysisResults(car);
        return new RepositoryAnalysisResults(null, analysis, null);
    }

    private Contributor contributor(String email, String latestCommitDate) {
        Contributor c = new Contributor();
        c.setEmail(email);
        c.setCommitsCount(1);
        c.setLatestCommitDate(latestCommitDate);
        c.setFirstCommitDate(latestCommitDate);
        return c;
    }

    private LandscapeAnalysisResults landscapeWith(TeamsConfig teamsConfig, RepositoryAnalysisResults repo) {
        LandscapeAnalysisResults results = new LandscapeAnalysisResults(teamsConfig, new PeopleConfig());
        results.setRepositoryAnalysisResults(List.of(repo));
        return results;
    }

    // One configured team matching alice@*; everyone else falls into the remainder.
    private TeamsConfig teamsConfigMatchingAlice() {
        TeamConfig team = new TeamConfig();
        team.setName("Alpha");
        team.setEmailPatterns(List.of("alice@.*"));
        TeamsConfig teamsConfig = new TeamsConfig();
        teamsConfig.setTeams(List.of(team));
        return teamsConfig;
    }

    private ContributorRepositories teamByName(List<ContributorRepositories> teams, String name) {
        return teams.stream()
                .filter(t -> t.getContributor().getEmail().equals(name))
                .findFirst().orElse(null);
    }

    private List<String> memberEmails(ContributorRepositories team) {
        return team.getMembers().stream()
                .map(m -> m.getContributor().getEmail())
                .collect(Collectors.toList());
    }

    @Test
    void undefinedTeamIncludesActiveUnmatchedContributor() {
        Contributor activeUnmatched = contributor("bob@example.com", daysAgo(10));
        LandscapeAnalysisResults results = landscapeWith(teamsConfigMatchingAlice(), repositoryWith(activeUnmatched));

        ContributorRepositories undefined = teamByName(results.getTeams(), UNDEFINED_TEAM);

        assertNotNull(undefined, "Undefined Team should exist for an active unmatched contributor");
        assertTrue(memberEmails(undefined).contains("bob@example.com"));
    }

    @Test
    void undefinedTeamExcludesInactiveUnmatchedContributor() {
        // Last commit well beyond the 180-day activity threshold.
        Contributor inactiveUnmatched = contributor("carol@example.com", daysAgo(400));
        LandscapeAnalysisResults results = landscapeWith(teamsConfigMatchingAlice(), repositoryWith(inactiveUnmatched));

        ContributorRepositories undefined = teamByName(results.getTeams(), UNDEFINED_TEAM);

        // With no active unmatched contributors, the remainder has no members/repos and is dropped.
        assertNull(undefined, "Undefined Team should not include an inactive unmatched contributor");
    }

    @Test
    void undefinedTeamKeepsActiveAndDropsInactiveTogether() {
        Contributor active = contributor("bob@example.com", daysAgo(10));
        Contributor inactive = contributor("carol@example.com", daysAgo(400));
        LandscapeAnalysisResults results =
                landscapeWith(teamsConfigMatchingAlice(), repositoryWith(active, inactive));

        ContributorRepositories undefined = teamByName(results.getTeams(), UNDEFINED_TEAM);

        assertNotNull(undefined);
        List<String> members = memberEmails(undefined);
        assertTrue(members.contains("bob@example.com"), "active contributor should be in Undefined Team");
        assertFalse(members.contains("carol@example.com"), "inactive contributor should be excluded");
    }

    @Test
    void configuredTeamKeepsInactiveMembers() {
        // alice matches the configured "Alpha" team and is inactive — configured teams are not
        // subject to the active-only filter (only the Undefined Team is).
        Contributor inactiveAlice = contributor("alice@example.com", daysAgo(400));
        LandscapeAnalysisResults results = landscapeWith(teamsConfigMatchingAlice(), repositoryWith(inactiveAlice));

        ContributorRepositories alpha = teamByName(results.getTeams(), "Alpha");

        assertNotNull(alpha, "Configured team should exist even with an inactive member");
        assertTrue(memberEmails(alpha).contains("alice@example.com"),
                "Configured team must keep its members regardless of activity");
    }
}
