/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;

import java.util.*;
import java.util.stream.Collectors;

public class LandscapeAnalysisResults {
    public static final int RECENT_THRESHOLD_DAYS = 30;

    @JsonIgnore
    private List<ComponentDependency> peopleDependencies30Days = new ArrayList<>();

    @JsonIgnore
    private List<ComponentDependency> peopleDependencies90Days = new ArrayList<>();

    @JsonIgnore
    private List<ComponentDependency> peopleDependencies180Days = new ArrayList<>();

    @JsonIgnore
    private List<ContributorConnections> connectionsViaProjects30Days = new ArrayList<>();

    @JsonIgnore
    private List<Double> connectionsViaProjects30DaysCountHistory = new ArrayList<>();

    @JsonIgnore
    private List<Double> peopleDependenciesCount30DaysHistory = new ArrayList<>();

    @JsonIgnore
    private List<Double> activeContributors30DaysHistory = new ArrayList<>();

    @JsonIgnore
    private List<ContributorConnections> connectionsViaProjects90Days = new ArrayList<>();

    @JsonIgnore
    private List<ContributorConnections> connectionsViaProjects180Days = new ArrayList<>();

    @JsonIgnore
    private LandscapeConfiguration configuration = new LandscapeConfiguration();

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

    private String latestCommitDate = "";

    private List<Double> cIndex30DaysHistory = new ArrayList<>();
    private List<Double> pIndex30DaysHistory = new ArrayList<>();

    private List<Double> cMean30DaysHistory = new ArrayList<>();
    private List<Double> pMean30DaysHistory = new ArrayList<>();

    private List<Double> cMedian30DaysHistory = new ArrayList<>();
    private List<Double> pMedian30DaysHistory = new ArrayList<>();

    @JsonIgnore
    private List<ProjectAnalysisResults> projectAnalysisResults = new ArrayList<>();

    public LandscapeConfiguration getConfiguration() {
        return configuration;
    }

    @JsonIgnore
    public void setConfiguration(LandscapeConfiguration configuration) {
        this.configuration = configuration;
    }

    @JsonIgnore
    public List<ProjectAnalysisResults> getProjectAnalysisResults() {
        return projectAnalysisResults;
    }

    @JsonIgnore
    public void setProjectAnalysisResults(List<ProjectAnalysisResults> projectAnalysisResults) {
        this.projectAnalysisResults = projectAnalysisResults;
    }

