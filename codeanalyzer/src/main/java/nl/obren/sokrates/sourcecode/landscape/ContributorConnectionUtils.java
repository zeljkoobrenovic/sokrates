package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorConnections;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositoryInfo;

import java.util.*;
import java.util.stream.Collectors;

public class ContributorConnectionUtils {

    public static final int MAX_HISTORY_LENGTH = 500000;
    public static final int MAX_PEOPLE_DEPENDENCIES_SIZE = 10000;

    public static List<ContributorConnection> getContributorConnections(List<ComponentDependency> peopleDependencies,
                                                                        List<Contributor> contributors, ContributionCounter contributionCounter) {
        List<ContributorConnection> contributorConnections = new ArrayList<>();
        Map<String, ContributorConnection> map = new HashMap<>();
        contributors.forEach(contributor -> {
            ContributorConnection contributorConnection = new ContributorConnection(contributor.getEmail(), contributor.getUserName(), 0, contributionCounter.count(contributor));
            map.put(contributor.getEmail(), contributorConnection);
            contributorConnections.add(contributorConnection);
        });
        peopleDependencies.forEach(dependency -> {
            String email1 = dependency.getFromComponent();
            String email2 = dependency.getToComponent();
            if (!email1.equalsIgnoreCase(email2)) {
                ContributorConnection contributorConnection1 = map.get(email1);
                if (contributorConnection1 != null) {
                    contributorConnection1.setCount(contributorConnection1.getCount() + 1);
                }
                ContributorConnection contributorConnection2 = map.get(email2);
                if (contributorConnection2 != null) {
                    contributorConnection2.setCount(contributorConnection2.getCount() + 1);
                }
            }
        });
        contributorConnections.sort((a, b) -> b.getCount() - a.getCount());
        return contributorConnections;
    }

    public static List<ComponentDependency> getPeopleDependencies(CodeAnalysisResults codeAnalysisResults, int daysAgo) {
        String processingName = "analysis/contributors/get people dependencies/" + daysAgo + " days";
        ProcessingStopwatch.start(processingName);
        Map<String, List<String>> contributionMap = new HashMap<>();

        codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory(Integer.MAX_VALUE).forEach(fileModificationHistory -> {
            fileModificationHistory.getCommits().stream()
                    .filter(commit -> DateUtils.isCommittedBetween(commit.getDate(), 0, daysAgo))
                    .forEach(commit -> {
                        String path = fileModificationHistory.getPath();
                        String email = commit.getEmail();
                        if (contributionMap.containsKey(path)) {
                            List<String> emails = contributionMap.get(path);
                            if (!emails.contains(email)) {
                                emails.add(email);
                            }
                        } else {
                            contributionMap.put(path, new ArrayList<>(Arrays.asList(email)));
                        }
                    });

        });

        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();

        contributionMap.keySet().forEach(path -> {
            List<String> emails = contributionMap.get(path);
            emails.forEach(email1 -> {
                emails.forEach(email2 -> {
                    if (dependencies.size() > MAX_PEOPLE_DEPENDENCIES_SIZE) {
                        return;
                    }
                    if (email1.equalsIgnoreCase(email2)) return;

                    String key1 = email1 + "::" + email2;
                    String key2 = email2 + "::" + email1;

                    ComponentDependency dependency;
                    if (dependenciesMap.containsKey(key1)) {
                        dependency = dependenciesMap.get(key1);
                    } else if (dependenciesMap.containsKey(key2)) {
                        dependency = dependenciesMap.get(key2);
                    } else {
                        dependency = new ComponentDependency(email1, email2);
                        dependenciesMap.put(key1, dependency);
                        dependencies.add(dependency);
                    }

                    if (!dependency.getData().contains(path)) {
                        dependency.getData().add(path);
                    }

                    dependency.setCount(dependency.getData().size());
                });
            });
        });
        dependencies.sort((a, b) -> b.getCount() - a.getCount());
        ProcessingStopwatch.end(processingName);
        return dependencies;
    }

