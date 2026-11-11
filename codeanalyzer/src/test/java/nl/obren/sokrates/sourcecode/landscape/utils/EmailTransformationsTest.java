package nl.obren.sokrates.sourcecode.landscape.utils;

import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailTransformationsTest {

    private PersonConfig person(String email, String userName, String... patterns) {
        PersonConfig p = new PersonConfig();
        p.setEmail(email);
        p.setUserName(userName);
        p.setEmailPatterns(List.of(patterns));
        return p;
    }

    private PeopleConfig peopleConfig(PersonConfig... people) {
        PeopleConfig config = new PeopleConfig();
        config.setPeople(List.of(people));
        return config;
    }

    private PersonConfig personWithUserNamePatterns(String email, String userName,
                                                    List<String> emailPatterns, List<String> userNamePatterns) {
        PersonConfig p = new PersonConfig();
        p.setEmail(email);
        p.setUserName(userName);
        p.setEmailPatterns(emailPatterns);
        p.setUserNamePatterns(userNamePatterns);
        return p;
    }

    @Test
    void collapsesToCanonicalEmailWhenSet() {
        PeopleConfig config = peopleConfig(person("alice@corp.com", "Alice", "alice@.*"));

        assertEquals("alice@corp.com",
                EmailTransformations.transformEmail("alice@gmail.com", new ArrayList<>(), config));
    }

    @Test
    void matchesByUserNamePatternWhenEmailDoesNotMatch() {
        // Email does not match any emailPattern, but the commit userName matches a userNamePattern.
        PeopleConfig config = peopleConfig(personWithUserNamePatterns(
                "alice@corp.com", "Alice", List.of("alice@corp[.]com"), List.of("alice.*")));

        assertEquals("alice@corp.com",
                EmailTransformations.transformEmail("random123@noreply.github.com", "alice-bot", new ArrayList<>(), config));
    }

    @Test
    void userNamePatternWithBlankEmailCollapsesToDisplayName() {
        PeopleConfig config = peopleConfig(personWithUserNamePatterns(
                "", "Ahmed Hached", List.of("hachedahmeddev"), List.of("ahached")));

        // A different email, but userName "ahached" matches -> collapses to the display name.
        assertEquals("Ahmed Hached",
                EmailTransformations.transformEmail("another@email.com", "ahached", new ArrayList<>(), config));
    }

    @Test
    void emailMatchingIsCaseInsensitive() {
        PeopleConfig config = peopleConfig(person("alice@corp.com", "Alice", "alice@corp[.]com"));

        // A differently-cased commit email must resolve to the same person.
        assertEquals("alice@corp.com",
                EmailTransformations.transformEmail("Alice@Corp.COM", new ArrayList<>(), config));
    }

    @Test
    void userNameMatchingIsCaseInsensitive() {
        PeopleConfig config = peopleConfig(personWithUserNamePatterns(
                "", "Ahmed Hached", List.of("nomatch"), List.of("ahached")));

        assertEquals("Ahmed Hached",
                EmailTransformations.transformEmail("x@y.com", "AHACHED", new ArrayList<>(), config));
    }

    @Test
    void fallsBackToUserNameWhenEmailBlank() {
        // Legacy config entries group emails via emailPatterns and leave `email` blank, keeping only
        // a display name. The person must still collapse to a stable, non-blank id (the userName) so
        // they are NOT dropped from the reports (getAllContributors skips blank ids).
        PeopleConfig config = peopleConfig(person("", "ahmed hached", "hachedahmeddev"));

        assertEquals("ahmed hached",
                EmailTransformations.transformEmail("hachedahmeddev", new ArrayList<>(), config));
    }

    @Test
    void keepsOriginalIdWhenNoConfiguredPersonMatches() {
        PeopleConfig config = peopleConfig(person("", "ahmed hached", "hachedahmeddev"));

        assertEquals("stranger@nowhere.com",
                EmailTransformations.transformEmail("stranger@nowhere.com", new ArrayList<>(), config));
    }

    @Test
    void nullPeopleConfigIsIdentity() {
        assertEquals("bob@corp.com",
                EmailTransformations.transformEmail("bob@corp.com", new ArrayList<>(), null));
    }
}
