package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AgentContextSummaryTest {
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

    /** A main file with the given commits, each "id" (recent) or "id@date". */
    private static SourceFile file(String path, int loc, String... commits) {
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
            file.setFileModificationHistory(history);
        }
        return file;
    }

    private static FilePairChangedTogether pair(SourceFile a, SourceFile b, String... commits) {
        FilePairChangedTogether pair = new FilePairChangedTogether(a, b);
        // the ids are CommitInfo ids: "<date> <sha>"
        pair.setCommits(Arrays.stream(commits).map(id -> new CommitInfo(id, id.equals("old") ? OLD : RECENT).getId())
                .collect(Collectors.toList()));
        return pair;
    }

    private static AgentContextSummary summary(List<SourceFile> files, List<FilePairChangedTogether> pairs) {
        return new AgentContextSummary(files, pairs, 365, Collections.emptySet());
    }

    @Test
    void bandsFilesByReadWindowWithTheirLinesAndChanges() {
        List<SourceFile> files = Arrays.asList(
                file("Small.java", 100, "c1"),
                file("Medium.java", 1000, "c1", "c2"),
                file("Large.java", 3000, "c3"),
                file("Huge.java", 6000, "c3", "c4", "c5"),
                file("Untouched.java", 50));

        AgentContextSummary summary = summary(files, Collections.emptyList());

        List<AgentContextSummary.Band> bands = summary.getFileBands();
        assertEquals(Arrays.asList(2, 1, 0, 1, 1), bands.stream().map(AgentContextSummary.Band::getCount).collect(Collectors.toList()));
        assertEquals(Arrays.asList(150L, 1000L, 0L, 3000L, 6000L), bands.stream().map(AgentContextSummary.Band::getLinesOfCode).collect(Collectors.toList()));
        assertEquals(Arrays.asList(1, 2, 0, 1, 3), bands.stream().map(AgentContextSummary.Band::getChanges).collect(Collectors.toList()));
        assertEquals(4, summary.fileBandIndex(6000));
        assertEquals(2, summary.fileBandIndex(1001));
        assertEquals(5, summary.getCommitsCount());
        // c3, c4 and c5 touch a file over one read window
        assertEquals(3, summary.getCommitsTouchingLargeFiles());
    }

    @Test
    void leavesOutCommitsOutsideTheWindowAndMassCommits() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < AgentContextSummary.MAX_FILES_PER_COMMIT + 1; i++) {
            files.add(file("f" + i + ".java", 10, "mass", "old@" + OLD));
        }
        files.add(file("Kept.java", 10, "kept"));

        AgentContextSummary summary = summary(files, Collections.emptyList());

        assertEquals(1, summary.getCommitsCount());
        assertEquals(1, summary.getIgnoredLargeCommitsCount());
        assertEquals(Collections.singletonList("Kept.java"),
                summary.getChangedFiles().stream().map(c -> c.getFile().getRelativePath()).collect(Collectors.toList()));
    }

    @Test
    void measuresCommitReadingInBandsWithMedianPercentileAndAiCommits() {
        List<SourceFile> files = Arrays.asList(
                file("A.java", 1500, "c1", "c2", "c3"),
                file("B.java", 9000, "c3", "c4"),
                file("C.java", 60000, "c5"));

        AgentContextSummary summary = new AgentContextSummary(files, Collections.emptyList(), 365, new HashSet<>(Arrays.asList("c4", "c5")));

        // commit lines: c1 1500, c2 1500, c3 10500, c4 9000, c5 60000
        assertEquals(Arrays.asList(0, 2, 1, 1, 1), summary.getCommitBands().stream().map(AgentContextSummary.Band::getCount).collect(Collectors.toList()));
        assertEquals(Arrays.asList(0, 0, 1, 0, 1), summary.getCommitBands().stream().map(AgentContextSummary.Band::getAiCount).collect(Collectors.toList()));
        assertEquals(9000, summary.getMedianCommitLines());
        assertEquals(60000, summary.getP90CommitLines());
        assertEquals(2, summary.getAiCommitsCount());
        assertEquals(9000, summary.getMedianAiCommitLines());
    }

    @Test
    void partnersUseTheSharedCommitsInTheWindowAgainstTheFilesOwnChanges() {
        SourceFile a = file("A.java", 1000, "c1", "c2", "c3", "c4", "c5", "c6", "old@" + OLD);
        SourceFile b = file("B.java", 200, "c1", "c2", "old@" + OLD);
        SourceFile c = file("C.java", 300, "c3");
        AgentContextSummary summary = summary(Arrays.asList(a, b, c), Arrays.asList(
                pair(a, b, "c1", "c2", "old"),   // 2 shared in the window: 2/6 of A, 2/2 of B
                pair(a, c, "c3")));              // shared only once: never a partner

        Map<String, AgentContextSummary.FileContext> contexts = summary.getChangedFiles().stream()
                .collect(Collectors.toMap(ctx -> ctx.getFile().getRelativePath(), ctx -> ctx));
        AgentContextSummary.FileContext contextA = contexts.get("A.java");
        assertEquals(6, contextA.getChanges());
        assertEquals(1, contextA.getPartners().size());
        assertEquals("B.java", contextA.getPartners().get(0).getFile().getRelativePath());
        assertEquals(2.0 / 6, contextA.getPartners().get(0).getShare(), 1e-9);
        assertEquals(1200, contextA.getWorkingSetLines());
        assertEquals(1.0, contexts.get("B.java").getPartners().get(0).getShare(), 1e-9);
        assertTrue(contexts.get("C.java").getPartners().isEmpty());
    }

    @Test
    void ranksHotspotsByChangesTimesWorkingSetAndFlagsSplitCandidates() {
        SourceFile big = file("Big.java", 4000, "c1", "c2", "c3", "c4", "c5", "c6");
        SourceFile rare = file("Rare.java", 9000, "c7");
        SourceFile small = file("Small.java", 100, "c1", "c2", "c3", "c4", "c5", "c6", "c8");

        AgentContextSummary summary = summary(Arrays.asList(big, rare, small), Collections.emptyList());

        assertEquals(Arrays.asList("Big.java", "Rare.java", "Small.java"),
                summary.hotspots().stream().map(c -> c.getFile().getRelativePath()).collect(Collectors.toList()));
        assertEquals(Collections.singletonList("Big.java"),
                summary.splitCandidates().stream().map(c -> c.getFile().getRelativePath()).collect(Collectors.toList()));
        assertEquals(2000, summary.splitCandidates().get(0).getSplitSavingLines());
        // 2,000 lines saved per change pays back after ~250 changes: decades at 6 changes a year
        assertEquals("not within a few years at its current pace", summary.paybackPace(summary.splitCandidates().get(0)));
    }

    @Test
    void roundsTokensToNiceRangesThatNeverLookPrecise() {
        assertEquals("~7k–15k", AgentContextSummary.tokenRange(1000));
        assertEquals("~10k–30k", AgentContextSummary.tokenRange(1500));
        assertEquals("~300k–700k", AgentContextSummary.tokenRange(50000));
        assertEquals("~1M–3M", AgentContextSummary.tokenRange(150000));
        assertEquals("under 1k", AgentContextSummary.tokenRange(20));
        assertEquals(1500, AgentContextSummary.niceCeil(1400));
        assertEquals(1000, AgentContextSummary.niceFloor(1400));
        assertEquals(10000, AgentContextSummary.niceCeil(7001));
        assertEquals("1.5M", AgentContextSummary.compact(1_500_000));
        assertEquals("~3k", AgentContextSummary.roundedLines(2600));
        assertEquals("<1%", AgentContextSummary.percentage(1, 1000));
        assertEquals("33%", AgentContextSummary.percentage(1, 3));
    }

    @Test
    void paybackIsAnOrderOfMagnitude() {
        // the article's case: ~15k lines saved per change -> about 34 changes
        assertEquals("tens of changes", AgentContextSummary.paybackMagnitude(15000));
        assertEquals("hundreds of changes", AgentContextSummary.paybackMagnitude(1000));
        assertEquals("a handful of changes", AgentContextSummary.paybackMagnitude(100000));
        assertEquals("never", AgentContextSummary.paybackMagnitude(0));
    }

    @Test
    void isEmptyWithoutHistory() {
        AgentContextSummary summary = summary(Arrays.asList(file("A.java", 100), file("B.java", 5000)), Collections.emptyList());

        assertTrue(summary.isEmpty());
        assertTrue(summary.hotspots().isEmpty());
        assertEquals(5100, summary.getTotalLinesOfCode());
    }
}
