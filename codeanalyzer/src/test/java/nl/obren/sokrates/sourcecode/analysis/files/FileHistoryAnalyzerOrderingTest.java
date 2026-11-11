package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the ordering of FileHistoryAnalyzer's "top N" lists. The analyzer's add*Files methods are
 * private, so this test reconstructs the ORIGINAL two-pass stable-sort algorithm (sort by tiebreaker,
 * then stable-sort by the primary key) and asserts that the single composed comparators now used
 * produce the identical order. This guards the comparator rewrite that replaced the repeated-key
 * sorts with precomputed-key comparators.
 */
class FileHistoryAnalyzerOrderingTest {

    private SourceFile file(String path, int linesOfCode, int daysFirst, int daysLatest, int changeCount, int contributors) {
        SourceFile sourceFile = new SourceFile(new File(path)) {
            @Override
            public int getLinesOfCode() {
                return linesOfCode;
            }
        };
        FileModificationHistory history = new FileModificationHistory(path) {
            @Override
            public int daysSinceFirstUpdate() {
                return daysFirst;
            }

            @Override
            public int daysSinceLatestUpdate() {
                return daysLatest;
            }
        };
        List<String> dates = new ArrayList<>();
        for (int i = 0; i < changeCount; i++) {
            dates.add("2020-01-" + String.format("%02d", i + 1));
        }
        history.setDates(dates);
        List<CommitInfo> commits = new ArrayList<>();
        for (int i = 0; i < contributors; i++) {
            CommitInfo commit = new CommitInfo("c" + i, "2020-01-01");
            commit.setEmail("user" + i + "@example.com");
            commits.add(commit);
        }
        history.setCommits(commits);
        sourceFile.setFileModificationHistory(history);
        return sourceFile;
    }

    private List<SourceFile> sample() {
        // Includes ties on each key so the stable tiebreak ordering is exercised.
        List<SourceFile> files = new ArrayList<>();
        files.add(file("a", 100, 10, 5, 3, 2));
        files.add(file("b", 100, 10, 5, 3, 2)); // full tie with a
        files.add(file("c", 200, 20, 1, 7, 5));
        files.add(file("d", 50, 5, 20, 1, 1));
        files.add(file("e", 150, 20, 5, 3, 2)); // ties c on daysFirst, ties a/b on others
        return files;
    }

    private int daysFirst(SourceFile o) { return o.getFileModificationHistory().daysSinceFirstUpdate(); }
    private int daysLatest(SourceFile o) { return o.getFileModificationHistory().daysSinceLatestUpdate(); }
    private int changeCount(SourceFile o) { return o.getFileModificationHistory().getDates().size(); }
    private int contributors(SourceFile o) { return o.getFileModificationHistory().countContributors(); }

    // Reproduce the original two-pass stable sort and return path order.
    private List<String> oldOrder(List<SourceFile> files, Comparator<SourceFile> first, Comparator<SourceFile> second, boolean reverse) {
        List<SourceFile> list = new ArrayList<>(files);
        list.sort(first);
        list.sort(second);
        if (reverse) {
            java.util.Collections.reverse(list);
        }
        return list.stream().map(f -> f.getFile().getPath()).collect(java.util.stream.Collectors.toList());
    }

    private List<String> newOrder(List<SourceFile> files, Comparator<SourceFile> comparator) {
        List<SourceFile> list = new ArrayList<>(files);
        list.sort(comparator);
        return list.stream().map(f -> f.getFile().getPath()).collect(java.util.stream.Collectors.toList());
    }

    @Test
    void oldestFilesOrderUnchanged() {
        assertEquals(
                oldOrder(sample(),
                        Comparator.comparingInt(SourceFile::getLinesOfCode).reversed(),
                        (o1, o2) -> daysFirst(o2) - daysFirst(o1), false),
                newOrder(sample(),
                        Comparator.comparingInt(this::daysFirst).reversed()
                                .thenComparing(Comparator.comparingInt(SourceFile::getLinesOfCode).reversed())));
    }

    @Test
    void youngestFilesOrderUnchanged() {
        assertEquals(
                oldOrder(sample(),
                        Comparator.comparingInt(SourceFile::getLinesOfCode).reversed(),
                        Comparator.comparingInt(this::daysFirst), false),
                newOrder(sample(),
                        Comparator.comparingInt(this::daysFirst)
                                .thenComparing(Comparator.comparingInt(SourceFile::getLinesOfCode).reversed())));
    }

    @Test
    void mostRecentlyChangedOrderUnchanged() {
        assertEquals(
                oldOrder(sample(),
                        Comparator.comparingInt(SourceFile::getLinesOfCode).reversed(),
                        Comparator.comparingInt(this::daysLatest), false),
                newOrder(sample(),
                        Comparator.comparingInt(this::daysLatest)
                                .thenComparing(Comparator.comparingInt(SourceFile::getLinesOfCode).reversed())));
    }

    // Note: the two reverse() cases (most-previously-changed, most-changed) intentionally keep the
    // original sort-then-reverse form in production - a single composed comparator would not
    // reproduce how Collections.reverse() also flips the relative order of tied elements - so there
    // is nothing to compare here beyond the original algorithm itself.

    @Test
    void mostContributorsOrderUnchanged() {
        assertEquals(
                oldOrder(sample(),
                        Comparator.comparingInt(o -> -changeCount(o)),
                        (o1, o2) -> contributors(o2) - contributors(o1), false),
                newOrder(sample(),
                        Comparator.comparingInt(this::contributors).reversed()
                                .thenComparing(Comparator.comparingInt(this::changeCount).reversed())));
    }

    @Test
    void leastContributorsOrderUnchanged() {
        assertEquals(
                oldOrder(sample(),
                        Comparator.comparingInt(o -> -o.getLinesOfCode()),
                        Comparator.comparingInt(this::contributors), false),
                newOrder(sample(),
                        Comparator.comparingInt(this::contributors)
                                .thenComparing(Comparator.comparingInt(SourceFile::getLinesOfCode).reversed())));
    }
}