    public static List<ComponentDependency> getPeopleFileDependencies(CodeAnalysisResults codeAnalysisResults, int daysAgo) {
        String processingName = "analysis/contributors/get people file dependencies/" + daysAgo + " days";
        ProcessingStopwatch.start(processingName);
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();

        codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory(Integer.MAX_VALUE).forEach(fileModificationHistory -> {
            fileModificationHistory.getCommits().stream()
                    .filter(commit -> DateUtils.isCommittedBetween(commit.getDate(), 0, daysAgo))
                    .forEach(commit -> {
                        if (dependencies.size() > MAX_PEOPLE_DEPENDENCIES_SIZE) {
                            return;
                        }
                        String path = "[" + fileModificationHistory.getPath() + "]";
                        String email = commit.getEmail();
                        String key1 = email + "::" + path;
                        String key2 = path + "::" + email;

                        ComponentDependency dependency;
                        if (dependenciesMap.containsKey(key1)) {
                            dependency = dependenciesMap.get(key1);
                        } else if (dependenciesMap.containsKey(key2)) {
                            dependency = dependenciesMap.get(key2);
                        } else {
                            dependency = new ComponentDependency(email, path);
                            dependenciesMap.put(key1, dependency);
                            dependencies.add(dependency);
                        }

                        dependency.setCount(dependency.getCount() + 1);
                    });
        });
        dependencies.sort((a, b) -> b.getCount() - a.getCount());
        ProcessingStopwatch.end(processingName);
        return dependencies;
    }

    public static List<ComponentDependency> getPeopleDependencies(List<ContributorRepositories> contributors, int daysAgo1, int daysAgo2) {
        Map<String, List<String>> repositoriesMap = new HashMap<>();

        contributors.stream()
                .forEach(contributorRepositories -> {
                    contributorRepositories.getRepositories().stream()
                            .filter(repository -> DateUtils.isAnyDateCommittedBetween(repository.getCommitDates(), daysAgo1, daysAgo2))
                            .forEach(repository -> {
                                String email = contributorRepositories.getContributor().getEmail();
                                String repositoryName = "[" + repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName() + "]";
                                if (repositoriesMap.containsKey(repositoryName)) {
                                    List<String> emails = repositoriesMap.get(repositoryName);
                                    if (!emails.contains(email)) {
                                        emails.add(email);
                                    }
                                } else {
                                    repositoriesMap.put(repositoryName, new ArrayList<>(Arrays.asList(email)));
                                }
                            });
                });

        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();
        Map<String, List<String>> repositoryNamesMap = new HashMap<>();

        repositoriesMap.keySet().forEach(repositoryName -> {
            List<String> emails = repositoriesMap.get(repositoryName);
            emails.forEach(email1 -> {
                emails.stream().filter(email2 -> !email1.equalsIgnoreCase(email2)).forEach(email2 -> {
                    if (dependencies.size() > MAX_PEOPLE_DEPENDENCIES_SIZE) {
                        return;
                    }
                    String key1 = email1 + "::" + email2;
                    String key2 = email2 + "::" + email1;

                    if (dependenciesMap.containsKey(key1)) {
                        if (!repositoryNamesMap.get(key1).contains(repositoryName)) {
                            dependenciesMap.get(key1).increment(1);
                            repositoryNamesMap.get(key1).add(repositoryName);
                        }
                    } else if (dependenciesMap.containsKey(key2)) {
                        if (!repositoryNamesMap.get(key2).contains(repositoryName)) {
                            dependenciesMap.get(key2).increment(1);
                            repositoryNamesMap.get(key2).add(repositoryName);
                        }
                    } else {
                        ComponentDependency dependency = new ComponentDependency(email1, email2);
                        dependenciesMap.put(key1, dependency);
                        dependencies.add(dependency);
                        repositoryNamesMap.put(key1, new ArrayList<>(Arrays.asList(repositoryName)));
                    }
                });
            });
        });

        return dependencies;
    }

