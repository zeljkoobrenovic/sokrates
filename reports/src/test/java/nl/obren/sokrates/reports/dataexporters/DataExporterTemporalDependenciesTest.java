/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What data/text/ says about the co-change windows.
 *
 * <p>Two of the four analyzed windows were exported, and the two unexported ones were the deepest -
 * so a repository configured for 180 days received 30 days of data. Worse, an unanalyzed window
 * still got its file, holding the column header and nothing else: byte-identical to a repository
 * whose files genuinely never change together. Absence now carries that distinction.
 */
class DataExporterTemporalDependenciesTest {

    private static final String HEADER = "file 1\tfile 2\t# same commits\t# commits file 1\t# commits file 2\n";

    @Test
    void everyAnalyzedWindowIsExported(@TempDir File folder) {
        // Previously only the first two of these were written, at the default depth of 365 days.
        export(365, folder);

        assertEquals(Arrays.asList(
                "temporal_dependencies.txt",
                "temporal_dependencies_180_days.txt",
                "temporal_dependencies_30_days.txt",
                "temporal_dependencies_90_days.txt",
                "temporal_dependencies_different_folders.txt",
                "temporal_dependencies_different_folders_180_days.txt",
                "temporal_dependencies_different_folders_30_days.txt",
                "temporal_dependencies_different_folders_90_days.txt"),
                fileNames(folder));
    }

    @Test
    void aWindowTheAnalyzerSkippedIsNotWrittenAtAll(@TempDir File folder) {
        // At exactly 180 days the all-time analysis does not run. Writing its empty result would
        // claim, in the same bytes an empty repository produces, that nothing changes together.
        export(180, folder);

        assertEquals(Arrays.asList(
                "temporal_dependencies_180_days.txt",
                "temporal_dependencies_30_days.txt",
                "temporal_dependencies_90_days.txt",
                "temporal_dependencies_different_folders_180_days.txt",
                "temporal_dependencies_different_folders_30_days.txt",
                "temporal_dependencies_different_folders_90_days.txt"),
                fileNames(folder));
    }

    @Test
    void withoutCommitHistoryNoWindowIsExported(@TempDir File folder) {
        CodeAnalysisResults results = resultsWithPairsInEveryWindow(365);
        results.getFilesHistoryAnalysisResults().setHistory(new ArrayList<>());

        new DataExporter(null).saveTemporalDependencies(results, folder);

        assertEquals(Collections.emptyList(), fileNames(folder));
    }

    @Test
    void anAnalyzedWindowThatFoundNothingStillGetsItsHeaderOnlyFile(@TempDir File folder) throws IOException {
        // The other half of the distinction: analyzed-and-empty remains a present, empty file.
        CodeAnalysisResults results = resultsWithPairsInEveryWindow(365);
        results.getFilesHistoryAnalysisResults().setFilePairsChangedTogether90Days(new ArrayList<>());

        new DataExporter(null).saveTemporalDependencies(results, folder);

        assertEquals(HEADER, read(folder, "temporal_dependencies_90_days.txt"));
        assertEquals(HEADER + "a/one.java\tb/two.java\t1\t1\t1\n", read(folder, "temporal_dependencies_30_days.txt"));
    }

    private void export(int maxTemporalDependenciesDepthDays, File folder) {
        new DataExporter(null).saveTemporalDependencies(resultsWithPairsInEveryWindow(maxTemporalDependenciesDepthDays), folder);
    }

    private CodeAnalysisResults resultsWithPairsInEveryWindow(int maxTemporalDependenciesDepthDays) {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(CodeConfiguration.getDefaultConfiguration());
        results.getCodeConfiguration().getAnalysis().setMaxTemporalDependenciesDepthDays(maxTemporalDependenciesDepthDays);

        FilesHistoryAnalysisResults history = results.getFilesHistoryAnalysisResults();
        history.setHistory(Collections.singletonList(new FileModificationHistory("a/one.java")));
        history.setFilePairsChangedTogether30Days(pair());
        history.setFilePairsChangedTogether90Days(pair());
        history.setFilePairsChangedTogether180Days(pair());
        history.setFilePairsChangedTogether(pair());
        return results;
    }

    private List<FilePairChangedTogether> pair() {
        SourceFile file1 = new SourceFile(new File("a/one.java"));
        file1.setRelativePath("a/one.java");
        SourceFile file2 = new SourceFile(new File("b/two.java"));
        file2.setRelativePath("b/two.java");
        FilePairChangedTogether pair = new FilePairChangedTogether(file1, file2);
        pair.setCommits(new ArrayList<>(Collections.singletonList("commit-1")));
        pair.setCommitsCountFile1(1);
        pair.setCommitsCountFile2(1);
        return new ArrayList<>(Collections.singletonList(pair));
    }

    private List<String> fileNames(File folder) {
        String[] names = folder.list();
        List<String> sorted = names == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(names));
        Collections.sort(sorted);
        return sorted;
    }

    private String read(File folder, String name) throws IOException {
        return FileUtils.readFileToString(new File(folder, name), StandardCharsets.UTF_8);
    }
}
