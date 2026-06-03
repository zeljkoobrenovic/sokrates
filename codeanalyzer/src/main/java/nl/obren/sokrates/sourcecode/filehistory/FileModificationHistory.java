/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

public class FileModificationHistory {
    private List<String> dates = new ArrayList<>();
    private List<CommitInfo> commits = new ArrayList<>();
    private String path = "";
    private boolean sorted = false;
    // O(1) companion set for the (serialized) dates list, so addDateIfAbsent stays O(1) per commit
    // instead of an O(n) contains() scan. Kept in sync with dates.
    @JsonIgnore
    private Set<String> datesSet = new HashSet<>();
    // Memoized distinct-contributor count. countContributors() is called repeatedly (e.g. once per
    // comparison while sorting files by contributor count), so cache the result and invalidate it
    // when commits are replaced. Like the `sorted` flag, this assumes commits are not mutated
    // through getCommits() after the count is first read (true in the analysis flow).
    @JsonIgnore
    private int contributorCount = -1;

    public FileModificationHistory() {
    }

    public FileModificationHistory(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
        this.datesSet = new HashSet<>(dates);
    }

    // Adds a date only if not already present, keeping the dates list distinct in O(1).
    @JsonIgnore
    public void addDateIfAbsent(String date) {
        if (datesSet.add(date)) {
            dates.add(date);
        }
    }

    public String getOldestDate() {
        sortOldestFirst();
        return dates.get(0);
    }

    public String getLatestDate() {
        sortOldestFirst();
        return dates.get(dates.size() - 1);
    }

    public String getOldestContributor() {
        sortOldestFirst();
        return commits.get(0).getEmail();
    }

    public String getLatestContributor() {
        sortOldestFirst();
        return commits.get(commits.size() - 1).getEmail();
    }

    public void sortOldestFirst() {
        if (!sorted) {
            sorted = true;
            Collections.sort(dates);
            Collections.sort(commits, (a, b) -> a.getDate().compareTo(b.getDate()));
        }
    }

    public int daysSinceFirstUpdate() {
        return FileHistoryUtils.daysFromToday(getOldestDate());
    }

    public int daysSinceLatestUpdate() {
        return FileHistoryUtils.daysFromToday(getLatestDate());
    }

    public List<CommitInfo> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitInfo> commits) {
        this.commits = commits;
        this.contributorCount = -1;
    }

    public int countContributors() {
        if (contributorCount < 0) {
            Set<String> contributorIds = new HashSet<>();
            commits.forEach(commit -> contributorIds.add(commit.getEmail()));
            contributorCount = contributorIds.size();
        }
        return contributorCount;
    }
}
