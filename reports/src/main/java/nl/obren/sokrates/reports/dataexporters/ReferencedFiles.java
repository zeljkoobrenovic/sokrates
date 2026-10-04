/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.reports.core.HealthSummary;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;

import java.util.HashSet;
import java.util.Set;

/**
 * The files whose source the shared viewer ({@code src/viewer.html}) caches: the ones the reports link
 * to — the top-N file lists, the files of the longest duplicates and the Overview hotspots. One list for
 * {@link SourceViewerExporter}, which caches them, and the explorers, which link only to cached files.
 */
public class ReferencedFiles {
    private ReferencedFiles() {
    }

    public static Set<SourceFile> of(CodeAnalysisResults results) {
        Set<SourceFile> referenced = new HashSet<>();
        referenced.addAll(results.getFilesAnalysisResults().getLongestFiles());
        referenced.addAll(results.getFilesAnalysisResults().getFilesWithMostUnits());
        referenced.addAll(results.getFilesAnalysisResults().getMostComplexFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getFilesWithLeastContributors());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getFilesWithMostContributors());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getMostChangedFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getOldestFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getMostPreviouslyChangedFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getMostRecentlyChangedFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getYoungestFiles());
        results.getDuplicationAnalysisResults().getLongestDuplicates().forEach(d ->
                d.getDuplicatedFileBlocks().forEach(b -> referenced.add(b.getSourceFile())));
        new HealthSummary(results).hotspots().forEach(hotspot -> referenced.add(hotspot.getFile()));
        return referenced;
    }
}
