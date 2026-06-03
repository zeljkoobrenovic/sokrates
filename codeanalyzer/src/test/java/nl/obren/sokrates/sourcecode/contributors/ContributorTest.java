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