/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TemporalDependenciesHelper {
    public static final int DEPENDENCIES_LIST_LIMIT = 1000000;
    private static final Log LOG = LogFactory.getLog(TemporalDependenciesHelper.class);
    private List<ComponentDependency> componentDependencies = new ArrayList<>();
    private Map<String, ComponentDependency> componentDependenciesMap = new HashMap<>();
    private Map<ComponentDependency, Collection<String>> commitsMap = new HashMap<>();

    public TemporalDependenciesHelper() {
    }


    public List<ComponentDependency> extractComponentDependencies(String logicalDecompositionKey, List<FilePairChangedTogether> filePairsChangedTogether) {
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();
        Map<String, Set<String>> commitsMap = new HashMap<>();

        filePairsChangedTogether.forEach(pair -> {
            SourceFile sourceFile1 = pair.getSourceFile1();
            SourceFile sourceFile2 = pair.getSourceFile2();

            String component1 = getLogicalComponentName(logicalDecompositionKey, sourceFile1);
            String component2 = getLogicalComponentName(logicalDecompositionKey, sourceFile2);

            if (component1 != null && component2 != null) {
                String key1 = component1 + "::" + component2;
                String key2 = component2 + "::" + component1;

                ComponentDependency dependency = dependenciesMap.get(key1);
                Set<String> commits = commitsMap.get(key1);
                if (dependency == null) {
                    dependency = dependenciesMap.get(key2);
                    commits = commitsMap.get(key2);
                }
                if (dependency == null) {
                    dependency = new ComponentDependency(component1, component2);
                    dependenciesMap.put(key1, dependency);
                    dependencies.add(dependency);
                    commits = new HashSet<>();
                    commitsMap.put(key1, commits);
                }
                commits.addAll(pair.getCommits());
                dependency.setCount(commits.size());
            }
        });

        return dependencies;
    }

    private String getLogicalComponentName(String key, SourceFile sourceFile) {
        List<NamedSourceCodeAspect> compoenents = sourceFile.getLogicalComponents(key).stream().collect(Collectors.toList());
        return compoenents.size() > 0 ? compoenents.get(0).getName() : null;
    }

    public List<ComponentDependency> extractFileDependencies(List<FilePairChangedTogether> filePairInstances) {
        return extractFileDependencies(filePairInstances, DEPENDENCIES_LIST_LIMIT);
    }

    /**
     * The file dependencies in the order of the pairs, at most maxDependencies of them. Pairs beyond
     * that still add their commits to dependencies already in the list (the same file pair can occur
     * twice), so the kept dependencies have the same counts as in an unlimited run.
     */
    public List<ComponentDependency> extractFileDependencies(List<FilePairChangedTogether> filePairInstances, int maxDependencies) {
        filePairInstances.forEach(filePairChangedTogether -> {
            String file1 = filePairChangedTogether.getSourceFile1().getRelativePath();
            String file2 = filePairChangedTogether.getSourceFile2().getRelativePath();
            String component1 = "[" + file1 + "]";
            String component2 = "[" + file2 + "]";

            if (!component1.equalsIgnoreCase(component2)) {
                addDependency(filePairChangedTogether, component1, component2, maxDependencies);
            }
        });

        return componentDependencies;
    }

    public List<ComponentDependency> extractDependenciesWithCommits(List<FilePairChangedTogether> filePairInstances) {
        return extractDependenciesWithCommits(filePairInstances, DEPENDENCIES_LIST_LIMIT);
    }

    /**
     * The commit-file links in the order of the pairs, at most maxDependencies of them; pairs beyond
     * that still count towards the links already in the list.
     */
    public List<ComponentDependency> extractDependenciesWithCommits(List<FilePairChangedTogether> filePairInstances, int maxDependencies) {
        List<ComponentDependency> componentDependencies = new ArrayList<>();
        Map<String, ComponentDependency> componentDependenciesMap = new HashMap<>();
        boolean[] limitReported = {false};
        filePairInstances.forEach(filePairChangedTogether -> {
            String file1 = filePairChangedTogether.getSourceFile1().getRelativePath();
            String file2 = filePairChangedTogether.getSourceFile2().getRelativePath();
            String component1 = "[" + file1 + "]";
            String component2 = "[" + file2 + "]";

            if (!component1.equalsIgnoreCase(component2)) {
                filePairChangedTogether.getCommits().forEach(commit -> {
                    String commitId = "commit_" + commit;
                    boolean create = componentDependencies.size() < maxDependencies;
                    ComponentDependency dependency1 = getDependency(commitId, component1, componentDependencies, componentDependenciesMap, create);
                    if (dependency1 != null) {
                        dependency1.setCount(dependency1.getCount() + 1);
                    }
                    create = componentDependencies.size() < maxDependencies;
                    ComponentDependency dependency2 = getDependency(commitId, component2, componentDependencies, componentDependenciesMap, create);
                    if (dependency2 != null) {
                        dependency2.setCount(dependency2.getCount() + 1);
                    }
                });
            }

            if (componentDependencies.size() >= maxDependencies && !limitReported[0]) {
                limitReported[0] = true;
                LOG.info("Reached the limit of the graph size (" + maxDependencies + " dependencies)");
            }
        });

        return componentDependencies;
    }

    private void addDependency(FilePairChangedTogether filePairChangedTogether, String component1, String component2, int maxDependencies) {
        ComponentDependency dependency = getDependency(component1, component2, componentDependencies, componentDependenciesMap,
                componentDependencies.size() < maxDependencies);
        if (dependency == null) {
            return;
        }

        List<String> pairCommits = filePairChangedTogether.getCommits();
        Collection<String> commits = commitsMap.get(dependency);
        if (commits == null) {
            // The first pair of a dependency is kept by reference, not copied: a file pair usually
            // occurs once, so a set per dependency would only duplicate the pair's commit list.
            commitsMap.put(dependency, pairCommits);
            dependency.setCount(pairCommits.size() <= 1 ? pairCommits.size() : new HashSet<>(pairCommits).size());
        } else {
            Set<String> union = commits instanceof Set ? (Set<String>) commits : new HashSet<>(commits);
            union.addAll(pairCommits);
            commitsMap.put(dependency, union);
            dependency.setCount(union.size());
        }
    }

    private ComponentDependency getDependency(String name1, String name2,
                                              List<ComponentDependency> componentDependencies, Map<String, ComponentDependency> componentDependenciesMap) {
        return getDependency(name1, name2, componentDependencies, componentDependenciesMap, true);
    }

    private ComponentDependency getDependency(String name1, String name2,
                                              List<ComponentDependency> componentDependencies, Map<String, ComponentDependency> componentDependenciesMap,
                                              boolean create) {
        String key = name1 + "::" + name2;
        String alternativeKey = name2 + "::" + name1;

        ComponentDependency componentDependency = componentDependenciesMap.get(key);
        if (componentDependency == null) {
            componentDependency = componentDependenciesMap.get(alternativeKey);
            if (componentDependency == null && create) {
                componentDependency = new ComponentDependency(name1, name2);
                componentDependency.setCount(0);

                componentDependencies.add(componentDependency);
                componentDependenciesMap.put(key, componentDependency);
            }
        }

        return componentDependency;
    }

}
