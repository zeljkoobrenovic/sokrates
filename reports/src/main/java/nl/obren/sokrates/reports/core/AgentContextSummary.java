/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;

import java.util.*;

/**
 * Rules of thumb for how much an AI coding agent has to read to maintain the main code, from two inputs
 * only: file size (lines of code) and the git history (which main files each commit touched, and the
 * files changed together — the same temporal dependencies as the Temporal Dependencies report). Static
 * dependencies, complexity and duplication are deliberately left out (see the report's about section).
 *
 * <p>Everything here is a band, a share or a rounded range: tokens are lines × {@link #TOKENS_PER_LINE_LOW}
 * to {@link #TOKENS_PER_LINE_HIGH}. Commits touching more than {@link #MAX_FILES_PER_COMMIT} main files
 * (mass renames, reformats) are ignored, as they would create fake coupling.
 */
public class AgentContextSummary {
    public static final int TOKENS_PER_LINE_LOW = 7;
    public static final int TOKENS_PER_LINE_HIGH = 14;
    public static final int COMFORTABLE_LINES = 350;
    public static final int SHORT_READ_LINES = 1000;
    public static final int READ_WINDOW_LINES = 2000;
    public static final int MONOLITH_LINES = 5000;
    public static final int MAX_FILES_PER_COMMIT = 30;
    public static final double MIN_PARTNER_SHARE = 0.3;
    public static final int MIN_SHARED_COMMITS = 2;
    public static final int MAX_HOTSPOTS = 20;
    public static final int MIN_CHANGES_TO_SPLIT = 6;
    // Upper bound of the tokens spent on the refactoring in Edwards-Alexander's experiment (martinfowler.com, 2026).
    public static final long REFACTORING_TOKENS = 5_000_000L;
    static final int[] COMMIT_READ_BANDS = {1000, 2000, 10000, 50000};

    /** A size band: files of minLines..maxLines (maxLines = -1: no upper limit). */
    public static class Band {
        private final String label;
        private final String description;
        private final int minLines;
        private final int maxLines;
        private int count;
        private long linesOfCode;
        private int changes;
        private int aiCount;

        Band(String label, String description, int minLines, int maxLines) {
            this.label = label;
            this.description = description;
            this.minLines = minLines;
            this.maxLines = maxLines;
        }

        boolean contains(long lines) {
            return lines >= minLines && (maxLines < 0 || lines <= maxLines);
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public int getMinLines() {
            return minLines;
        }

        public int getMaxLines() {
            return maxLines;
        }

        /** Files (file bands) or commits (commit bands) in the band. */
        public int getCount() {
            return count;
        }

        public long getLinesOfCode() {
            return linesOfCode;
        }

        /** File bands: the commits (in the window) touching the band's files, counted per file. */
        public int getChanges() {
            return changes;
        }

        /** Commit bands: the AI co-authored commits in the band. */
        public int getAiCount() {
            return aiCount;
        }
    }

    /** A file changed together with another one in at least {@link #MIN_PARTNER_SHARE} of its commits. */
    public static class Partner {
        private final SourceFile file;
        private final int sharedCommits;
        private final double share;

        Partner(SourceFile file, int sharedCommits, double share) {
            this.file = file;
            this.sharedCommits = sharedCommits;
            this.share = share;
        }

        public SourceFile getFile() {
            return file;
        }

        public int getSharedCommits() {
            return sharedCommits;
        }

        public double getShare() {
            return share;
        }
    }

    /** A changed main file with its co-change partners and working set (the file plus its partners). */
    public static class FileContext {
        private final SourceFile file;
        private final int changes;
        private final List<Partner> partners = new ArrayList<>();
        private long workingSetLines;

        FileContext(SourceFile file, int changes) {
            this.file = file;
            this.changes = changes;
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

        public List<Partner> getPartners() {
            return partners;
        }

        public long getWorkingSetLines() {
            return workingSetLines;
        }

        public long getScore() {
            return (long) changes * workingSetLines;
        }

        public boolean isSplitCandidate() {
            return getLinesOfCode() > READ_WINDOW_LINES && changes >= MIN_CHANGES_TO_SPLIT;
        }

        /** Lines an agent would no longer read per change if the file were split to one read window. */
        public long getSplitSavingLines() {
            return Math.max(0, getLinesOfCode() - READ_WINDOW_LINES);
        }
    }

