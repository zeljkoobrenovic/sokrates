/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContributorsImport {
    private List<Contributor> contributors = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerYear = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>();
    // Parallel time-slot lists per scope (main, test, build, generated, other), backing the scope
    // tabs in the activity diagrams. Each map is keyed by scope name; absent scope = no such files /
    // no git history. Empty for older analyses (callers then show only the "All" diagrams).
    private Map<String, List<ContributionTimeSlot>> contributorsPerYearByScope = new LinkedHashMap<>();
    private Map<String, List<ContributionTimeSlot>> contributorsPerMonthByScope = new LinkedHashMap<>();
    private Map<String, List<ContributionTimeSlot>> contributorsPerDayByScope = new LinkedHashMap<>();
    private Map<String, List<ContributionTimeSlot>> contributorsPerWeekByScope = new LinkedHashMap<>();
    private List<ContributionTimeSlot> rookiesPerYear = new ArrayList<>();
    private List<ContributionTimeSlot> leaversPerYear = new ArrayList<>();

    private String latestCommitDate = "";
    private String firstCommitDate = "";

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

    public Map<String, List<ContributionTimeSlot>> getContributorsPerYearByScope() {
        return contributorsPerYearByScope;
    }

    public void setContributorsPerYearByScope(Map<String, List<ContributionTimeSlot>> contributorsPerYearByScope) {
        this.contributorsPerYearByScope = contributorsPerYearByScope;
    }

    public Map<String, List<ContributionTimeSlot>> getContributorsPerMonthByScope() {
        return contributorsPerMonthByScope;
    }

    public void setContributorsPerMonthByScope(Map<String, List<ContributionTimeSlot>> contributorsPerMonthByScope) {
        this.contributorsPerMonthByScope = contributorsPerMonthByScope;
    }

    public Map<String, List<ContributionTimeSlot>> getContributorsPerDayByScope() {
        return contributorsPerDayByScope;
    }

    public void setContributorsPerDayByScope(Map<String, List<ContributionTimeSlot>> contributorsPerDayByScope) {
        this.contributorsPerDayByScope = contributorsPerDayByScope;
    }

    public Map<String, List<ContributionTimeSlot>> getContributorsPerWeekByScope() {
        return contributorsPerWeekByScope;
    }

    public void setContributorsPerWeekByScope(Map<String, List<ContributionTimeSlot>> contributorsPerWeekByScope) {
        this.contributorsPerWeekByScope = contributorsPerWeekByScope;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }

    public String getFirstCommitDate() {
        return firstCommitDate;
    }

    public void setFirstCommitDate(String firstCommitDate) {
        this.firstCommitDate = firstCommitDate;
    }
}
