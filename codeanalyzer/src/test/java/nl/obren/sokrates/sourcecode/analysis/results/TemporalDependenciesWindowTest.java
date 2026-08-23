/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The co-change windows a consumer may present.
 *
 * <p>Before this, three places decided independently which windows exist: the analyzer's four
 * branches, the report generator's four tab conditions, and the data exporter (which knew about only
 * two of the four). A window the analyzer skipped still got an exported file - a header line and
 * nothing else, byte-identical to a repository whose files genuinely never change together.
 */
class TemporalDependenciesWindowTest {

    @Test
    void aWindowIsAnalyzedOnlyOnceTheConfiguredDepthReachesIt() {
        assertEquals("[]", analyzedAt(0));
        assertEquals("[]", analyzedAt(29));
        assertEquals("[PAST_30_DAYS]", analyzedAt(30));
        assertEquals("[PAST_30_DAYS]", analyzedAt(89));
        assertEquals("[PAST_30_DAYS, PAST_90_DAYS]", analyzedAt(90));
        assertEquals("[PAST_30_DAYS, PAST_90_DAYS]", analyzedAt(179));
        assertEquals("[PAST_30_DAYS, PAST_90_DAYS, PAST_180_DAYS]", analyzedAt(180));
        assertEquals("[PAST_30_DAYS, PAST_90_DAYS, PAST_180_DAYS, ALL_TIME]", analyzedAt(181));
        // The default configured depth (AnalysisConfig.maxTemporalDependenciesDepthDays).
        assertEquals("[PAST_30_DAYS, PAST_90_DAYS, PAST_180_DAYS, ALL_TIME]", analyzedAt(365));
    }

    @Test
    void theAllTimeWindowNeedsMoreThanTheFixedOneHundredAndEightyDays() {
        // The boundary that started this: at exactly 180 the all-time window would only repeat the
        // 180-day one, so the analyzer skips it - and its result stays empty for that reason alone.
        assertFalse(TemporalDependenciesWindow.ALL_TIME.isAnalyzed(180));
        assertTrue(TemporalDependenciesWindow.ALL_TIME.isAnalyzed(181));
        assertTrue(TemporalDependenciesWindow.PAST_180_DAYS.isAnalyzed(180));
    }

    @Test
    void noWindowIsAnalyzedWithoutCommitHistoryHoweverDeepTheConfiguration() {
        // The third way a window ends up empty, and the one the issue's two-state framing missed:
        // the analyzer never enters the co-change block at all when no history was imported.
        FilesHistoryAnalysisResults noHistory = new FilesHistoryAnalysisResults();

        assertFalse(noHistory.hasHistory());
        assertEquals(Collections.emptyList(), TemporalDependenciesWindow.analyzedWindows(noHistory, 365));
        assertEquals(Collections.emptyList(), TemporalDependenciesWindow.analyzedWindows(noHistory, Integer.MAX_VALUE));
    }

    @Test
    void eachWindowReadsItsOwnResultList() {
        // Guards against a copy-paste in the accessor wiring - the kind that already exists in this
        // model, where setFilePairsChangedTogether90Days names its parameter ...30Days.
        FilesHistoryAnalysisResults results = new FilesHistoryAnalysisResults();
        results.setFilePairsChangedTogether(pairs("all-time"));
        results.setFilePairsChangedTogether30Days(pairs("30"));
        results.setFilePairsChangedTogether90Days(pairs("90"));
        results.setFilePairsChangedTogether180Days(pairs("180"));

        assertEquals("30", firstPath(TemporalDependenciesWindow.PAST_30_DAYS, results));
        assertEquals("90", firstPath(TemporalDependenciesWindow.PAST_90_DAYS, results));
        assertEquals("180", firstPath(TemporalDependenciesWindow.PAST_180_DAYS, results));
        assertEquals("all-time", firstPath(TemporalDependenciesWindow.ALL_TIME, results));
    }

    @Test
    void everyWindowHasItsOwnPairOfDataFileNames() {
        // The all-time window keeps the unsuffixed historical names; a collision here would make two
        // windows overwrite each other's export.
        assertEquals("[temporal_dependencies_30_days.txt, temporal_dependencies_90_days.txt, "
                        + "temporal_dependencies_180_days.txt, temporal_dependencies.txt]",
                names(TemporalDependenciesWindow::getDataFileName));
        assertEquals("[temporal_dependencies_different_folders_30_days.txt, "
                        + "temporal_dependencies_different_folders_90_days.txt, "
                        + "temporal_dependencies_different_folders_180_days.txt, "
                        + "temporal_dependencies_different_folders.txt]",
                names(TemporalDependenciesWindow::getDifferentFoldersDataFileName));
    }

    private String analyzedAt(int maxTemporalDependenciesDepthDays) {
        FilesHistoryAnalysisResults results = new FilesHistoryAnalysisResults();
        results.setHistory(Collections.singletonList(new FileModificationHistory("a.java")));
        return TemporalDependenciesWindow.analyzedWindows(results, maxTemporalDependenciesDepthDays).toString();
    }

    private String names(java.util.function.Function<TemporalDependenciesWindow, String> nameOf) {
        return Arrays.stream(TemporalDependenciesWindow.values()).map(nameOf).collect(Collectors.toList()).toString();
    }

    private List<FilePairChangedTogether> pairs(String marker) {
        return Collections.singletonList(
                new FilePairChangedTogether(new SourceFile(new File(marker)), new SourceFile(new File("other"))));
    }

    private String firstPath(TemporalDependenciesWindow window, FilesHistoryAnalysisResults results) {
        return window.getFilePairs(results).get(0).getSourceFile1().getFile().getPath();
    }
}
