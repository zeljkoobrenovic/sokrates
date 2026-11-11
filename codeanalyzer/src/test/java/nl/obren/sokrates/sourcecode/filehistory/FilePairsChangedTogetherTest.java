package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilePairsChangedTogetherTest {

    @BeforeEach
    void resetDateCaches() {
        // populate() uses DateUtils range checks; keep ranges disabled so tests are date-independent.
        DateUtils.dateParam = null;
        DateUtils.reset();
    }

    private SourceFile file(String relativePath) {
        SourceFile sourceFile = new SourceFile(new File(relativePath));
        sourceFile.setRelativePath(relativePath);
        return sourceFile;
    }

    private NamedSourceCodeAspect aspect(SourceFile... files) {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect("main");
        aspect.setSourceFiles(new ArrayList<>(Arrays.asList(files)));
        return aspect;
    }

    private FileModificationHistory history(String path, CommitInfo... commits) {
        FileModificationHistory history = new FileModificationHistory(path);
        history.setCommits(new ArrayList<>(Arrays.asList(commits)));
        return history;
    }

    private CommitInfo commit(String id, String date) {
        return new CommitInfo(id, date);
    }

    @Test
    void twoFilesSharingOneCommitFormOnePair() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        NamedSourceCodeAspect aspect = aspect(a, b);

        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01")),
                history("b.java", commit("c1", "2020-01-01")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        List<FilePairChangedTogether> result = pairs.getFilePairsList();
        assertEquals(1, result.size());
        FilePairChangedTogether pair = result.get(0);
        assertEquals(1, pair.getCommits().size());
        // CommitInfo stores the id as "<date> <id>", so the recorded shared commit reflects that format.
        assertTrue(pair.getCommits().contains("2020-01-01 c1"));
    }

    @Test
    void sharedCommitsAreAccumulatedAndDeduplicatedPerPair() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        NamedSourceCodeAspect aspect = aspect(a, b);

        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01"), commit("c2", "2020-01-02")),
                history("b.java", commit("c1", "2020-01-01"), commit("c2", "2020-01-02")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        assertEquals(1, pairs.getFilePairsList().size());
        FilePairChangedTogether pair = pairs.getFilePairsList().get(0);
        assertEquals(2, pair.getCommits().size(), "both shared commits should be recorded once each");
        assertEquals(2, pair.getCommitsCountFile1());
        assertEquals(2, pair.getCommitsCountFile2());
        assertEquals("2020-01-02", pair.getLatestCommit(), "latest commit date should be the max");
    }

    @Test
    void filesNotSharingCommitsProduceNoPair() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        NamedSourceCodeAspect aspect = aspect(a, b);

        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01")),
                history("b.java", commit("c2", "2020-01-02")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        assertTrue(pairs.getFilePairsList().isEmpty());
    }

    @Test
    void filesOutsideAspectAreIgnored() {
        SourceFile a = file("a.java");
        // b.java is referenced in history but not part of the aspect
        NamedSourceCodeAspect aspect = aspect(a);

        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01")),
                history("b.java", commit("c1", "2020-01-01")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        assertTrue(pairs.getFilePairsList().isEmpty(),
                "no pair can form when one file is not scoped into the aspect");
    }

    @Test
    void threeFilesInOneCommitProduceAllThreePairs() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        SourceFile c = file("c.java");
        NamedSourceCodeAspect aspect = aspect(a, b, c);

        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01")),
                history("b.java", commit("c1", "2020-01-01")),
                history("c.java", commit("c1", "2020-01-01")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        assertEquals(3, pairs.getFilePairsList().size(), "N files in a commit -> N*(N-1)/2 pairs");
    }

    @Test
    void pairOrderAndCaseDoNotCreateDuplicatePairs() {
        // Same logical pair seen via different file orderings and casing must collapse to one pair.
        SourceFile a = file("Dir/A.java");
        SourceFile b = file("Dir/B.java");
        NamedSourceCodeAspect aspect = aspect(a, b);

        List<FileModificationHistory> histories = Arrays.asList(
                history("Dir/A.java", commit("c1", "2020-01-01"), commit("c2", "2020-01-02")),
                history("Dir/B.java", commit("c1", "2020-01-01"), commit("c2", "2020-01-02")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        assertEquals(1, pairs.getFilePairsList().size());
    }

    @Test
    void resultsSortedByDescendingSharedCommitCount() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        SourceFile c = file("c.java");
        NamedSourceCodeAspect aspect = aspect(a, b, c);

        // a & b share 2 commits; a & c share 1 commit
        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01"), commit("c2", "2020-01-02"), commit("c3", "2020-01-03")),
                history("b.java", commit("c1", "2020-01-01"), commit("c2", "2020-01-02")),
                history("c.java", commit("c3", "2020-01-03")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        List<FilePairChangedTogether> result = pairs.getFilePairsList();
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getCommits().size(), "pair with most shared commits comes first");
        assertEquals(1, result.get(1).getCommits().size());
    }

    @Test
    void aFileListingTheSameCommitTwiceIsCountedOnce() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        NamedSourceCodeAspect aspect = aspect(a, b);

        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01"), commit("c1", "2020-01-01")),
                history("b.java", commit("c1", "2020-01-01"), commit("c1", "2020-01-01")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        assertEquals(1, pairs.getFilePairsList().size(), "no pair of a file with itself");
        assertEquals(1, pairs.getFilePairsList().get(0).getCommits().size());
    }

    @Test
    void pairsWithTheSameCountKeepTheOrderInWhichTheyWereFound() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        SourceFile c = file("c.java");
        NamedSourceCodeAspect aspect = aspect(a, b, c);

        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01")),
                history("b.java", commit("c1", "2020-01-01")),
                history("c.java", commit("c1", "2020-01-01")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1);
        pairs.populate(aspect, histories);

        List<FilePairChangedTogether> result = pairs.getFilePairsList();
        assertEquals("b.java a.java", result.get(0).getSourceFile1().getRelativePath() + " " + result.get(0).getSourceFile2().getRelativePath());
        assertEquals("c.java a.java", result.get(1).getSourceFile1().getRelativePath() + " " + result.get(1).getSourceFile2().getRelativePath());
        assertEquals("c.java b.java", result.get(2).getSourceFile1().getRelativePath() + " " + result.get(2).getSourceFile2().getRelativePath());
    }

    @Test
    void commitsWithMoreFilesThanTheLimitAreLeftOut() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        SourceFile c = file("c.java");
        NamedSourceCodeAspect aspect = aspect(a, b, c);

        // big touches all three files, small only a and b
        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("big", "2020-01-01"), commit("small", "2020-01-02")),
                history("b.java", commit("big", "2020-01-01"), commit("small", "2020-01-02")),
                history("c.java", commit("big", "2020-01-01")));

        FilePairsChangedTogether limited = new FilePairsChangedTogether(-1, 2);
        limited.populate(aspect, histories);

        assertEquals(1, limited.getSkippedCommitsCount());
        assertEquals(1, limited.getFilePairsList().size(), "only the a-b pair of the small commit");
        assertEquals(1, limited.getFilePairsList().get(0).getCommits().size());
        assertTrue(limited.getFilePairsList().get(0).getCommits().contains("2020-01-02 small"));

        FilePairsChangedTogether atTheLimit = new FilePairsChangedTogether(-1, 3);
        atTheLimit.populate(aspect, histories);
        assertEquals(0, atTheLimit.getSkippedCommitsCount(), "a commit with exactly the limit is kept");
        assertEquals(3, atTheLimit.getFilePairsList().size());

        FilePairsChangedTogether noLimit = new FilePairsChangedTogether(-1, 0);
        noLimit.populate(aspect, histories);
        assertEquals(3, noLimit.getFilePairsList().size());
    }

    @Test
    void filesOutsideTheAspectDoNotCountTowardsTheLimit() {
        SourceFile a = file("a.java");
        SourceFile b = file("b.java");
        NamedSourceCodeAspect aspect = aspect(a, b);

        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", commit("c1", "2020-01-01")),
                history("b.java", commit("c1", "2020-01-01")),
                history("test/x.java", commit("c1", "2020-01-01")),
                history("test/y.java", commit("c1", "2020-01-01")));

        FilePairsChangedTogether pairs = new FilePairsChangedTogether(-1, 2);
        pairs.populate(aspect, histories);

        assertEquals(0, pairs.getSkippedCommitsCount());
        assertEquals(1, pairs.getFilePairsList().size());
    }
}
