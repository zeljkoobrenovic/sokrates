/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspectUtils;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import nl.obren.sokrates.sourcecode.landscape.utils.EmailTransformations;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
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
    private TeamsConfig teamsConfig;
    @JsonIgnore
    private PeopleConfig peopleConfig;

    @JsonIgnore
    private Set<String> level1SubLandscapes = new HashSet<>();
    @JsonIgnore
    private List<ComponentDependency> subLandscapeDependenciesViaRepositoriesWithSameContributors = new ArrayList<>();

    @JsonIgnore
    private List<ComponentDependency> subLandscapeIndirectDependenciesViaRepositoriesWithSameContributors = new ArrayList<>();

    @JsonIgnore
    private List<ComponentDependency> subLandscapeDependenciesViaRepositoriesWithSameName = new ArrayList<>();

    @JsonIgnore
    private List<ComponentDependency> subLandscapeIndirectDependenciesViaRepositoriesWithSameName = new ArrayList<>();

    @JsonIgnore
    private LandscapeConfiguration configuration = new LandscapeConfiguration();

    @JsonIgnore
    private final Map<String, List<Contributor>> contributorsPerMonthMap = null;

    @JsonIgnore
    private final Map<String, List<Contributor>> contributorsPerYearMap = null;

    private final PeopleConnectionsMetrics peopleConnections = new PeopleConnectionsMetrics();
    private String firstCommitDate = "";

    private String latestCommitDate = "";

    @JsonIgnore
    private List<RepositoryAnalysisResults> repositoryAnalysisResults = new ArrayList<>();
    @JsonIgnore
    private List<ContributorRepositories> contributorsCache;
    @JsonIgnore
    private List<ContributorRepositories> teamsCache;
    @JsonIgnore
    private List<ContributorRepositories> botsCache;

    public LandscapeAnalysisResults(TeamsConfig teamsConfig, PeopleConfig peopleConfig) {
        this.teamsConfig = teamsConfig;
        this.peopleConfig = peopleConfig;
    }

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
        int[] count = {0};
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
        int[] count = {0};
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
        int[] count = {0};
        repositoriesAnalysisResults.forEach(repositoryAnalysisResults -> {
            FilesHistoryAnalysisResults filesHistoryAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getFilesHistoryAnalysisResults();
            SourceFileAgeDistribution overallFileFirstModifiedDistribution = filesHistoryAnalysisResults.getOverallFileFirstModifiedDistribution();
            if (overallFileFirstModifiedDistribution != null) {
                count[0] += overallFileFirstModifiedDistribution.getTotalValue() - overallFileFirstModifiedDistribution.getVeryHighRiskValue();
            }
        });
        return count[0];
    }

    @JsonIgnore
    public Set<String> getLevel1SubLandscapes() {
        return level1SubLandscapes;
    }

    @JsonIgnore
    public void setLevel1SubLandscapes(Set<String> level1SubLandscapes) {
        this.level1SubLandscapes = level1SubLandscapes;
    }

    @JsonIgnore
    public List<ComponentDependency> getSubLandscapeDependenciesViaRepositoriesWithSameContributors() {
        return subLandscapeDependenciesViaRepositoriesWithSameContributors;
    }

    @JsonIgnore
    public void setSubLandscapeDependenciesViaRepositoriesWithSameContributors(List<ComponentDependency> subLandscapeDependenciesViaRepositoriesWithSameContributors) {
        this.subLandscapeDependenciesViaRepositoriesWithSameContributors = subLandscapeDependenciesViaRepositoriesWithSameContributors;
    }

    @JsonIgnore
    public List<ComponentDependency> getSubLandscapeIndirectDependenciesViaRepositoriesWithSameContributors() {
        return subLandscapeIndirectDependenciesViaRepositoriesWithSameContributors;
    }

    @JsonIgnore
    public void setSubLandscapeIndirectDependenciesViaRepositoriesWithSameContributors(List<ComponentDependency> subLandscapeIndirectDependenciesViaRepositoriesWithSameContributors) {
        this.subLandscapeIndirectDependenciesViaRepositoriesWithSameContributors = subLandscapeIndirectDependenciesViaRepositoriesWithSameContributors;
    }

    @JsonIgnore
    public List<ComponentDependency> getSubLandscapeDependenciesViaRepositoriesWithSameName() {
        return subLandscapeDependenciesViaRepositoriesWithSameName;
    }

    @JsonIgnore
    public void setSubLandscapeDependenciesViaRepositoriesWithSameName(List<ComponentDependency> subLandscapeDependenciesViaRepositoriesWithSameName) {
        this.subLandscapeDependenciesViaRepositoriesWithSameName = subLandscapeDependenciesViaRepositoriesWithSameName;
    }

    @JsonIgnore
    public List<ComponentDependency> getSubLandscapeIndirectDependenciesViaRepositoriesWithSameName() {
        return subLandscapeIndirectDependenciesViaRepositoriesWithSameName;
    }

    @JsonIgnore
    public void setSubLandscapeIndirectDependenciesViaRepositoriesWithSameName(List<ComponentDependency> subLandscapeIndirectDependenciesViaRepositoriesWithSameName) {
        this.subLandscapeIndirectDependenciesViaRepositoriesWithSameName = subLandscapeIndirectDependenciesViaRepositoriesWithSameName;
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
        int[] loc = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            loc[0] += repositoryAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
        });
        return loc[0];
    }

    public int getMainFilesCount() {
        int[] count = {0};
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
        int[] count = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getGeneratedLoc() {
        int[] count = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getBuildAndDeploymentLoc() {
        int[] count = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getOtherLoc() {
        int[] count = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    public int getTestFilesCount() {
        int[] count = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getGeneratedFilesCount() {
        int[] count = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getBuildAndDeploymentFilesCount() {
        int[] count = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getOtherFilesCount() {
        int[] count = {0};
        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            count[0] += repositoryAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    public int getAllLoc() {
        int[] count = {0};
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
                // older reports name these "  *.java"; current ones "java" — merge both under the bare extension
                String id = SourceCodeAspectUtils.extensionName(metric.getName());
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
                String name = SourceCodeAspectUtils.extensionName(metric.getName());
                if (mergedMap.containsKey(name)) {
                    mergedMap.get(name).setValue(mergedMap.get(name).getValue().doubleValue() + metric.getValue().doubleValue());
                } else {
                    NumericMetric newMetric = new NumericMetric(name, metric.getValue());
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
                .filter(c -> !isBot(c.getContributor().getEmail()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (configuration.isAnonymizeContributors()) {
            int[] counter = {1};
            contributorRepositories.forEach(contributorRepository -> {
                contributorRepository.getContributor().setEmail("Email " + counter[0]);
                contributorRepository.getContributor().setUserName("User " + counter[0]);
                counter[0] += 1;
            });
        }
        contributorsCache = contributorRepositories;
        return contributorRepositories;
    }

    @JsonIgnore
    public List<ContributorRepositories> getTeams() {
        if (teamsCache != null) {
            return teamsCache;
        }
        List<ContributorRepositories> teamRepositories = getAllTeams()
                .stream().collect(Collectors.toCollection(ArrayList::new));

        teamsCache = teamRepositories;
        return teamRepositories;
    }

    private boolean isBot(String email) {
        // Case-insensitive bot identity matching.
        return RegexUtils.matchesAnyPatternIgnoreCase(email, configuration.getBots());
    }

    @JsonIgnore
    public List<ContributorRepositories> getBots() {
        if (botsCache != null) {
            return botsCache;
        }
        int thresholdCommits = configuration.getContributorThresholdCommits();
        List<ContributorRepositories> contributorRepositories = getAllContributors().stream()
                .filter(c -> c.getContributor().getCommitsCount() >= thresholdCommits)
                .filter(c -> isBot(c.getContributor().getEmail()))
                .collect(Collectors.toCollection(ArrayList::new));

        botsCache = contributorRepositories;
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
                CommitsPerExtension commitsPerExtension = commitsPerExtensions.get(repositoryExtData.getExtension());
                if (commitsPerExtension != null) {
                    addCommitsPerExtension(commitsPerExtension, repositoryExtData,
                            email -> EmailTransformations.transformEmail(email, configuration.getTransformContributorEmails(), peopleConfig));
                }
            });
        });

        configuration.getMergeExtensions().forEach(merge -> {
            CommitsPerExtension primary = commitsPerExtensions.get(merge.getPrimary());
            CommitsPerExtension secondary = commitsPerExtensions.get(merge.getSecondary());

            if (primary != null && secondary != null) {
                addCommitsPerExtension(primary, secondary, email -> email);
                commitsPerExtensions.remove(merge.getSecondary());
            }
        });

        ArrayList<CommitsPerExtension> list = new ArrayList<>(commitsPerExtensions.values());
        Collections.sort(list, (a, b) -> b.getCommitters30Days().size() - a.getCommitters30Days().size());

        return list;
    }

    /** Adds the source's commit and file counts to the target and its committers (mapped through {@code contributorId}) when not yet present. */
    private static void addCommitsPerExtension(CommitsPerExtension target, CommitsPerExtension source, java.util.function.Function<String, String> contributorId) {
        target.setCommitsCount(target.getCommitsCount() + source.getCommitsCount());
        target.setCommitsCount30Days(target.getCommitsCount30Days() + source.getCommitsCount30Days());
        target.setCommitsCount90Days(target.getCommitsCount90Days() + source.getCommitsCount90Days());
        target.setFilesCount(target.getFilesCount() + source.getFilesCount());
        target.setFilesCount30Days(target.getFilesCount30Days() + source.getFilesCount30Days());
        target.setFilesCount90Days(target.getFilesCount90Days() + source.getFilesCount90Days());
        addCommitters(target.getCommitters(), source.getCommitters(), contributorId);
        addCommitters(target.getCommitters30Days(), source.getCommitters30Days(), contributorId);
        addCommitters(target.getCommitters90Days(), source.getCommitters90Days(), contributorId);
    }

    private static void addCommitters(List<String> target, List<String> emails, java.util.function.Function<String, String> contributorId) {
        emails.forEach(email -> {
            String id = contributorId.apply(email);
            if (!target.contains(id)) {
                target.add(id);
            }
        });
    }

    private LandscapeContributorsAggregator contributorsAggregator() {
        return new LandscapeContributorsAggregator(configuration, peopleConfig, teamsConfig, this::isBot);
    }

    private List<ContributorRepositories> getAllContributors() {
        return contributorsAggregator().contributors(getFilteredRepositoryAnalysisResults());
    }

    private List<ContributorRepositories> getAllTeams() {
        return contributorsAggregator().teams(getAllContributors());
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
    public List<ContributionTimeSlot> getContributorsPerDay() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerDay = contributorsAnalysisResults.getContributorsPerDay();
            updateContributors(list, map, contributorsPerDay);
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

    // Per-scope aggregations: sum each repository's per-scope time-slot maps (main/test/build/
    // generated/other/unscoped) into landscape-level totals, mirroring the all-scope getters above.
    // The per-scope maps already live on each repository's serialized ContributorsAnalysisResults
    // (read from analysisResults.json), so no re-analysis is needed. Repositories analyzed before
    // per-scope churn existed simply have empty maps and contribute nothing to a scope, so the
    // per-scope totals can be smaller than the all-scope total until every repository is re-analyzed.
    @JsonIgnore
    public Map<String, List<ContributionTimeSlot>> getContributorsPerYearByScope() {
        return aggregatePerScope(ContributorsAnalysisResults::getContributorsPerYearByScope, true);
    }

    @JsonIgnore
    public Map<String, List<ContributionTimeSlot>> getContributorsPerMonthByScope() {
        return aggregatePerScope(ContributorsAnalysisResults::getContributorsPerMonthByScope, true);
    }

    @JsonIgnore
    public Map<String, List<ContributionTimeSlot>> getContributorsPerWeekByScope() {
        return aggregatePerScope(ContributorsAnalysisResults::getContributorsPerWeekByScope, false);
    }

    @JsonIgnore
    public Map<String, List<ContributionTimeSlot>> getContributorsPerDayByScope() {
        return aggregatePerScope(ContributorsAnalysisResults::getContributorsPerDayByScope, false);
    }

    private Map<String, List<ContributionTimeSlot>> aggregatePerScope(
            java.util.function.Function<ContributorsAnalysisResults, Map<String, List<ContributionTimeSlot>>> perScopeMapGetter,
            boolean reversed) {
        // scope -> (timeSlot -> aggregated slot); LinkedHashMap preserves first-seen scope order.
        Map<String, List<ContributionTimeSlot>> resultLists = new LinkedHashMap<>();
        Map<String, Map<String, ContributionTimeSlot>> indexByScope = new LinkedHashMap<>();

        getFilteredRepositoryAnalysisResults().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            Map<String, List<ContributionTimeSlot>> perScope = perScopeMapGetter.apply(contributorsAnalysisResults);
            if (perScope == null) {
                return;
            }
            perScope.forEach((scope, slots) -> {
                List<ContributionTimeSlot> list = resultLists.computeIfAbsent(scope, k -> new ArrayList<>());
                Map<String, ContributionTimeSlot> map = indexByScope.computeIfAbsent(scope, k -> new HashMap<>());
                updateContributors(list, map, slots);
            });
        });

        Comparator<ContributionTimeSlot> comparator = Comparator.comparing(ContributionTimeSlot::getTimeSlot);
        if (reversed) {
            comparator = comparator.reversed();
        }
        for (List<ContributionTimeSlot> list : resultLists.values()) {
            Collections.sort(list, comparator);
        }

        return resultLists;
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

    @JsonIgnore
    public List<Pair<String, List<ContributionTimeSlot>>> getContributorsCommits() {
        Map<String, Pair<String, Map<String, ContributionTimeSlot>>> map = new HashMap<>();

        getAllContributors().forEach(contributor -> {
            Map<String, ContributionTimeSlot> commits = new HashMap<>();
            contributor.getContributor().getCommitDates().forEach(commitDate -> {
                String month = DateUtils.getMonth(commitDate);
                if (commits.containsKey(month)) {
                    commits.get(month).setCommitsCount(commits.get(month).getCommitsCount() + 1);
                } else {
                    ContributionTimeSlot timeSlot = new ContributionTimeSlot(month, Thresholds.defaultCommitFilesCountThresholds());
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
                    ContributionTimeSlot contributionTimeSlot = new ContributionTimeSlot(timeSlot, Thresholds.defaultCommitFilesCountThresholds());
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
                contributionTimeSlot = new ContributionTimeSlot(Thresholds.defaultFileUpdateFrequencyThresholds());
                contributionTimeSlot.setTimeSlot(timeSlot.getTimeSlot());
                contributionTimeSlot.setContributorsCount(timeSlot.getContributorsCount());
                contributionTimeSlot.setCommitsCount(timeSlot.getCommitsCount());
                contributionTimeSlot.setAiCoAuthoredCommitsCount(timeSlot.getAiCoAuthoredCommitsCount());
                contributionTimeSlot.addChurn(timeSlot.getLinesAdded(), timeSlot.getLinesDeleted());
                list.add(contributionTimeSlot);
                map.put(timeSlot.getTimeSlot(), contributionTimeSlot);
            } else {
                contributionTimeSlot.setContributorsCount(contributionTimeSlot.getContributorsCount() + timeSlot.getContributorsCount());
                contributionTimeSlot.setCommitsCount(contributionTimeSlot.getCommitsCount() + timeSlot.getCommitsCount());
                contributionTimeSlot.setAiCoAuthoredCommitsCount(contributionTimeSlot.getAiCoAuthoredCommitsCount() + timeSlot.getAiCoAuthoredCommitsCount());
                contributionTimeSlot.addChurn(timeSlot.getLinesAdded(), timeSlot.getLinesDeleted());
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

    public int getContributorsCount(List<ContributorRepositories> contributors) {
        return contributors.size();
    }

    @JsonIgnore
    public List<ContributorRepositories> getRecentContributors(List<ContributorRepositories> contributors) {
        return contributors.stream().filter(c -> c.getContributor().getCommitsCount30Days() > 0).collect(Collectors.toCollection(ArrayList::new));
    }

    public int getRecentContributorsCount() {
        Set<String> ids = new HashSet<>();

        for (RepositoryAnalysisResults repository: repositoryAnalysisResults) {
            for (Contributor contributor : repository.getAnalysisResults().getContributorsAnalysisResults().getContributors()) {
                if (contributor.getCommitsCount30Days() > 0) {
                    ids.add(contributor.getEmail());
                }
            }

        }
        return ids.size();
    }

    public int getRecentContributorsCount(List<ContributorRepositories> contributors) {
        return getRecentContributors(contributors).size();
    }

    public int getRecentContributorsCount6Months(List<ContributorRepositories> contributors) {
        return (int) contributors.stream().filter(c -> c.getContributor().isActive(180)).count();
    }

    public int getRecentContributorsCount3Months(List<ContributorRepositories> contributors) {
        return (int) contributors.stream().filter(c -> c.getContributor().isActive(90)).count();
    }

    public int getRookiesContributorsCount(List<ContributorRepositories> contributors) {
        return (int) contributors.stream().filter(c -> c.getContributor().isRookie(RECENT_THRESHOLD_DAYS)).count();
    }

    /** The people-connection metrics (dependencies, connections, c/p index-mean-median per window and their histories); serialized flat, as before. */
    @JsonUnwrapped
    public PeopleConnectionsMetrics getPeopleConnections() {
        return peopleConnections;
    }

    public String getFirstCommitDate() {
        return firstCommitDate;
    }

    public void setFirstCommitDate(String firstCommitDate) {
        this.firstCommitDate = firstCommitDate;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }

    public TeamsConfig getTeamsConfig() {
        return teamsConfig;
    }

    public void setTeamsConfig(TeamsConfig teamsConfig) {
        this.teamsConfig = teamsConfig;
    }

    public PeopleConfig getPeopleConfig() {
        return peopleConfig;
    }

    public void setPeopleConfig(PeopleConfig peopleConfig) {
        this.peopleConfig = peopleConfig;
    }

    enum CodeCategory {
        MAIN, TEST, OTHER
    }

}
