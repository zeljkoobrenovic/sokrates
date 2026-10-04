/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.FileReadsForChangesConfig;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;

import java.util.*;

/**
 * The main files read most for changes: to change a file, an AI coding agent (or a person) has to read it,
 * so a file is read lines × changes for the changes of the window (every commit counted, at the file's current
 * size). Shown in the File Size report's "Large Files That Change Often" table, with the tokens to read the
 * file, the lines edited per change (the file's churn per commit over the whole history) and the lines read
 * per line changed. Numbers are rounded rules of thumb. Configured by {@code analysis.fileReadsForChanges}
 * (window, list size, tokens per line); invalid values fall back to sensible ones.
 */
public class FileReadsForChanges {

    /** A main file changed in the window. */
    public static class ChangedFile {
        private final SourceFile file;
        private final int changes;
        private final double editedLinesPerChange;

        ChangedFile(SourceFile file, int changes, double editedLinesPerChange) {
            this.file = file;
            this.changes = changes;
            this.editedLinesPerChange = editedLinesPerChange;
        }

        public SourceFile getFile() {
            return file;
        }

        public int getLinesOfCode() {
            return file.getLinesOfCode();
        }

        public int getChanges() {
            return changes;
        }

        /** Lines read for the changes: lines × changes. */
        public long getReadLines() {
            return (long) changes * file.getLinesOfCode();
        }

        /** The file's lines added + deleted per commit (whole history); 0 without churn data. */
        public double getEditedLinesPerChange() {
            return editedLinesPerChange;
        }

        /** Lines read per line changed; 0 without churn data. */
        public double getReadPerEditedLine() {
            return editedLinesPerChange > 0 ? file.getLinesOfCode() / editedLinesPerChange : 0;
        }
    }

    private final int windowDays;
    private final int maxFiles;
    private final int tokensPerLineMin;
    private final int tokensPerLineMax;
    private final List<ChangedFile> files = new ArrayList<>();

    public FileReadsForChanges(List<SourceFile> mainFiles, FileReadsForChangesConfig config) {
        this.windowDays = config.getWindowDays();
        this.maxFiles = Math.max(1, config.getMaxFiles());
        this.tokensPerLineMin = Math.max(1, Math.min(config.getTokensPerLineMin(), config.getTokensPerLineMax()));
        this.tokensPerLineMax = Math.max(tokensPerLineMin, Math.max(config.getTokensPerLineMin(), config.getTokensPerLineMax()));
        mainFiles.forEach(file -> {
            FileModificationHistory history = file.getFileModificationHistory();
            if (history == null) {
                return;
            }
            Set<String> allCommits = new HashSet<>();
            Set<String> windowCommits = new HashSet<>();
            for (CommitInfo commit : history.getCommits()) {
                allCommits.add(commit.getId());
                if (windowDays <= 0 || DateUtils.isDateWithinRange(commit.getDate(), windowDays)) {
                    windowCommits.add(commit.getId());
                }
            }
            if (!windowCommits.isEmpty()) {
                int churn = history.getLinesAdded() + history.getLinesDeleted();
                files.add(new ChangedFile(file, windowCommits.size(), churn > 0 ? (double) churn / allCommits.size() : 0));
            }
        });
        files.sort(Comparator.comparingLong(ChangedFile::getReadLines).reversed()
                .thenComparing(changed -> changed.file.getRelativePath()));
    }

    public static FileReadsForChanges of(CodeAnalysisResults results) {
        List<SourceFile> mainFiles = results.getMainAspectAnalysisResults().getAspect() == null
                ? Collections.emptyList()
                : results.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        return new FileReadsForChanges(mainFiles, results.getCodeConfiguration().getAnalysis().getFileReadsForChanges());
    }

    /** The files read most for changes (lines × changes), at most maxFiles. */
    public List<ChangedFile> topFiles() {
        return files.size() > maxFiles ? new ArrayList<>(files.subList(0, maxFiles)) : files;
    }

    public int getTokensPerLineMin() {
        return tokensPerLineMin;
    }

    public int getTokensPerLineMax() {
        return tokensPerLineMax;
    }

    /** "~4k–9k" tokens to read the given lines, with the configured tokens per line. */
    public String tokenRange(long lines) {
        return tokenRange(lines, tokensPerLineMin, tokensPerLineMax);
    }

    public String windowLabel() {
        if (windowDays <= 0) {
            return "in the whole history";
        }
        return windowDays == 365 ? "in the past year" : "in the past " + windowDays + " days";
    }

    /** "1y", "90d" or "all", for short labels like "changes (1y)". */
    public String windowShortLabel() {
        if (windowDays <= 0) {
            return "all";
        }
        return windowDays == 365 ? "1y" : windowDays + "d";
    }

    // Rounding: numbers are rounded to "nice" values (1, 1.5, 2, 3, 5, 7 × a power of ten) and token figures
    // are ranges of lines × 7..14, the low end rounded down and the high end up, so nothing reads as precise.

    private static final double[] NICE = {1, 1.5, 2, 3, 5, 7, 10};

    static long niceFloor(long value) {
        if (value <= 0) {
            return 0;
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        double normalized = value / magnitude;
        double result = NICE[0];
        for (double nice : NICE) {
            if (nice <= normalized + 1e-9) {
                result = nice;
            }
        }
        return Math.round(result * magnitude);
    }

    static long niceCeil(long value) {
        if (value <= 0) {
            return 0;
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        double normalized = value / magnitude;
        for (double nice : NICE) {
            if (nice >= normalized - 1e-9) {
                return Math.round(nice * magnitude);
            }
        }
        return Math.round(10 * magnitude);
    }

    /** The nice value closest to the given one. */
    static long niceRound(double value) {
        if (value < 1) {
            return Math.round(value);
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        double best = magnitude;
        for (double nice : NICE) {
            if (Math.abs(nice * magnitude - value) < Math.abs(best - value)) {
                best = nice * magnitude;
            }
        }
        return Math.round(best);
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format(Locale.US, "%.1f", value);
    }

    private static String kilo(long value) {
        return value >= 1_000_000 ? trim(value / 1_000_000.0) + "M" : value >= 1000 ? trim(value / 1000.0) + "k" : String.valueOf(value);
    }

    /** "~4k–9k" tokens to read the given lines at min..max tokens per line ("under 1k" for very small numbers). */
    static String tokenRange(long lines, int tokensPerLineMin, int tokensPerLineMax) {
        long low = niceFloor(lines * tokensPerLineMin);
        long high = niceCeil(lines * tokensPerLineMax);
        if (high < 1000) {
            return "under 1k";
        }
        return low == high ? "~" + kilo(low) : "~" + kilo(low) + "–" + kilo(high);
    }

    /** "~30" for a number, rounded to a nice value. */
    public static String about(double value) {
        long rounded = niceRound(value);
        return "~" + (rounded >= 10_000 ? kilo(rounded) : String.format(Locale.US, "%,d", rounded));
    }
}
