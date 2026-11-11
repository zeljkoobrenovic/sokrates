package nl.obren.sokrates.sourcecode.contributors;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ContributorTest {

    @Test
    void isRookieAtDate() {
        Contributor c = new Contributor();

        c.setFirstCommitDate("2018-04-05");

        assertTrue(c.isRookieAtDate("2017-04-05"));
        assertTrue(c.isRookieAtDate("2018-04-04"));
        assertTrue(c.isRookieAtDate("2018-04-05"));
        assertTrue(c.isRookieAtDate("2018-04-06"));
        assertTrue(c.isRookieAtDate("2019-04-05"));
        assertFalse(c.isRookieAtDate("2019-04-06"));
        assertFalse(c.isRookieAtDate("2019-07-06"));
        assertFalse(c.isRookieAtDate("2020-02-15"));
        assertFalse(c.isRookieAtDate("2021-03-11"));
    }

    @Test
    void addCommitAccumulatesChurnAcrossCommits() {
        Contributor c = new Contributor("a@example.com");

        c.addCommit("2020-01-01", 1, 10, 4);
        c.addCommit("2020-01-02", 2, 5, 3);
        c.addCommit("2020-01-02", 1); // no-churn overload contributes 0

        assertEquals(15, c.getLinesAdded());
        assertEquals(7, c.getLinesDeleted());
    }

    @Test
    void addChurnMergesTotals() {
        Contributor c = new Contributor("a@example.com");
        c.addCommit("2020-01-01", 1, 10, 4);

        c.addChurn(100, 50, 7, 2, 30, 12, 55, 22, 80, 40);

        assertEquals(110, c.getLinesAdded());
        assertEquals(54, c.getLinesDeleted());
        assertEquals(7, c.getLinesAdded30Days());
        assertEquals(2, c.getLinesDeleted30Days());
        assertEquals(30, c.getLinesAdded90Days());
        assertEquals(12, c.getLinesDeleted90Days());
        assertEquals(55, c.getLinesAdded180Days());
        assertEquals(22, c.getLinesDeleted180Days());
        assertEquals(80, c.getLinesAdded365Days());
        assertEquals(40, c.getLinesDeleted365Days());
    }

    @Test
    void addCommitDeduplicatesDatesAndTracksTotalCount() {
        Contributor c = new Contributor("a@example.com");

        c.addCommit("2020-01-01", 1);
        c.addCommit("2020-01-01", 2); // same day, distinct date kept once
        c.addCommit("2020-01-02", 1);

        assertEquals(Arrays.asList("2020-01-01", "2020-01-02"), c.getCommitDates());
        // commitsCount counts every commit, not just distinct days
        assertEquals(3, c.getCommitsCount());
        assertEquals("2020-01-01", c.getFirstCommitDate());
        assertEquals("2020-01-02", c.getLatestCommitDate());
        // commitsPerDate retains the per-day commit volume (2 commits on Jan 1, 1 on Jan 2)
        assertEquals(Integer.valueOf(2), c.getCommitsPerDate().get("2020-01-01"));
        assertEquals(Integer.valueOf(1), c.getCommitsPerDate().get("2020-01-02"));
    }

    @Test
    void addCommitsPerDateSumsCountsForSharedDates() {
        Contributor c = new Contributor("a@example.com");
        c.addCommit("2020-01-01", 1);
        c.addCommit("2020-01-01", 1);

        c.addCommitsPerDate(new java.util.HashMap<>() {{
            put("2020-01-01", 3); // same day -> summed
            put("2020-01-05", 4); // new day -> added
        }});

        assertEquals(Integer.valueOf(5), c.getCommitsPerDate().get("2020-01-01"));
        assertEquals(Integer.valueOf(4), c.getCommitsPerDate().get("2020-01-05"));
    }

    @Test
    void activeYearsAreDistinctAndSorted() {
        Contributor c = new Contributor("a@example.com");

        c.addCommit("2021-05-01", 1);
        c.addCommit("2019-03-01", 1);
        c.addCommit("2021-06-01", 1); // duplicate year
        c.addCommit("2020-01-01", 1);

        assertEquals(Arrays.asList("2019", "2020", "2021"), c.getActiveYears());
    }

    @Test
    void addCommitDatesMergesDistinctDates() {
        Contributor c = new Contributor("a@example.com");
        c.addCommit("2020-01-01", 1);

        c.addCommitDates(Arrays.asList("2020-01-01", "2020-01-03", "2020-01-02"));

        assertEquals(Arrays.asList("2020-01-01", "2020-01-03", "2020-01-02"), c.getCommitDates());
    }

    @Test
    void addActiveYearsMergesDistinctSortedYears() {
        Contributor c = new Contributor("a@example.com");
        c.addCommit("2020-01-01", 1);

        c.addActiveYears(Arrays.asList("2022", "2020", "2018"));

        assertEquals(Arrays.asList("2018", "2020", "2022"), c.getActiveYears());
    }

    @Test
    void setCommitDatesKeepsDedupConsistent() {
        Contributor c = new Contributor("a@example.com");
        c.setCommitDates(new java.util.ArrayList<>(Arrays.asList("2020-01-01", "2020-01-02")));

        // adding an already-present date via addCommit must not duplicate it
        c.addCommit("2020-01-02", 1);
        c.addCommit("2020-01-03", 1);

        assertEquals(Arrays.asList("2020-01-01", "2020-01-02", "2020-01-03"), c.getCommitDates());
    }
}