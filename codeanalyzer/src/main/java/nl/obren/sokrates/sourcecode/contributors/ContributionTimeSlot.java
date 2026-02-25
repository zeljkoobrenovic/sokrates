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
}
