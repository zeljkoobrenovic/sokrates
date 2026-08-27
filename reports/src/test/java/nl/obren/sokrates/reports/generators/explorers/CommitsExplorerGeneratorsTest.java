package nl.obren.sokrates.reports.generators.explorers;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.sourcecode.githistory.CoAuthor;
import nl.obren.sokrates.sourcecode.githistory.FileUpdate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommitsExplorerGeneratorsTest {

    private static final String SHA_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String SHA_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String SHA_C = "cccccccccccccccccccccccccccccccccccccccc";

    private FileUpdate update(String date, String email, String sha, String path, int added, int deleted) {
        FileUpdate fileUpdate = new FileUpdate(date, email, email, sha, path, false);
        fileUpdate.setLinesAdded(added);
        fileUpdate.setLinesDeleted(deleted);
        return fileUpdate;
    }

    @Test
    void groupsFileUpdatesIntoCommitsNewestFirst() {
        List<CommitFileExport> currentFiles = new ArrayList<>(Arrays.asList(
                new CommitFileExport("src/Foo.java", "main", 100),
                new CommitFileExport("src/Bar.java", "main", 50)));
        // git-history.txt order: newest first; commit A touches both files, commit B one.
        List<FileUpdate> updates = Arrays.asList(
                update("2024-05-01", "a@x.com", SHA_A, "src/Foo.java", 10, 2),
                update("2024-05-01", "a@x.com", SHA_A, "src/Bar.java", 3, 1),
                update("2024-01-15", "b@x.com", SHA_B, "src/Foo.java", 7, 0));

        CommitsExplorerData data = CommitsExplorerGenerators.buildData(currentFiles, updates, Collections.emptyMap(), 10000);

        assertEquals(2, data.getCommits().size());
        assertEquals(2, data.getTotalCommitsCount());

        CommitExport newest = data.getCommits().get(0);
        assertEquals("aaaaaaaaaa", newest.getSha());
        assertEquals("2024-05-01", newest.getDate());
        assertEquals("a@x.com", newest.getEmail());
        assertEquals(13, newest.getLinesAdded());
        assertEquals(3, newest.getLinesDeleted());
        assertEquals(Arrays.asList(0, 1), newest.getFileIds());

        CommitExport oldest = data.getCommits().get(1);
        assertEquals("2024-01-15", oldest.getDate());
        assertEquals(Arrays.asList(0), oldest.getFileIds());

        // No history-only paths: the file list is exactly the current files.
        assertEquals(2, data.getFiles().size());
    }

    @Test
    void pathsNotInCurrentFilesBecomeDeletedEntries() {
        List<CommitFileExport> currentFiles = new ArrayList<>(Arrays.asList(
                new CommitFileExport("src/Foo.java", "main", 100)));
        List<FileUpdate> updates = Arrays.asList(
                update("2024-05-01", "a@x.com", SHA_A, "src/Removed.java", 0, 20),
                update("2024-05-01", "a@x.com", SHA_A, "src/Foo.java", 1, 1));

        CommitsExplorerData data = CommitsExplorerGenerators.buildData(currentFiles, updates, Collections.emptyMap(), 10000);

        assertEquals(2, data.getFiles().size());
        CommitFileExport deleted = data.getFiles().get(1);
        assertEquals("src/Removed.java", deleted.getPath());
        assertEquals(CommitFileExport.SCOPE_DELETED, deleted.getScope());
        // Size proxy: the largest lines-added/deleted seen for the path (20 deleted here).
        assertEquals(20, deleted.getLinesOfCode());
        assertEquals(Arrays.asList(1, 0), data.getCommits().get(0).getFileIds());
    }

    @Test
    void pathMatchIsCaseInsensitive() {
        List<CommitFileExport> currentFiles = new ArrayList<>(Arrays.asList(
                new CommitFileExport("src/Foo.java", "main", 100)));
        List<FileUpdate> updates = Arrays.asList(
                update("2024-05-01", "a@x.com", SHA_A, "SRC/FOO.JAVA", 1, 1));

        CommitsExplorerData data = CommitsExplorerGenerators.buildData(currentFiles, updates, Collections.emptyMap(), 10000);

        // Matched the existing file (mirrors FileHistoryAnalyzer's lower-cased path lookup);
        // no "deleted" entry added.
        assertEquals(1, data.getFiles().size());
        assertEquals(Arrays.asList(0), data.getCommits().get(0).getFileIds());
    }

    @Test
    void capsAtMaxCommitsKeepingTheNewestAndSkipsUnreferencedDeletedFiles() {
        List<CommitFileExport> currentFiles = new ArrayList<>();
        // Three commits; the oldest one is the only one touching a history-only path.
        List<FileUpdate> updates = Arrays.asList(
                update("2024-03-01", "a@x.com", SHA_A, "kept1.java", 1, 0),
                update("2024-02-01", "b@x.com", SHA_B, "kept2.java", 1, 0),
                update("2024-01-01", "c@x.com", SHA_C, "dropped.java", 1, 0));

        CommitsExplorerData data = CommitsExplorerGenerators.buildData(currentFiles, updates, Collections.emptyMap(), 2);

        assertEquals(3, data.getTotalCommitsCount());
        assertEquals(2, data.getCommits().size());
        assertEquals("2024-03-01", data.getCommits().get(0).getDate());
        assertEquals("2024-02-01", data.getCommits().get(1).getDate());
        // Only paths referenced by KEPT commits are exported.
        assertEquals(2, data.getFiles().size());
        assertTrue(data.getFiles().stream().noneMatch(f -> f.getPath().equals("dropped.java")));
    }

    @Test
    void sameDateCommitsKeepGitLogOrder() {
        // Two commits on the same date; git-history.txt lists the newer one first, and the
        // stable date sort must not swap them.
        List<FileUpdate> updates = Arrays.asList(
                update("2024-05-01", "a@x.com", SHA_A, "a.java", 1, 0),
                update("2024-05-01", "b@x.com", SHA_B, "b.java", 1, 0));

        CommitsExplorerData data = CommitsExplorerGenerators.buildData(new ArrayList<>(), updates, Collections.emptyMap(), 10000);

        assertEquals("aaaaaaaaaa", data.getCommits().get(0).getSha());
        assertEquals("bbbbbbbbbb", data.getCommits().get(1).getSha());
    }

    @Test
    void duplicatePathLinesWithinACommitAreDeduplicated() {
        List<FileUpdate> updates = Arrays.asList(
                update("2024-05-01", "a@x.com", SHA_A, "a.java", 1, 0),
                update("2024-05-01", "a@x.com", SHA_A, "a.java", 2, 0));

        CommitsExplorerData data = CommitsExplorerGenerators.buildData(new ArrayList<>(), updates, Collections.emptyMap(), 10000);

        assertEquals(1, data.getCommits().size());
        assertEquals(Arrays.asList(0), data.getCommits().get(0).getFileIds());
        // Churn still sums over all lines.
        assertEquals(3, data.getCommits().get(0).getLinesAdded());
    }

    @Test
    void attachesFirstMessageLineFromSidecarMapWhenPresent() {
        List<FileUpdate> updates = Arrays.asList(
                update("2024-05-01", "a@x.com", SHA_A, "a.java", 1, 0),
                update("2024-04-01", "b@x.com", SHA_B, "b.java", 1, 0));
        Map<String, String> messages = new HashMap<>();
        messages.put(SHA_A, "fix: collapse whitespace variants");
        // SHA_B has no entry (e.g. history predates the sidecar or partial file).

        CommitsExplorerData data = CommitsExplorerGenerators.buildData(new ArrayList<>(), updates, messages, 10000);

        assertEquals("fix: collapse whitespace variants", data.getCommits().get(0).getMessage());
        assertEquals("", data.getCommits().get(1).getMessage());
    }

    @Test
    void carriesCoAuthorsOntoTheirCommitOnly() throws Exception {
        List<CommitFileExport> currentFiles = new ArrayList<>(Arrays.asList(new CommitFileExport("src/Foo.java", "main", 100)));
        List<FileUpdate> updates = Arrays.asList(
                update("2024-05-01", "a@x.com", SHA_A, "src/Foo.java", 10, 2),
                update("2024-01-15", "b@x.com", SHA_B, "src/Foo.java", 7, 0));
        Map<String, List<CoAuthor>> coAuthorsBySha = new HashMap<>();
        coAuthorsBySha.put(SHA_A, Arrays.asList(
                new CoAuthor("Claude", "noreply@anthropic.com", "Claude Code"),
                new CoAuthor("Carol", "carol@x.com", null)));

        CommitsExplorerData data = CommitsExplorerGenerators.buildData(currentFiles, updates, Collections.emptyMap(), coAuthorsBySha, 10000);

        CommitExport a = data.getCommits().get(0);
        assertEquals("aaaaaaaaaa", a.getSha());
        assertEquals(2, a.getCoAuthors().size());
        assertEquals("Claude Code", a.getCoAuthors().get(0).getAgent());
        assertEquals("carol@x.com", a.getCoAuthors().get(1).getEmail());
        assertNull(a.getCoAuthors().get(1).getAgent());
        assertTrue(data.getCommits().get(1).getCoAuthors().isEmpty());

        // Empty co-author lists and null agents are omitted from the embedded JSON.
        String json = new ObjectMapper().writeValueAsString(data.getCommits());
        assertEquals(1, json.split("\"coAuthors\"").length - 1);
        assertEquals(1, json.split("\"agent\"").length - 1);
    }
}
