/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

public class LandscapeAnalysisResults {
    public static final int RECENT_THRESHOLD_DAYS = 30;
    private static final Log LOG = LogFactory.getLog(LandscapeAnalysisResults.class);
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

    @JsonIgnore
    private LandscapeConfiguration configuration = new LandscapeConfiguration();

    @JsonIgnore
    private Map<String, List<Contributor>> contributorsPerMonthMap = null;

    @JsonIgnore
    private Map<String, List<Contributor>> contributorsPerYearMap = null;

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
    private List<RepositoryAnalysisResults> repositoryAnalysisResults = new ArrayList<>();
    @JsonIgnore
    private List<ContributorRepositories> contributorsCache;

    public static SourceFileAgeDistribution getOverallFileLastModifiedDistribution(List<RepositoryAnalysisResults> repositoriesAnalysisResults) {
        SourceFileAgeDistribution distribution = new SourceFileAgeDistribution();
        repositoriesAnalysisResults.forEach(repositoryAnalysisResults -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getFilesHistoryAnalysisResults();
            SourceFileAgeDistribution repositoryDistribution = filesHistoryAnalysisResults.getOverallFileLastModifiedDistribution();
            if (repositoryDistribution == null) {
                return;
            }
            updateDistribution(distribution, repositoryDistribution);
        });
        return distribution;
    }

    public static SourceFileAgeDistribution getOverallFileFirstModifiedDistribution(List<RepositoryAnalysisResults> repositoriesAnalysisResults) {
        SourceFileAgeDistribution distribution = new SourceFileAgeDistribution();
        repositoriesAnalysisResults.forEach(repositoryAnalysisResults -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getFilesHistoryAnalysisResults();
            SourceFileAgeDistribution repositoryDistribution = filesHistoryAnalysisResults.getOverallFileFirstModifiedDistribution();
            if (repositoryDistribution == null) {
                return;
            }
            updateDistribution(distribution, repositoryDistribution);
        });
        return distribution;
    }

    private static void updateDistribution(SourceFileAgeDistribution distribution, SourceFileAgeDistribution repositoryDistribution) {
        distribution.setNegligibleRiskLabel(repositoryDistribution.getNegligibleRiskLabel());
        distribution.setNegligibleRiskCount(distribution.getNegligibleRiskCount() + repositoryDistribution.getNegligibleRiskCount());
        distribution.setNegligibleRiskValue(distribution.getNegligibleRiskValue() + repositoryDistribution.getNegligibleRiskValue());

        distribution.setLowRiskLabel(repositoryDistribution.getLowRiskLabel());
        distribution.setLowRiskCount(distribution.getLowRiskCount() + repositoryDistribution.getLowRiskCount());
        distribution.setLowRiskValue(distribution.getLowRiskValue() + repositoryDistribution.getLowRiskValue());

        distribution.setMediumRiskLabel(repositoryDistribution.getMediumRiskLabel());
        distribution.setMediumRiskCount(distribution.getMediumRiskCount() + repositoryDistribution.getMediumRiskCount());
        distribution.setMediumRiskValue(distribution.getMediumRiskValue() + repositoryDistribution.getMediumRiskValue());

        distribution.setHighRiskLabel(repositoryDistribution.getHighRiskLabel());
        distribution.setHighRiskCount(distribution.getHighRiskCount() + repositoryDistribution.getHighRiskCount());
        distribution.setHighRiskValue(distribution.getHighRiskValue() + repositoryDistribution.getHighRiskValue());

        distribution.setVeryHighRiskLabel(repositoryDistribution.getVeryHighRiskLabel());
        distribution.setVeryHighRiskCount(distribution.getVeryHighRiskCount() + repositoryDistribution.getVeryHighRiskCount());
        distribution.setVeryHighRiskValue(distribution.getVeryHighRiskValue() + repositoryDistribution.getVeryHighRiskValue());
    }

    public static int getLoc1YearActive(List<RepositoryAnalysisResults> repositoriesAnalysisResults) {
        int count[] = {0};
        repositoriesAnalysisResults.forEach(repositoryAnalysisResults -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getFilesHistoryAnalysisResults();
            SourceFileAgeDistribution overallFileLastModifiedDistribution = filesHistoryAnalysisResults.getOverallFileLastModifiedDistribution();
            if (overallFileLastModifiedDistribution != null) {
                count[0] += overallFileLastModifiedDistribution.getTotalValue() - overallFileLastModifiedDistribution.getVeryHighRiskValue();
            }
        });
        return count[0];
    }

    public static int getLoc30DaysActive(List<RepositoryAnalysisResults> repositoriesAnalysisResults) {
        int count[] = {0};
        repositoriesAnalysisResults.forEach(repositoryAnalysisResults -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getFilesHistoryAnalysisResults();
            SourceFileAgeDistribution overallFileLastModifiedDistribution = filesHistoryAnalysisResults.getOverallFileLastModifiedDistribution();
            if (overallFileLastModifiedDistribution != null) {
                count[0] += overallFileLastModifiedDistribution.getNegligibleRiskValue();
            }
        });
        return count[0];
    }

    public static int getLocNew(List<RepositoryAnalysisResults> repositoriesAnalysisResults) {
        int count[] = {0};
        repositoriesAnalysisResults.forEach(repositoryAnalysisResults -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getFilesHistoryAnalysisResults();
            SourceFileAgeDistribution overallFileFirstModifiedDistribution = filesHistoryAnalysisResults.getOverallFileFirstModifiedDistribution();
            if (overallFileFirstModifiedDistribution != null) {
                count[0] += overallFileFirstModifiedDistribution.getTotalValue() - overallFileFirstModifiedDistribution.getVeryHighRiskValue();
            }
        });
        return count[0];
    }

    public SourceFileAgeDistribution getOverallFileLastModifiedDistribution() {
        return getOverallFileLastModifiedDistribution(this.getFilteredRepositoryAnalysisResults());
    }

    public SourceFileAgeDistribution getOverallFileFirstModifiedDistribution() {
        return getOverallFileFirstModifiedDistribution(this.getFilteredRepositoryAnalysisResults());
    }

    public LandscapeConfiguration getConfiguration() {
        return configuration;
    }

    @JsonIgnore
    public void setConfiguration(LandscapeConfiguration configuration) {
        this.configuration = configuration;
    }

    @JsonIgnore
    public List<RepositoryAnalysisResults> getRepositoryAnalysisResults() {
        return repositoryAnalysisResults;
    }

    @JsonIgnore
    public void setRepositoryAnalysisResults(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        this.repositoryAnalysisResults = repositoryAnalysisResults;
    }

    @JsonIgnore
    public List<RepositoryAnalysisResults> getFilteredRepositoryAnalysisResults() {
        int thresholdLoc = configuration.getRepositoryThresholdLocMain();
        int thresholdContributors = configuration.getRepositoryThresholdContributors();

        String updatedBefore = configuration.getIgnoreRepositoriesLastUpdatedBefore();

        return repositoryAnalysisResults
                .stream()
                .filter(p -> StringUtils.isBlank(updatedBefore) ||
                        p.getAnalysisResults().getContributorsAnalysisResults().getLatestCommitDate().compareTo(updatedBefore) >= 0)
                .filter(p -> {
                    CodeAnalysisResults results = p.getAnalysisResults();
                    int contributorsCount = results.getContributorsAnalysisResults().getContributors().size();
                    return results.getMainAspectAnalysisResults().getLinesOfCode() >= thresholdLoc
                            && (contributorsCount == 0 || contributorsCount >= thresholdContributors);
                })
                .collect(Collectors.toList());
    }
    @JsonIgnore
    public List<RepositoryAnalysisResults> getIgnoredRepositoryAnalysisResults() {
        List<RepositoryAnalysisResults> filteredRepositoryAnalysisResults = getFilteredRepositoryAnalysisResults();

        return repositoryAnalysisResults
                .stream()
                .filter(p -> !filteredRepositoryAnalysisResults.contains(p))
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<RepositoryAnalysisResults> getAllRepositories() {
        return this.repositoryAnalysisResults;
    }

    private boolean ignoreExtension(String extension) {
        return configuration.getIgnoreExtensions().contains(extension);
    }

    @JsonIgnore
    public List<HistoryPerExtension> getYearlyCommitHistoryPerExtension() {
        Map<String, HistoryPerExtension> map = new HashMap<>();
        this.repositoryAnalysisResults.forEach(repository -> {
            List<HistoryPerExtension> history = repository.getAnalysisResults().getFilesHistoryAnalysisResults().getHistoryPerExtensionPerYear();
            history.stream().filter(e -> !ignoreExtension(e.getExtension())).forEach(extensionYear -> {
                String extension = extensionYear.getExtension().toLowerCase();
                String key = extension + "::" + extensionYear.getYear();
                if (map.containsKey(key)) {
                    map.get(key).setCommitsCount(map.get(key).getCommitsCount() + extensionYear.getCommitsCount());
                    map.get(key).getContributors().addAll(extensionYear.getContributors());
                } else {
                    HistoryPerExtension newHistoryPerExtension = new HistoryPerExtension(extension,
                            extensionYear.getYear(), extensionYear.getCommitsCount());
                    newHistoryPerExtension.getContributors().addAll(extensionYear.getContributors());
                    map.put(key, newHistoryPerExtension);
                }
            });
        });
        return new ArrayList<>(map.values());
    }

    public int getRepositoriesCount() {
        return getFilteredRepositoryAnalysisResults().size();
    }

    public int getMainLoc() {
        int loc[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            loc[0] += repositoryAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
        });
        return loc[0];
    }
    public int getMainFilesCount() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getSecondaryLoc() {
        return getTestLoc() + getGeneratedLoc() + getBuildAndDeploymentLoc() + getOtherLoc();
    }

    public int getSecondaryFilesCount() {
        return getTestFilesCount() + getGeneratedFilesCount() + getBuildAndDeploymentFilesCount() + getOtherFilesCount();
    }

    public int getMainLoc1YearActive() {
        return getLoc1YearActive(getFilteredRepositoryAnalysisResults());
    }

    public int getMainLoc30DaysActive() {
        return getLoc30DaysActive(getFilteredRepositoryAnalysisResults());
    }

    public int getMainLocNew() {
        return getLocNew(getFilteredRepositoryAnalysisResults());
    }

    public int getTestLoc() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getGeneratedLoc() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getBuildAndDeploymentLoc() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getOtherLoc() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }
    public int getTestFilesCount() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getGeneratedFilesCount() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getBuildAndDeploymentFilesCount() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getOtherFilesCount() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getAllLoc() {
        int count[] = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            count[0] += repositoryAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode();
            count[0] += repositoryAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode();
            count[0] += repositoryAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode();
            count[0] += repositoryAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public List<NumericMetric> getMainLinesOfCodePerExtension() {
        return getLinesOfCodePerExtension(CodeCategory.MAIN);
    }

    @JsonIgnore
    public List<NumericMetric> getTestLinesOfCodePerExtension() {
        return getLinesOfCodePerExtension(CodeCategory.TEST);
    }

    @JsonIgnore
    public List<NumericMetric> getOtherLinesOfCodePerExtension() {
        return getLinesOfCodePerExtension(CodeCategory.OTHER);
    }

    @JsonIgnore
    public List<NumericMetric> getLinesOfCodePerExtension(CodeCategory type) {
        List<NumericMetric> linesOfCodePerExtension = new ArrayList<>();
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            String repositoryName = repositoryAnalysisResults.getAnalysisResults().getMetadata().getName();
            List<NumericMetric> repositoryLinesOfCodePerExtension;
            if (type == CodeCategory.TEST) {
                repositoryLinesOfCodePerExtension = repositoryAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCodePerExtension();
            } else if (type == CodeCategory.OTHER) {
                List<NumericMetric> build = repositoryAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCodePerExtension();
                List<NumericMetric> generated = repositoryAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCodePerExtension();
                List<NumericMetric> other = repositoryAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCodePerExtension();
                repositoryLinesOfCodePerExtension = merge(Arrays.asList(build, generated, other));
            } else {
                repositoryLinesOfCodePerExtension = repositoryAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension();
            }
            repositoryLinesOfCodePerExtension.forEach(metric -> {
                String id = metric.getName().toLowerCase();
                Optional<NumericMetric> existingMetric = linesOfCodePerExtension.stream().filter(c -> c.getName().equalsIgnoreCase(id)).findAny();
                if (existingMetric.isPresent()) {
                    NumericMetric metricObject = existingMetric.get();
                    metricObject.getDescription().add(new NumericMetric(repositoryName, metric.getValue()));
                    metricObject.setValue(metricObject.getValue().intValue() + metric.getValue().intValue());
                } else {
                    NumericMetric metricObject = new NumericMetric(id, metric.getValue());
                    metricObject.getDescription().add(new NumericMetric(repositoryName, metric.getValue()));
                    linesOfCodePerExtension.add(metricObject);
                }
            });
        });

        Collections.sort(linesOfCodePerExtension, (a, b) -> b.getValue().intValue() - a.getValue().intValue());
        return linesOfCodePerExtension;
    }

    private List<NumericMetric> merge(List<List<NumericMetric>> metricLists) {
        List<NumericMetric> merged = new ArrayList<>();
        Map<String, NumericMetric> mergedMap = new HashMap<>();

        metricLists.forEach(list -> {
            list.forEach(metric -> {
                if (mergedMap.containsKey(metric.getName())) {
                    mergedMap.get(metric.getName()).setValue(mergedMap.get(metric.getName()).getValue().doubleValue() + metric.getValue().doubleValue());
                } else {
                    NumericMetric newMetric = new NumericMetric(metric.getName(), metric.getValue());
                    merged.add(newMetric);
                    mergedMap.put(newMetric.getName(), newMetric);
                }
            });
        });

        return merged;
    }

    @JsonIgnore
    public List<String> getAllExtensions() {
        List<String> extensions = new ArrayList<>();
        List<String> ignoreExtensions = configuration.getIgnoreExtensions();
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults().getCommitsPerExtensions().forEach(perExtension -> {
                String extension = perExtension.getExtension();
                if (!ignoreExtensions.contains(extension) && !extensions.contains(extension)) {
                    extensions.add(extension);
                }
            });
        });

        return extensions;
    }

    @JsonIgnore
    public List<ContributorRepositories> getContributors() {
        if (contributorsCache != null) {
            return contributorsCache;
        }
        int thresholdCommits = configuration.getContributorThresholdCommits();
        List<ContributorRepositories> contributorRepositories = getAllContributors().stream()
                .filter(c -> c.getContributor().getCommitsCount() >= thresholdCommits)
                .collect(Collectors.toCollection(ArrayList::new));

        if (configuration.isAnonymizeContributors()) {
            int counter[] = {1};
            contributorRepositories.forEach(contributorRepository -> {
                contributorRepository.getContributor().setEmail("Contributor " + counter[0]);
                counter[0] += 1;
            });
        }
        contributorsCache = contributorRepositories;
        return contributorRepositories;
    }

    @JsonIgnore
    public List<CommitsPerExtension> getContributorsPerExtension() {
        Map<String, CommitsPerExtension> commitsPerExtensions = new HashMap<>();

        getAllExtensions().forEach(extension -> {
            commitsPerExtensions.put(extension, new CommitsPerExtension(extension));
        });

        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            List<CommitsPerExtension> repositoryData = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults().getCommitsPerExtensions();

            repositoryData.forEach(repositoryExtData -> {
                String extension = repositoryExtData.getExtension();
                if (commitsPerExtensions.containsKey(extension)) {
                    CommitsPerExtension commitsPerExtension = commitsPerExtensions.get(extension);
                    commitsPerExtension.setCommitsCount(commitsPerExtension.getCommitsCount() + repositoryExtData.getCommitsCount());

                    commitsPerExtension.setCommitsCount30Days(commitsPerExtension.getCommitsCount30Days() + repositoryExtData.getCommitsCount30Days());
                    commitsPerExtension.setCommitsCount90Days(commitsPerExtension.getCommitsCount90Days() + repositoryExtData.getCommitsCount90Days());

                    commitsPerExtension.setFilesCount(commitsPerExtension.getFilesCount() + repositoryExtData.getFilesCount());
                    commitsPerExtension.setFilesCount30Days(commitsPerExtension.getFilesCount30Days() + repositoryExtData.getFilesCount30Days());
                    commitsPerExtension.setFilesCount90Days(commitsPerExtension.getFilesCount90Days() + repositoryExtData.getFilesCount90Days());

                    repositoryExtData.getCommitters().forEach(email -> {
                        if (!commitsPerExtension.getCommitters().contains(email)) {
                            commitsPerExtension.getCommitters().add(email);
                        }
                    });
                    repositoryExtData.getCommitters30Days().forEach(email -> {
                        if (!commitsPerExtension.getCommitters30Days().contains(email)) {
                            commitsPerExtension.getCommitters30Days().add(email);
                        }
                    });
                    repositoryExtData.getCommitters90Days().forEach(email -> {
                        if (!commitsPerExtension.getCommitters90Days().contains(email)) {
                            commitsPerExtension.getCommitters90Days().add(email);
                        }
                    });
                }
            });
        });


        configuration.getMergeExtensions().forEach(merge -> {
            CommitsPerExtension primary = commitsPerExtensions.get(merge.getPrimary());
            CommitsPerExtension secondary = commitsPerExtensions.get(merge.getSecondary());

            if (primary != null && secondary != null) {
                primary.setCommitsCount(primary.getCommitsCount() + secondary.getCommitsCount());
                primary.setCommitsCount30Days(primary.getCommitsCount30Days() + secondary.getCommitsCount30Days());
                primary.setCommitsCount90Days(primary.getCommitsCount90Days() + secondary.getCommitsCount90Days());
                primary.setFilesCount(primary.getFilesCount() + secondary.getFilesCount());
                primary.setFilesCount30Days(primary.getFilesCount30Days() + secondary.getFilesCount30Days());
                primary.setFilesCount90Days(primary.getFilesCount90Days() + secondary.getFilesCount90Days());
                secondary.getCommitters().stream()
                        .filter(c -> !primary.getCommitters().contains(c))
                        .forEach(commiter -> primary.getCommitters().add(commiter));
                secondary.getCommitters30Days().stream()
                        .filter(c -> !primary.getCommitters30Days().contains(c))
                        .forEach(commiter -> primary.getCommitters30Days().add(commiter));
                secondary.getCommitters90Days().stream()
                        .filter(c -> !primary.getCommitters90Days().contains(c))
                        .forEach(commiter -> primary.getCommitters90Days().add(commiter));

                commitsPerExtensions.remove(merge.getSecondary());
            }
        });

        ArrayList<CommitsPerExtension> list = new ArrayList<>(commitsPerExtensions.values());
        Collections.sort(list, (a, b) -> b.getCommitters30Days().size() - a.getCommitters30Days().size());

        return list;
    }

    @JsonIgnore
    private List<ContributorRepositories> getAllContributors() {
        List<ContributorRepositories> list = new ArrayList<>();
        Map<String, ContributorRepositories> map = new HashMap<>();

        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            contributorsAnalysisResults.getContributors().forEach(contributor -> {
                String contributorId = contributor.getEmail().toLowerCase();
                if (GitHistoryUtils.shouldIgnore(contributorId, configuration.getIgnoreContributors())) {
                    return;
                }
                if (configuration.getTransformContributorEmails().size() > 0) {
                    ComplexOperation operation = new ComplexOperation(configuration.getTransformContributorEmails());
                    contributorId = operation.exec(contributorId);
                    if (!contributorId.equals(contributor.getEmail())) {
                        LOG.info(contributor.getEmail() + " -> " + contributorId);
                    }
                }
                if (GitHistoryUtils.shouldIgnore(contributorId, configuration.getIgnoreContributors())) {
                    return;
                }

                if (StringUtils.isBlank(contributorId)) {
                    return;
                }

                int repositoryCommits = contributor.getCommitsCount();
                List<String> commitDates = contributor.getCommitDates();
                int repositoryCommits30Days = contributor.getCommitsCount30Days();
                int repositoryCommits90Days = contributor.getCommitsCount90Days();
                int repositoryCommits180Days = contributor.getCommitsCount180Days();
                int repositoryCommits365Days = contributor.getCommitsCount365Days();

                String latestCommitDate = contributor.getLatestCommitDate();
                String firstCommitDate = contributor.getFirstCommitDate();

                if (map.containsKey(contributorId)) {
                    ContributorRepositories existingContributor = map.get(contributorId);
                    Contributor contributorInfo = existingContributor.getContributor();

                    contributorInfo.setCommitsCount(contributorInfo.getCommitsCount() + repositoryCommits);
                    contributorInfo.setCommitsCount30Days(contributorInfo.getCommitsCount30Days() + repositoryCommits30Days);
                    contributorInfo.setCommitsCount90Days(contributorInfo.getCommitsCount90Days() + repositoryCommits90Days);
                    contributorInfo.setCommitsCount180Days(contributorInfo.getCommitsCount180Days() + repositoryCommits180Days);
                    contributorInfo.setCommitsCount365Days(contributorInfo.getCommitsCount365Days() + repositoryCommits365Days);

                    contributor.getActiveYears().forEach(activeYear -> {
                        if (!contributorInfo.getActiveYears().contains(activeYear)) {
                            contributorInfo.getActiveYears().add(activeYear);
                        }
                    });
                    contributor.getCommitDates().forEach(commitDate -> {
                        if (!contributorInfo.getCommitDates().contains(commitDate)) {
                            contributorInfo.getCommitDates().add(commitDate);
                        }
                    });

                    existingContributor.addRepository(repositoryAnalysisResults,
                            firstCommitDate, latestCommitDate,
                            repositoryCommits, repositoryCommits30Days, repositoryCommits90Days, new ArrayList<>(commitDates));

                    if (firstCommitDate.compareTo(contributorInfo.getFirstCommitDate()) < 0) {
                        contributorInfo.setFirstCommitDate(firstCommitDate);
                    }
                    if (latestCommitDate.compareTo(contributorInfo.getLatestCommitDate()) > 0) {
                        contributorInfo.setLatestCommitDate(latestCommitDate);
                    }
                } else {
                    Contributor newContributor = new Contributor();

                    newContributor.setEmail(contributorId);
                    newContributor.setCommitsCount(repositoryCommits);
                    newContributor.setCommitsCount30Days(repositoryCommits30Days);
                    newContributor.setCommitsCount90Days(repositoryCommits90Days);
                    newContributor.setCommitsCount180Days(repositoryCommits180Days);
                    newContributor.setCommitsCount365Days(repositoryCommits365Days);
                    newContributor.setFirstCommitDate(firstCommitDate);
                    newContributor.setLatestCommitDate(latestCommitDate);
                    newContributor.setActiveYears(new ArrayList<>(contributor.getActiveYears()));
                    newContributor.setCommitDates(new ArrayList<>(contributor.getCommitDates()));

                    ContributorRepositories newContributorWithRepositories = new ContributorRepositories(newContributor);

                    newContributorWithRepositories.addRepository(repositoryAnalysisResults, newContributor.getFirstCommitDate(),
                            newContributor.getLatestCommitDate(), repositoryCommits, repositoryCommits30Days, repositoryCommits90Days, new ArrayList<>(commitDates));

                    map.put(contributorId, newContributorWithRepositories);
                    list.add(newContributorWithRepositories);
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

        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            updateContributors(list, map, contributorsAnalysisResults.getContributorsPerYear());
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        return list;
    }

    @JsonIgnore
    public List<ContributionTimeSlot> getContributorsPerWeek() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerWeek = contributorsAnalysisResults.getContributorsPerWeek();
            updateContributors(list, map, contributorsPerWeek);
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot));

        return list;
    }

    @JsonIgnore
    public List<ContributionTimeSlot> getContributorsPerMonth() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerMonth = contributorsAnalysisResults.getContributorsPerMonth();
            updateContributors(list, map, contributorsPerMonth);
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        return list;
    }

    @JsonIgnore
    public List<Pair<String, List<ContributionTimeSlot>>> getContributorsPerRepositoryAndMonth() {
        List<Pair<String, List<ContributionTimeSlot>>> list = new ArrayList<>();

        getFilteredRepositoryAnalysisResults().forEach(repository -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repository.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>(contributorsAnalysisResults.getContributorsPerMonth());
            Collections.sort(contributorsPerMonth, Comparator.comparing(ContributionTimeSlot::getTimeSlot));
            String name = repository.getAnalysisResults().getMetadata().getName();
            list.add(Pair.of(name, contributorsPerMonth));
        });

        return list;
    }

    public List<Pair<String, List<ContributionTimeSlot>>> getContributorsCommits() {
        Map<String, Pair<String, Map<String, ContributionTimeSlot>>> map = new HashMap<>();

        getAllContributors().forEach(contributor -> {
            Map<String, ContributionTimeSlot> commits = new HashMap<>();
            contributor.getContributor().getCommitDates().forEach(commitDate -> {
                String month = DateUtils.getMonth(commitDate);
                if (commits.containsKey(month)) {
                    commits.get(month).setCommitsCount(commits.get(month).getCommitsCount() + 1);
                } else {
                    ContributionTimeSlot timeSlot = new ContributionTimeSlot(month);
                    timeSlot.setContributorsCount(1);
                    commits.put(month, timeSlot);
                }
            });
            String email = contributor.getContributor().getEmail();

            Pair<String, Map<String, ContributionTimeSlot>> pair = map.get(email);

            if (pair == null) {
                pair = Pair.of(email, new HashMap<>());
                map.put(email, pair);
            }

            Pair<String, Map<String, ContributionTimeSlot>> finalPair = pair;
            commits.values().forEach(commitTimeSlot -> {
                String timeSlot = commitTimeSlot.getTimeSlot();
                if (finalPair.getRight().containsKey(timeSlot)) {
                    finalPair.getRight().get(timeSlot).setCommitsCount(finalPair.getRight().get(timeSlot).getCommitsCount() + commitTimeSlot.getCommitsCount());
                } else {
                    ContributionTimeSlot contributionTimeSlot = new ContributionTimeSlot(timeSlot);
                    contributionTimeSlot.setCommitsCount(commitTimeSlot.getCommitsCount());
                    contributionTimeSlot.setContributorsCount(1);
                    finalPair.getRight().put(timeSlot, contributionTimeSlot);
                }
            });
        });

        List<Pair<String, List<ContributionTimeSlot>>> list = new ArrayList<>();

        map.values().forEach(pair -> list.add(Pair.of(pair.getLeft(), new ArrayList<>(pair.getRight().values()))));

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
        return this.repositoryAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults()
                        .getContributorsAnalysisResults().getCommitsCount()).sum();
    }

    public int getCommitsCount30Days() {
        return this.repositoryAnalysisResults.stream()
                .mapToInt(p -> p.getAnalysisResults()
                        .getContributorsAnalysisResults().getCommitsCount30Days()).sum();
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

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }

    enum CodeCategory {
        MAIN, TEST, OTHER
    }
}
