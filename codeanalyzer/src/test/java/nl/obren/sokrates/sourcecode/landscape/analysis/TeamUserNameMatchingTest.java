package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * config-teams.json {@code userNamePatterns}: a contributor joins a team when their email matches an
 * {@code emailPattern} OR their userName matches a {@code userNamePattern}. Team matching runs AFTER
 * config-people.json transformations, so the email/userName seen here are the people-config-canonical
 * values (or the original commit values when no person config applied).
 */
class TeamUserNameMatchingTest {

    private static String daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    private Contributor contributor(String email, String userName) {
        Contributor c = new Contributor();
        c.setEmail(email);
        c.setUserName(userName);
        c.setCommitsCount(3);
        c.setLatestCommitDate(daysAgo(5));
        c.setFirstCommitDate(daysAgo(5));
        return c;
    }

    private RepositoryAnalysisResults repositoryWith(Contributor... contributors) {
        ContributorsAnalysisResults car = new ContributorsAnalysisResults();
        car.setContributors(List.of(contributors));
        CodeAnalysisResults analysis = new CodeAnalysisResults();
        analysis.getMetadata().setName("repo-1");
        analysis.getMainAspectAnalysisResults().setLinesOfCode(100);
        analysis.setContributorsAnalysisResults(car);
        return new RepositoryAnalysisResults(null, analysis, null);
    }

    private LandscapeAnalysisResults landscape(TeamsConfig teamsConfig, PeopleConfig peopleConfig,
                                               RepositoryAnalysisResults repo) {
        LandscapeAnalysisResults results = new LandscapeAnalysisResults(teamsConfig, peopleConfig);
        results.setRepositoryAnalysisResults(List.of(repo));
        return results;
    }

    private TeamsConfig teamsConfig(TeamConfig... teams) {
        TeamsConfig config = new TeamsConfig();
        config.setTeams(List.of(teams));
        return config;
    }

    private TeamConfig team(String name, List<String> emailPatterns, List<String> userNamePatterns) {
        TeamConfig t = new TeamConfig();
        t.setName(name);
        t.setEmailPatterns(emailPatterns);
        t.setUserNamePatterns(userNamePatterns);
        return t;
    }

    private ContributorRepositories teamByName(List<ContributorRepositories> teams, String name) {
        return teams.stream()
                .filter(t -> t.getContributor().getEmail().equals(name))
                .findFirst().orElse(null);
    }

    private List<String> memberEmails(ContributorRepositories team) {
        return team.getMembers().stream().map(m -> m.getContributor().getEmail()).collect(java.util.stream.Collectors.toList());
    }

    @Test
    void contributorJoinsTeamByUserNamePattern() {
        // The email matches no emailPattern, but the commit userName "ahached" matches.
        LandscapeAnalysisResults results = landscape(
                teamsConfig(team("Alpha", List.of("alice@.*"), List.of("ahached"))),
                new PeopleConfig(),
                repositoryWith(contributor("noreply-1@github.com", "ahached")));

        ContributorRepositories alpha = teamByName(results.getTeams(), "Alpha");

        assertNotNull(alpha, "contributor should join Alpha via userNamePattern");
        assertTrue(memberEmails(alpha).contains("noreply-1@github.com"));
    }

    @Test
    void teamMatchesAgainstPeopleConfigCanonicalValues() {
        // config-people.json remaps hachedahmeddev -> canonical email + display name "Ahmed Hached".
        PersonConfig person = new PersonConfig();
        person.setEmail("ahmed@corp.com");
        person.setUserName("Ahmed Hached");
        person.setEmailPatterns(List.of("hachedahmeddev"));
        PeopleConfig peopleConfig = new PeopleConfig();
        peopleConfig.setPeople(List.of(person));

        // The team matches on the POST-transformation display name, not the raw commit name "ahached".
        LandscapeAnalysisResults results = landscape(
                teamsConfig(team("Alpha", List.of("nobody@.*"), List.of("Ahmed Hached"))),
                peopleConfig,
                repositoryWith(contributor("hachedahmeddev", "ahached")));

        ContributorRepositories alpha = teamByName(results.getTeams(), "Alpha");

        assertNotNull(alpha, "team should match the people-config canonical userName");
        assertTrue(memberEmails(alpha).contains("ahmed@corp.com"),
                "member is keyed by the canonical email after people-config transformation");
    }

    @Test
    void emailPatternStillMatchesWithoutUserNamePatterns() {
        LandscapeAnalysisResults results = landscape(
                teamsConfig(team("Alpha", List.of("alice@.*"), List.of())),
                new PeopleConfig(),
                repositoryWith(contributor("alice@corp.com", "Alice")));

        assertNotNull(teamByName(results.getTeams(), "Alpha"));
    }
}
