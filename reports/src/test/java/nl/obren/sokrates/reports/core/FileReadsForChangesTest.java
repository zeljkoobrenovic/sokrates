package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.core.FileReadsForChangesConfig;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FileReadsForChangesTest {
    private static final String RECENT = "2026-09-01";
    private static final String OLD = "2024-01-01";

    @BeforeEach
    void fixTheAnalysisDate() {
        DateUtils.reset();
        DateUtils.setDateParam("2026-10-01");
    }

    @AfterEach
    void resetTheAnalysisDate() {
        DateUtils.reset();
        DateUtils.setDateParam(null);
    }

    /** A main file with the given churn (lines added, whole history) and commits, each "id" (recent) or "id@date". */
    private static SourceFile file(String path, int loc, int churn, String... commits) {
        SourceFile file = new SourceFile();
        file.setRelativePath(path);
        file.setLinesOfCode(loc);
        if (commits.length > 0) {
            FileModificationHistory history = new FileModificationHistory(path);
            List<CommitInfo> infos = new ArrayList<>();
            for (String commit : commits) {
                String[] parts = commit.split("@");
                String date = parts.length > 1 ? parts[1] : RECENT;
                infos.add(new CommitInfo(parts[0], date));
                history.addDateIfAbsent(date);
            }
            history.setCommits(infos);
            history.setLinesAdded(churn);
            file.setFileModificationHistory(history);
        }
        return file;
    }

    private static List<String> paths(FileReadsForChanges reads) {
        return reads.topFiles().stream().map(f -> f.getFile().getRelativePath()).collect(Collectors.toList());
    }

    @Test
    void ranksTheChangedFilesByLinesTimesChangesInTheWindow() {
        FileReadsForChanges reads = new FileReadsForChanges(Arrays.asList(
                file("Small.java", 100, 0, "c1", "c2", "c3"),           // 300
                file("Big.java", 3000, 0, "c1"),                         // 3,000
                file("Busy.java", 500, 0, "c1", "c2", "c3", "c4", "c5", "c6", "c7"), // 3,500
                file("Old.java", 9000, 0, "o1@" + OLD),                  // not changed in the window
                file("Untouched.java", 50, 0)), new FileReadsForChangesConfig());

        assertEquals(Arrays.asList("Busy.java", "Big.java", "Small.java"), paths(reads));
        assertEquals(3500, reads.topFiles().get(0).getReadLines());
        assertEquals(7, reads.topFiles().get(0).getChanges());
    }

    @Test
    void linesReadPerLineChangedUseTheChurnPerCommitOverTheWholeHistory() {
        // 40 lines added over 4 commits (one before the window): 10 per change, 3 changes in the window
        FileReadsForChanges reads = new FileReadsForChanges(Collections.singletonList(
                file("A.java", 600, 40, "c1", "c2", "c3", "old@" + OLD)), new FileReadsForChangesConfig());

        FileReadsForChanges.ChangedFile a = reads.topFiles().get(0);
        assertEquals(3, a.getChanges());
        assertEquals(10.0, a.getEditedLinesPerChange(), 1e-9);
        assertEquals(60.0, a.getReadPerEditedLine(), 1e-9);
    }

    @Test
    void withoutChurnThereIsNoRatio() {
        FileReadsForChanges reads = new FileReadsForChanges(Collections.singletonList(file("A.java", 600, 0, "c1")), new FileReadsForChangesConfig());

        assertEquals(0, reads.topFiles().get(0).getEditedLinesPerChange());
        assertEquals(0, reads.topFiles().get(0).getReadPerEditedLine());
    }

    @Test
    void capsTheListAndSkipsFilesWithoutHistory() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < new FileReadsForChangesConfig().getMaxFiles() + 5; i++) {
            files.add(file("F" + i + ".java", 100 + i, 0, "c" + i));
        }
        files.add(file("NoHistory.java", 10000, 0));

        FileReadsForChanges reads = new FileReadsForChanges(files, new FileReadsForChangesConfig());

        assertEquals(new FileReadsForChangesConfig().getMaxFiles(), reads.topFiles().size());
        assertFalse(paths(reads).contains("NoHistory.java"));
    }

    @Test
    void roundsToNiceNumbersThatNeverLookPrecise() {
        assertEquals("~7k–15k", FileReadsForChanges.tokenRange(1000, 7, 14));
        assertEquals("~3k–10k", FileReadsForChanges.tokenRange(600, 7, 14));
        assertEquals("~1M–3M", FileReadsForChanges.tokenRange(150000, 7, 14));
        assertEquals("under 1k", FileReadsForChanges.tokenRange(20, 7, 14));
        assertEquals("~700", FileReadsForChanges.about(640));
        assertEquals("~30", FileReadsForChanges.about(28.4));
        assertEquals("~1,500", FileReadsForChanges.about(1600));
        assertEquals("~15k", FileReadsForChanges.about(17155));
    }

    @Test
    void theWindowListSizeAndTokensPerLineAreConfigurable() {
        FileReadsForChangesConfig config = new FileReadsForChangesConfig();
        config.setWindowDays(0);          // the whole history
        config.setMaxFiles(1);
        config.setTokensPerLineMin(10);
        config.setTokensPerLineMax(10);
        FileReadsForChanges reads = new FileReadsForChanges(Arrays.asList(
                file("Old.java", 1000, 0, "o1@" + OLD, "o2@" + OLD),
                file("New.java", 100, 0, "c1")), config);

        assertEquals(Collections.singletonList("Old.java"), paths(reads));
        assertEquals(2, reads.topFiles().get(0).getChanges());
        assertEquals("~10k", reads.tokenRange(1000));
        assertEquals("in the whole history", reads.windowLabel());
    }

    @Test
    void invalidTokensPerLineFallBackToAnOrderedPositiveRange() {
        FileReadsForChangesConfig config = new FileReadsForChangesConfig();
        config.setTokensPerLineMin(14);
        config.setTokensPerLineMax(7);
        config.setMaxFiles(0);
        FileReadsForChanges reads = new FileReadsForChanges(Collections.singletonList(file("A.java", 100, 0, "c1")), config);

        assertEquals(7, reads.getTokensPerLineMin());
        assertEquals(14, reads.getTokensPerLineMax());
        assertEquals(1, reads.topFiles().size());

        config.setTokensPerLineMin(0);
        config.setTokensPerLineMax(0);
        FileReadsForChanges zero = new FileReadsForChanges(Collections.emptyList(), config);
        assertEquals(1, zero.getTokensPerLineMin());
        assertEquals(1, zero.getTokensPerLineMax());
    }
}
