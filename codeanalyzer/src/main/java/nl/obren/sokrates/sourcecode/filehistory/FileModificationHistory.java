/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import java.util.*;
import java.util.stream.Stream;

public class FileModificationHistory {
    private List<String> dates = new ArrayList<>();
    private List<CommitInfo> commits = new ArrayList<>();
    private String path = "";
    private boolean sorted = false;

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
    }

    public int countContributors() {
        Set<String> contributorIds = new HashSet<>();
        commits.forEach(commit -> contributorIds.add(commit.getEmail()));
        return contributorIds.size();
    }
}
