/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ControlStatus;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScores;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The "health at a glance" of a repository, shown at the top of the Overview tab
 * ({@link ReportHealthSection}): a few headline tiles with a status, and the hotspots — the files most
 * worth looking at first. Pure computation over {@link CodeAnalysisResults}, no HTML.
 * <p>
 * Tile statuses use fixed bands ({@link #DUPLICATION_BANDS} etc.) on the share of code above the
 * "high" risk threshold of each distribution (so they follow the thresholds configured for the
 * analysis); the goals tile summarizes the configured controls.
 * <p>
 * A hotspot's score is its complexity (the McCabe index sum of its units, or its lines of code when the
 * language has no unit analysis) times the number of days it changed in the past year: complex code that
 * keeps changing is where defects and slow changes concentrate. Without git history the most complex
 * files are listed.
 */
public class HealthSummary {
    public static final int MAX_HOTSPOTS = 10;
    public static final int SPARKLINE_MONTHS = 12;
    static final int HOTSPOT_WINDOW_DAYS = 365;

    // {good up to, watch up to} in percent; above the second bound the status is "high".
    static final double[] DUPLICATION_BANDS = {5, 10};
    static final double[] COMPLEX_UNITS_BANDS = {5, 15};
    static final double[] LONG_UNITS_BANDS = {10, 25};
    static final double[] LARGE_FILES_BANDS = {10, 30};
    static final double[] CHANGES_IN_LARGE_FILES_BANDS = {5, 15};
    public static final int MAX_LARGE_CHANGING_FILES = 5;

    public enum Status {
        GOOD("good"), WATCH("watch"), HIGH("high"), NEUTRAL("");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class Tile {
        final String label;
        final String value;
        final String caption;
        final Status status;
        final String link;
        final String tooltip;
        final List<String> sparklineSlots;
        final List<Integer> sparklineValues;
        // A maintainability grade (A-E), shown as the grade scale in place of the status; null for other tiles
        String grade;

        Tile(String label, String value, String caption, Status status, String link, String tooltip) {
            this(label, value, caption, status, link, tooltip, Collections.emptyList(), Collections.emptyList());
        }

        Tile(String label, String value, String caption, Status status, String link, String tooltip,
             List<String> sparklineSlots, List<Integer> sparklineValues) {
            this.label = label;
            this.value = value;
            this.caption = caption;
            this.status = status;
            this.link = link;
            this.tooltip = tooltip;
            this.sparklineSlots = sparklineSlots;
            this.sparklineValues = sparklineValues;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public Status getStatus() {
            return status;
        }

        public String getGrade() {
            return grade;
        }

        public String getLink() {
            return link;
        }

        public List<Integer> getSparklineValues() {
            return sparklineValues;
        }
    }

    public static class Hotspot {
        final SourceFile file;
        final int complexity;
        final boolean complexityIsLinesOfCode;
        final int changeDays;
        final int contributors;
        final long score;

        Hotspot(SourceFile file, int complexity, boolean complexityIsLinesOfCode, int changeDays, int contributors, long score) {
            this.file = file;
            this.complexity = complexity;
            this.complexityIsLinesOfCode = complexityIsLinesOfCode;
            this.changeDays = changeDays;
            this.contributors = contributors;
            this.score = score;
        }

        public SourceFile getFile() {
            return file;
        }

        public int getComplexity() {
            return complexity;
        }

        public int getChangeDays() {
            return changeDays;
        }

        public int getContributors() {
            return contributors;
        }

        public long getScore() {
            return score;
        }
    }

    private final CodeAnalysisResults results;
    private final boolean history;

    public HealthSummary(CodeAnalysisResults results) {
        this.results = results;
        this.history = results.getContributorsAnalysisResults().getCommitsCount() > 0;
    }

    public boolean hasHistory() {
        return history;
    }

    public List<Tile> tiles() {
        List<Tile> tiles = new ArrayList<>();
        if (results.getMainAspectAnalysisResults().getFilesCount() == 0) {
            return tiles;
        }
        MaintainabilityScores scores = shownScores(results);
        if (scores != null) {
            tiles.add(scoreTile("human score*", scores.getHuman(), "for people to understand and change"));
            tiles.add(scoreTile("AI score*", scores.getAi(), scores.getContextLinesPerChange() > 0
                    ? String.format(Locale.US, "~%,d lines read per change", scores.getContextLinesPerChange())
                    : "for AI agents to understand and change"));
        }
        if (!results.skipDuplicationAnalysis() && results.getDuplicationAnalysisResults().getOverallDuplication() != null) {
            double duplication = results.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage().doubleValue();
            tiles.add(new Tile("Duplication", percentage(duplication), "of main code duplicated",
                    status(duplication, DUPLICATION_BANDS), "Duplication.html", bandsTooltip(DUPLICATION_BANDS)));
        }
        if (results.getUnitsAnalysisResults().getTotalNumberOfUnits() > 0) {
            RiskDistributionStats complexity = results.getUnitsAnalysisResults().getConditionalComplexityRiskDistribution();
            double complexShare = highShare(complexity);
            tiles.add(new Tile("Complex units", percentage(complexShare),
                    "of unit code with McCabe > " + complexity.getHighRiskThreshold(),
                    status(complexShare, COMPLEX_UNITS_BANDS), "ConditionalComplexity.html", bandsTooltip(COMPLEX_UNITS_BANDS)));
            RiskDistributionStats unitSize = results.getUnitsAnalysisResults().getUnitSizeRiskDistribution();
            double longShare = highShare(unitSize);
            tiles.add(new Tile("Long units", percentage(longShare),
                    "of unit code in units > " + unitSize.getHighRiskThreshold() + " lines",
                    status(longShare, LONG_UNITS_BANDS), "UnitSize.html", bandsTooltip(LONG_UNITS_BANDS)));
        }
        RiskDistributionStats fileSize = results.getFilesAnalysisResults().getOverallFileSizeDistribution();
        if (fileSize != null && fileSize.getTotalValue() > 0) {
            double largeShare = highShare(fileSize);
            tiles.add(new Tile("Large files", percentage(largeShare),
                    "of main code in files > " + fileSize.getHighRiskThreshold() + " lines",
                    status(largeShare, LARGE_FILES_BANDS), "FileSize.html", bandsTooltip(LARGE_FILES_BANDS)));
        }
        if (history) {
            Tile changesInLargeFiles = changesInLargeFilesTile(largeFileReads());
            if (changesInLargeFiles != null) {
                tiles.add(changesInLargeFiles);
            }
            addActivityTiles(tiles);
        }
        Tile goals = goalsTile();
        if (goals != null) {
            tiles.add(goals);
        }
        return tiles;
    }

    /** The file reads for changes when the File Size section is enabled (analysis.fileReadsForChanges), else null. */
    public FileReadsForChanges largeFileReads() {
        return results.getCodeConfiguration().getAnalysis().getFileReadsForChanges().isEnabled() ? FileReadsForChanges.of(results) : null;
    }

    /** The changed files over the large-file size, for the conditional "Large files that change often" list. */
    public List<FileReadsForChanges.ChangedFile> largeChangingFiles() {
        FileReadsForChanges reads = history ? largeFileReads() : null;
        return reads == null ? Collections.emptyList() : reads.largeFiles(MAX_LARGE_CHANGING_FILES);
    }

    // The share of the window's changes that touched a file larger than an agent reads at once; null without changes.
    static Tile changesInLargeFilesTile(FileReadsForChanges reads) {
        if (reads == null || reads.getTotalChanges() == 0) {
            return null;
        }
        double share = 100.0 * reads.getChangesInLargeFiles() / reads.getTotalChanges();
        return new Tile("Changes in large files", percentage(share),
                "of changes (" + reads.windowShortLabel() + ") to files > " + String.format(Locale.US, "%,d", reads.getLargeFileLines()) + " lines",
                status(share, CHANGES_IN_LARGE_FILES_BANDS), "FileSize.html",
                "People and AI agents read large files in pieces for every change. " + bandsTooltip(CHANGES_IN_LARGE_FILES_BANDS));
    }

    /** The scores when they exist and analysis.maintainabilityScores.show is on (they stay in the data either way), else null. */
    public static MaintainabilityScores shownScores(CodeAnalysisResults results) {
        MaintainabilityScores scores = results.getMaintainabilityScores();
        boolean show = results.getCodeConfiguration() == null || results.getCodeConfiguration().getAnalysis().getMaintainabilityScores().isShow();
        return show && scores != null && scores.getHuman() != null && scores.getAi() != null ? scores : null;
    }

    // No link: the breakdown card is right below the tiles. A score missing sub-scores says so instead of its caption.
    static Tile scoreTile(String label, MaintainabilityScore score, String caption) {
        String value = String.format(Locale.US, "%.1f", score.getValue());
        String tooltip = "0-10, grade " + score.getGrade() + " (A: 8+, B: 6.5+, C: 5+, D: 3.5+, E: below)"
                + (score.getCoverageText().isEmpty() ? "" : "; " + score.getCoverageText());
        if (!score.isFullyMeasured()) {
            caption = "measured on " + score.getSubScores().size() + " of " + score.getSubScoresTotal() + " sub-scores";
        }
        Tile tile = new Tile(label, value, caption, gradeStatus(score.getGrade()), null, tooltip);
        tile.grade = score.getGrade();
        return tile;
    }

    /** A and B good, C watch, D and E high: follows the grade, so configured grade thresholds carry over. */
    static Status gradeStatus(String grade) {
        return "A".equals(grade) || "B".equals(grade) ? Status.GOOD : ("C".equals(grade) ? Status.WATCH : Status.HIGH);
    }

    static Status scoreStatus(double score) {
        return score >= 6.5 ? Status.GOOD : (score >= 5 ? Status.WATCH : Status.HIGH);
    }

    private void addActivityTiles(List<Tile> tiles) {
        ContributorsAnalysisResults contributors = results.getContributorsAnalysisResults();
        ContributorsReportUtils.WindowTotals last30Days = ContributorsReportUtils
                .buildActivitySummary(contributors, null, new int[]{30}, 1).windows[0];
        List<String> months = pastMonths();
        Map<String, ContributionTimeSlot> perMonth = new HashMap<>();
        contributors.getContributorsPerMonth().forEach(slot -> perMonth.put(slot.getTimeSlot(), slot));
        List<Integer> commits = months.stream()
                .map(m -> perMonth.containsKey(m) ? perMonth.get(m).getCommitsCount() : 0).collect(Collectors.toList());
        List<Integer> people = months.stream()
                .map(m -> perMonth.containsKey(m) ? perMonth.get(m).getContributorsCount() : 0).collect(Collectors.toList());
        tiles.add(new Tile("Commits", String.format(Locale.US, "%,d", last30Days.commits), "in the past 30 days",
                Status.NEUTRAL, "Commits.html", "Bars: commits per month, past 12 months", months, commits));
        tiles.add(new Tile("Contributors", String.format(Locale.US, "%,d", last30Days.contributors), "active in the past 30 days",
                Status.NEUTRAL, "Contributors.html", "Bars: contributors per month, past 12 months", months, people));
    }

    private Tile goalsTile() {
        List<ControlStatus> statuses = new ArrayList<>();
        results.getControlResults().getGoalsAnalysisResults().forEach(goal -> statuses.addAll(goal.getControlStatuses()));
        if (statuses.isEmpty()) {
            return null;
        }
        long ok = statuses.stream().filter(s -> "OK".equalsIgnoreCase(s.getStatus())).count();
        long failed = statuses.stream().filter(s -> "FAILED".equalsIgnoreCase(s.getStatus())).count();
        Status status = failed > 0 ? Status.HIGH : (ok < statuses.size() ? Status.WATCH : Status.GOOD);
        return new Tile("Goals", ok + " / " + statuses.size(), "controls in range",
                status, "Controls.html", "Controls from goalsAndControls in config.json");
    }

    /** The files most worth looking at first, highest score first (at most {@link #MAX_HOTSPOTS}). */
    public List<Hotspot> hotspots() {
        List<SourceFile> files = results.getMainAspectAnalysisResults().getAspect() == null
                ? Collections.emptyList()
                : results.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        return hotspots(files, history, MAX_HOTSPOTS);
    }

    static List<Hotspot> hotspots(List<SourceFile> files, boolean history, int max) {
        boolean anyUnits = files.stream().anyMatch(f -> f.getUnitsMcCabeIndexSum() > 0);
        List<Hotspot> hotspots = new ArrayList<>();
        for (SourceFile file : files) {
            int complexity = anyUnits ? file.getUnitsMcCabeIndexSum() : file.getLinesOfCode();
            if (complexity <= 0) {
                continue;
            }
            FileModificationHistory fileHistory = file.getFileModificationHistory();
            int changeDays = fileHistory == null ? 0 : (int) fileHistory.getDates().stream()
                    .filter(date -> DateUtils.isDateWithinRange(date, HOTSPOT_WINDOW_DAYS)).count();
            int contributors = fileHistory == null ? 0 : fileHistory.countContributors();
            if (history && changeDays == 0) {
                continue;
            }
            long score = history ? (long) complexity * changeDays : complexity;
            hotspots.add(new Hotspot(file, complexity, !anyUnits, changeDays, contributors, score));
        }
        hotspots.sort(Comparator.comparingLong((Hotspot h) -> h.score).reversed()
                .thenComparing(h -> h.file.getRelativePath()));
        return hotspots.size() > max ? new ArrayList<>(hotspots.subList(0, max)) : hotspots;
    }

    static List<String> pastMonths() {
        String reference = new SimpleDateFormat("yyyy-MM-dd").format(DateUtils.getCalendar().getTime());
        List<String> months = new ArrayList<>(DateUtils.getPastMonths(SPARKLINE_MONTHS - 1, reference));
        Collections.reverse(months);
        return months;
    }

    static double highShare(RiskDistributionStats stats) {
        int total = stats.getTotalValue();
        return total == 0 ? 0 : 100.0 * (stats.getHighRiskValue() + stats.getVeryHighRiskValue()) / total;
    }

    static Status status(double value, double[] bands) {
        return value <= bands[0] ? Status.GOOD : (value <= bands[1] ? Status.WATCH : Status.HIGH);
    }

    static String percentage(double value) {
        if (value > 0 && value < 1) {
            return "<1%";
        }
        return Math.round(value) + "%";
    }

    private static String bandsTooltip(double[] bands) {
        return "good: up to " + Math.round(bands[0]) + "%, watch: up to " + Math.round(bands[1]) + "%, high: above";
    }
}
