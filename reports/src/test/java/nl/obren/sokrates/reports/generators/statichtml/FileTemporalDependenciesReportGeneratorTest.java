/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextFragment;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "data..." links under each co-change tab.
 *
 * <p>Every tab used to link to text/temporal_dependencies.txt and its different-folders sibling -
 * the all-time files - because the tab was never consulted when building the link. On a default
 * configuration that silently served all-time data from the 30-day tab, which is the one that opens
 * first; below 181 days it served a file the analyzer had left empty.
 */
class FileTemporalDependenciesReportGeneratorTest {

    private static final Pattern DATA_FILE = Pattern.compile("downloadDataFile\\('text/(temporal_dependencies[^']*)'\\)");

    @Test
    void eachTabLinksToTheDataFileOfItsOwnWindow(@TempDir File reportsFolder) {
        RichTextReport report = renderWith(365, reportsFolder);

        // In tab order, each tab contributing its own pair of files.
        assertEquals(Arrays.asList(
                "temporal_dependencies_30_days.txt", "temporal_dependencies_different_folders_30_days.txt",
                "temporal_dependencies_90_days.txt", "temporal_dependencies_different_folders_90_days.txt",
                "temporal_dependencies_180_days.txt", "temporal_dependencies_different_folders_180_days.txt",
                "temporal_dependencies.txt", "temporal_dependencies_different_folders.txt"),
                linkedDataFiles(report));
    }

    @Test
    void aWindowTheAnalyzerSkippedGetsNoTabAndNoLink(@TempDir File reportsFolder) {
        // At 180 days the all-time analysis does not run. Rendering its (empty) result as a tab would
        // report "no file pairs changed together" as a finding, and link a file that is not written.
        RichTextReport report = renderWith(180, reportsFolder);

        String html = htmlOf(report);
        assertTrue(html.contains("temporal_dependencies_180_days.txt"), html);
        assertEquals(Collections.emptyList(), linkedDataFiles(report).stream()
                .filter(name -> name.equals("temporal_dependencies.txt")
                        || name.equals("temporal_dependencies_different_folders.txt"))
                .collect(Collectors.toList()));
    }

    @Test
    void withoutCommitHistoryNothingIsPresentedAsAnalyzed(@TempDir File reportsFolder) {
        CodeAnalysisResults results = resultsWithPairsInEveryWindow();
        results.getFilesHistoryAnalysisResults().setHistory(new ArrayList<>());

        RichTextReport report = new RichTextReport("", "");
        new FileTemporalDependenciesReportGenerator(results).addTemporalDependenciesToReport(reportsFolder, report);

        assertEquals(Collections.emptyList(), linkedDataFiles(report));
        assertTrue(htmlOf(report).contains("no commit history is available"), htmlOf(report));
    }

    @Test
    void aDepthShorterThanTheShortestWindowSaysSoRatherThanShowingAnEmptyTab(@TempDir File reportsFolder) {
        RichTextReport report = renderWith(10, reportsFolder);

        assertEquals(Collections.emptyList(), linkedDataFiles(report));
        assertTrue(htmlOf(report).contains("shorter than the shortest window"), htmlOf(report));
    }

    private RichTextReport renderWith(int maxTemporalDependenciesDepthDays, File reportsFolder) {
        CodeAnalysisResults results = resultsWithPairsInEveryWindow();
        results.getCodeConfiguration().getAnalysis().setMaxTemporalDependenciesDepthDays(maxTemporalDependenciesDepthDays);

        RichTextReport report = new RichTextReport("", "");
        new FileTemporalDependenciesReportGenerator(results).addTemporalDependenciesToReport(reportsFolder, report);
        return report;
    }

    /** Every window holds one pair, in two different folders so both link sites render. */
    private CodeAnalysisResults resultsWithPairsInEveryWindow() {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(CodeConfiguration.getDefaultConfiguration());

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

    private List<String> linkedDataFiles(RichTextReport report) {
        List<String> names = new ArrayList<>();
        Matcher matcher = DATA_FILE.matcher(htmlOf(report));
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private String htmlOf(RichTextReport report) {
        return report.getRichTextFragments().stream()
                .map(RichTextFragment::getFragment)
                .collect(Collectors.joining("\n"));
    }
}
