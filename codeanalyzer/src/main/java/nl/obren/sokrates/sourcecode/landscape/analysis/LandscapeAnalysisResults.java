/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionYear;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;

import java.util.*;
import java.util.stream.Collectors;

public class LandscapeAnalysisResults {
    public static final int RECENT_THRESHOLD_DAYS = 40;

    @JsonIgnore
    private LandscapeConfiguration configuration = new LandscapeConfiguration();

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
                int projectCommits = contributor.getCommitsCount();
                int projectCommits30Days = contributor.getCommitsCount30Days();
                int projectCommits90Days = contributor.getCommitsCount90Days();

                if (map.containsKey(contributorId)) {
                    ContributorProjects existingContributor = map.get(contributorId);
                    Contributor contributorInfo = existingContributor.getContributor();
                    contributorInfo.setCommitsCount(contributorInfo.getCommitsCount() + projectCommits);

                    contributorInfo.setCommitsCount30Days(
                            contributorInfo.getCommitsCount30Days() + projectCommits30Days);

                    contributorInfo.setCommitsCount90Days(
                            contributorInfo.getCommitsCount30Days() + projectCommits90Days);

                    contributorInfo.getActiveYears().addAll(contributor.getActiveYears());

                    existingContributor.addProject(projectAnalysisResults,
                            contributorInfo.getFirstCommitDate(), contributorInfo.getLatestCommitDate(), projectCommits);

                    if (contributor.getFirstCommitDate().compareTo(contributorInfo.getFirstCommitDate()) < 0) {
                        contributorInfo.setFirstCommitDate(contributor.getFirstCommitDate());
                    }
                    if (contributor.getLatestCommitDate().compareTo(contributorInfo.getLatestCommitDate()) > 0) {
                        contributorInfo.setLatestCommitDate(contributor.getLatestCommitDate());
                    }
                } else {
                    Contributor newContributor = new Contributor();

                    newContributor.setEmail(contributor.getEmail());
                    newContributor.setCommitsCount(projectCommits);
                    newContributor.setCommitsCount30Days(projectCommits30Days);
                    newContributor.setCommitsCount90Days(projectCommits90Days);
                    newContributor.setFirstCommitDate(contributor.getFirstCommitDate());
                    newContributor.setLatestCommitDate(contributor.getLatestCommitDate());
                    newContributor.setActiveYears(contributor.getActiveYears());

                    ContributorProjects newContributorWithProjects = new ContributorProjects(newContributor);

                    newContributorWithProjects.addProject(projectAnalysisResults,
                            newContributor.getFirstCommitDate(), newContributor.getLatestCommitDate(), projectCommits);

                    map.put(contributorId, newContributorWithProjects);
                    list.add(newContributorWithProjects);
                }
            });
        });

        Collections.sort(list, (a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount());

        return list;
    }

    @JsonIgnore
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

    public int getCommitsCount() {
        return this.projectAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults()
                        .getContributorsAnalysisResults().getCommitsCount()).sum();
    }

    public int getContributorsCount() {
        return getContributors().size();
    }

    public int getRecentContributorsCount() {
        return (int) getContributors().stream().filter(c -> c.getContributor().isActive(RECENT_THRESHOLD_DAYS)).count();
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

}
