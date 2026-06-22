package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContributorsAnalysisResults {
    List<ComponentDependency> peopleDependencies30Days = new ArrayList<>();
    List<ComponentDependency> peopleDependencies90Days = new ArrayList<>();
    List<ComponentDependency> peopleDependencies180Days = new ArrayList<>();
    List<ComponentDependency> peopleDependencies365Days = new ArrayList<>();
    private String latestCommitDate = "";
    private List<Contributor> contributors = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerYear = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerDay = new ArrayList<>();
    private List<ContributionTimeSlot> contributorsPerWeek = new ArrayList<>();
    // Parallel time-slot lists per scope (main, test, build, generated, other), keyed by scope name,
    // backing the scope tabs in the activity diagrams. Empty for older analyses (callers fall back to
    // showing only "All").
    private Map<String, List<ContributionTimeSlot>> contributorsPerYearByScope = new LinkedHashMap<>();
    private Map<String, List<ContributionTimeSlot>> contributorsPerMonthByScope = new LinkedHashMap<>();
    private Map<String, List<ContributionTimeSlot>> contributorsPerDayByScope = new LinkedHashMap<>();
    private Map<String, List<ContributionTimeSlot>> contributorsPerWeekByScope = new LinkedHashMap<>();
    private List<CommitsPerExtension> commitsPerExtensions = new ArrayList<>();
    // Extensions of git-history files that fall in NO scope (the "unscoped"/residual tab) with their
    // DISTINCT file counts as the value. These files are never analyzed, so there is no lines-of-code —
    // the report shows "-" for the number and uses the count only for ordering. Empty for older analyses
    // and for repos with no unscoped activity.
    private List<NumericMetric> unscopedExtensionFileCounts = new ArrayList<>();
    private List<ComponentDependency> peopleFileDependencies30Days;
    private List<ComponentDependency> peopleFileDependencies90Days;
    private List<ComponentDependency> peopleFileDependencies180Days;
    private List<ComponentDependency> peopleFileDependencies365Days;

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }

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

    public List<CommitsPerExtension> getCommitsPerExtensions() {
        return commitsPerExtensions;
    }

    public void setCommitsPerExtensions(List<CommitsPerExtension> commitsPerExtensions) {
        this.commitsPerExtensions = commitsPerExtensions;
    }

    public List<NumericMetric> getUnscopedExtensionFileCounts() {
        return unscopedExtensionFileCounts;
    }

    public void setUnscopedExtensionFileCounts(List<NumericMetric> unscopedExtensionFileCounts) {
        this.unscopedExtensionFileCounts = unscopedExtensionFileCounts;
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

    @JsonIgnore
    public int getCommitsCount() {
        return contributors.stream().mapToInt(c -> c.getCommitsCount()).sum();
    }

    @JsonIgnore
    public int getCommitsCount30Days() {
        return contributors.stream().mapToInt(c -> c.getCommitsCount30Days()).sum();
    }

    @JsonIgnore
    public int getFileUpdatesCount30Days() {
        return contributors.stream().mapToInt(c -> c.getFileUpdatesCount30Days()).sum();
    }

    @JsonIgnore
    public int getLinesAdded30Days() {
        return contributors.stream().mapToInt(c -> c.getLinesAdded30Days()).sum();
    }

    @JsonIgnore
    public int getLinesDeleted30Days() {
        return contributors.stream().mapToInt(c -> c.getLinesDeleted30Days()).sum();
    }

    // Total line churn (added + deleted) across all contributors, in the recent windows and all time.
    @JsonIgnore
    public int getChurn30Days() {
        return contributors.stream().mapToInt(c -> c.getLinesAdded30Days() + c.getLinesDeleted30Days()).sum();
    }

    @JsonIgnore
    public int getChurn90Days() {
        return contributors.stream().mapToInt(c -> c.getLinesAdded90Days() + c.getLinesDeleted90Days()).sum();
    }

    @JsonIgnore
    public int getChurn() {
        return contributors.stream().mapToInt(c -> c.getLinesAdded() + c.getLinesDeleted()).sum();
    }

    @JsonIgnore
    public int getCommitsCount90Days() {
        return contributors.stream().mapToInt(c -> c.getCommitsCount90Days()).sum();
    }
    @JsonIgnore
    public int getCommitsCount180Days() {
        return contributors.stream().mapToInt(c -> c.getCommitsCount180Days()).sum();
    }
    @JsonIgnore
    public int getCommitsCount365Days() {
        return contributors.stream().mapToInt(c -> c.getCommitsCount365Days()).sum();
    }

    @JsonIgnore
    public int getCommitsThisYear() {
        String year = "" + Calendar.getInstance().get(Calendar.YEAR);

        return contributorsPerYear.stream().filter(c -> c.getTimeSlot().equals(year)).mapToInt(c -> c.getCommitsCount()).sum();
    }


    @JsonIgnore
    public List<ComponentDependency> getPeopleDependencies30Days() {
        return peopleDependencies30Days;
    }

    @JsonIgnore
    public void setPeopleDependencies30Days(List<ComponentDependency> peopleDependencies30Days) {
        this.peopleDependencies30Days = peopleDependencies30Days;
    }

    @JsonIgnore
    public List<ComponentDependency> getPeopleDependencies90Days() {
        return peopleDependencies90Days;
    }

    @JsonIgnore
    public void setPeopleDependencies90Days(List<ComponentDependency> peopleDependencies90Days) {
        this.peopleDependencies90Days = peopleDependencies90Days;
    }

    @JsonIgnore
    public List<ComponentDependency> getPeopleDependencies180Days() {
        return peopleDependencies180Days;
    }

    @JsonIgnore
    public void setPeopleDependencies180Days(List<ComponentDependency> peopleDependencies180Days) {
        this.peopleDependencies180Days = peopleDependencies180Days;
    }

    @JsonIgnore
    public List<ComponentDependency> getPeopleDependencies365Days() {
        return peopleDependencies365Days;
    }

    @JsonIgnore
    public void setPeopleDependencies365Days(List<ComponentDependency> peopleDependencies365Days) {
        this.peopleDependencies365Days = peopleDependencies365Days;
    }

    @JsonIgnore
    public List<ComponentDependency> getPeopleFileDependencies30Days() {
        return peopleFileDependencies30Days;
    }

    @JsonIgnore
    public void setPeopleFileDependencies30Days(List<ComponentDependency> peopleFileDependencies30Days) {
        this.peopleFileDependencies30Days = peopleFileDependencies30Days;
    }

    @JsonIgnore
    public List<ComponentDependency> getPeopleFileDependencies90Days() {
        return peopleFileDependencies90Days;
    }

    @JsonIgnore
    public void setPeopleFileDependencies90Days(List<ComponentDependency> peopleFileDependencies90Days) {
        this.peopleFileDependencies90Days = peopleFileDependencies90Days;
    }

    @JsonIgnore
    public List<ComponentDependency> getPeopleFileDependencies180Days() {
        return peopleFileDependencies180Days;
    }

    @JsonIgnore
    public void setPeopleFileDependencies180Days(List<ComponentDependency> peopleFileDependencies180Days) {
        this.peopleFileDependencies180Days = peopleFileDependencies180Days;
    }

    @JsonIgnore
    public List<ComponentDependency> getPeopleFileDependencies365Days() {
        return peopleFileDependencies365Days;
    }

    @JsonIgnore
    public void setPeopleFileDependencies365Days(List<ComponentDependency> peopleFileDependencies365Days) {
        this.peopleFileDependencies365Days = peopleFileDependencies365Days;
    }
}
