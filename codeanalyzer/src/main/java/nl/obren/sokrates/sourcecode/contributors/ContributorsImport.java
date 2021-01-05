/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import java.util.ArrayList;
import java.util.List;

public class ContributorsImport {
    private List<Contributor> contributors = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerYear = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>();
    private List<ContributionTimeSlot> rookiesPerYear = new ArrayList<>();
    private List<ContributionTimeSlot> leaversPerYear = new ArrayList<>();

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    public List<ContributionTimeSlot> getContributorsPerYear() {
        return contributorsPerYear;
    }

    public void setContributorsPerYear(List<ContributionTimeSlot> contributorsPerYear) {
        this.contributorsPerYear = contributorsPerYear;
    }

    public List<ContributionTimeSlot> getContributorsPerMonth() {
        return contributorsPerMonth;
    }

    public void setContributorsPerMonth(List<ContributionTimeSlot> contributorsPerMonth) {
        this.contributorsPerMonth = contributorsPerMonth;
    }

    public List<ContributionTimeSlot> getContributorsPerDay() {
        return contributorsPerDay;
    }

    public void setContributorsPerDay(List<ContributionTimeSlot> contributorsPerDay) {
        this.contributorsPerDay = contributorsPerDay;
    }

    public List<ContributionTimeSlot> getRookiesPerYear() {
        return rookiesPerYear;
    }

    public void setRookiesPerYear(List<ContributionTimeSlot> rookiesPerYear) {
        this.rookiesPerYear = rookiesPerYear;
    }

    public List<ContributionTimeSlot> getLeaversPerYear() {
        return leaversPerYear;
    }

    public void setLeaversPerYear(List<ContributionTimeSlot> leaversPerYear) {
        this.leaversPerYear = leaversPerYear;
    }

    public List<ContributionTimeSlot> getContributorsPerWeek() {
        return contributorsPerWeek;
    }

    public void setContributorsPerWeek(List<ContributionTimeSlot> contributorsPerWeek) {
        this.contributorsPerWeek = contributorsPerWeek;
    }
}
