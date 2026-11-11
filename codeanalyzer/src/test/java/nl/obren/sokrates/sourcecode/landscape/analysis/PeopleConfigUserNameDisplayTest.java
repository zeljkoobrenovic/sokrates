package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end aggregation checks for config-people.json display names. A person entry that groups
 * emails via {@code emailPatterns} but leaves {@code email} blank (only a display name / userName)
 * must still (a) keep the contributor in the list — not collapse them to a blank id and drop them —
 * and (b) show the configured display name, NOT the commit-derived userName.
 */
class PeopleConfigUserNameDisplayTest {

    private static String daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    private Contributor contributor(String email, String commitUserName) {
        Contributor c = new Contributor();
        c.setEmail(email);
        c.setUserName(commitUserName);
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

    private LandscapeAnalysisResults landscapeWith(PeopleConfig peopleConfig, RepositoryAnalysisResults repo) {
        LandscapeAnalysisResults results = new LandscapeAnalysisResults(new TeamsConfig(), peopleConfig);
        results.setRepositoryAnalysisResults(List.of(repo));
        return results;
    }

    private PeopleConfig peopleConfigWith(PersonConfig... people) {
        PeopleConfig config = new PeopleConfig();
        config.setPeople(List.of(people));
        return config;
    }

    private ContributorRepositories findByUserName(List<ContributorRepositories> list, String userName) {
        return list.stream()
                .filter(c -> userName.equals(c.getContributor().getUserName()))
                .findFirst().orElse(null);
    }

    @Test
    void blankEmailEntryKeepsContributorAndShowsConfiguredUserName() {
        PersonConfig person = new PersonConfig();
        person.setUserName("ahmed hached");         // display name only, no canonical email
        person.setEmailPatterns(List.of("hachedahmeddev"));

        LandscapeAnalysisResults results = landscapeWith(
                peopleConfigWith(person),
                repositoryWith(contributor("hachedahmeddev", "ahached")));

        List<ContributorRepositories> contributors = results.getContributors();

        // The contributor is NOT dropped (was collapsed to a blank id before the fix).
        assertEquals(1, contributors.size(), "contributor must remain in the list");
        // The display name is the configured userName, not the commit-derived "ahached".
        ContributorRepositories c = contributors.get(0);
        assertEquals("ahmed hached", c.getContributor().getUserName());
        assertNotEquals("ahached", c.getContributor().getUserName());
    }

    @Test
    void blankEmailEntryGroupsMultipleEmailsUnderOneContributor() {
        PersonConfig person = new PersonConfig();
        person.setUserName("ahmed hached");
        person.setEmailPatterns(List.of("hachedahmeddev", "ahmed@work.com"));

        LandscapeAnalysisResults results = landscapeWith(
                peopleConfigWith(person),
                repositoryWith(
                        contributor("hachedahmeddev", "ahached"),
                        contributor("ahmed@work.com", "Ahmed H")));

        List<ContributorRepositories> contributors = results.getContributors();

        // Both emails collapse to one contributor keyed on the display name.
        assertEquals(1, contributors.size(), "both emails should group into one contributor");
        assertEquals("ahmed hached", findByUserName(contributors, "ahmed hached").getContributor().getUserName());
    }

    @Test
    void matchesContributorByUserNamePatternWhenEmailDoesNotMatch() {
        PersonConfig person = new PersonConfig();
        person.setEmail("ahmed@corp.com");
        person.setUserName("Ahmed Hached");
        person.setEmailPatterns(List.of("ahmed@corp[.]com"));
        person.setUserNamePatterns(List.of("ahached"));

        // The commit email doesn't match any emailPattern, but the commit userName "ahached" does.
        LandscapeAnalysisResults results = landscapeWith(
                peopleConfigWith(person),
                repositoryWith(contributor("noreply-12345@github.com", "ahached")));

        List<ContributorRepositories> contributors = results.getContributors();

        assertEquals(1, contributors.size());
        assertEquals("ahmed@corp.com", contributors.get(0).getContributor().getEmail());
        assertEquals("Ahmed Hached", contributors.get(0).getContributor().getUserName());
    }

    @Test
    void groupsContributorsMatchedByEmailAndByUserNameUnderOnePerson() {
        PersonConfig person = new PersonConfig();
        person.setEmail("ahmed@corp.com");
        person.setUserName("Ahmed Hached");
        person.setEmailPatterns(List.of("ahmed@corp[.]com"));
        person.setUserNamePatterns(List.of("ahached"));

        LandscapeAnalysisResults results = landscapeWith(
                peopleConfigWith(person),
                repositoryWith(
                        contributor("ahmed@corp.com", "Ahmed"),          // matched by email
                        contributor("noreply-12345@github.com", "ahached"))); // matched by userName

        List<ContributorRepositories> contributors = results.getContributors();

        // Both collapse to the same canonical email -> one grouped contributor.
        assertEquals(1, contributors.size(), "email-matched and userName-matched should group");
        assertEquals("ahmed@corp.com", contributors.get(0).getContributor().getEmail());
        assertEquals("Ahmed Hached", contributors.get(0).getContributor().getUserName());
    }

    @Test
    void canonicalEmailEntryUsesEmailAsKeyAndConfiguredUserName() {
        PersonConfig person = new PersonConfig();
        person.setEmail("ahmed@corp.com");
        person.setUserName("Ahmed Hached");
        person.setEmailPatterns(List.of("hachedahmeddev", "ahmed@corp.com"));

        LandscapeAnalysisResults results = landscapeWith(
                peopleConfigWith(person),
                repositoryWith(contributor("hachedahmeddev", "ahached")));

        List<ContributorRepositories> contributors = results.getContributors();

        assertEquals(1, contributors.size());
        assertEquals("ahmed@corp.com", contributors.get(0).getContributor().getEmail());
        assertEquals("Ahmed Hached", contributors.get(0).getContributor().getUserName());
    }
}
