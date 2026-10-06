package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class HealthSummaryTest {

    @BeforeEach
    void fixTheAnalysisDate() {
        DateUtils.reset();
        DateUtils.setDateParam("2026-10-01");
    }

    @AfterEach
    void resetTheAnalysisDate() {
        DateUtils.reset();
        DateUtils.setDateParam(null);
    }

    private static SourceFile file(String path, int loc, int mcCabe, String... changeDatesAndEmails) {
        SourceFile file = new SourceFile();
        file.setRelativePath(path);
        file.setLinesOfCode(loc);
        file.setUnitsMcCabeIndexSum(mcCabe);
        if (changeDatesAndEmails.length > 0) {
            FileModificationHistory history = new FileModificationHistory(path);
            List<String> dates = new ArrayList<>();
            List<CommitInfo> commits = new ArrayList<>();
            for (int i = 0; i < changeDatesAndEmails.length; i += 2) {
                dates.add(changeDatesAndEmails[i]);
                CommitInfo commit = new CommitInfo("c" + i, changeDatesAndEmails[i]);
                commit.setEmail(changeDatesAndEmails[i + 1]);
                commits.add(commit);
            }
            history.setDates(dates);
            history.setCommits(commits);
            file.setFileModificationHistory(history);
        }
        return file;
    }

    private static List<String> paths(List<HealthSummary.Hotspot> hotspots) {
        return hotspots.stream().map(h -> h.getFile().getRelativePath()).collect(Collectors.toList());
    }

    @Test
    void ranksByComplexityTimesChangeDaysInThePastYear() {
        List<SourceFile> files = Arrays.asList(
                file("a/Complex.java", 500, 100, "2026-09-01", "x@a", "2026-08-01", "y@a"),       // 100 x 2 = 200
                file("a/Busy.java", 200, 20, "2026-09-01", "x@a", "2026-09-02", "x@a", "2026-09-03", "x@a",
                        "2026-09-04", "x@a", "2026-09-05", "x@a", "2026-09-06", "x@a", "2026-09-07", "x@a",
                        "2026-09-08", "x@a", "2026-09-09", "x@a", "2026-09-10", "x@a", "2026-09-11", "x@a"), // 20 x 11 = 220
                file("a/Old.java", 900, 300, "2024-01-01", "x@a"),                                 // no change in the past year
                file("a/Simple.java", 50, 0, "2026-09-01", "x@a"));                                 // no units complexity

        List<HealthSummary.Hotspot> hotspots = HealthSummary.hotspots(files, true, 10);

        assertEquals(Arrays.asList("a/Busy.java", "a/Complex.java"), paths(hotspots));
        assertEquals(220, hotspots.get(0).getScore());
        assertEquals(11, hotspots.get(0).getChangeDays());
        assertEquals(1, hotspots.get(0).getContributors());
        assertEquals(2, hotspots.get(1).getContributors());
    }

    @Test
    void withoutHistoryListsTheMostComplexFilesAndCapsTheList() {
        List<SourceFile> files = Arrays.asList(file("A.java", 10, 5), file("B.java", 10, 50), file("C.java", 10, 20));

        assertEquals(Arrays.asList("B.java", "C.java"), paths(HealthSummary.hotspots(files, false, 2)));
    }

    @Test
    void usesLinesOfCodeWhenNoFileHasUnits() {
        List<SourceFile> files = Arrays.asList(file("small.sql", 10, 0, "2026-09-01", "x@a"), file("big.sql", 300, 0, "2026-09-01", "x@a"));

        List<HealthSummary.Hotspot> hotspots = HealthSummary.hotspots(files, true, 10);

        assertEquals(Arrays.asList("big.sql", "small.sql"), paths(hotspots));
        assertEquals(300, hotspots.get(0).getComplexity());
    }

    @Test
    void statusBandsAndShares() {
        assertEquals(HealthSummary.Status.GOOD, HealthSummary.status(5, HealthSummary.DUPLICATION_BANDS));
        assertEquals(HealthSummary.Status.WATCH, HealthSummary.status(5.1, HealthSummary.DUPLICATION_BANDS));
        assertEquals(HealthSummary.Status.HIGH, HealthSummary.status(10.5, HealthSummary.DUPLICATION_BANDS));

        RiskDistributionStats stats = new RiskDistributionStats(5, 10, 25, 50);
        stats.update(3, 50);   // negligible
        stats.update(30, 30);  // high (> 25)
        stats.update(60, 20);  // very high (> 50)
        assertEquals(50.0, HealthSummary.highShare(stats), 0.001);
        assertEquals(0.0, HealthSummary.highShare(new RiskDistributionStats(5, 10, 25, 50)));

        assertEquals("<1%", HealthSummary.percentage(0.4));
        assertEquals("0%", HealthSummary.percentage(0));
        assertEquals("12%", HealthSummary.percentage(11.6));
    }

    @Test
    void sparklineMonthsEndAtTheAnalysisMonth() {
        List<String> months = HealthSummary.pastMonths();

        assertEquals(12, months.size());
        assertEquals("2026-10", months.get(11));
        assertEquals("2025-11", months.get(0));
    }

    @Test
    void hotspotRowEscapesTheRepositoryPath() {
        SourceFile file = file("src/<img src=x onerror=alert(1)>/a&b.java", 10, 5, "2026-09-01", "x@a");
        HealthSummary.Hotspot hotspot = HealthSummary.hotspots(Arrays.asList(file), true, 1).get(0);

        String html = ReportHealthSection.hotspot(1, hotspot, hotspot.getScore(), true, true);

        assertFalse(html.contains("<img"), html);
        assertTrue(html.contains("a&amp;b.java"), html);
        assertTrue(html.contains("../src/viewer.html#aspect=main&file=src/%3Cimg%20src%3Dx%20onerror%3Dalert%281%29%3E/a%26b.java"), html);
        assertTrue(html.contains("<span class='sk-chip sk-chip-warn'>1 contributor</span>"), "a single contributor is flagged");
    }

    @Test
    void sparklineHasOneBarPerMonth() {
        String svg = ReportHealthSection.sparkline(Arrays.asList("2026-08", "2026-09", "2026-10"), Arrays.asList(0, 4, 2));

        assertEquals(3, svg.split("<rect").length - 1);
        assertTrue(svg.contains("<title>2026-09: 4</title>"));
    }

    @Test
    void changesInLargeFilesTileShowsTheShareOfChangesTouchingLargeFiles() {
        nl.obren.sokrates.sourcecode.core.FileReadsForChangesConfig config = new nl.obren.sokrates.sourcecode.core.FileReadsForChangesConfig();
        FileReadsForChanges reads = new FileReadsForChanges(Arrays.asList(
                file("Small.java", 300, 0, "2026-09-01", "x@a", "2026-09-02", "x@a", "2026-09-03", "x@a"),
                file("Huge.java", 6000, 0, "2026-09-04", "x@a")), config);

        HealthSummary.Tile tile = HealthSummary.changesInLargeFilesTile(reads);

        assertEquals("Changes in large files", tile.getLabel());
        assertEquals("25%", tile.getValue());
        assertEquals(HealthSummary.Status.HIGH, tile.getStatus());
        assertEquals("FileSize.html", tile.getLink());
        assertNull(HealthSummary.changesInLargeFilesTile(new FileReadsForChanges(Collections.emptyList(), config)));
        assertNull(HealthSummary.changesInLargeFilesTile(null));
    }

    @Test
    void scoreTilesHaveAGradeAStatusAndNoLink() {
        nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore score = new nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore();
        score.setValue(5.4);
        score.setGrade("C");

        HealthSummary.Tile tile = HealthSummary.scoreTile("AI Score*", score, "caption");

        assertEquals("5.4", tile.getValue());
        assertEquals("C", tile.getGrade());
        assertEquals(HealthSummary.Status.WATCH, tile.getStatus());
        assertNull(tile.getLink());
        String html = ReportHealthSection.tile(tile);
        assertTrue(html.startsWith("<div class='sk-tile'"));
        assertTrue(html.endsWith("</div>"));
        assertFalse(html.contains("href"));
        assertTrue(html.contains("<span class='sk-grade sk-grade-c sk-grade-on'>C</span>"));
        assertTrue(html.contains("<span class='sk-grade sk-grade-a'>A</span>"));
    }

    @Test
    void hiddenScoresStayInTheDataButAreNotShown() {
        nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults results = new nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults();
        results.setCodeConfiguration(new nl.obren.sokrates.sourcecode.core.CodeConfiguration());
        nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScores scores = new nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScores();
        scores.setHuman(new nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore());
        scores.setAi(new nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore());
        results.setMaintainabilityScores(scores);

        assertSame(scores, HealthSummary.shownScores(results));

        results.getCodeConfiguration().getAnalysis().getMaintainabilityScores().setShow(false);
        assertNull(HealthSummary.shownScores(results));
        assertSame(scores, results.getMaintainabilityScores());
    }
}
