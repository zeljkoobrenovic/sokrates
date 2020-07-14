/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionYear;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.*;
import java.util.stream.Collectors;

public class LandscapeAnalysisResults {
    private LandscapeConfiguration configuration = new LandscapeConfiguration();
    private List<ProjectAnalysisResults> projectAnalysisResults = new ArrayList<>();

    public LandscapeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(LandscapeConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<ProjectAnalysisResults> getProjectAnalysisResults() {
        return projectAnalysisResults;
    }

    @JsonIgnore
    public List<ProjectAnalysisResults> getFilteredProjectAnalysisResults() {
        int thresholdLoc = configuration.getProjectThresholdLocMain();
        int thresholdContributors = configuration.getProjectThresholdContributors();

        return projectAnalysisResults.stream()
                .filter(p -> {
                    CodeAnalysisResults results = p.getAnalysisResults();
                    return results.getMainAspectAnalysisResults().getLinesOfCode() >= thresholdLoc
                            && results.getContributorsAnalysisResults().getContributors().size() >= thresholdContributors;
                })
                .collect(Collectors.toList());
    }

    public void setProjectAnalysisResults(List<ProjectAnalysisResults> projectAnalysisResults) {
        this.projectAnalysisResults = projectAnalysisResults;
    }

    @JsonIgnore
    public List<ProjectAnalysisResults> getAllProjects() {
        return this.projectAnalysisResults;
    }

    @JsonIgnore
    public int getProjectsCount() {
        return projectAnalysisResults.size();
    }

    @JsonIgnore
    public int getMainLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getTestLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getGeneratedLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getBuildAndDeploymentLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getOtherLoc() {
        int count[] = {0};
        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
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
    public List<ContributorProject> getContributors() {
        int thresholdCommits = configuration.getContributorThresholdCommits();
        return getAllContributors().stream()
                .filter(c -> c.getContributor().getCommitsCount() >= thresholdCommits)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @JsonIgnore
    public List<ContributorProject> getAllContributors() {
        List<ContributorProject> list = new ArrayList<>();
        Map<String, ContributorProject> map = new HashMap<>();

        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            contributorsAnalysisResults.getContributors().forEach(contributor -> {
                String contributorId = contributor.getId();
                int projectCommits = contributor.getCommitsCount();

                if (map.containsKey(contributorId)) {
                    ContributorProject existingContributor = map.get(contributorId);
                    existingContributor.getContributor().setCommitsCount(existingContributor.getContributor().getCommitsCount() + projectCommits);
                    existingContributor.addProject(projectAnalysisResults, projectCommits);
                    if (contributor.getFirstCommitDate().compareTo(existingContributor.getContributor().getFirstCommitDate()) < 0) {
                        existingContributor.getContributor().setFirstCommitDate(contributor.getFirstCommitDate());
                    }
                    if (contributor.getLatestCommitDate().compareTo(existingContributor.getContributor().getLatestCommitDate()) > 0) {
                        existingContributor.getContributor().setLatestCommitDate(contributor.getLatestCommitDate());
                    }
                } else {
                    Contributor newContributor = new Contributor();

                    newContributor.setName(contributor.getName());
                    newContributor.setEmail(contributor.getEmail());
                    newContributor.setCommitsCount(projectCommits);
                    newContributor.setFirstCommitDate(contributor.getFirstCommitDate());
                    newContributor.setLatestCommitDate(contributor.getLatestCommitDate());

                    ContributorProject newContributorWithProjects = new ContributorProject(newContributor);
                    newContributorWithProjects.addProject(projectAnalysisResults, projectCommits);

                    map.put(contributorId, newContributorWithProjects);
                    list.add(newContributorWithProjects);
                }
            });
        });

        Collections.sort(list, (a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount());

        return list;
    }

    public List<ContributionYear> getContributorsPerYear() {
        List<ContributionYear> list = new ArrayList<>();
        Map<String, ContributionYear> map = new HashMap<>();

        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            contributorsAnalysisResults.getContributorsPerYear().forEach(year -> {
                ContributionYear contributionYear = map.get(year.getYear());
                if (contributionYear == null) {
                    contributionYear = new ContributionYear();
                    contributionYear.setYear(year.getYear());
                    contributionYear.setContributorsCount(year.getContributorsCount());
                    contributionYear.setCommitsCount(year.getCommitsCount());
                    list.add(contributionYear);
                    map.put(year.getYear(), contributionYear);
                } else {
                    contributionYear.setContributorsCount(contributionYear.getContributorsCount() + year.getContributorsCount());
                    contributionYear.setCommitsCount(contributionYear.getCommitsCount() + year.getCommitsCount());
                }
            });
        });

        Collections.sort(list, Comparator.comparing(ContributionYear::getYear));

        return list;
    }

    @JsonIgnore
    public int getCommitsCount() {
        return this.projectAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults()
                .getContributorsAnalysisResults().getCommitsCount()).sum();
    }
}
