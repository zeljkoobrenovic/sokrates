package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import java.util.*;

/**
 * The landscape's people-connection metrics: people and people-repository dependencies and connections per window
 * (not serialized), the c-index / p-index, means and medians per window and their 30-day histories (serialized flat
 * into landscapeAnalysisResults.json through the owner's unwrapped getter). Filled by
 * LandscapeAnalyzer.updatePeopleDependencies. Moved out of {@link LandscapeAnalysisResults}.
 */
public class PeopleConnectionsMetrics {
    @JsonIgnore
    private List<ComponentDependency> peopleDependencies30Days = new ArrayList<>();
    @JsonIgnore
    private List<ComponentDependency> peopleRepositoryDependencies30Days = new ArrayList<>();
    @JsonIgnore
    private List<ComponentDependency> peopleDependencies90Days = new ArrayList<>();
    @JsonIgnore
    private List<ComponentDependency> peopleDependencies180Days = new ArrayList<>();
    @JsonIgnore
    private List<ContributorConnections> connectionsViaRepositories30Days = new ArrayList<>();
    @JsonIgnore
    private List<Double> connectionsViaRepositories30DaysCountHistory = new ArrayList<>();
    @JsonIgnore
    private List<Double> peopleDependenciesCount30DaysHistory = new ArrayList<>();
    @JsonIgnore
    private List<Double> activeContributors30DaysHistory = new ArrayList<>();
    @JsonIgnore
    private List<ContributorConnections> connectionsViaRepositories90Days = new ArrayList<>();
    @JsonIgnore
    private List<ContributorConnections> connectionsViaRepositories180Days = new ArrayList<>();
    private double c2cConnectionsCount30Days;
    private double c2pConnectionsCount30Days;
    private double cIndex30Days;
    private double cIndex90Days;
    private double cIndex180Days;
    private double cMean30Days;
    private double cMean90Days;
    private double cMean180Days;
    private double cMedian30Days;
    private double cMedian90Days;
    private double cMedian180Days;
    private double pIndex30Days;
    private double pIndex90Days;
    private double pIndex180Days;
    private double pMean30Days;
    private double pMean90Days;
    private double pMean180Days;
    private double pMedian30Days;
    private double pMedian90Days;
    private double pMedian180Days;
    private List<Double> cIndex30DaysHistory = new ArrayList<>();
    private List<Double> pIndex30DaysHistory = new ArrayList<>();
    private List<Double> cMean30DaysHistory = new ArrayList<>();
    private List<Double> pMean30DaysHistory = new ArrayList<>();
    private List<Double> cMedian30DaysHistory = new ArrayList<>();
    private List<Double> pMedian30DaysHistory = new ArrayList<>();

    @JsonIgnore
    public List<ComponentDependency> getPeopleDependencies30Days() {
        return peopleDependencies30Days;
    }

    @JsonIgnore
    public void setPeopleDependencies30Days(List<ComponentDependency> peopleDependencies30Days) {
        this.peopleDependencies30Days = peopleDependencies30Days;
    }

    public List<ComponentDependency> getPeopleRepositoryDependencies30Days() {
        return peopleRepositoryDependencies30Days;
    }