    public static List<ComponentDependency> getPeopleRepositoryDependencies(List<ContributorRepositories> contributors, int daysAgo1, int daysAgo2) {
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();
        Map<String, List<String>> repositoryNamesMap = new HashMap<>();

        contributors.stream()
                .forEach(contributorRepositories -> {
                    contributorRepositories.getRepositories().stream()
                            .filter(repository -> DateUtils.isAnyDateCommittedBetween(repository.getCommitDates(), daysAgo1, daysAgo2))
                            .forEach(repository -> {
                                if (dependencies.size() > MAX_PEOPLE_DEPENDENCIES_SIZE) {
                                    return;
                                }
                                String email = contributorRepositories.getContributor().getEmail();
                                String repositoryName = "[" + repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName() + "]";
                                String key1 = email + "::" + repositoryName;
                                String key2 = repositoryName + "::" + email;

                                if (dependenciesMap.containsKey(key1)) {
                                    if (!repositoryNamesMap.get(key1).contains(repositoryName)) {
                                        dependenciesMap.get(key1).increment(1);
                                        repositoryNamesMap.get(key1).add(repositoryName);
                                    }
                                } else if (dependenciesMap.containsKey(key2)) {
                                    if (!repositoryNamesMap.get(key2).contains(repositoryName)) {
                                        dependenciesMap.get(key2).increment(1);
                                        repositoryNamesMap.get(key2).add(repositoryName);
                                    }
                                } else {
                                    ComponentDependency dependency = new ComponentDependency(email, repositoryName);
                                    dependenciesMap.put(key1, dependency);
                                    dependencies.add(dependency);
                                    repositoryNamesMap.put(key1, new ArrayList<>(Arrays.asList(repositoryName)));
                                }
                            });
                });

        return dependencies;
    }

    public static int getRepositoryCount(List<ContributorRepositories> contributors, String email, int daysAgo1, int daysAgo2) {
        Set<String> repositoryNames = new HashSet<>();
        contributors.stream().filter(c -> c.getContributor().getEmail().equalsIgnoreCase(email)).forEach(contributorRepositories -> {
            List<ContributorRepositoryInfo> repositories = contributorRepositories.getRepositories();
            repositories.stream().filter(p -> DateUtils.isAnyDateCommittedBetween(p.getCommitDates(), daysAgo1, daysAgo2)).forEach(repository -> {
                repositoryNames.add(repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName());
            });
        });

        return repositoryNames.size();
    }

    public static List<ContributorConnections> getConnectionsViaRepositories(List<ContributorRepositories> contributors, List<ComponentDependency> peopleDependencies, int daysAgo1, int daysAgo2) {
        Map<String, ContributorConnections> map = new HashMap<>();

        peopleDependencies.forEach(dependency -> {
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();

            ContributorConnections contributorConnections1 = map.get(from);
            ContributorConnections contributorConnections2 = map.get(to);

            if (contributorConnections1 == null) {
                contributorConnections1 = new ContributorConnections();
                contributorConnections1.setEmail(from);
                contributorConnections1.setRepositoriesCount(ContributorConnectionUtils.getRepositoryCount(contributors, from, daysAgo1, daysAgo2));
                contributorConnections1.setConnectionsCount(1);
                map.put(from, contributorConnections1);
            } else {
                contributorConnections1.setConnectionsCount(contributorConnections1.getConnectionsCount() + 1);
            }

            if (contributorConnections2 == null) {
                contributorConnections2 = new ContributorConnections();
                contributorConnections2.setEmail(to);
                contributorConnections2.setRepositoriesCount(ContributorConnectionUtils.getRepositoryCount(contributors, to, daysAgo1, daysAgo2));
                contributorConnections2.setConnectionsCount(1);
                map.put(to, contributorConnections2);
            } else {
                contributorConnections2.setConnectionsCount(contributorConnections2.getConnectionsCount() + 1);
            }
        });

        List<ContributorConnections> names = new ArrayList<>(map.values());
        names.sort((a, b) -> b.getConnectionsCount() - a.getConnectionsCount());

        return names;
    }

    public static List<ComponentDependency> getRepositoryDependenciesViaPeople(List<ContributorRepositories> contributors, int daysAgo1, int daysAgo2) {
        Map<String, ComponentDependency> map = new HashMap<>();

        contributors.forEach(contributorRepositories -> {
            List<ContributorRepositoryInfo> repositories = contributorRepositories.getRepositories().stream()
                    .filter(p -> DateUtils.isAnyDateCommittedBetween(p.getCommitDates(), daysAgo1, daysAgo2)).collect(Collectors.toList());
            repositories.forEach(repository1 -> {
                repositories.forEach(repository2 -> {
                    if (repository1 == repository2) return;

                    String name1 = "[" + repository1.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName() + "]";
                    String name2 = "[" + repository2.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName() + "]";

                    String key1 = name1 + "::" + name2;
                    String key2 = name2 + "::" + name1;

                    if (map.containsKey(key1)) {
                        map.get(key1).increment(1);
                    } else if (map.containsKey(key2)) {
                        map.get(key2).increment(1);
                    } else {
                        ComponentDependency dependency = new ComponentDependency(name1, name2);
                        map.put(key1, dependency);
                    }
                });
            });
        });

        List<ComponentDependency> repositoryDependencies = new ArrayList<>(map.values());
        repositoryDependencies.sort((a, b) -> b.getCount() - a.getCount());

        return repositoryDependencies;
    }

