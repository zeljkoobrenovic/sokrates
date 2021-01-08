/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ContributionTimeSlot {
    private String timeSlot = "";
    private int contributorsCount;
    private int commitsCount;

    public ContributionTimeSlot() {
    }

    public ContributionTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
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
}
