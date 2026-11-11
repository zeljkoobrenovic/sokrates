package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class FileHistoryScopingUtilsTest {

    // --- fixture builders -------------------------------------------------

    /** A source file whose modification history has {@code updateCount} dates ending today. */
    private SourceFile fileWithUpdateCount(String path, int updateCount) {
        SourceFile sourceFile = sourceFile(path);
        FileModificationHistory history = new FileModificationHistory(path);
        List<String> dates = IntStream.range(0, updateCount)
                .mapToObj(i -> daysAgo(i))
                .collect(Collectors.toList());
        history.setDates(new ArrayList<>(dates));
        sourceFile.setFileModificationHistory(history);
        return sourceFile;
    }

    /** A source file whose history spans from {@code firstDaysAgo} to {@code latestDaysAgo}. */
    private SourceFile fileWithSpan(String path, int firstDaysAgo, int latestDaysAgo) {
        SourceFile sourceFile = sourceFile(path);
        FileModificationHistory history = new FileModificationHistory(path);
        history.setDates(new ArrayList<>(Arrays.asList(daysAgo(firstDaysAgo), daysAgo(latestDaysAgo))));
        sourceFile.setFileModificationHistory(history);
        return sourceFile;
    }

    private SourceFile sourceFile(String path) {
        SourceFile sourceFile = new SourceFile(new File(path));
        sourceFile.setRelativePath(path);
        return sourceFile;
    }

    private String daysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
    }

    private CodeAnalysisResults resultsFor(SourceFile... files) {
        NamedSourceCodeAspect main = new NamedSourceCodeAspect("main");
        main.setSourceFiles(new ArrayList<>(Arrays.asList(files)));

        AspectAnalysisResults mainAspect = new AspectAnalysisResults("main");
        mainAspect.setAspect(main);

        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setMainAspectAnalysisResults(mainAspect);
        results.setCodeConfiguration(new CodeConfiguration());
        return results;
    }

    private NamedSourceCodeAspect bucket(LogicalDecomposition decomposition, int index) {
        return decomposition.getComponents().get(index);
    }

    // --- update frequency -------------------------------------------------

    @Test
    void updateFrequencyDecompositionHasFiveOrderedBuckets() {
        CodeAnalysisResults results = resultsFor(fileWithUpdateCount("a.java", 1));

        List<LogicalDecomposition> decompositions =
                FileHistoryScopingUtils.getLogicalDecompositionsFileUpdateFrequency(results);

        assertEquals(1, decompositions.size());
        LogicalDecomposition decomposition = decompositions.get(0);
        assertEquals("frequency of changes", decomposition.getName());
        assertEquals(5, decomposition.getComponents().size());
        // metadata expected by the report generators
        assertEquals(0, decomposition.getComponentsFolderDepth());
        assertFalse(decomposition.getDependenciesFinder().isUseBuiltInDependencyFinders());
    }

    @Test
    void updateFrequencyPlacesFilesInExpectedBuckets() {
        // Default frequency thresholds are (5, 20, 50, 100).
        CodeAnalysisResults results = resultsFor(
                fileWithUpdateCount("negligible.java", 3),   // <= 5  -> bucket 0
                fileWithUpdateCount("low.java", 10),          // <= 20 -> bucket 1
                fileWithUpdateCount("medium.java", 30),       // <= 50 -> bucket 2
                fileWithUpdateCount("high.java", 80),         // <= 100 -> bucket 3
                fileWithUpdateCount("veryHigh.java", 150));   // > 100 -> bucket 4

        LogicalDecomposition decomposition =
                FileHistoryScopingUtils.getLogicalDecompositionsFileUpdateFrequency(results).get(0);

        assertEquals(Arrays.asList("negligible.java"), bucket(decomposition, 0).getFiles());
        assertEquals(Arrays.asList("low.java"), bucket(decomposition, 1).getFiles());
        assertEquals(Arrays.asList("medium.java"), bucket(decomposition, 2).getFiles());
        assertEquals(Arrays.asList("high.java"), bucket(decomposition, 3).getFiles());
        assertEquals(Arrays.asList("veryHigh.java"), bucket(decomposition, 4).getFiles());
    }

    @Test
    void boundaryCountLandsInTheLowerBucket() {
        // Exactly at the "low" threshold (5) must stay in the negligible bucket (<= low).
        CodeAnalysisResults results = resultsFor(
                fileWithUpdateCount("boundary.java", 5),
                fileWithUpdateCount("justOver.java", 6));

        LogicalDecomposition decomposition =
                FileHistoryScopingUtils.getLogicalDecompositionsFileUpdateFrequency(results).get(0);

        assertEquals(Arrays.asList("boundary.java"), bucket(decomposition, 0).getFiles());
        assertEquals(Arrays.asList("justOver.java"), bucket(decomposition, 1).getFiles());
    }

    @Test
    void filesWithoutHistoryAreSkipped() {
        SourceFile noHistory = sourceFile("nohistory.java"); // fileModificationHistory stays null
        CodeAnalysisResults results = resultsFor(
                fileWithUpdateCount("tracked.java", 3),
                noHistory);

        LogicalDecomposition decomposition =
                FileHistoryScopingUtils.getLogicalDecompositionsFileUpdateFrequency(results).get(0);

        long totalFiles = decomposition.getComponents().stream()
                .mapToLong(component -> component.getFiles().size()).sum();
        assertEquals(1, totalFiles, "the file without modification history must not be classified");
    }

    // --- age (days since first update) -----------------------------------

    @Test
    void ageDecompositionClassifiesByDaysSinceFirstUpdate() {
        // Default age thresholds are (30, 90, 180, 365).
        CodeAnalysisResults results = resultsFor(
                fileWithSpan("new.java", 10, 1),        // ~10 days old   -> bucket 0
                fileWithSpan("mature.java", 120, 1),    // ~120 days old  -> bucket 2
                fileWithSpan("veryOld.java", 500, 1));  // ~500 days old  -> bucket 4

        LogicalDecomposition decomposition =
                FileHistoryScopingUtils.getLogicalDecompositionsByAge(results).get(0);

        assertEquals("file age", decomposition.getName());
        assertEquals(Arrays.asList("new.java"), bucket(decomposition, 0).getFiles());
        assertEquals(Arrays.asList("mature.java"), bucket(decomposition, 2).getFiles());
        assertEquals(Arrays.asList("veryOld.java"), bucket(decomposition, 4).getFiles());
    }

    // --- freshness (days since latest update) ----------------------------

    @Test
    void freshnessDecompositionClassifiesByDaysSinceLatestUpdate() {
        // A file first touched long ago but updated recently is "fresh".
        CodeAnalysisResults results = resultsFor(
                fileWithSpan("fresh.java", 500, 5),      // latest ~5 days ago  -> bucket 0
                fileWithSpan("stale.java", 500, 400));   // latest ~400 days ago -> bucket 4

        LogicalDecomposition decomposition =
                FileHistoryScopingUtils.getLogicalDecompositionsByFreshness(results).get(0);

        assertEquals("file freshness", decomposition.getName());
        assertEquals(Arrays.asList("fresh.java"), bucket(decomposition, 0).getFiles());
        assertEquals(Arrays.asList("stale.java"), bucket(decomposition, 4).getFiles());
    }
}
