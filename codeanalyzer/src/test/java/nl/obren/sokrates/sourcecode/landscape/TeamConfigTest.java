package nl.obren.sokrates.sourcecode.landscape;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamConfigTest {

    private TeamConfig team(String name, List<String> emailPatterns, List<String> userNamePatterns) {
        TeamConfig t = new TeamConfig();
        t.setName(name);
        t.setEmailPatterns(emailPatterns);
        t.setUserNamePatterns(userNamePatterns);
        return t;
    }

    @Test
    void matchesByEmailPattern() {
        TeamConfig t = team("Alpha", List.of("alice@.*"), List.of());
        assertTrue(t.matches("alice@corp.com", "whoever"));
        assertFalse(t.matches("bob@corp.com", "whoever"));
    }

    @Test
    void matchesByUserNamePatternWhenEmailDoesNotMatch() {
        TeamConfig t = team("Alpha", List.of("alice@.*"), List.of("Ahmed.*"));
        assertTrue(t.matches("noreply@github.com", "Ahmed Hached"));
    }

    @Test
    void doesNotMatchWhenNeitherPatternMatches() {
        TeamConfig t = team("Alpha", List.of("alice@.*"), List.of("Ahmed.*"));
        assertFalse(t.matches("bob@corp.com", "Bob"));
    }

    @Test
    void blankUserNameDoesNotMatchUserNamePatterns() {
        TeamConfig t = team("Alpha", List.of("alice@.*"), List.of(".*"));
        // A blank userName must not match a permissive userNamePattern (would team everyone).
        assertFalse(t.matches("bob@corp.com", ""));
        assertFalse(t.matches("bob@corp.com", null));
    }

    @Test
    void matchingIsCaseInsensitive() {
        TeamConfig t = team("Alpha", List.of("alice@corp[.]com"), List.of("ahached"));
        assertTrue(t.matches("Alice@Corp.COM", "whoever"), "email match must be case-insensitive");
        assertTrue(t.matches("noreply@github.com", "AHACHED"), "userName match must be case-insensitive");
    }

    @Test
    void teamsConfigGetTeamMatchesByEmailOrUserName() {
        TeamsConfig config = new TeamsConfig();
        config.setTeams(List.of(team("Alpha", List.of("alice@.*"), List.of("ahached"))));

        assertEquals("Alpha", config.getTeam("alice@corp.com", "irrelevant"));
        assertEquals("Alpha", config.getTeam("noreply@github.com", "ahached"));
        assertNull(config.getTeam("bob@corp.com", "Bob"));
    }

    @Test
    void teamsConfigEmailOnlyOverloadStillWorks() {
        TeamsConfig config = new TeamsConfig();
        config.setTeams(List.of(team("Alpha", List.of("alice@.*"), List.of("ahached"))));

        assertEquals("Alpha", config.getTeam("alice@corp.com"));
        // userName-only match is impossible via the email-only overload (no userName passed).
        assertNull(config.getTeam("noreply@github.com"));
    }
}