    private final int windowDays;
    private final List<Band> fileBands = new ArrayList<>();
    private final List<Band> commitBands = new ArrayList<>();
    private final List<FileContext> changedFiles = new ArrayList<>();
    private int commitsCount;
    private int aiCommitsCount;
    private int ignoredLargeCommitsCount;
    private int commitsTouchingLargeFiles;
    private long medianCommitLines;
    private long p90CommitLines;
    private long medianAiCommitLines;

    /** aiCommitIds: the shas of the AI co-authored commits. */
    public AgentContextSummary(List<SourceFile> mainFiles, List<FilePairChangedTogether> pairs, int windowDays, Set<String> aiCommitIds) {
        this.windowDays = windowDays;
        // Five bands, lowest first, so they map onto the five risk categories (negligible .. very high).
        fileBands.add(new Band("comfortable", "read at a glance", 0, COMFORTABLE_LINES));
        fileBands.add(new Band("one short read", "one read, small part of an agent's window", COMFORTABLE_LINES + 1, SHORT_READ_LINES));
        fileBands.add(new Band("one full read", "fills one read window", SHORT_READ_LINES + 1, READ_WINDOW_LINES));
        fileBands.add(new Band("several reads", "read in pieces, often re-read", READ_WINDOW_LINES + 1, MONOLITH_LINES));
        fileBands.add(new Band("monolith", "far beyond one read window", MONOLITH_LINES + 1, -1));
        commitBands.add(new Band("small", "", 0, COMMIT_READ_BANDS[0]));
        commitBands.add(new Band("moderate", "", COMMIT_READ_BANDS[0] + 1, COMMIT_READ_BANDS[1]));
        commitBands.add(new Band("medium", "", COMMIT_READ_BANDS[1] + 1, COMMIT_READ_BANDS[2]));
        commitBands.add(new Band("large", "", COMMIT_READ_BANDS[2] + 1, COMMIT_READ_BANDS[3]));
        commitBands.add(new Band("very large", "", COMMIT_READ_BANDS[3] + 1, -1));

        Map<String, Set<SourceFile>> filesPerCommit = filesPerCommit(mainFiles, windowDays);
        filesPerCommit.values().removeIf(files -> {
            boolean large = files.size() > MAX_FILES_PER_COMMIT;
            if (large) {
                ignoredLargeCommitsCount++;
            }
            return large;
        });

        Map<SourceFile, Integer> changesPerFile = new HashMap<>();
        List<Long> commitLines = new ArrayList<>();
        List<Long> aiCommitLines = new ArrayList<>();
        filesPerCommit.forEach((commitId, files) -> {
            long lines = files.stream().mapToLong(SourceFile::getLinesOfCode).sum();
            boolean ai = aiCommitIds.contains(sha(commitId));
            commitsCount++;
            commitLines.add(lines);
            if (ai) {
                aiCommitsCount++;
                aiCommitLines.add(lines);
            }
            if (files.stream().anyMatch(file -> file.getLinesOfCode() > READ_WINDOW_LINES)) {
                commitsTouchingLargeFiles++;
            }
            commitBands.stream().filter(band -> band.contains(lines)).findFirst().ifPresent(band -> {
                band.count++;
                band.linesOfCode += lines;
                if (ai) {
                    band.aiCount++;
                }
            });
            files.forEach(file -> changesPerFile.merge(file, 1, Integer::sum));
        });
        medianCommitLines = percentile(commitLines, 0.5);
        p90CommitLines = percentile(commitLines, 0.9);
        medianAiCommitLines = percentile(aiCommitLines, 0.5);

        mainFiles.forEach(file -> fileBands.stream().filter(band -> band.contains(file.getLinesOfCode())).findFirst().ifPresent(band -> {
            band.count++;
            band.linesOfCode += file.getLinesOfCode();
            band.changes += changesPerFile.getOrDefault(file, 0);
        }));

        Map<String, FileContext> contexts = new HashMap<>();
        changesPerFile.forEach((file, changes) -> contexts.put(file.getRelativePath(), new FileContext(file, changes)));
        addPartners(contexts, pairs, filesPerCommit.keySet());
        contexts.values().forEach(context -> {
            context.partners.sort(Comparator.comparingDouble((Partner p) -> p.share).reversed()
                    .thenComparing(p -> p.file.getRelativePath()));
            context.workingSetLines = context.getLinesOfCode()
                    + context.partners.stream().mapToLong(p -> p.file.getLinesOfCode()).sum();
        });
        changedFiles.addAll(contexts.values());
        changedFiles.sort(Comparator.comparingLong(FileContext::getScore).reversed()
                .thenComparing(c -> c.file.getRelativePath()));
    }

