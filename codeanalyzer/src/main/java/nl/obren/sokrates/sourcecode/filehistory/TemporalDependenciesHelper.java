/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemporalDependenciesHelper {
    private static final Log LOG = LogFactory.getLog(TemporalDependenciesHelper.class);

    private List<ComponentDependency> componentDependencies = new ArrayList<>();
    private Map<String, ComponentDependency> componentDependenciesMap = new HashMap<>();
    private Map<ComponentDependency, List<String>> datesMap = new HashMap<>();

    public TemporalDependenciesHelper() {
    }

    public List<ComponentDependency> extractDependencies(List<FilePairChangedTogether> filePairInstances) {
        filePairInstances.forEach(filePairChangedTogether -> {
            String file1 = filePairChangedTogether.getSourceFile1().getRelativePath();
            String file2 = filePairChangedTogether.getSourceFile2().getRelativePath();
            String component1 = "[" + file1 + "]";
            String component2 = "[" + file2 + "]";

            if (!component1.equalsIgnoreCase(component2)) {
                addDependency(filePairChangedTogether, component1, component2);
            }
        });

        return componentDependencies;
    }

    public List<ComponentDependency> extractDependenciesWithCommits(List<FilePairChangedTogether> filePairInstances) {
        List<ComponentDependency> componentDependencies = new ArrayList<>();
        Map<String, ComponentDependency> componentDependenciesMap = new HashMap<>();
        filePairInstances.forEach(filePairChangedTogether -> {
            String file1 = filePairChangedTogether.getSourceFile1().getRelativePath();
            String file2 = filePairChangedTogether.getSourceFile2().getRelativePath();
            String component1 = "[" + file1 + "]";
            String component2 = "[" + file2 + "]";

            if (!component1.equalsIgnoreCase(component2)) {
                filePairChangedTogether.getCommits().forEach(commit -> {
                    String commitId = "commit_" + commit;
                    ComponentDependency dependency1 = getDependency(commitId, component1, componentDependencies, componentDependenciesMap);
                    dependency1.setCount(dependency1.getCount() + 1);
                    ComponentDependency dependency2 = getDependency(commitId, component2, componentDependencies, componentDependenciesMap);
                    dependency2.setCount(dependency2.getCount() + 1);
                });
            }

            if (componentDependencies.size() > 100000) {
                LOG.info("Reached the limit of graphsize");
                return;
            }
        });

        return componentDependencies;
    }

    private void addDependency(FilePairChangedTogether filePairChangedTogether, String component1, String component2) {
        ComponentDependency dependency = getDependency(component1, component2);

        dependency.setCount(dependency.getCount() + 1);

        List<String> commits = datesMap.get(dependency);
        if (commits == null) {
            commits = new ArrayList<>();
            datesMap.put(dependency, commits);
        }

        List<String> finalCommits = commits;
        filePairChangedTogether.getCommits().forEach(commit -> {
            if (!finalCommits.contains(commit)) {
                finalCommits.add(commit);
            }
        });
        dependency.setCount(finalCommits.size());
    }

    private ComponentDependency getDependency(String name1, String name2,
                                              List<ComponentDependency> componentDependencies,  Map<String, ComponentDependency> componentDependenciesMap) {
        String key = name1 + "::" + name2;
        String alternativeKey = name2 + "::" + name1;

        ComponentDependency componentDependency = componentDependenciesMap.get(key);
        if (componentDependency == null) {
            componentDependency = componentDependenciesMap.get(alternativeKey);
            if (componentDependency == null) {
                componentDependency = new ComponentDependency(name1, name2);
                componentDependency.setCount(0);

                componentDependencies.add(componentDependency);
                componentDependenciesMap.put(key, componentDependency);
            }
        }

        return componentDependency;
    }

    private ComponentDependency getDependency(String name1, String name2) {
        return getDependency(name1, name2, componentDependencies, componentDependenciesMap);
    }
}
