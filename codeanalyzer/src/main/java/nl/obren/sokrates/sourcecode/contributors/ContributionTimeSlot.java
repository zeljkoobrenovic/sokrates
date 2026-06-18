/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

public class ContributionTimeSlot {
    private String timeSlot = "";
    private int contributorsCount;
    private int commitsCount;
    private int fileUpdatesCount;
    // Lines added/deleted in this time slot, summed from the per-commit churn columns of
    // git-history.txt. 0 for history files without churn data (older exports).
    private int linesAdded;
    private int linesDeleted;

    private RiskDistributionStats fileUpdatesCountStats;

    public ContributionTimeSlot(Thresholds fileUpdateFrequencyThresholds) {
        this.fileUpdatesCountStats = new RiskDistributionStats(fileUpdateFrequencyThresholds);
        this.fileUpdatesCountStats.setValueUnit("file updates");
        this.fileUpdatesCountStats.setCountUnit("commits");
    }

    public ContributionTimeSlot(String timeSlot, Thresholds fileUpdateFrequencyThresholds) {
        this(fileUpdateFrequencyThresholds);

        this.timeSlot = timeSlot;
    }

    public ContributionTimeSlot() {
        this(Thresholds.defaultFileUpdateFrequencyThresholds());
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public int getContributorsCount() {
        return contributorsCount;
    }

    public void setContributorsCount(int contributorsCount) {
        this.contributorsCount = contributorsCount;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }

    @JsonIgnore
    public void incrementCommitsCount() {
        this.commitsCount += 1;
    }

    public int getFileUpdatesCount() {
        return fileUpdatesCount;
    }

    public void setFileUpdatesCount(int fileUpdatesCount) {
        this.fileUpdatesCount = fileUpdatesCount;
    }

    public RiskDistributionStats getFileUpdatesCountStats() {
        return fileUpdatesCountStats;
    }

    public void setFileUpdatesCountStats(RiskDistributionStats fileUpdatesCountStats) {
        this.fileUpdatesCountStats = fileUpdatesCountStats;
    }

    @JsonIgnore
    public void incrementFileUpdatesCount(int increment) {
        this.fileUpdatesCount += increment;
        this.fileUpdatesCountStats.update(increment, increment);
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

    @JsonIgnore
    public void addChurn(int added, int deleted) {
        this.linesAdded += added;
        this.linesDeleted += deleted;
    }
}
