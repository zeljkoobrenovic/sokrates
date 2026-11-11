/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyEvidence;

import java.util.*;

public class DuplicationDependenciesHelper {
    private List<ComponentDependency> componentDependencies = new ArrayList<>();
    private Map<String, ComponentDependency> componentDependenciesMap = new HashMap<>();
    private Map<String, Set<String>> componentDuplicatedLines = new HashMap<>();
    private List<DuplicationInstance> instances = new ArrayList<>();
    // Membership guard for `instances` so the list stays ordered/duplicate-free without an O(n) contains scan.
    private Set<DuplicationInstance> instancesSet = Collections.newSetFromMap(new IdentityHashMap<>());
    // Cache of each source file's first logical component name in `group` (null = no component in this group).
    // getLogicalComponents() allocates and streams on every call and is otherwise hit O(blocks^2) times per instance.
    private Map<SourceFile, String> componentNameCache = new HashMap<>();
    // Per-dependency set of file-pair keys already recorded as evidence, replacing ComponentDependency.hasPathFrom's
    // linear scan of the whole evidence list.
    private Map<ComponentDependency, Set<String>> dependencyFilePairKeys = new HashMap<>();

    private String group = "";

    public DuplicationDependenciesHelper(String group) {
        this.group = group;
    }

    public List<ComponentDependency> extractDependencies(List<DuplicationInstance> duplicationInstances) {
        duplicationInstances.forEach(instance -> {
            instance.getDuplicatedFileBlocks().forEach(file1 -> {
                String name1 = componentName(file1.getSourceFile());
                if (name1 != null) {
                    instance.getDuplicatedFileBlocks().forEach(file2 -> {
                        if (file1 != file2) {
                            String name2 = componentName(file2.getSourceFile());
                            if (name2 != null) {
                                processDuplicationInstance(file1, file2, name1, name2, instance);
                            }
                        }
                    });
                }
            });
        });

        return componentDependencies;
    }

    // Resolves (and memoizes) the first logical component name for a file in this group, mirroring the original
    // getLogicalComponents(group).get(0).getName() with a null sentinel for getLogicalComponents(group).isEmpty().
    private String componentName(SourceFile sourceFile) {
        if (componentNameCache.containsKey(sourceFile)) {
            return componentNameCache.get(sourceFile);
        }
        List<NamedSourceCodeAspect> components = sourceFile.getLogicalComponents(group);
        String name = components.isEmpty() ? null : components.get(0).getName();
        componentNameCache.put(sourceFile, name);
        return name;
    }

    private void processDuplicationInstance(DuplicatedFileBlock file1, DuplicatedFileBlock file2, String name1, String name2, DuplicationInstance instance) {
        if (!name1.equalsIgnoreCase(name2)) {
            if (instancesSet.add(instance)) instances.add(instance);
            Set<String> duplicatedLines = updateDuplicatedLines(file1, file2, name1, name2);
            ComponentDependency dependency = getDependency(name1, name2);
            int size = duplicatedLines.size();
            dependency.setCount(size);
            updateUniqueFilePairs(file1, file2, dependency);
        }
    }

    private void updateUniqueFilePairs(DuplicatedFileBlock file1, DuplicatedFileBlock file2, ComponentDependency dependency) {
        String key = file1.getSourceFile().getRelativePath()+ "\n" + file2.getSourceFile().getRelativePath();
        String alternativeKey = file2.getSourceFile().getRelativePath() + "\n" + file1.getSourceFile().getRelativePath();

        // Mirrors ComponentDependency.hasPathFrom (case-insensitive on pathFrom), but O(1) per check: the set
        // holds the lowercased evidence keys already added to this dependency.
        Set<String> seenKeys = dependencyFilePairKeys.computeIfAbsent(dependency, d -> new HashSet<>());
        if (!seenKeys.contains(key.toLowerCase()) && !seenKeys.contains(alternativeKey.toLowerCase())) {
            dependency.getEvidence().add(new DependencyEvidence(key, ""));
            seenKeys.add(key.toLowerCase());
        }
    }

    private Set<String> updateDuplicatedLines(DuplicatedFileBlock file1, DuplicatedFileBlock file2, String name1, String name2) {
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
            duplicatedLines.add(file1.getSourceFile().getRelativePath() + "::" + i);
        }
        for (int i = file2.getCleanedStartLine(); i <= file2.getCleanedEndLine(); i++) {
            duplicatedLines.add(file2.getSourceFile().getRelativePath() + "::" + i);
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

    public List<DuplicationInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<DuplicationInstance> instances) {
        this.instances = instances;
    }
}
