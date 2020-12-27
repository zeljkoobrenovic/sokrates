package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorConnections;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjectInfo;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjects;

import java.util.*;
import java.util.stream.Collectors;

public class ContributorConnectionUtils {
    public static List<ContributorConnection> getContributorConnections(List<ComponentDependency> peopleDependencies,
                                                                        List<Contributor> contributors, ContributionCounter contributionCounter) {
        List<ContributorConnection> contributorConnections = new ArrayList<>();
        Map<String, ContributorConnection> map = new HashMap<>();
        contributors.forEach(contributor -> {
            ContributorConnection contributorConnection = new ContributorConnection(contributor.getEmail(), 0, contributionCounter.count(contributor));
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
        Map<String, List<String>> contributionMap = new HashMap<>();

        codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory().forEach(fileModificationHistory -> {
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
                    if (email1.equalsIgnoreCase(email2)) return;

                    String key1 = email1 + "::" + email2;
                    String key2 = email2 + "::" + email1;

                    if (dependenciesMap.containsKey(key1)) {
                        dependenciesMap.get(key1).increment(1);
                    } else if (dependenciesMap.containsKey(key2)) {
                        dependenciesMap.get(key2).increment(1);
                    } else {
                        ComponentDependency dependency = new ComponentDependency(email1, email2);
                        dependenciesMap.put(key1, dependency);
                        dependencies.add(dependency);
                    }
                });
            });
        });
        dependencies.sort((a, b) -> b.getCount() - a.getCount());
        return dependencies;
    }

    public static List<ComponentDependency> getPeopleDependencies(List<ContributorProjects> contributors, int daysAgo1, int daysAgo2) {
        Map<String, List<String>> projectsMap = new HashMap<>();

        contributors.stream()
                .forEach(contributorProjects -> {
                    contributorProjects.getProjects().stream()
                            .filter(project -> DateUtils.isAnyDateCommittedBetween(project.getCommitDates(), daysAgo1, daysAgo2))
                            .forEach(project -> {
                                String email = contributorProjects.getContributor().getEmail();
                                String projectName = project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName();
                                if (projectsMap.containsKey(projectName)) {
                                    List<String> emails = projectsMap.get(projectName);
                                    if (!emails.contains(email)) {
                                        emails.add(email);
                                    }
                                } else {
                                    projectsMap.put(projectName, new ArrayList<>(Arrays.asList(email)));
                                }
                            });
                });

        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();
        Map<String, List<String>> projectNamesMap = new HashMap<>();

        projectsMap.keySet().forEach(projectName -> {
            List<String> emails = projectsMap.get(projectName);
            emails.forEach(email1 -> {
                emails.stream().filter(email2 -> !email1.equalsIgnoreCase(email2)).forEach(email2 -> {
                    String key1 = email1 + "::" + email2;
                    String key2 = email2 + "::" + email1;

                    if (dependenciesMap.containsKey(key1)) {
                        if (!projectNamesMap.get(key1).contains(projectName)) {
                            dependenciesMap.get(key1).increment(1);
                            projectNamesMap.get(key1).add(projectName);
                        }
                    } else if (dependenciesMap.containsKey(key2)) {
                        if (!projectNamesMap.get(key2).contains(projectName)) {
                            dependenciesMap.get(key2).increment(1);
                            projectNamesMap.get(key2).add(projectName);
                        }
                    } else {
                        ComponentDependency dependency = new ComponentDependency(email1, email2);
                        dependenciesMap.put(key1, dependency);
                        dependencies.add(dependency);
                        projectNamesMap.put(key1, new ArrayList<>(Arrays.asList(projectName)));
                    }
                });
            });
        });

        return dependencies;
    }

    public static int getProjectCount(List<ContributorProjects> contributors, String email, int daysAgo1, int daysAgo2) {
        Set<String> projectNames = new HashSet<>();
        contributors.stream().filter(c -> c.getContributor().getEmail().equalsIgnoreCase(email)).forEach(contributorProjects -> {
            List<ContributorProjectInfo> projects = contributorProjects.getProjects();
            projects.stream().filter(p -> DateUtils.isAnyDateCommittedBetween(p.getCommitDates(), daysAgo1, daysAgo2)).forEach(project -> {
                projectNames.add(project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName());
            });
        });

        return projectNames.size();
    }

    public static List<ContributorConnections> getConnectionsViaProjects(List<ContributorProjects> contributors, List<ComponentDependency> peopleDependencies, int daysAgo1, int daysAgo2) {
        Map<String, ContributorConnections> map = new HashMap<>();

        peopleDependencies.forEach(dependency -> {
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();

            ContributorConnections contributorConnections1 = map.get(from);
            ContributorConnections contributorConnections2 = map.get(to);

            if (contributorConnections1 == null) {
                contributorConnections1 = new ContributorConnections();
                contributorConnections1.setEmail(from);
                contributorConnections1.setProjectsCount(ContributorConnectionUtils.getProjectCount(contributors, from, daysAgo1, daysAgo2));
                contributorConnections1.setConnectionsCount(1);
                map.put(from, contributorConnections1);
            } else {
                contributorConnections1.setConnectionsCount(contributorConnections1.getConnectionsCount() + 1);
            }

            if (contributorConnections2 == null) {
                contributorConnections2 = new ContributorConnections();
                contributorConnections2.setEmail(to);
                contributorConnections2.setProjectsCount(ContributorConnectionUtils.getProjectCount(contributors, to, daysAgo1, daysAgo2));
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

    public static List<ComponentDependency> getProjectDependenciesViaPeople(List<ContributorProjects> contributors, int daysAgo1, int daysAgo2) {
        Map<String, ComponentDependency> map = new HashMap<>();

        contributors.forEach(contributorProjects -> {
            List<ContributorProjectInfo> projects = contributorProjects.getProjects().stream()
                    .filter(p -> DateUtils.isAnyDateCommittedBetween(p.getCommitDates(), daysAgo1, daysAgo2)).collect(Collectors.toList());
            projects.forEach(project1 -> {
                projects.forEach(project2 -> {
                    if (project1 == project2) return;

                    String name1 = project1.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName();
                    String name2 = project2.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName();

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

        List<ComponentDependency> projectDependencies = new ArrayList<>(map.values());
        projectDependencies.sort((a, b) -> b.getCount() - a.getCount());

        return projectDependencies;
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
        list.sort((a, b) -> b.getProjectsCount() - a.getProjectsCount());
        int n = list.size();
        if (n > 0) {
            int middle = n / 2;
            if (n % 2 == 1) {
                return list.get(middle).getProjectsCount();
            } else {
                return (list.get(middle - 1).getProjectsCount() + list.get(middle).getProjectsCount()) / 2.0;
            }
        }
        return 0;
    }

    public static double getPMean(List<ContributorConnections> contributorConnections) {
        List<ContributorConnections> list = new ArrayList<>(contributorConnections);
        ;
        if (list.size() > 0) {
            int total[] = {0};
            list.forEach(connections -> total[0] += connections.getProjectsCount());
            return (double) total[0] / list.size();
        }
        return 0;
    }

    public static double getPIndex(List<ContributorConnections> contributorConnections) {
        List<ContributorConnections> list = new ArrayList<>(contributorConnections);
        list.sort((a, b) -> b.getProjectsCount() - a.getProjectsCount());
        for (int factor = 0; factor < list.size(); factor++) {
            if (factor == list.get(factor).getProjectsCount()) {
                return factor;
            } else if (factor > list.get(factor).getProjectsCount()) {
                return factor - 1;
            }
        }
        return 0;
    }

    public static long getContributorsActiveInPeriodCount(List<ContributorProjects> contributors, int daysAgo1, int daysAgo2) {
        int count[] = {0};

        contributors.forEach(contributorProjects -> {
            boolean active[] = {false};
            contributorProjects.getProjects().forEach(contributorProject -> {
                if (DateUtils.isAnyDateCommittedBetween(contributorProject.getCommitDates(), daysAgo1, daysAgo2)) {
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
