package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.sourcecode.githistory.FileUpdate;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCostEstimatorGeneratorTest {

    private static String sha(char c) {
        return String.valueOf(c).repeat(40);
    }

    private static FileUpdate update(String date, String email, String sha, String path, int added, int deleted) {
        return update(date, email, sha, path, added, deleted, false);
    }

    private static FileUpdate update(String date, String email, String sha, String path, int added, int deleted, boolean bot) {
        FileUpdate fileUpdate = new FileUpdate(date, email, email, sha, path, bot);
        fileUpdate.setLinesAdded(added);
        fileUpdate.setLinesDeleted(deleted);
        return fileUpdate;
    }

    private static List<CommitFileExport> currentFiles() {
        return new ArrayList<>(Arrays.asList(
                new CommitFileExport("src/Big.java", "main", 5000),
                new CommitFileExport("src/Small.java", "main", 100),
                new CommitFileExport("src/New.java", "main", 600),
                new CommitFileExport("src/Other.java", "main", 300),
                new CommitFileExport("gen/Api.java", "generated", 900)));
    }

    @Test
    void groupsConsecutiveOverlappingCommitsOfOneAuthorIntoATask() {
        // git-history.txt order: newest first.
        List<FileUpdate> updates = Arrays.asList(
                update("2024-03-20", "a@x.com", sha('d'), "src/Big.java", 5, 5),        // > 1 day later: new task
                update("2024-03-02", "a@x.com", sha('c'), "src/Other.java", 4, 0),      // next day, no overlap: new task
                update("2024-03-02", "b@x.com", sha('b'), "src/Small.java", 1, 1),      // other author: own task
                update("2024-03-01", "a@x.com", sha('a'), "src/Small.java", 8, 2),
                update("2024-03-01", "a@x.com", sha('a'), "src/Big.java", 10, 0),
                update("2024-02-29", "a@x.com", sha('0'), "src/Big.java", 2, 1));      // day before, overlaps: same task as a

        AiCostEstimatorData data = AiCostEstimatorGenerator.buildData(currentFiles(), updates, Collections.emptyMap(), path -> true, 1000);

        assertEquals(4, data.getTasks().size());
        AiCostEstimatorData.Task first = data.getTasks().get(0);
        assertEquals(2, first.getCommits().size());
        assertEquals("2024-02-29", first.getStart());
        assertEquals("2024-03-01", first.getEnd());
        assertEquals("0000000000", first.getCommits().get(0).getSha());
        // Big.java: 12 added, 1 deleted; Small.java: first appearance without deletions is not new (2 deleted).
        assertEquals(2, first.getFiles());
        assertEquals(20, first.getEditAdded());
        assertEquals(3, first.getEditDeleted());
        // One session; the big file's read is capped at 2,000 lines.
        assertEquals(1, first.getSessions().size());
        assertArrayEquals(new int[]{2, 2000 + 100, 20, 3, 0}, first.getSessions().get(0));
        assertEquals(AiCostEstimatorGenerator.TYPE_FIX, first.getType());
        assertEquals(2, data.getAuthors().size());
    }

    @Test
    void newFilesAreWrittenNotReadAndDeletedFilesCostNothing() {
        List<FileUpdate> updates = Arrays.asList(
                update("2024-01-02", "a@x.com", sha('b'), "src/Gone.java", 0, 80),
                update("2024-01-01", "a@x.com", sha('a'), "src/New.java", 600, 0),
                update("2023-12-01", "a@x.com", sha('0'), "src/Gone.java", 80, 0));

        AiCostEstimatorData data = AiCostEstimatorGenerator.buildData(currentFiles(), updates, Collections.emptyMap(), path -> true, 1000);

        AiCostEstimatorData.Task task = data.getTasks().get(1);
        assertEquals(600, task.getNewLines());
        assertEquals(AiCostEstimatorGenerator.TYPE_NEW, task.getType());
        // 600 lines in a new file: two sessions of at most 400 lines, nothing read.
        assertEquals(2, task.getSessions().size());
        assertArrayEquals(new int[]{1, 0, 0, 0, 300}, task.getSessions().get(0));
        // Deleting Gone.java (a day later, no overlap with New.java) costs nothing: no task for it.
        assertEquals(2, data.getTasks().size());
        assertEquals("2023-12-01", data.getTasks().get(0).getStart());
    }

    @Test
    void dropsNoise() {
        List<FileUpdate> updates = new ArrayList<>();
        updates.add(update("2024-01-05", "bot@x.com", sha('e'), "src/Small.java", 3, 3, true));
        updates.add(update("2024-01-04", "a@x.com", sha('d'), "src/Small.java", 3500, 0));
        updates.add(update("2024-01-03", "a@x.com", sha('c'), "package-lock.json", 900, 900));
        updates.add(update("2024-01-03", "a@x.com", sha('c'), "vendor/lib/x.go", 10, 0));
        updates.add(update("2024-01-03", "a@x.com", sha('c'), "gen/Api.java", 10, 0));
        updates.add(update("2024-01-03", "a@x.com", sha('c'), "src/Other.java", 5, 5));
        for (int i = 0; i < 301; i++) {
            updates.add(update("2024-01-02", "a@x.com", sha('b'), "src/f" + i + ".java", 1, 0));
        }

        AiCostEstimatorData data = AiCostEstimatorGenerator.buildData(currentFiles(), updates, Collections.emptyMap(), path -> true, 1000);

        AiCostEstimatorData.Noise noise = data.getNoise();
        assertEquals(1, noise.getBotCommits());
        assertEquals(1, noise.getMassCommits());
        assertEquals(1, noise.getLockFileChanges());
        assertEquals(1, noise.getVendoredChanges());
        assertEquals(1, noise.getGeneratedChanges());
        assertEquals(1, noise.getOversizedChanges());
        assertEquals(1, noise.getEmptiedCommits());
        assertEquals(1, data.getTasks().size());
        assertEquals(10, data.getTasks().get(0).churn());
        assertEquals(4, data.getTotalCommitsCount());
        assertEquals(1, data.getKeptCommitsCount());
    }

    @Test
    void groupsCommitsByTicketAcrossAuthorsAndDays() {
        List<FileUpdate> updates = new ArrayList<>();
        Map<String, String> messages = new HashMap<>();
        char c = 'a';
        for (int i = 1; i <= 5; i++, c++) {
            updates.add(0, update("2024-02-0" + i, "a@x.com", sha(c), "src/Other.java", 1, 1));
            messages.put(sha(c), "PROJ-" + (100 + i) + " step; UTF-8 fix");
        }
        // A second author on PROJ-101 a week later: same task.
        updates.add(0, update("2024-02-09", "b@x.com", sha('z'), "src/Small.java", 2, 0));
        messages.put(sha('z'), "Follow-up for PROJ-101");

        AiCostEstimatorData data = AiCostEstimatorGenerator.buildData(currentFiles(), updates, messages, path -> true, 1000);

        assertEquals(Collections.singletonList("PROJ"), data.getTicketPrefixes());
        assertEquals(5, data.getTasks().size());
        AiCostEstimatorData.Task first = data.getTasks().get(0);
        assertEquals("PROJ-101", first.getTicket());
        assertEquals(2, first.getCommits().size());
        assertEquals(1, first.getCommits().get(1).getAuthor());
        assertTrue(data.getTasks().stream().allMatch(task -> task.getTicket().startsWith("PROJ-")));
    }

    @Test
    void largeTasksAreSplitIntoSessionsOfTenFiles() {
        List<FileUpdate> updates = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            updates.add(update("2024-01-01", "a@x.com", sha('a'), "src/Other.java".replace("Other", "F" + i), 4, 2));
        }

        AiCostEstimatorData data = AiCostEstimatorGenerator.buildData(currentFiles(), updates, Collections.emptyMap(), path -> true, 1000);

        AiCostEstimatorData.Task task = data.getTasks().get(0);
        assertEquals(3, task.getSessions().size());
        assertEquals(10, task.getSessions().get(0)[0]);
        assertEquals(5, task.getSessions().get(2)[0]);
        assertEquals(AiCostEstimatorGenerator.TYPE_FEATURE, task.getType());
    }

    @Test
    void onlyTheAnalyzedScopesCount() {
        List<FileUpdate> updates = Arrays.asList(
                update("2024-01-01", "a@x.com", sha('a'), "README.md", 50, 10),
                update("2024-01-01", "a@x.com", sha('a'), "src/Removed.java", 0, 40),
                update("2024-01-01", "a@x.com", sha('a'), "src/Small.java", 5, 5),
                update("2023-01-01", "a@x.com", sha('0'), "src/Removed.java", 40, 0));
        // README.md is still on disk but in no scope; src/Removed.java is gone (an analyzed extension).
        AiCostEstimatorData data = AiCostEstimatorGenerator.buildData(currentFiles(), updates, Collections.emptyMap(),
                path -> path.endsWith(".java"), 1000);

        assertEquals(1, data.getNoise().getUnscopedChanges());
        AiCostEstimatorData.Task task = data.getTasks().get(1);
        assertEquals(1, task.getFiles());
        assertEquals(1, task.getDeletedFiles());
        assertEquals(10, task.churn());
        assertEquals("md", AiCostEstimatorGenerator.extensionOf("docs/README.MD"));
        assertEquals("", AiCostEstimatorGenerator.extensionOf("Makefile"));
    }
}
