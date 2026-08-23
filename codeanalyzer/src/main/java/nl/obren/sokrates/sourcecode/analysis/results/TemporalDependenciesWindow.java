/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The "files changed together" (co-change) windows that FileHistoryAnalyzer computes, and the
 * condition under which each one is computed for a given maxTemporalDependenciesDepthDays.
 *
 * <p>The analyzer runs a window only when the configured depth reaches it, so at a depth of e.g. 90
 * days the 180-day and all-time results are never populated. Consumers - the data exporter and the
 * temporal dependencies report - must ask the same question before presenting a window, otherwise a
 * window that was never analyzed is indistinguishable from one that was analyzed and found nothing.
 *
 * <p>The predicates mirror the branches in FileHistoryAnalyzer.analyze();
 * TemporalDependenciesWindowTest pins them to the analyzer so the two cannot drift.
 */
public enum TemporalDependenciesWindow {
    PAST_30_DAYS("30_days", "_30_days", 30, true, FilesHistoryAnalysisResults::getFilePairsChangedTogether30Days),
    PAST_90_DAYS("90_days", "_90_days", 90, true, FilesHistoryAnalysisResults::getFilePairsChangedTogether90Days),
    PAST_180_DAYS("180_days", "_180_days", 180, true, FilesHistoryAnalysisResults::getFilePairsChangedTogether180Days),
    // The all-time window is only worth computing when the configured depth is strictly wider than
    // the fixed 180-day window; at exactly 180 days it would duplicate PAST_180_DAYS.
    ALL_TIME("all_time", "", 180, false, FilesHistoryAnalysisResults::getFilePairsChangedTogether);

    private final String id;
    private final String fileNameSuffix;
    private final int depthDays;
    private final boolean inclusive;
    private final Function<FilesHistoryAnalysisResults, List<FilePairChangedTogether>> accessor;

    TemporalDependenciesWindow(String id, String fileNameSuffix, int depthDays, boolean inclusive,
                               Function<FilesHistoryAnalysisResults, List<FilePairChangedTogether>> accessor) {
        this.id = id;
        this.fileNameSuffix = fileNameSuffix;
        this.depthDays = depthDays;
        this.inclusive = inclusive;
        this.accessor = accessor;
    }

    /**
     * The tab id used in the temporal dependencies report, and the suffix identifying the window
     * elsewhere (e.g. in visualisation file names).
     */
    public String getId() {
        return id;
    }

    public int getDepthDays() {
        return depthDays;
    }

    /**
     * True if FileHistoryAnalyzer computes this window at the given configured depth.
     */
    public boolean isAnalyzed(int maxTemporalDependenciesDepthDays) {
        return inclusive
                ? maxTemporalDependenciesDepthDays >= depthDays
                : maxTemporalDependenciesDepthDays > depthDays;
    }

    public List<FilePairChangedTogether> getFilePairs(FilesHistoryAnalysisResults results) {
        return accessor.apply(results);
    }

    public String getDataFileName() {
        return "temporal_dependencies" + fileNameSuffix + ".txt";
    }

    public String getDifferentFoldersDataFileName() {
        return "temporal_dependencies_different_folders" + fileNameSuffix + ".txt";
    }

    /**
     * The windows that were actually computed for these results - in window order, shortest first.
     *
     * <p>Empty when there is no commit history at all: in that case the analyzer never entered the
     * co-change block, so every window's empty result is an artefact of the missing history rather
     * than a statement about the code. Consumers must present only these windows.
     */
    public static List<TemporalDependenciesWindow> analyzedWindows(FilesHistoryAnalysisResults results,
                                                                   int maxTemporalDependenciesDepthDays) {
        List<TemporalDependenciesWindow> analyzed = new ArrayList<>();
        if (!results.hasHistory()) {
            return analyzed;
        }
        for (TemporalDependenciesWindow window : values()) {
            if (window.isAnalyzed(maxTemporalDependenciesDepthDays)) {
                analyzed.add(window);
            }
        }
        return analyzed;
    }
}