    /** From the analysis results; AI co-authored commits are not split out (see {@link #withAiCommits}). */
    public static AgentContextSummary of(CodeAnalysisResults results) {
        return withAiCommits(results, Collections.emptySet());
    }

    public static AgentContextSummary withAiCommits(CodeAnalysisResults results, Set<String> aiCommitIds) {
        List<SourceFile> mainFiles = results.getMainAspectAnalysisResults().getAspect() == null
                ? Collections.emptyList()
                : results.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        return new AgentContextSummary(mainFiles,
                results.getFilesHistoryAnalysisResults().getFilePairsChangedTogether(),
                results.getCodeConfiguration().getAnalysis().getMaxTemporalDependenciesDepthDays(),
                aiCommitIds);
    }

    private static Map<String, Set<SourceFile>> filesPerCommit(List<SourceFile> mainFiles, int windowDays) {
        Map<String, Set<SourceFile>> filesPerCommit = new HashMap<>();
        mainFiles.forEach(file -> {
            FileModificationHistory history = file.getFileModificationHistory();
            if (history == null) {
                return;
            }
            for (CommitInfo commit : history.getCommits()) {
                if (windowDays <= 0 || DateUtils.isDateWithinRange(commit.getDate(), windowDays)) {
                    filesPerCommit.computeIfAbsent(commit.getId(), id -> new HashSet<>()).add(file);
                }
            }
        });
        return filesPerCommit;
    }

    // A partner's share uses the commits counted here (in the window, large commits left out), not the
    // pair's all-time commitsCountFile1/2.
    private static void addPartners(Map<String, FileContext> contexts, List<FilePairChangedTogether> pairs, Set<String> commitIds) {
        pairs.forEach(pair -> {
            if (pair.getSourceFile1() == null || pair.getSourceFile2() == null) {
                return;
            }
            FileContext context1 = contexts.get(pair.getSourceFile1().getRelativePath());
            FileContext context2 = contexts.get(pair.getSourceFile2().getRelativePath());
            if (context1 == null || context2 == null || context1 == context2) {
                return;
            }
            int shared = (int) new HashSet<>(pair.getCommits()).stream().filter(commitIds::contains).count();
            if (shared < MIN_SHARED_COMMITS) {
                return;
            }
            addPartner(context1, context2, shared);
            addPartner(context2, context1, shared);
        });
    }

    private static void addPartner(FileContext context, FileContext other, int shared) {
        double share = (double) shared / context.changes;
        if (share >= MIN_PARTNER_SHARE) {
            context.partners.add(new Partner(other.file, shared, Math.min(1.0, share)));
        }
    }

    // CommitInfo ids are "<date> <sha>"; the co-author sidecar is keyed by the sha alone.
    static String sha(String commitId) {
        return commitId.substring(commitId.lastIndexOf(' ') + 1);
    }

    static long percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
    }

    public int getWindowDays() {
        return windowDays;
    }

    public List<Band> getFileBands() {
        return fileBands;
    }

