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
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
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
    public List<ContributorProject> getContributors() {
        int thresholdCommits = configuration.getContributorThresholdCommits();
        List<ContributorProject> contributorProjects = getAllContributors().stream()
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
    public List<ContributorProject> getAllContributors() {
        List<ContributorProject> list = new ArrayList<>();
        Map<String, ContributorProject> map = new HashMap<>();

        getFilteredProjectAnalysisResults().forEach(projectAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            contributorsAnalysisResults.getContributors().forEach(contributor -> {
                String contributorId = contributor.getEmail();
                int projectCommits = contributor.getCommitsCount();
                int projectCommits30Days = contributor.getCommitsCount30Days();
                int projectCommits90Days = contributor.getCommitsCount90Days();

                if (map.containsKey(contributorId)) {
                    ContributorProject existingContributor = map.get(contributorId);
                    existingContributor.getContributor().setCommitsCount(existingContributor.getContributor().getCommitsCount() + projectCommits);

                    existingContributor.getContributor().setCommitsCount30Days(
                            existingContributor.getContributor().getCommitsCount30Days() + projectCommits30Days);

                    existingContributor.getContributor().setCommitsCount90Days(
                            existingContributor.getContributor().getCommitsCount30Days() + projectCommits90Days);

                    existingContributor.addProject(projectAnalysisResults, projectCommits);
                    if (contributor.getFirstCommitDate().compareTo(existingContributor.getContributor().getFirstCommitDate()) < 0) {
                        existingContributor.getContributor().setFirstCommitDate(contributor.getFirstCommitDate());
                    }
                    if (contributor.getLatestCommitDate().compareTo(existingContributor.getContributor().getLatestCommitDate()) > 0) {
                        existingContributor.getContributor().setLatestCommitDate(contributor.getLatestCommitDate());
                    }
                } else {
                    Contributor newContributor = new Contributor();

                    newContributor.setEmail(contributor.getEmail());
                    newContributor.setCommitsCount(projectCommits);
                    newContributor.setCommitsCount30Days(projectCommits30Days);
                    newContributor.setCommitsCount90Days(projectCommits90Days);
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
