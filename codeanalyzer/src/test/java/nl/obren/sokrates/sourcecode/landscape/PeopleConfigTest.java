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
    void getPersonByNameIsCaseInsensitive() {
        PeopleConfig config = config(person("alice@corp.com", "Alice", List.of("alice@corp[.]com"), List.of()));

        assertEquals("Alice", config.getPersonByName("Alice@Corp.COM").getUserName());
    }
}