    @JsonIgnore
    public List<ProjectAnalysisResults> getFilteredProjectAnalysisResults() {
        int thresholdLoc = configuration.getProjectThresholdLocMain();
        int thresholdContributors = configuration.getProjectThresholdContributors();

        return projectAnalysisResults.stream()
                .filter(p -> {
                    CodeAnalysisResults results = p.getAnalysisResults();
                    int contributorsCount = results.getContributorsAnalysisResults().getContributors().size();
                    return results.getMainAspectAnalysisResults().getLinesOfCode() >= thresholdLoc
                            && (contributorsCount == 0 || contributorsCount >= thresholdContributors);
                })
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<ProjectAnalysisResults> getAllProjects() {
        return this.projectAnalysisResults;
    }

    public int getProjectsCount() {
        return projectAnalysisResults.size();
    }

    public int getMainLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getMainLocActive() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = projectAnalysisResults.getAnalysisResults().getFilesHistoryAnalysisResults();
            SourceFileAgeDistribution overallFileLastModifiedDistribution = filesHistoryAnalysisResults.getOverallFileLastModifiedDistribution();
            if (overallFileLastModifiedDistribution != null) {
                count[0] += overallFileLastModifiedDistribution.getTotalValue() - overallFileLastModifiedDistribution.getVeryHighRiskValue();
            }
        });
        return count[0];
    }

    public int getMainLocNew() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = projectAnalysisResults.getAnalysisResults().getFilesHistoryAnalysisResults();
            SourceFileAgeDistribution overallFileFirstModifiedDistribution = filesHistoryAnalysisResults.getOverallFileFirstModifiedDistribution();
            if (overallFileFirstModifiedDistribution != null) {
                count[0] += overallFileFirstModifiedDistribution.getTotalValue() - overallFileFirstModifiedDistribution.getVeryHighRiskValue();
            }
        });
        return count[0];
    }

    public int getTestLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getGeneratedLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getBuildAndDeploymentLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getOtherLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getAllLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            count[0] += projectAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode();
            count[0] += projectAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode();
            count[0] += projectAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode();
            count[0] += projectAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public List<NumericMetric> getLinesOfCodePerExtension() {
        List<NumericMetric> linesOfCodePerExtension = new ArrayList<>();
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            AspectAnalysisResults main = projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults();
            List<NumericMetric> projectLinesOfCodePerExtension = main.getLinesOfCodePerExtension();
            projectLinesOfCodePerExtension.forEach(metric -> {
                String id = metric.getName();
                Optional<NumericMetric> existingMetric = linesOfCodePerExtension.stream().filter(c -> c.getName().equalsIgnoreCase(id)).findAny();
                if (existingMetric.isPresent()) {
                    existingMetric.get().setValue(existingMetric.get().getValue().intValue() + metric.getValue().intValue());
                } else {
                    linesOfCodePerExtension.add(new NumericMetric(metric.getName(), metric.getValue()));
                }
            });
        });

        Collections.sort(linesOfCodePerExtension, (a, b) -> b.getValue().intValue() - a.getValue().intValue());
        return linesOfCodePerExtension;
    }

    @JsonIgnore
    public List<String> getAllExtension() {
        List<String> extensions = new ArrayList<>();
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults().getCommitsPerExtensions().forEach(perExtension -> {
                String extension = perExtension.getExtension();
                if (!extensions.contains(extension)) {
                    extensions.add(extension);
                }
            });
        });

        return extensions;
    }

    @JsonIgnore
    public List<ContributorProjects> getContributors() {
        int thresholdCommits = configuration.getContributorThresholdCommits();
        List<ContributorProjects> contributorProjects = getAllContributors().stream()
                .filter(c -> c.getContributor().getCommitsCount() >= thresholdCommits)
                .collect(Collectors.toCollection(ArrayList::new));

        if (configuration.isAnonymizeContributors()) {
            int counter[] = {1};
            contributorProjects.forEach(contributorProject -> {
                contributorProject.getContributor().setEmail("Contributor " + counter[0]);
                counter[0] += 1;
            });
        }
        return contributorProjects;
    }

    @JsonIgnore
    public List<CommitsPerExtension> getContributorsPerExtension() {
        int thresholdCommits = configuration.getContributorThresholdCommits();
        Map<String, CommitsPerExtension> commitsPerExtensions = new HashMap<>();

        getAllExtension().forEach(extension -> {
            commitsPerExtensions.put(extension, new CommitsPerExtension(extension));
        });

        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            List<CommitsPerExtension> projectData = projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults().getCommitsPerExtensions();

            projectData.forEach(projectExtData -> {
                String extension = projectExtData.getExtension();
                if (commitsPerExtensions.containsKey(extension)) {
                    CommitsPerExtension commitsPerExtension = commitsPerExtensions.get(extension);
                    commitsPerExtension.setCommitsCount(commitsPerExtension.getCommitsCount() + projectExtData.getCommitsCount());

                    commitsPerExtension.setCommitsCount30Days(commitsPerExtension.getCommitsCount30Days() + projectExtData.getCommitsCount30Days());
                    commitsPerExtension.setCommitsCount90Days(commitsPerExtension.getCommitsCount90Days() + projectExtData.getCommitsCount90Days());

                    commitsPerExtension.setFilesCount(commitsPerExtension.getFilesCount() + projectExtData.getFilesCount());
                    commitsPerExtension.setFilesCount30Days(commitsPerExtension.getFilesCount30Days() + projectExtData.getFilesCount30Days());
                    commitsPerExtension.setFilesCount90Days(commitsPerExtension.getFilesCount90Days() + projectExtData.getFilesCount90Days());

                    projectExtData.getCommitters().forEach(email -> {
                        if (!commitsPerExtension.getCommitters().contains(email)) {
                            commitsPerExtension.getCommitters().add(email);
                        }
                    });
                    projectExtData.getCommitters30Days().forEach(email -> {
                        if (!commitsPerExtension.getCommitters30Days().contains(email)) {
                            commitsPerExtension.getCommitters30Days().add(email);
                        }
                    });
                    projectExtData.getCommitters90Days().forEach(email -> {
                        if (!commitsPerExtension.getCommitters90Days().contains(email)) {
                            commitsPerExtension.getCommitters90Days().add(email);
                        }
                    });
                }
            });
        });

        ArrayList<CommitsPerExtension> list = new ArrayList<>(commitsPerExtensions.values());
        Collections.sort(list, (a, b) -> b.getCommitters30Days().size() - a.getCommitters30Days().size());

        return list;
    }

    @JsonIgnore
    public List<ContributorProjects> getAllContributors() {
        List<ContributorProjects> list = new ArrayList<>();
        Map<String, ContributorProjects> map = new HashMap<>();

        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            contributorsAnalysisResults.getContributors().forEach(contributor -> {
                String contributorId = contributor.getEmail();
                if (GitHistoryUtils.shouldIgnore(contributorId, configuration.getIgnoreContributors())) {
                    return;
                }
                if (configuration.getTransformContributorEmails().size() > 0) {
                    ComplexOperation operation = new ComplexOperation(configuration.getTransformContributorEmails());
                    contributorId = operation.exec(contributorId);
                }
                int projectCommits = contributor.getCommitsCount();
                List<String> commitDates = contributor.getCommitDates();
                int projectCommits30Days = contributor.getCommitsCount30Days();
                int projectCommits90Days = contributor.getCommitsCount90Days();
                int projectCommits180Days = contributor.getCommitsCount180Days();

                if (map.containsKey(contributorId)) {
                    ContributorProjects existingContributor = map.get(contributorId);
                    Contributor contributorInfo = existingContributor.getContributor();

                    contributorInfo.setCommitsCount(contributorInfo.getCommitsCount() + projectCommits);
                    contributorInfo.setCommitsCount30Days(contributorInfo.getCommitsCount30Days() + projectCommits30Days);
                    contributorInfo.setCommitsCount90Days(contributorInfo.getCommitsCount90Days() + projectCommits90Days);
                    contributorInfo.setCommitsCount180Days(contributorInfo.getCommitsCount180Days() + projectCommits180Days);

                    contributorInfo.getActiveYears().addAll(contributor.getActiveYears());

                    existingContributor.addProject(projectAnalysisResults,
                            contributorInfo.getFirstCommitDate(), contributorInfo.getLatestCommitDate(),
                            projectCommits, projectCommits30Days, projectCommits90Days, commitDates);

                    if (contributor.getFirstCommitDate().compareTo(contributorInfo.getFirstCommitDate()) < 0) {
                        contributorInfo.setFirstCommitDate(contributor.getFirstCommitDate());
                    }
                    if (contributor.getLatestCommitDate().compareTo(contributorInfo.getLatestCommitDate()) > 0) {
                        contributorInfo.setLatestCommitDate(contributor.getLatestCommitDate());
                    }
                } else {
                    Contributor newContributor = new Contributor();

                    newContributor.setEmail(contributorId);
                    newContributor.setCommitsCount(projectCommits);
                    newContributor.setCommitsCount30Days(projectCommits30Days);
                    newContributor.setCommitsCount90Days(projectCommits90Days);
                    newContributor.setCommitsCount180Days(projectCommits180Days);
                    newContributor.setFirstCommitDate(contributor.getFirstCommitDate());
                    newContributor.setLatestCommitDate(contributor.getLatestCommitDate());
                    newContributor.setActiveYears(contributor.getActiveYears());

                    ContributorProjects newContributorWithProjects = new ContributorProjects(newContributor);

                    newContributorWithProjects.addProject(projectAnalysisResults, newContributor.getFirstCommitDate(),
                            newContributor.getLatestCommitDate(), projectCommits, projectCommits30Days, projectCommits90Days, commitDates);

                    map.put(contributorId, newContributorWithProjects);
                    list.add(newContributorWithProjects);
                }
            });
        });

        Collections.sort(list, (a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount());

        return list;
    }

    @JsonIgnore
    public List<ContributionTimeSlot> getContributorsPerYear() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            updateContributors(list, map, contributorsAnalysisResults.getContributorsPerYear());
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        return list;
    }

    @JsonIgnore
    public List<ContributionTimeSlot> getContributorsPerWeek() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerWeek = contributorsAnalysisResults.getContributorsPerWeek();
            updateContributors(list, map, contributorsPerWeek);
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot));

        return list;
    }

    private void updateContributors(List<ContributionTimeSlot> list, Map<String, ContributionTimeSlot> map, List<ContributionTimeSlot> contributorsPerTimeSlot) {
        contributorsPerTimeSlot.forEach(timeSlot -> {
            ContributionTimeSlot contributionTimeSlot = map.get(timeSlot.getTimeSlot());
            if (contributionTimeSlot == null) {
                contributionTimeSlot = new ContributionTimeSlot();
                contributionTimeSlot.setTimeSlot(timeSlot.getTimeSlot());
                contributionTimeSlot.setContributorsCount(timeSlot.getContributorsCount());
                contributionTimeSlot.setCommitsCount(timeSlot.getCommitsCount());
                list.add(contributionTimeSlot);
                map.put(timeSlot.getTimeSlot(), contributionTimeSlot);
            } else {
                contributionTimeSlot.setContributorsCount(contributionTimeSlot.getContributorsCount() + timeSlot.getContributorsCount());
                contributionTimeSlot.setCommitsCount(contributionTimeSlot.getCommitsCount() + timeSlot.getCommitsCount());
            }
        });
    }

    public int getCommitsCount() {
        return this.projectAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults()
                        .getContributorsAnalysisResults().getCommitsCount()).sum();
    }

    public int getCommitsCount30Days() {
        return this.projectAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults()
                        .getContributorsAnalysisResults().getCommitsCount30Days()).sum();
    }

    public int getContributorsCount() {
        return getContributors().size();
    }

    public int getRecentContributorsCount() {
        return (int) getContributors().stream().filter(c -> c.getContributor().getCommitsCount30Days() > 0).count();
    }

    public int getRecentContributorsCount6Months() {
        return (int) getContributors().stream().filter(c -> c.getContributor().isActive(180)).count();
    }

    public int getRecentContributorsCount3Months() {
        return (int) getContributors().stream().filter(c -> c.getContributor().isActive(90)).count();
    }

    public int getRookiesContributorsCount() {
        return (int) getContributors().stream().filter(c -> c.getContributor().isRookie(RECENT_THRESHOLD_DAYS)).count();
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
    public List<ContributorConnections> getConnectionsViaProjects30Days() {
        return connectionsViaProjects30Days;
    }

    @JsonIgnore
    public void setConnectionsViaProjects30Days(List<ContributorConnections> connectionsViaProjects30Days) {
        this.connectionsViaProjects30Days = connectionsViaProjects30Days;
    }

    @JsonIgnore
    public List<ContributorConnections> getConnectionsViaProjects90Days() {
        return connectionsViaProjects90Days;
    }

    @JsonIgnore
    public void setConnectionsViaProjects90Days(List<ContributorConnections> connectionsViaProjects90Days) {
        this.connectionsViaProjects90Days = connectionsViaProjects90Days;
    }

    @JsonIgnore
    public List<ContributorConnections> getConnectionsViaProjects180Days() {
        return connectionsViaProjects180Days;
    }

    @JsonIgnore
    public void setConnectionsViaProjects180Days(List<ContributorConnections> connectionsViaProjects180Days) {
        this.connectionsViaProjects180Days = connectionsViaProjects180Days;
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

    public List<Double> getConnectionsViaProjects30DaysCountHistory() {
        return connectionsViaProjects30DaysCountHistory;
    }

    public void setConnectionsViaProjects30DaysCountHistory(List<Double> connectionsViaProjects30DaysCountHistory) {
        this.connectionsViaProjects30DaysCountHistory = connectionsViaProjects30DaysCountHistory;
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

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }
}
