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
    // Lines added/deleted across all commits touching this file, summed from the per-file churn
    // columns of git-history.txt. 0 for histories without churn data (older exports).
    private int linesAdded = 0;
    private int linesDeleted = 0;
    // The same churn restricted to the last 30 / 90 days, so the files explorer can show recent churn
    // windows alongside the all-time total.
    private int linesAdded30Days = 0;
    private int linesDeleted30Days = 0;
    private int linesAdded90Days = 0;
    private int linesDeleted90Days = 0;
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

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesDeleted() {
        return linesDeleted;
    }

    public void setLinesDeleted(int linesDeleted) {
        this.linesDeleted = linesDeleted;
    }

    public int getLinesAdded30Days() {
        return linesAdded30Days;
    }

    public void setLinesAdded30Days(int linesAdded30Days) {
        this.linesAdded30Days = linesAdded30Days;
    }

    public int getLinesDeleted30Days() {
        return linesDeleted30Days;
    }

    public void setLinesDeleted30Days(int linesDeleted30Days) {
        this.linesDeleted30Days = linesDeleted30Days;
    }

    public int getLinesAdded90Days() {
        return linesAdded90Days;
    }

    public void setLinesAdded90Days(int linesAdded90Days) {
        this.linesAdded90Days = linesAdded90Days;
    }

    public int getLinesDeleted90Days() {
        return linesDeleted90Days;
    }

    public void setLinesDeleted90Days(int linesDeleted90Days) {
        this.linesDeleted90Days = linesDeleted90Days;
    }

    // Total lines touched (added + deleted) across all commits, and within the recent windows.
    @JsonIgnore
    public int getChurn() {
        return linesAdded + linesDeleted;
    }

    @JsonIgnore
    public int getChurn30Days() {
        return linesAdded30Days + linesDeleted30Days;
    }

    @JsonIgnore
    public int getChurn90Days() {
        return linesAdded90Days + linesDeleted90Days;
    }

    @JsonIgnore
    public void addChurn(int added, int deleted) {
        this.linesAdded += added;
        this.linesDeleted += deleted;
    }

    // Adds churn for a commit on the given date, also crediting the 30/90-day windows when the date
    // falls inside them. Used by the history builder, which knows each commit's date.
    @JsonIgnore
    public void addChurn(String date, int added, int deleted) {
        this.linesAdded += added;
        this.linesDeleted += deleted;
        if (DateUtils.isCommittedLessThanDaysAgo(date, 30)) {
            this.linesAdded30Days += added;
            this.linesDeleted30Days += deleted;
        }
        if (DateUtils.isCommittedLessThanDaysAgo(date, 90)) {
            this.linesAdded90Days += added;
            this.linesDeleted90Days += deleted;
        }
    }
}
