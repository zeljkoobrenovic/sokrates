package nl.obren.sokrates.sourcecode.contributors;

import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.githistory.AuthorCommit;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GitContributorsUtilTest {

    // Clear the static history cache in GitHistoryUtils so each test reads its own file.
    private void resetHistoryCache() throws Exception {
        Field updates = GitHistoryUtils.class.getDeclaredField("updates");
        updates.setAccessible(true);
        updates.set(null, null);
    }

    private File writeHistory(String content) throws Exception {
        File file = File.createTempFile("git-history", ".txt");
        file.deleteOnExit();
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private ContributionTimeSlot slot(List<ContributionTimeSlot> slots, String timeSlot) {
        return slots.stream().filter(s -> s.getTimeSlot().equals(timeSlot)).findFirst().orElseThrow();
    }

    @Test
    void perScopeTimeSlotsCountOnlyThatScopeActivity() throws Exception {
        resetHistoryCache();

        // c1 touches a main file (+10/-2) AND a test file (+5/-1); c2 touches only a test file.
        // git-history columns: date email commit path name added deleted
        String history =
                "2020-01-10 alice@org.com c1 src/Main.java alice 10 2\n" +
                "2020-01-10 alice@org.com c1 test/MainTest.java alice 5 1\n" +
                "2020-01-20 bob@org.com c2 test/Other.java bob 7 3\n";

        File file = writeHistory(history);

        // Lowercased relative paths per scope, as ContributorsAnalyzer builds them.
        Map<String, Set<String>> pathsByScope = new LinkedHashMap<>();
        pathsByScope.put("main", new HashSet<>(List.of("src/main.java")));
        pathsByScope.put("test", new HashSet<>(List.of("test/maintest.java", "test/other.java")));

        ContributorsImport result = GitContributorsUtil.importGitContributorsExport(
                file, new FileHistoryAnalysisConfig(), pathsByScope);

        // All scope: both commits (c1, c2), 3 file updates, churn summed across every file.
        ContributionTimeSlot allYear = slot(result.getContributorsPerYear(), "2020");
        assertEquals(2, allYear.getCommitsCount());
        assertEquals(2, allYear.getContributorsCount());
        assertEquals(3, allYear.getFileUpdatesCount());
        assertEquals(22, allYear.getLinesAdded());   // 10 + 5 + 7
        assertEquals(6, allYear.getLinesDeleted());   // 2 + 1 + 3

        // Main scope: only c1 has a main file; only that file's churn counts. c2 drops out entirely.
        ContributionTimeSlot mainYear = slot(result.getContributorsPerYearByScope().get("main"), "2020");
        assertEquals(1, mainYear.getCommitsCount());
        assertEquals(1, mainYear.getContributorsCount());
        assertEquals(1, mainYear.getFileUpdatesCount());
        assertEquals(10, mainYear.getLinesAdded());
        assertEquals(2, mainYear.getLinesDeleted());

        // Test scope: c1's test file (+5/-1) and c2's test file (+7/-3) -> both commits, 2 file updates.
        ContributionTimeSlot testYear = slot(result.getContributorsPerYearByScope().get("test"), "2020");
        assertEquals(2, testYear.getCommitsCount());
        assertEquals(2, testYear.getContributorsCount());
        assertEquals(2, testYear.getFileUpdatesCount());
        assertEquals(12, testYear.getLinesAdded());   // 5 + 7
        assertEquals(4, testYear.getLinesDeleted());   // 1 + 3

        // Every file here is in some scope, so the residual "unscoped" tab has no activity.
        List<ContributionTimeSlot> unscoped = result.getContributorsPerYearByScope()
                .get(GitContributorsUtil.UNSCOPED);
        assertTrue(unscoped == null || unscoped.isEmpty());
    }

    @Test
    void unscopedResidualCapturesChurnOfFilesInNoScope() throws Exception {
        resetHistoryCache();

        // c1 touches a main file (+10/-2) and a now-deleted file (+99/-1) not in any scope;
        // c2 touches only a deleted/ignored file (+7/-3). The deleted-file churn is in "All" but in
        // no scope tab -> it must show up in the "unscoped" residual so the scopes sum to All.
        String history =
                "2020-01-10 alice@org.com c1 src/Main.java alice 10 2\n" +
                "2020-01-10 alice@org.com c1 deleted/Gone.java alice 99 1\n" +
                "2020-01-20 bob@org.com c2 vendor/Ignored.java bob 7 3\n";

        File file = writeHistory(history);

        Map<String, Set<String>> pathsByScope = new LinkedHashMap<>();
        pathsByScope.put("main", new HashSet<>(List.of("src/main.java")));

        ContributorsImport result = GitContributorsUtil.importGitContributorsExport(
                file, new FileHistoryAnalysisConfig(), pathsByScope);

        // All: both commits, 3 file updates, all churn.
        ContributionTimeSlot allYear = slot(result.getContributorsPerYear(), "2020");
        assertEquals(2, allYear.getCommitsCount());
        assertEquals(3, allYear.getFileUpdatesCount());
        assertEquals(116, allYear.getLinesAdded());   // 10 + 99 + 7
        assertEquals(6, allYear.getLinesDeleted());    // 2 + 1 + 3

        // Main: only the in-scope file of c1.
        ContributionTimeSlot mainYear = slot(result.getContributorsPerYearByScope().get("main"), "2020");
        assertEquals(1, mainYear.getCommitsCount());
        assertEquals(1, mainYear.getFileUpdatesCount());
        assertEquals(10, mainYear.getLinesAdded());
        assertEquals(2, mainYear.getLinesDeleted());

        // Unscoped: the deleted file from c1 (+99/-1) and the ignored file from c2 (+7/-3). Both
        // commits touch an unscoped file, so 2 commits, 2 file updates.
        ContributionTimeSlot unscopedYear = slot(
                result.getContributorsPerYearByScope().get(GitContributorsUtil.UNSCOPED), "2020");
        assertEquals(2, unscopedYear.getCommitsCount());
        assertEquals(2, unscopedYear.getFileUpdatesCount());
        assertEquals(106, unscopedYear.getLinesAdded());   // 99 + 7
        assertEquals(4, unscopedYear.getLinesDeleted());    // 1 + 3

        // Partition is exact: main + unscoped churn == all churn (the whole point of the residual tab).
        assertEquals(allYear.getLinesAdded(),
                mainYear.getLinesAdded() + unscopedYear.getLinesAdded());
        assertEquals(allYear.getLinesDeleted(),
                mainYear.getLinesDeleted() + unscopedYear.getLinesDeleted());
        assertEquals(allYear.getFileUpdatesCount(),
                mainYear.getFileUpdatesCount() + unscopedYear.getFileUpdatesCount());
    }

    @Test
    void perScopeCommitDatesAreRecordedOnContributors() throws Exception {
        resetHistoryCache();

        // alice: 01-10 touches main+test, 01-12 touches only test; bob: 01-20 touches only a deleted
        // (unscoped) file. So alice's main dates = {01-10}, test dates = {01-10, 01-12}; bob's unscoped
        // dates = {01-20} and he has no main/test dates.
        String history =
                "2020-01-10 alice@org.com c1 src/Main.java alice 10 2\n" +
                "2020-01-10 alice@org.com c1 test/MainTest.java alice 5 1\n" +
                "2020-01-12 alice@org.com c2 test/Other.java alice 4 0\n" +
                "2020-01-20 bob@org.com c3 deleted/Gone.java bob 7 3\n";

        File file = writeHistory(history);

        Map<String, Set<String>> pathsByScope = new LinkedHashMap<>();
        pathsByScope.put("main", new HashSet<>(List.of("src/main.java")));
        pathsByScope.put("test", new HashSet<>(List.of("test/maintest.java", "test/other.java")));

        ContributorsImport result = GitContributorsUtil.importGitContributorsExport(
                file, new FileHistoryAnalysisConfig(), pathsByScope);

        Contributor alice = result.getContributors().stream()
                .filter(c -> c.getEmail().equals("alice@org.com")).findFirst().orElseThrow();
        Contributor bob = result.getContributors().stream()
                .filter(c -> c.getEmail().equals("bob@org.com")).findFirst().orElseThrow();

        // alice: main on 01-10 only; test on 01-10 and 01-12; nothing unscoped.
        assertEquals(List.of("2020-01-10"), alice.getCommitDatesByScope().get("main"));
        assertEquals(new HashSet<>(List.of("2020-01-10", "2020-01-12")),
                new HashSet<>(alice.getCommitDatesByScope().get("test")));
        assertTrue(alice.getCommitDatesByScope().getOrDefault(GitContributorsUtil.UNSCOPED, List.of()).isEmpty());

        // bob: only the unscoped (deleted) file; no main/test dates.
        assertEquals(List.of("2020-01-20"), bob.getCommitDatesByScope().get(GitContributorsUtil.UNSCOPED));
        assertTrue(bob.getCommitDatesByScope().getOrDefault("main", List.of()).isEmpty());
        assertTrue(bob.getCommitDatesByScope().getOrDefault("test", List.of()).isEmpty());
    }

    @Test
    void nullScopeMapLeavesPerScopeTimeSlotsEmpty() throws Exception {
        resetHistoryCache();

        String history = "2020-01-10 alice@org.com c1 src/Main.java alice 10 2\n";
        File file = writeHistory(history);

        // null pathsByScope -> the all-scope behaviour, no per-scope lists computed.
        ContributorsImport result = GitContributorsUtil.importGitContributorsExport(
                file, new FileHistoryAnalysisConfig(), null);

        assertFalse(result.getContributorsPerYear().isEmpty());
        assertTrue(result.getContributorsPerYearByScope().isEmpty());
    }

    @Test
    void aiCoAuthoredCommitsAreCountedPerTimeSlotFromTheTrailersSidecar() throws Exception {
        resetHistoryCache();

        // Own folder so the sidecar sits next to the history file.
        File folder = Files.createTempDirectory("git-history-coauthors").toFile();
        File file = new File(folder, GitHistoryUtils.GIT_HISTORY_FILE_NAME);
        FileUtils.writeStringToFile(file,
                "2020-01-10 alice@org.com c1 src/Main.java alice 10 2\n" +
                "2020-01-10 alice@org.com c1 test/MainTest.java alice 5 1\n" +
                "2020-01-20 bob@org.com c2 src/Other.java bob 7 3\n" +
                "2021-03-01 bob@org.com c3 src/Third.java bob 1 1\n", StandardCharsets.UTF_8);
        Files.write(new File(folder, GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME).toPath(), Arrays.asList(
                "c1 Co-Authored-By: Claude <noreply@anthropic.com>",
                "c1 Co-authored-by: Carol <carol@org.com>",
                "c2 Co-authored-by: Carol <carol@org.com>",
                "c3 Signed-off-by: Bob <bob@org.com>"), StandardCharsets.UTF_8);

        List<AuthorCommit> commits = GitHistoryUtils.getAuthorCommits(file, new FileHistoryAnalysisConfig());

        assertEquals(3, commits.size());
        AuthorCommit c1 = commits.get(0);
        assertEquals(2, c1.getCoAuthors().size());
        assertTrue(c1.hasAiCoAuthor());
        assertEquals(List.of("Claude Code"), c1.getAiAgents());
        assertFalse(commits.get(1).hasAiCoAuthor());
        assertEquals("carol@org.com", commits.get(1).getCoAuthors().get(0).getEmail());
        assertTrue(commits.get(2).getCoAuthors().isEmpty());

        List<ContributionTimeSlot> perYear = GitContributorsUtil.getContributorsPerTimeSlot(commits, AuthorCommit::getYear);
        assertEquals(2, slot(perYear, "2020").getCommitsCount());
        assertEquals(1, slot(perYear, "2020").getAiCoAuthoredCommitsCount());
        assertEquals(0, slot(perYear, "2021").getAiCoAuthoredCommitsCount());
        // Contributors count stays author-based (co-authors are not counted as slot contributors).
        assertEquals(2, slot(perYear, "2020").getContributorsCount());
    }
}
