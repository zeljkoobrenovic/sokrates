package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.contributors.ContributionYear;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ContributorsAnalysisResults {
    List<ComponentDependency> peopleDependencies30Days = new ArrayList<>();
    List<ComponentDependency> peopleDependencies90Days = new ArrayList<>();
    List<ComponentDependency> peopleDependencies180Days = new ArrayList<>();
    List<ComponentDependency> peopleDependencies365Days = new ArrayList<>();
    List<ComponentDependency> peopleDependenciesAllTime = new ArrayList<>();

    private List<Contributor> contributors = new ArrayList<>();
    private List<ContributionYear> contributorsPerYear = new ArrayList<>();
    private List<CommitsPerExtension> commitsPerExtensions = new ArrayList<>();

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    public List<ContributionYear> getContributorsPerYear() {
        return contributorsPerYear;
    }

    public void setContributorsPerYear(List<ContributionYear> contributorsPerYear) {
        this.contributorsPerYear = contributorsPerYear;
    }

    public List<CommitsPerExtension> getCommitsPerExtensions() {
        return commitsPerExtensions;
    }

    public void setCommitsPerExtensions(List<CommitsPerExtension> commitsPerExtensions) {
        this.commitsPerExtensions = commitsPerExtensions;
    }

    @JsonIgnore
    public int getCommitsCount() {
        return contributors.stream().mapToInt(c -> c.getCommitsCount()).sum();
    }

    @JsonIgnore
    public int getCommitsThisYear() {
        String year = "" + Calendar.getInstance().get(Calendar.YEAR);

        return contributorsPerYear.stream().filter(c -> c.getYear().equals(year)).mapToInt(c -> c.getCommitsCount()).sum();
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
    public List<ComponentDependency> getPeopleDependenciesAllTime() {
        return peopleDependenciesAllTime;
    }

    @JsonIgnore
    public void setPeopleDependenciesAllTime(List<ComponentDependency> peopleDependenciesAllTime) {
        this.peopleDependenciesAllTime = peopleDependenciesAllTime;
    }

    @JsonIgnore
    public List<ComponentDependency> getPeopleDependencies365Days() {
        return peopleDependencies365Days;
    }

    @JsonIgnore
    public void setPeopleDependencies365Days(List<ComponentDependency> peopleDependencies365Days) {
        this.peopleDependencies365Days = peopleDependencies365Days;
    }
}
