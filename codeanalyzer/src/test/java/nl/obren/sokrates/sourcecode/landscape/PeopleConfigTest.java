package nl.obren.sokrates.sourcecode.landscape;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeopleConfigTest {

    private PersonConfig person(String email, String userName, List<String> emailPatterns, List<String> userNamePatterns) {
        PersonConfig p = new PersonConfig();
        p.setEmail(email);
        p.setUserName(userName);
        p.setEmailPatterns(emailPatterns);
        p.setUserNamePatterns(userNamePatterns);
        return p;
    }

    private PeopleConfig config(PersonConfig... people) {
        PeopleConfig c = new PeopleConfig();
        c.setPeople(List.of(people));
        return c;
    }

    @Test
    void getPersonFromEmailPatternsIsCaseInsensitive() {
        PeopleConfig config = config(person("alice@corp.com", "Alice", List.of("alice@corp[.]com"), List.of()));

        assertEquals("Alice", config.getPersonFromEmailPatterns("Alice@Corp.COM").getUserName());
    }

    @Test
    void getPersonMatchesEmailOrUserNameCaseInsensitively() {
        PeopleConfig config = config(person("alice@corp.com", "Alice", List.of("alice@corp[.]com"), List.of("al1ce")));

        assertEquals("Alice", config.getPerson("ALICE@CORP.COM", "irrelevant").getUserName());
        assertEquals("Alice", config.getPerson("noreply@github.com", "AL1CE").getUserName());
    }

    @Test
    void getPersonFromEmailPatternsMatchesPlainEmailFieldWithoutPatterns() {
        // A person defined with just an email (no emailPatterns) still absorbs that contributor.
        PeopleConfig config = config(person("alice@corp.com", "Alice", List.of(), List.of()));

        assertEquals("Alice", config.getPersonFromEmailPatterns("ALICE@corp.com").getUserName());
    }

    @Test
    void getPersonMatchesPlainEmailFieldWithoutPatterns() {
        PeopleConfig config = config(person("alice@corp.com", "Alice", List.of(), List.of()));

        assertEquals("Alice", config.getPerson("Alice@Corp.COM", "whatever").getUserName());
    }

    @Test
    void getPersonMatchesPlainUserNameFieldWithoutPatterns() {
        // A person defined with just a userName (no userNamePatterns) still absorbs the contributor
        // whose commit userName matches — instead of a second, unmatched person with the same name.
        PeopleConfig config = config(person("alice@corp.com", "Alice", List.of(), List.of()));

        assertEquals("alice@corp.com", config.getPerson("noreply@github.com", "ALICE").getEmail());
    }

    @Test
    void getPersonByEmailIsCaseInsensitive() {
        PeopleConfig config = config(person("alice@corp.com", "Alice", List.of("alice@corp[.]com"), List.of()));

        assertEquals("Alice", config.getPersonByEmail("Alice@Corp.COM").getUserName());
    }
}
