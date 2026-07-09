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

    private ContributorIdentity id(String email, String userName, String date) {
        return new ContributorIdentity(email, userName, date);
    }

    private PersonConfig person(PeopleConfig config, String userName) {
        return config.getPeople().stream()
                .filter(p -> p.getUserName().equals(userName))
                .findFirst().orElseThrow();
    }

    @Test
    void groupsEmailsBySharedUserNameIntoOneEntryWithLatestEmail() {
        PeopleConfig config = new PeopleConfig();
        List<ContributorIdentity> identities = Arrays.asList(
                id("guido@python.org", "Guido van Rossum", "2020-01-01"),
                id("guido@dropbox.com", "Guido van Rossum", "2024-05-01"),
                id("guido@google.com", "Guido van Rossum", "2022-01-01"));

        int added = new PeopleConfigByUserNameUpdater().update(config, identities);

        // One entry; all three emails become patterns; email field = latest-used (2024 = dropbox).
        assertEquals(3, added);
        assertEquals(1, config.getPeople().size());
        PersonConfig guido = person(config, "Guido van Rossum");
        assertEquals("guido@dropbox.com", guido.getEmail());
        assertEquals(3, guido.getEmailPatterns().size());
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
    void doesNotOverwriteAnExistingNonBlankEmail() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("Guido van Rossum");
        existing.setEmail("guido@python.org"); // already set
        config.getPeople().add(existing);

        new PeopleConfigByUserNameUpdater().update(config, Arrays.asList(
                id("guido@python.org", "Guido van Rossum", "2020-01-01"),
                id("guido@dropbox.com", "Guido van Rossum", "2024-05-01"))); // newer

        // email field is frozen (not overwritten with the newer one); patterns still accumulate both.
        PersonConfig guido = person(config, "Guido van Rossum");
        assertEquals("guido@python.org", guido.getEmail());
        assertEquals(2, guido.getEmailPatterns().size());
        assertTrue(RegexUtils.matchesAnyPattern("guido@dropbox.com", guido.getEmailPatterns()));
    }

    @Test
    void fillsBlankEmailWithLatestUsed() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("Guido van Rossum");
        existing.setEmail(""); // blank — should be filled
        config.getPeople().add(existing);

        new PeopleConfigByUserNameUpdater().update(config, Arrays.asList(
                id("guido@python.org", "Guido van Rossum", "2020-01-01"),
                id("guido@dropbox.com", "Guido van Rossum", "2024-05-01")));

        assertEquals("guido@dropbox.com", person(config, "Guido van Rossum").getEmail());
    }

    @Test
    void matchesExistingEntryByUserNameCaseInsensitively() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("Guido van Rossum");
        config.getPeople().add(existing);

        new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("guido@dropbox.com", "guido van rossum", "2024-01-01")));

        // Still one entry; the (blank) email of the existing differently-cased entry got filled.
        assertEquals(1, config.getPeople().size());
        assertEquals("guido@dropbox.com", person(config, "Guido van Rossum").getEmail());
    }

    @Test
    void neverRemovesExistingEntriesOrPatterns() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig untouched = new PersonConfig();
        untouched.setUserName("Old Person");
        untouched.setEmail("old@x.com");
        untouched.getEmailPatterns().add("\\Qold@x.com\\E");
        config.getPeople().add(untouched);

        // Identities for an unrelated person only.
        new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("new@x.com", "New Person")));

        assertEquals(2, config.getPeople().size());
        // Old entry kept verbatim.
        assertEquals("old@x.com", person(config, "Old Person").getEmail());
        assertEquals(1, person(config, "Old Person").getEmailPatterns().size());
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
    void doesNotAddAPatternAlreadyUsedByAnotherUserName() {
        PeopleConfig config = new PeopleConfig();
        // An existing person already claims shared@x.com as a literal pattern.
        PersonConfig other = new PersonConfig();
        other.setUserName("Alice");
        other.setEmail("shared@x.com");
        other.getEmailPatterns().add("\\Qshared@x.com\\E");
        config.getPeople().add(other);

        // A different userName commits under the same shared email plus a unique one.
        int added = new PeopleConfigByUserNameUpdater().update(config, Arrays.asList(
                id("shared@x.com", "Bob"),
                id("bob@x.com", "Bob")));

        PersonConfig bob = person(config, "Bob");
        // shared@x.com is NOT added to Bob (Alice already has that pattern); only bob@x.com is.
        assertEquals(1, added);
        assertEquals(1, bob.getEmailPatterns().size());
        assertEquals("\\Qbob@x.com\\E", bob.getEmailPatterns().get(0));
        // Alice keeps her single pattern (no duplication, nothing removed).
        assertEquals(1, person(config, "Alice").getEmailPatterns().size());
    }

    @Test
    void coversTheExistingEmailFieldWithAPatternEvenIfNotAmongObservedEmails() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("Guido van Rossum");
        existing.setEmail("guido@python.org"); // canonical email, no pattern covering it yet
        config.getPeople().add(existing);

        // The observed identity uses a DIFFERENT email under the same userName.
        int added = new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("guido@dropbox.com", "Guido van Rossum")));

        PersonConfig guido = person(config, "Guido van Rossum");
        // Both the observed email AND the canonical email field are now covered by patterns, so the
        // person collapses by email regardless of the display name.
        assertEquals(2, added);
        assertTrue(RegexUtils.matchesAnyPattern("guido@dropbox.com", guido.getEmailPatterns()));
        assertTrue(RegexUtils.matchesAnyPattern("guido@python.org", guido.getEmailPatterns()));
    }

    @Test
    void addsPatternForEachEmailUnderASharedDisplayNameSoTheyCollapseByEmail() {
        // Regression for the "james lesslar" / "James Lesslar" split: an entry whose only pattern
        // (a legacy substring) does not cover a second address the person also commits from. Once the
        // second address is observed under the same display name, it must get its own pattern so the
        // identity collapses by email — not only via the fragile userName match.
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("james lesslar");
        existing.setEmail("");
        existing.getEmailPatterns().add("jameslesslar"); // legacy substring; matches only "jameslesslar"
        config.getPeople().add(existing);

        new PeopleConfigByUserNameUpdater().update(config, Arrays.asList(
                id("jameslesslar", "James Lesslar", "2024-01-01"),
                id("jlesslar", "James Lesslar", "2024-02-01")));

        PersonConfig james = person(config, "james lesslar");
        // jameslesslar was already covered by the legacy pattern; jlesslar gets a new literal pattern.
        assertTrue(RegexUtils.matchesAnyPattern("jameslesslar", james.getEmailPatterns()));
        assertTrue(RegexUtils.matchesAnyPattern("jlesslar", james.getEmailPatterns()),
                "jlesslar must be covered by a pattern so it collapses by email");
    }

    @Test
    void groupsWhitespaceAndCaseVariantsOfADisplayNameIntoOneEntry() {
        PeopleConfig config = new PeopleConfig();
        // Same person committing under case/spacing variants of the display name, incl. the no-space form.
        int added = new PeopleConfigByUserNameUpdater().update(config, Arrays.asList(
                id("a@x.com", "James Lesslar", "2024-01-01"),
                id("b@x.com", "JAMES LESSLAR", "2024-02-01"),
                id("c@x.com", "James  Lesslar", "2024-03-01"),
                id("d@x.com", "JamesLesslar", "2024-04-01")));

        // One merged entry; the display name is the first-seen readable form; all four emails covered.
        assertEquals(1, config.getPeople().size());
        assertEquals(4, added);
        PersonConfig james = config.getPeople().get(0);
        assertEquals("James Lesslar", james.getUserName());
        assertEquals("d@x.com", james.getEmail()); // latest-used
        for (String email : new String[]{"a@x.com", "b@x.com", "c@x.com", "d@x.com"}) {
            assertTrue(RegexUtils.matchesAnyPattern(email, james.getEmailPatterns()));
        }
    }

    @Test
    void mergesIntoAnExistingEntryWhoseDisplayNameIsAWhitespaceVariant() {
        PeopleConfig config = new PeopleConfig();
        PersonConfig existing = new PersonConfig();
        existing.setUserName("James Lesslar"); // with space
        config.getPeople().add(existing);

        // Commit uses the no-space form; must land on the existing entry, not create a new one.
        new PeopleConfigByUserNameUpdater().update(config,
                Arrays.asList(id("jl@x.com", "JamesLesslar", "2024-01-01")));

        assertEquals(1, config.getPeople().size());
        assertEquals("jl@x.com", person(config, "James Lesslar").getEmail());
    }

    @Test
    void doesNotMergeDistinctNamesThatOnlyShareStrippedCharacters() {
        PeopleConfig config = new PeopleConfig();
        new PeopleConfigByUserNameUpdater().update(config, Arrays.asList(
                id("al@x.com", "Al Green"),
                id("alan@x.com", "Alan Green")));

        // "Al Green" -> "algreen", "Alan Green" -> "alangreen": different keys, two entries.
        assertEquals(2, config.getPeople().size());
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
