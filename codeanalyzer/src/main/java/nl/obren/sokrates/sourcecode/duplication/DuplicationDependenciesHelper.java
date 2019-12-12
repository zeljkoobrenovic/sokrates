/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;

import java.util.*;

public class DuplicationDependenciesHelper {
    private List<ComponentDependency> componentDependencies = new ArrayList<>();
    private Map<String, ComponentDependency> componentDependenciesMap = new HashMap<>();
    private Map<String, Set<String>> componentDuplicatedLines = new HashMap<>();

    private String group = "";

    public DuplicationDependenciesHelper(String group) {
        this.group = group;
    }

    public List<ComponentDependency> extractDependencies(List<DuplicationInstance> duplicationInstances) {

        duplicationInstances.forEach(instance -> {
            instance.getDuplicatedFileBlocks().forEach(file1 -> {
                List<NamedSourceCodeAspect> logicalComponents1 = file1.getSourceFile().getLogicalComponents(group);
                if (logicalComponents1.size() > 0) {
                    instance.getDuplicatedFileBlocks().forEach(file2 -> {
                        processDuplicationInstance(file1, logicalComponents1, file2);
                    });
                }
            });
        });

        // componentDependencies.forEach(d -> d.setCount(d.getCount() / 2));

        return componentDependencies;
    }

    private void processDuplicationInstance(DuplicatedFileBlock file1, List<NamedSourceCodeAspect> logicalComponents1, DuplicatedFileBlock file2) {
        if (file1 != file2) {
            List<NamedSourceCodeAspect> logicalComponents2 = file2.getSourceFile().getLogicalComponents(group);
            if (logicalComponents2.size() > 0) {
                String name1 = logicalComponents1.get(0).getName();
                String name2 = logicalComponents2.get(0).getName();
                if (!name1.equalsIgnoreCase(name2)) {
                    Set<String> duplicatedLines = updatedDuplicatedLines(componentDuplicatedLines, file1, file2, name1, name2);
                    ComponentDependency dependency = getDependency(name1, name2);
                    dependency.setCount(duplicatedLines.size());
                    String key = file1.getSourceFile().getRelativePath() + "\n" + file2.getSourceFile().getRelativePath();
                    String alternativeKey = file2.getSourceFile().getRelativePath() + "\n" + file1.getSourceFile().getRelativePath();
                    if (!dependency.getPathsFrom().contains(key) && !dependency.getPathsFrom().contains(alternativeKey)) {
                        dependency.getPathsFrom().add(key);
                    }
                }
            }
        }
    }

    private Set<String> updatedDuplicatedLines(Map<String, Set<String>> componentDuplicatedLines, DuplicatedFileBlock file1, DuplicatedFileBlock file2, String name1, String name2) {
        String key = name1 + "::" + name2;
        String alternativeKey = name2 + "::" + name1;

        Set<String> duplicatedLines;
        if (componentDuplicatedLines.containsKey(key)) {
            duplicatedLines = componentDuplicatedLines.get(key);
        } else if (componentDuplicatedLines.containsKey(alternativeKey)) {
            duplicatedLines = componentDuplicatedLines.get(alternativeKey);
        } else {
            duplicatedLines = new HashSet<>();
            componentDuplicatedLines.put(key, duplicatedLines);
        }
        for (int i = file1.getCleanedStartLine(); i <= file1.getCleanedEndLine(); i++) {
            String duplicatedLineId = file1.getSourceFile().getRelativePath() + "::" + i;
            duplicatedLines.add(duplicatedLineId);
        }
        for (int i = file2.getCleanedStartLine(); i <= file2.getCleanedEndLine(); i++) {
            String duplicatedLineId = file2.getSourceFile().getRelativePath() + "::" + i;
            duplicatedLines.add(duplicatedLineId);
        }
        return duplicatedLines;
    }

    private ComponentDependency getDependency(String name1, String name2) {
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
}
