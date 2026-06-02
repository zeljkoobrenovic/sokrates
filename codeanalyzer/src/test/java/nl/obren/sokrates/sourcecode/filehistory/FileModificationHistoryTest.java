package nl.obren.sokrates.sourcecode.filehistory;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FileModificationHistoryTest {

    private CommitInfo commit(String id, String date, String email) {
        CommitInfo commitInfo = new CommitInfo(id, date);
        commitInfo.setEmail(email);
        return commitInfo;
    }

    @Test
    void oldestAndLatestDateAreOrderIndependent() {
        FileModificationHistory history = new FileModificationHistory("a.java");
        history.setDates(new ArrayList<>(Arrays.asList("2020-03-01", "2020-01-01", "2020-02-01")));

        assertEquals("2020-01-01", history.getOldestDate());
        assertEquals("2020-03-01", history.getLatestDate());
    }

    @Test
    void oldestAndLatestContributorFollowCommitDateOrder() {
        FileModificationHistory history = new FileModificationHistory("a.java");
        history.setCommits(new ArrayList<>(Arrays.asList(
                commit("c2", "2020-02-01", "second@example.com"),
                commit("c1", "2020-01-01", "first@example.com"),
                commit("c3", "2020-03-01", "third@example.com"))));

        assertEquals("first@example.com", history.getOldestContributor());
        assertEquals("third@example.com", history.getLatestContributor());
    }

    @Test
    void countContributorsDeduplicatesByEmail() {
        FileModificationHistory history = new FileModificationHistory("a.java");
        history.setCommits(new ArrayList<>(Arrays.asList(
                commit("c1", "2020-01-01", "a@example.com"),
                commit("c2", "2020-01-02", "a@example.com"),
                commit("c3", "2020-01-03", "b@example.com"))));

        assertEquals(2, history.countContributors());
    }

    @Test
    void sortingHappensOnlyOnceButStaysCorrect() {
        FileModificationHistory history = new FileModificationHistory("a.java");
        history.setDates(new ArrayList<>(Arrays.asList("2020-03-01", "2020-01-01")));

        // First access triggers the sort; subsequent accesses must remain consistent.
        assertEquals("2020-01-01", history.getOldestDate());
        assertEquals("2020-01-01", history.getOldestDate());
        assertEquals("2020-03-01", history.getLatestDate());
    }
}
