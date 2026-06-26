package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.reports.landscape.statichtml.PeopleConfigByUserNameUpdater.ContributorIdentity;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeopleConfigByUserNameUpdaterTest {

    private ContributorIdentity id(String email, String userName) {
        return new ContributorIdentity(email, userName);
    }

    private PersonConfig person(PeopleConfig config, String userName) {
        return config.getPeople().stream()
                .filter(p -> p.getUserName().equals(userName))
                .findFirst().orElseThrow();
    }

    @Test
    void groupsEmailsBySharedUserNameIntoOneEntry() {
        PeopleConfig config = new PeopleConfig();
        List<ContributorIdentity> identities = Arrays.asList(
                id("guido@python.org", "Guido van Rossum"),
                id("guido@dropbox.com", "Guido van Rossum"),
                id("guido@google.com", "Guido van Rossum"));

        int added = new PeopleConfigByUserNameUpdater().update(config, identities);

        assertEquals(3, added);
        assertEquals(1, config.getPeople().size());
        PersonConfig guido = person(config, "Guido van Rossum");
        assertEquals("guido@python.org;guido@dropbox.com;guido@google.com", guido.getEmail());
    }

    @Test
    void createsSeparateEntriesForDifferentUserNames() {
        PeopleConfig config = new PeopleConfig();
        List<ContributorIdentity> identities = Arrays.asList(
                id("a@x.com", "Alice"),
                id("b@x.com", "Bob"));

        new PeopleConfigByUserNameUpdater().update(config, identities);

        assertEquals(2, config.getPeople().size());
        assertEquals("a@x.com", person(config, "Alice").getEmail());
        assertEquals("b@x.com", person(config, "Bob").getEmail());
    }

    @Test
    void appendsOnlyNewEmailsToExistingEntryAndKeepsOrder() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("Guido van Rossum");
        existing.setEmail("guido@python.org");
        config.getPeople().add(existing);

        List<ContributorIdentity> identities = Arrays.asList(
                id("guido@python.org", "Guido van Rossum"), // already present
                id("guido@dropbox.com", "Guido van Rossum")); // new

        int added = new PeopleConfigByUserNameUpdater().update(config, identities);

        assertEquals(1, added);
        assertEquals(1, config.getPeople().size());
        // Existing email preserved, new one appended after it.
        assertEquals("guido@python.org;guido@dropbox.com", person(config, "Guido van Rossum").getEmail());
    }

    @Test
    void deduplicatesEmailsCaseInsensitively() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("Alice");
        existing.setEmail("Alice@X.com");
        config.getPeople().add(existing);

        int added = new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("alice@x.com", "Alice"), id("new@x.com", "Alice")));

        assertEquals(1, added);
        assertEquals("Alice@X.com;new@x.com", person(config, "Alice").getEmail());
    }

    @Test
    void matchesExistingEntryByUserNameCaseInsensitively() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("Guido van Rossum");
        existing.setEmail("guido@python.org");
        config.getPeople().add(existing);

        new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("guido@dropbox.com", "guido van rossum")));

        // Still one entry; new email appended to the existing (differently-cased) userName entry.
        assertEquals(1, config.getPeople().size());
        assertEquals("guido@python.org;guido@dropbox.com", person(config, "Guido van Rossum").getEmail());
    }

    @Test
    void neverRemovesExistingEntriesOrEmails() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig untouched = new PersonConfig();
        untouched.setUserName("Old Person");
        untouched.setEmail("old@x.com;older@x.com");
        config.getPeople().add(untouched);

        // Identities for an unrelated person only.
        new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("new@x.com", "New Person")));

        assertEquals(2, config.getPeople().size());
        // Old entry kept verbatim.
        assertEquals("old@x.com;older@x.com", person(config, "Old Person").getEmail());
    }

    @Test
    void addsAnEmailPatternMatchingEachGroupedEmail() {
        PeopleConfig config = new PeopleConfig();
        new PeopleConfigByUserNameUpdater().update(config, Arrays.asList(
                id("guido@python.org", "Guido van Rossum"),
                id("guido@dropbox.com", "Guido van Rossum")));

        PersonConfig guido = person(config, "Guido van Rossum");
        assertEquals(2, guido.getEmailPatterns().size());
        // Each grouped email is fully matched by some pattern (this is the matching contract).
        assertTrue(RegexUtils.matchesAnyPattern("guido@python.org", guido.getEmailPatterns()));
        assertTrue(RegexUtils.matchesAnyPattern("guido@dropbox.com", guido.getEmailPatterns()));
    }

    @Test
    void emailPatternsMatchTheEmailLiterally_dotIsNotWildcard() {
        PeopleConfig config = new PeopleConfig();
        new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("a.b@x.io", "Dotted")));

        PersonConfig p = person(config, "Dotted");
        assertTrue(RegexUtils.matchesAnyPattern("a.b@x.io", p.getEmailPatterns()));
        // '.' must be literal, so a different address with letters where the dots are must NOT match.
        assertFalse(RegexUtils.matchesAnyPattern("axb@xyio", p.getEmailPatterns()));
    }

    @Test
    void doesNotDuplicateAPatternThatAlreadyCoversTheEmail() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("Guido van Rossum");
        existing.setEmail("guido@python.org");
        // Pre-existing hand-written pattern (char class) that already matches the email.
        existing.getEmailPatterns().add("guido[@]python.org");
        config.getPeople().add(existing);

        new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("guido@python.org", "Guido van Rossum")));

        // No new pattern added — the existing one already fully matches.
        assertEquals(1, person(config, "Guido van Rossum").getEmailPatterns().size());
        assertEquals("guido[@]python.org", person(config, "Guido van Rossum").getEmailPatterns().get(0));
    }

    @Test
    void ignoresIdentitiesWithBlankEmailOrUserName() {
        PeopleConfig config = new PeopleConfig();
        List<ContributorIdentity> identities = new ArrayList<>();
        identities.add(id("", "No Email"));
        identities.add(id("noname@x.com", ""));
        identities.add(id("good@x.com", "Good Person"));

        int added = new PeopleConfigByUserNameUpdater().update(config, identities);

        assertEquals(1, added);
        assertEquals(1, config.getPeople().size());
        assertEquals("good@x.com", person(config, "Good Person").getEmail());
    }
}