    public static double getCIndex(List<ContributorConnections> contributorConnections) {
        List<ContributorConnections> list = new ArrayList<>(contributorConnections);
        list.sort((a, b) -> b.getConnectionsCount() - a.getConnectionsCount());
        for (int factor = 0; factor < list.size(); factor++) {
            if (factor == list.get(factor).getConnectionsCount()) {
                return factor;
            } else if (factor > list.get(factor).getConnectionsCount()) {
                return factor - 1;
            }
        }
        return 0;
    }

    public static double getCMedian(List<ContributorConnections> contributorConnections) {
        List<ContributorConnections> list = new ArrayList<>(contributorConnections);
        list.sort((a, b) -> b.getConnectionsCount() - a.getConnectionsCount());
        int n = list.size();
        if (n > 0) {
            int middle = n / 2;
            if (n % 2 == 1) {
                return list.get(middle).getConnectionsCount();
            } else {
                return (list.get(middle - 1).getConnectionsCount() + list.get(middle).getConnectionsCount()) / 2.0;
            }
        }
        return 0;
    }

    public static double getCMean(List<ContributorConnections> contributorConnections) {
        List<ContributorConnections> list = new ArrayList<>(contributorConnections);
        list.sort((a, b) -> b.getConnectionsCount() - a.getConnectionsCount());
        if (list.size() > 0) {
            int total[] = {0};
            list.forEach(connections -> total[0] += connections.getConnectionsCount());
            return (double) total[0] / list.size();
        }
        return 0;
    }

    public static double getPMedian(List<ContributorConnections> contributorConnections) {
        List<ContributorConnections> list = new ArrayList<>(contributorConnections);
        ;
        list.sort((a, b) -> b.getRepositoriesCount() - a.getRepositoriesCount());
        int n = list.size();
        if (n > 0) {
            int middle = n / 2;
            if (n % 2 == 1) {
                return list.get(middle).getRepositoriesCount();
            } else {
                return (list.get(middle - 1).getRepositoriesCount() + list.get(middle).getRepositoriesCount()) / 2.0;
            }
        }
        return 0;
    }

    public static double getPMean(List<ContributorConnections> contributorConnections) {
        List<ContributorConnections> list = new ArrayList<>(contributorConnections);
        ;
        if (list.size() > 0) {
            int total[] = {0};
            list.forEach(connections -> total[0] += connections.getRepositoriesCount());
            return (double) total[0] / list.size();
        }
        return 0;
    }

    public static double getPIndex(List<ContributorConnections> contributorConnections) {
        List<ContributorConnections> list = new ArrayList<>(contributorConnections);
        list.sort((a, b) -> b.getRepositoriesCount() - a.getRepositoriesCount());
        for (int factor = 0; factor < list.size(); factor++) {
            if (factor == list.get(factor).getRepositoriesCount()) {
                return factor;
            } else if (factor > list.get(factor).getRepositoriesCount()) {
                return factor - 1;
            }
        }
        return 0;
    }

    public static long getContributorsActiveInPeriodCount(List<ContributorRepositories> contributors, int daysAgo1, int daysAgo2) {
        int count[] = {0};

        contributors.forEach(contributorRepoistories -> {
            boolean active[] = {false};
            contributorRepoistories.getRepositories().forEach(contributorRepository -> {
                if (DateUtils.isAnyDateCommittedBetween(contributorRepository.getCommitDates(), daysAgo1, daysAgo2)) {
                    active[0] = true;
                    return;
                }
            });

            if (active[0]) {
                count[0] += 1;
            }
        });

        return count[0];
    }
}
