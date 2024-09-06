package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
    private List<CommitsPerExtension> commitsPerExtensions = new ArrayList<>();
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

    @JsonIgnore
    public int getCommitsCount() {
        return contributors.stream().mapToInt(c -> c.getCommitsCount()).sum();
    }

    @JsonIgnore
    public int getCommitsCount30Days() {
        return contributors.stream().mapToInt(c -> c.getCommitsCount30Days()).sum();
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
