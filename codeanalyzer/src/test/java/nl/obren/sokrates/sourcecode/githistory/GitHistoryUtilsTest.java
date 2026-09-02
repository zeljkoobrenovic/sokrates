package nl.obren.sokrates.sourcecode.githistory;

import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;

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

    @Test
    void getCommitTrailersFromFileGroupsBySha() throws Exception {
        File file = Files.createTempFile("git-commit-trailers", ".txt").toFile();
        file.deleteOnExit();
        Files.write(file.toPath(), Arrays.asList(
                "aaa Co-authored-by: Claude <noreply@anthropic.com>",
                "aaa Committer: GitHub <noreply@github.com>",
                "bbb Signed-off-by: Jane <jane@acme.com>",
                "malformed line without a trailer",
                "ccc"), StandardCharsets.UTF_8);

        Map<String, List<CommitTrailer>> trailers = GitHistoryUtils.getCommitTrailersFromFile(file);

        assertEquals(2, trailers.size());
        assertEquals(2, trailers.get("aaa").size());
        assertEquals("Co-authored-by", trailers.get("aaa").get(0).getKey());
        assertEquals("noreply@anthropic.com", trailers.get("aaa").get(0).getEmail());
        assertEquals(GitHistoryUtils.COMMITTER_TRAILER_KEY, trailers.get("aaa").get(1).getKey());
        assertEquals("jane@acme.com", trailers.get("bbb").get(0).getEmail());
    }

    @Test
    void getCommitTrailersFromMissingFileIsEmpty() {
        assertTrue(GitHistoryUtils.getCommitTrailersFromFile(new File("does-not-exist.txt")).isEmpty());
        assertTrue(GitHistoryUtils.getCommitTrailersFromFile(null).isEmpty());
    }

    private File historyFolderWithTrailers(String... trailerLines) throws Exception {
        File folder = Files.createTempDirectory("git-history").toFile();
        Files.write(new File(folder, GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME).toPath(),
                Arrays.asList(trailerLines), StandardCharsets.UTF_8);
        return new File(folder, GitHistoryUtils.GIT_HISTORY_FILE_NAME);
    }

    @Test
    void getCoAuthorsByShaClassifiesAgentsAndPeople() throws Exception {
        File historyFile = historyFolderWithTrailers(
                "aaa Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>",
                "aaa Co-authored-by: Jane Doe <Jane@Acme.com>",
                "aaa Co-authored-by: Jane Doe <jane@acme.com>",
                "aaa Committer: GitHub <noreply@github.com>",
                "aaa Signed-off-by: Bob <bob@acme.com>",
                "bbb Generated-by: GitHub Copilot",
                "bbb Co-authored-by: renovate[bot] <renovate[bot]@users.noreply.github.com>",
                "ccc Signed-off-by: Bob <bob@acme.com>");
        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();

        Map<String, List<CoAuthor>> coAuthors = GitHistoryUtils.getCoAuthorsBySha(historyFile, config);

        assertEquals(2, coAuthors.size());
        assertNull(coAuthors.get("ccc"));
        List<CoAuthor> aaa = coAuthors.get("aaa");
        assertEquals(2, aaa.size());
        assertEquals("Claude Code", aaa.get(0).getAgent());
        assertTrue(aaa.get(0).isAi());
        assertEquals("Claude Fable 5", aaa.get(0).getName());
        assertEquals("jane@acme.com", aaa.get(1).getEmail());
        assertEquals("Jane Doe", aaa.get(1).getName());
        assertFalse(aaa.get(1).isAi());
        List<CoAuthor> bbb = coAuthors.get("bbb");
        assertEquals(2, bbb.size());
        assertEquals("GitHub Copilot", bbb.get(0).getAgent());
        assertEquals("", bbb.get(0).getEmail());
        assertEquals(CoAuthor.BOT_AGENT, bbb.get(1).getAgent());
    }

    @Test
    void getCoAuthorsByShaNormalizesPeopleLikeAuthors() throws Exception {
        File historyFile = historyFolderWithTrailers(
                "aaa Co-authored-by: Jane Doe <jane@acme.com>",
                "aaa Co-authored-by: Ignored <ignored@acme.com>",
                "aaa Co-authored-by: Johnny <john.old@acme.com>");
        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();
        config.setIgnoreContributors(Arrays.asList("ignored@.*"));
        config.setTransformContributorEmails(Arrays.asList(new OperationStatement("replace", Arrays.asList("[@].*", ""))));
        PeopleConfig peopleConfig = new PeopleConfig();
        PersonConfig john = new PersonConfig();
        john.setEmail("john");
        john.setUserName("John Smith");
        john.setEmailPatterns(Arrays.asList("john[.]old"));
        peopleConfig.getPeople().add(john);
        config.setPeopleConfig(peopleConfig);

        List<CoAuthor> aaa = GitHistoryUtils.getCoAuthorsBySha(historyFile, config).get("aaa");

        assertEquals(2, aaa.size());
        assertEquals("jane", aaa.get(0).getEmail());
        assertEquals("john", aaa.get(1).getEmail());
        assertEquals("John Smith", aaa.get(1).getName());
    }

    @Test
    void getCoAuthorsByShaWithoutSidecarIsEmpty() throws Exception {
        File folder = Files.createTempDirectory("git-history-empty").toFile();
        assertTrue(GitHistoryUtils.getCoAuthorsBySha(new File(folder, "git-history.txt"), new FileHistoryAnalysisConfig()).isEmpty());
        assertTrue(GitHistoryUtils.getCoAuthorsBySha(null, new FileHistoryAnalysisConfig()).isEmpty());
    }

    @Test
    void messageSignaturesResolveToAgentsOnlyAndDedupeWithTrailers() throws Exception {
        File historyFile = historyFolderWithTrailers(
                "aaa Message-Signature: 🤖 Generated with [Claude Code](https://claude.ai/code)",
                "aaa Co-Authored-By: Claude <noreply@anthropic.com>",
                "bbb Message-Signature: 🤖 Generated with [Claude Code](https://claude.com/claude-code)",
                "ccc Message-Signature: Made with SomeUnknownTool");

        Map<String, List<CoAuthor>> coAuthors = GitHistoryUtils.getCoAuthorsBySha(historyFile, new FileHistoryAnalysisConfig());

        assertEquals(1, coAuthors.get("aaa").size());
        assertEquals("Claude Code", coAuthors.get("aaa").get(0).getAgent());
        assertEquals(1, coAuthors.get("bbb").size());
        assertEquals("Claude Code", coAuthors.get("bbb").get(0).getAgent());
        // An unrecognised signature never becomes a (nameless) person co-author.
        assertNull(coAuthors.get("ccc"));
    }

    @Test
    void coAuthorAnalysisCanBeDisabledInConfig() throws Exception {
        File historyFile = historyFolderWithTrailers("aaa Co-Authored-By: Claude <noreply@anthropic.com>");
        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();
        assertTrue(config.getCoAuthors().isEnabled());
        assertEquals(1, GitHistoryUtils.getCoAuthorsBySha(historyFile, config).size());

        config.getCoAuthors().setEnabled(false);
        assertTrue(GitHistoryUtils.getCoAuthorsBySha(historyFile, config).isEmpty());
    }
}