    public void setPeopleRepositoryDependencies30Days(List<ComponentDependency> peopleRepositoryDependencies30Days) {
        this.peopleRepositoryDependencies30Days = peopleRepositoryDependencies30Days;
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
    public List<ContributorConnections> getConnectionsViaRepositories30Days() {
        return connectionsViaRepositories30Days;
    }

    @JsonIgnore
    public void setConnectionsViaRepositories30Days(List<ContributorConnections> connectionsViaRepositories30Days) {
        this.connectionsViaRepositories30Days = connectionsViaRepositories30Days;
    }

    @JsonIgnore
    public List<ContributorConnections> getConnectionsViaRepositories90Days() {
        return connectionsViaRepositories90Days;
    }

    @JsonIgnore
    public void setConnectionsViaRepositories90Days(List<ContributorConnections> connectionsViaRepositories90Days) {
        this.connectionsViaRepositories90Days = connectionsViaRepositories90Days;
    }

    @JsonIgnore
    public List<ContributorConnections> getConnectionsViaRepositories180Days() {
        return connectionsViaRepositories180Days;
    }

    @JsonIgnore
    public void setConnectionsViaRepositories180Days(List<ContributorConnections> connectionsViaRepositories180Days) {
        this.connectionsViaRepositories180Days = connectionsViaRepositories180Days;
    }

    public double getcIndex30Days() {
        return cIndex30Days;
    }

    public void setcIndex30Days(double cIndex30Days) {
        this.cIndex30Days = cIndex30Days;
    }

    public double getcIndex90Days() {
        return cIndex90Days;
    }

    public void setcIndex90Days(double cIndex90Days) {
        this.cIndex90Days = cIndex90Days;
    }

    public double getcIndex180Days() {
        return cIndex180Days;
    }

    public void setcIndex180Days(double cIndex180Days) {
        this.cIndex180Days = cIndex180Days;
    }

    public double getcMean30Days() {
        return cMean30Days;
    }

    public void setcMean30Days(double cMean30Days) {
        this.cMean30Days = cMean30Days;
    }

    public double getcMean90Days() {
        return cMean90Days;
    }

    public void setcMean90Days(double cMean90Days) {
        this.cMean90Days = cMean90Days;
    }

    public double getcMean180Days() {
        return cMean180Days;
    }

    public void setcMean180Days(double cMean180Days) {
        this.cMean180Days = cMean180Days;
    }

    public double getcMedian30Days() {
        return cMedian30Days;
    }

    public void setcMedian30Days(double cMedian30Days) {
        this.cMedian30Days = cMedian30Days;
    }

    public double getcMedian90Days() {
        return cMedian90Days;
    }

    public void setcMedian90Days(double cMedian90Days) {
        this.cMedian90Days = cMedian90Days;
    }

    public double getcMedian180Days() {
        return cMedian180Days;
    }

    public void setcMedian180Days(double cMedian180Days) {
        this.cMedian180Days = cMedian180Days;
    }

    public double getpIndex30Days() {
        return pIndex30Days;
    }

    public void setpIndex30Days(double pIndex30Days) {
        this.pIndex30Days = pIndex30Days;
    }

    public double getpIndex90Days() {
        return pIndex90Days;
    }

    public void setpIndex90Days(double pIndex90Days) {
        this.pIndex90Days = pIndex90Days;
    }

    public double getpIndex180Days() {
        return pIndex180Days;
    }

    public void setpIndex180Days(double pIndex180Days) {
        this.pIndex180Days = pIndex180Days;
    }

    public double getpMean30Days() {
        return pMean30Days;
    }

    public void setpMean30Days(double pMean30Days) {
        this.pMean30Days = pMean30Days;
    }

    public double getpMean90Days() {
        return pMean90Days;
    }

    public void setpMean90Days(double pMean90Days) {
        this.pMean90Days = pMean90Days;
    }

    public double getpMean180Days() {
        return pMean180Days;
    }

    public void setpMean180Days(double pMean180Days) {
        this.pMean180Days = pMean180Days;
    }

    public double getpMedian30Days() {
        return pMedian30Days;
    }

    public void setpMedian30Days(double pMedian30Days) {
        this.pMedian30Days = pMedian30Days;
    }

    public double getpMedian90Days() {
        return pMedian90Days;
    }

    public void setpMedian90Days(double pMedian90Days) {
        this.pMedian90Days = pMedian90Days;
    }

    public double getpMedian180Days() {
        return pMedian180Days;
    }

    public void setpMedian180Days(double pMedian180Days) {
        this.pMedian180Days = pMedian180Days;
    }

    public List<Double> getcIndex30DaysHistory() {
        return cIndex30DaysHistory;
    }

    public void setcIndex30DaysHistory(List<Double> cIndex30DaysHistory) {
        this.cIndex30DaysHistory = cIndex30DaysHistory;
    }

    public List<Double> getpIndex30DaysHistory() {
        return pIndex30DaysHistory;
    }

    public void setpIndex30DaysHistory(List<Double> pIndex30DaysHistory) {
        this.pIndex30DaysHistory = pIndex30DaysHistory;
    }

    public List<Double> getcMean30DaysHistory() {
        return cMean30DaysHistory;
    }

    public void setcMean30DaysHistory(List<Double> cMean30DaysHistory) {
        this.cMean30DaysHistory = cMean30DaysHistory;
    }

    public List<Double> getpMean30DaysHistory() {
        return pMean30DaysHistory;
    }

    public void setpMean30DaysHistory(List<Double> pMean30DaysHistory) {
        this.pMean30DaysHistory = pMean30DaysHistory;
    }

    public List<Double> getcMedian30DaysHistory() {
        return cMedian30DaysHistory;
    }

    public void setcMedian30DaysHistory(List<Double> cMedian30DaysHistory) {
        this.cMedian30DaysHistory = cMedian30DaysHistory;
    }

    public List<Double> getpMedian30DaysHistory() {
        return pMedian30DaysHistory;
    }

    public void setpMedian30DaysHistory(List<Double> pMedian30DaysHistory) {
        this.pMedian30DaysHistory = pMedian30DaysHistory;
    }

    public List<Double> getConnectionsViaRepositories30DaysCountHistory() {
        return connectionsViaRepositories30DaysCountHistory;
    }

    public void setConnectionsViaRepositories30DaysCountHistory(List<Double> connectionsViaRepositories30DaysCountHistory) {
        this.connectionsViaRepositories30DaysCountHistory = connectionsViaRepositories30DaysCountHistory;
    }

    public List<Double> getPeopleDependenciesCount30DaysHistory() {
        return peopleDependenciesCount30DaysHistory;
    }

    public void setPeopleDependenciesCount30DaysHistory(List<Double> peopleDependenciesCount30DaysHistory) {
        this.peopleDependenciesCount30DaysHistory = peopleDependenciesCount30DaysHistory;
    }

    public List<Double> getActiveContributors30DaysHistory() {
        return activeContributors30DaysHistory;
    }

    public void setActiveContributors30DaysHistory(List<Double> activeContributors30DaysHistory) {
        this.activeContributors30DaysHistory = activeContributors30DaysHistory;
    }

    public double getC2cConnectionsCount30Days() {
        return c2cConnectionsCount30Days;
    }

    public void setC2cConnectionsCount30Days(double c2cConnectionsCount30Days) {
        this.c2cConnectionsCount30Days = c2cConnectionsCount30Days;
    }

    public double getC2pConnectionsCount30Days() {
        return c2pConnectionsCount30Days;
    }

    public void setC2pConnectionsCount30Days(double c2pConnectionsCount30Days) {
        this.c2pConnectionsCount30Days = c2pConnectionsCount30Days;
    }
}