    public List<Band> getCommitBands() {
        return commitBands;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public int getAiCommitsCount() {
        return aiCommitsCount;
    }

    public int getIgnoredLargeCommitsCount() {
        return ignoredLargeCommitsCount;
    }

    public int getCommitsTouchingLargeFiles() {
        return commitsTouchingLargeFiles;
    }

    public long getMedianCommitLines() {
        return medianCommitLines;
    }

    public long getP90CommitLines() {
        return p90CommitLines;
    }

    public long getMedianAiCommitLines() {
        return medianAiCommitLines;
    }

    public int getTotalFileChanges() {
        return fileBands.stream().mapToInt(Band::getChanges).sum();
    }

    public long getTotalLinesOfCode() {
        return fileBands.stream().mapToLong(Band::getLinesOfCode).sum();
    }

    /** Main files changed in the window, highest changes × working set first. */
    public List<FileContext> getChangedFiles() {
        return changedFiles;
    }

    /** The band (index into {@link #getFileBands()}, 0 = comfortable) of a file of the given size. */
    public int fileBandIndex(long lines) {
        for (int i = 0; i < fileBands.size(); i++) {
            if (fileBands.get(i).contains(lines)) {
                return i;
            }
        }
        return 0;
    }

    /** The changes in the window per main file (relative path); files not changed are absent. */
    public Map<String, Integer> changesPerFile() {
        Map<String, Integer> changes = new HashMap<>();
        changedFiles.forEach(context -> changes.put(context.getFile().getRelativePath(), context.getChanges()));
        return changes;
    }

    public List<FileContext> hotspots() {
        return changedFiles.size() > MAX_HOTSPOTS ? new ArrayList<>(changedFiles.subList(0, MAX_HOTSPOTS)) : changedFiles;
    }

    public List<FileContext> splitCandidates() {
        List<FileContext> candidates = new ArrayList<>();
        changedFiles.stream().filter(FileContext::isSplitCandidate).forEach(candidates::add);
        candidates.sort(Comparator.comparingLong((FileContext c) -> c.getChanges() * c.getSplitSavingLines()).reversed()
                .thenComparing(c -> c.file.getRelativePath()));
        return candidates;
    }

    public boolean isEmpty() {
        return commitsCount == 0;
    }

    // Rounding: every token figure is a range of lines × 7..14, the low end rounded down and the high end
    // rounded up to a "nice" number (1, 1.5, 2, 3, 5, 7 × a power of ten), so it never reads as precise.

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

    static String compact(long value) {
        if (value >= 1_000_000) {
            return trim(value / 1_000_000.0) + "M";
        }
        if (value >= 1_000) {
            return trim(value / 1_000.0) + "k";
        }
        return String.valueOf(value);
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format(Locale.US, "%.1f", value);
    }

    /** "~7k–15k" tokens for the given lines (just "under 1k" for very small numbers). */
    public static String tokenRange(long lines) {
        long low = niceFloor(lines * TOKENS_PER_LINE_LOW);
        long high = niceCeil(lines * TOKENS_PER_LINE_HIGH);
        if (high < 1000) {
            return "under 1k";
        }
        return "~" + compact(low) + "–" + compact(high);
    }

    /** The low end of the token range of the given lines ("7k"). */
    public static String tokensLow(long lines) {
        return compact(niceFloor(lines * TOKENS_PER_LINE_LOW));
    }

    /** The high end of the token range of the given lines ("30k"). */
    public static String tokensHigh(long lines) {
        return compact(niceCeil(lines * TOKENS_PER_LINE_HIGH));
    }

    /** Lines rounded to a "nice" number, for prose ("~3k lines"). */
    public static String roundedLines(long lines) {
        return lines < 100 ? String.valueOf(lines) : "~" + compact(niceCeil(lines));
    }

    /** The order of magnitude of the changes needed to earn back a refactoring that saves the given lines per change. */
    public static String paybackMagnitude(long savedLinesPerChange) {
        if (savedLinesPerChange <= 0) {
            return "never";
        }
        double changes = paybackChanges(savedLinesPerChange);
        if (changes < 3) {
            return "a couple of changes";
        } else if (changes < 10) {
            return "a handful of changes";
        } else if (changes < 100) {
            return "tens of changes";
        } else if (changes < 1000) {
            return "hundreds of changes";
        }
        return "thousands of changes";
    }

    // The geometric middle of the 7..14 tokens per line range.
    static double paybackChanges(long savedLinesPerChange) {
        double tokensPerLine = Math.sqrt(TOKENS_PER_LINE_LOW * TOKENS_PER_LINE_HIGH);
        return REFACTORING_TOKENS / (savedLinesPerChange * tokensPerLine);
    }

    /** When a split earns itself back at the file's current pace of change (empty without a time window). */
    public String paybackPace(FileContext context) {
        if (windowDays <= 0 || context.getChanges() == 0 || context.getSplitSavingLines() <= 0) {
            return "";
        }
        double changesPerYear = context.getChanges() * 365.0 / windowDays;
        double years = paybackChanges(context.getSplitSavingLines()) / changesPerYear;
        if (years <= 1) {
            return "within a year at its current pace";
        } else if (years <= 3) {
            return "in one to three years at its current pace";
        }
        return "not within a few years at its current pace";
    }

    /** The share as a whole percentage, truncated ("<1%" for small non-zero shares). */
    public static String percentage(long part, long total) {
        if (total <= 0 || part <= 0) {
            return "0%";
        }
        double value = 100.0 * part / total;
        // Truncated like the stacked bars' labels (FormattingUtils), so list and bar agree.
        return value < 1 ? "<1%" : (int) value + "%";
    }

    public String windowLabel() {
        if (windowDays <= 0) {
            return "in the whole history";
        }
        return windowDays == 365 ? "in the past year" : "in the past " + windowDays + " days";
    }
}
