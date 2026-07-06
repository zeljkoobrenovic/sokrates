package nl.obren.sokrates.sourcecode.githistory;

import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitHistoryUtilsTest {

    @Test
    void parseLine() {
        String line = "2019-11-09 author@github.com 0bc5d0318b3814ebd5b52605668756a8d5598e24 common/src/main/resources/components/ace/src/snippets/maze.js";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, new FileHistoryAnalysisConfig());

        assertEquals(fileUpdate.getDate(), "2019-11-09");
        assertEquals(fileUpdate.getAuthorEmail(), "author@github.com");
        assertEquals(fileUpdate.getCommitId(), "0bc5d0318b3814ebd5b52605668756a8d5598e24");
        assertEquals(fileUpdate.getPath(), "common/src/main/resources/components/ace/src/snippets/maze.js");
        // Lines without churn columns default to 0.
        assertEquals(0, fileUpdate.getLinesAdded());
        assertEquals(0, fileUpdate.getLinesDeleted());
    }

    @Test
    void parseLineWithChurnColumns() {
        String line = "2019-11-09 author@github.com 0bc5d0318b3814ebd5b52605668756a8d5598e24 a/B.java Jane&nbsp;Doe 12 5";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, new FileHistoryAnalysisConfig());

        assertEquals("a/B.java", fileUpdate.getPath());
        assertEquals("Jane Doe", fileUpdate.getUserName());
        assertEquals(12, fileUpdate.getLinesAdded());
        assertEquals(5, fileUpdate.getLinesDeleted());
    }

    @Test
    void parseLineWithoutChurnColumnsKeepsZero() {
        // Name present but no churn columns (older history format).
        String line = "2019-11-09 author@github.com 0bc5d0318b3814ebd5b52605668756a8d5598e24 a/B.java Jane&nbsp;Doe";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, new FileHistoryAnalysisConfig());

        assertEquals("Jane Doe", fileUpdate.getUserName());
        assertEquals(0, fileUpdate.getLinesAdded());
        assertEquals(0, fileUpdate.getLinesDeleted());
    }

    @Test
    void parseLineDetectsBotFromEmail() {
        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();
        config.setBots(Arrays.asList(".*bot.*"));

        String botLine = "2019-11-09 ci-bot@github.com 0bc5d0318b3814ebd5b52605668756a8d5598e24 a/B.java";
        assertTrue(GitHistoryUtils.parseLine(botLine, config).isBot());

        String humanLine = "2019-11-09 alice@github.com 0bc5d0318b3814ebd5b52605668756a8d5598e24 a/B.java";
        assertFalse(GitHistoryUtils.parseLine(humanLine, config).isBot());
    }

    @Test
    void parseLineBotDetectionUsesTransformedEmail() {
        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();
        config.setBots(Arrays.asList(".*bot.*"));
        // Rewrite any address to a bot address; the bot flag must reflect the transformed email.
        config.setTransformContributorEmails(Arrays.asList(
                new nl.obren.sokrates.sourcecode.operations.OperationStatement("replace", Arrays.asList(".*", "service-bot@org.com"))));

        String line = "2019-11-09 alice@github.com 0bc5d0318b3814ebd5b52605668756a8d5598e24 a/B.java";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, config);

        assertEquals("service-bot@org.com", fileUpdate.getAuthorEmail());
        assertTrue(fileUpdate.isBot());
    }

    @Test
    void parseLineAppliesPeopleConfigRemapAndUserNameOverride() {
        nl.obren.sokrates.sourcecode.landscape.PeopleConfig peopleConfig =
                new nl.obren.sokrates.sourcecode.landscape.PeopleConfig();
        nl.obren.sokrates.sourcecode.landscape.PersonConfig person =
                new nl.obren.sokrates.sourcecode.landscape.PersonConfig();
        person.setUserName("Guido van Rossum");
        person.setEmail("guido@python.org");
        person.setEmailPatterns(Arrays.asList("\\Qguido@python.org\\E", "\\Qguido@dropbox.com\\E"));
        peopleConfig.setPeople(Arrays.asList(person));

        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();
        config.setPeopleConfig(peopleConfig);

        // A commit under the dropbox email is remapped to the canonical email and the configured name.
        String line = "2024-01-01 guido@dropbox.com c1 a/B.java Some&nbsp;Other&nbsp;Name";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, config);

        assertEquals("guido@python.org", fileUpdate.getAuthorEmail());
        assertEquals("Guido van Rossum", fileUpdate.getUserName());
    }

    @Test
    void parseLineMatchesPersonByUserNamePattern() {
        nl.obren.sokrates.sourcecode.landscape.PeopleConfig peopleConfig =
                new nl.obren.sokrates.sourcecode.landscape.PeopleConfig();
        nl.obren.sokrates.sourcecode.landscape.PersonConfig person =
                new nl.obren.sokrates.sourcecode.landscape.PersonConfig();
        person.setUserName("Ahmed Hached");
        person.setEmail("ahmed@corp.com");
        person.setEmailPatterns(Arrays.asList("\\Qahmed@corp.com\\E"));
        person.setUserNamePatterns(Arrays.asList("ahached"));
        peopleConfig.setPeople(Arrays.asList(person));

        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();
        config.setPeopleConfig(peopleConfig);

        // The email does NOT match any emailPattern, but the commit userName "ahached" does.
        String line = "2024-01-01 noreply-1@github.com c1 a/B.java ahached";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, config);

        assertEquals("ahmed@corp.com", fileUpdate.getAuthorEmail());
        assertEquals("Ahmed Hached", fileUpdate.getUserName());
    }

    @Test
    void parseLineLeavesUnmatchedContributorUntouchedWithPeopleConfig() {
        nl.obren.sokrates.sourcecode.landscape.PeopleConfig peopleConfig =
                new nl.obren.sokrates.sourcecode.landscape.PeopleConfig();
        nl.obren.sokrates.sourcecode.landscape.PersonConfig person =
                new nl.obren.sokrates.sourcecode.landscape.PersonConfig();
        person.setUserName("Guido van Rossum");
        person.setEmail("guido@python.org");
        person.setEmailPatterns(Arrays.asList("\\Qguido@python.org\\E"));
        peopleConfig.setPeople(Arrays.asList(person));

        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();
        config.setPeopleConfig(peopleConfig);

        // An unrelated contributor keeps their own email and userName.
        String line = "2024-01-01 alice@example.com c1 a/B.java Alice&nbsp;Example";
        FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, config);

        assertEquals("alice@example.com", fileUpdate.getAuthorEmail());
        assertEquals("Alice Example", fileUpdate.getUserName());
    }

    @Test
    void shouldIgnore() {
        List<String> ignore = Arrays.asList(new String[]{".*GITHUBBOT.*", "None", "none", "DL[-].*", "[a-zA-Z]+Releaser.*", "bot", "committed[-]by[-]bot.*"});

        assertFalse(GitHistoryUtils.shouldIgnore("", ignore));
        assertFalse(GitHistoryUtils.shouldIgnore("user@org", ignore));
        assertFalse(GitHistoryUtils.shouldIgnore("user", ignore));

        assertTrue(GitHistoryUtils.shouldIgnore("DL-Dev@org.com", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("DL-Dev", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("committed-by-bot", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("committed-by-bot@org.com", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("GITHUBBOT-test@org.com", ignore));
        assertTrue(GitHistoryUtils.shouldIgnore("dev-GITHUBBOT-test@org.com", ignore));
    }
}