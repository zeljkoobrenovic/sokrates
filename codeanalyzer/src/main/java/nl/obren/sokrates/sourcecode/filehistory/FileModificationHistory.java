/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileModificationHistory {
    private List<String> dates = new ArrayList<>();
    private List<CommitInfo> commits = new ArrayList<>();
    private String path = "";

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

    public void sortOldestFirst() {
        Collections.sort(dates);
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
}
