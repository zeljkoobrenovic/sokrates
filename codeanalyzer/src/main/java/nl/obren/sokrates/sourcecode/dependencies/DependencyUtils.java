/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyUtils {

    public static void addDependency(List<Dependency> dependencies, SourceFileDependency sourceFileDependency, DependencyAnchor sourceAnchor, DependencyAnchor targetAnchor) {
        addDependency(dependencies, null, sourceFileDependency, sourceAnchor, targetAnchor);
    }

    /**
     * Adds a (source -> target) dependency, merging into an existing one when present. When
     * {@code dependencyIndex} is non-null it is used (and kept in sync) for O(1) lookup of the
     * existing dependency instead of an O(n) scan of {@code dependencies}; the list still owns the
     * iteration order. Callers in a loop should pass a shared, mutable index.
     */
    public static void addDependency(List<Dependency> dependencies, Map<String, Dependency> dependencyIndex,
                                     SourceFileDependency sourceFileDependency, DependencyAnchor sourceAnchor, DependencyAnchor targetAnchor) {
        String targetKey = targetAnchor.getAnchor();
        String sourceKey = sourceAnchor.getAnchor();
        if (!targetKey.equalsIgnoreCase(sourceKey)) {
            Dependency newDependency = new Dependency(sourceAnchor, targetAnchor);
            Dependency existingDependency = dependencyIndex != null
                    ? dependencyIndex.get(newDependency.getDependencyString())
                    : (dependencies.contains(newDependency) ? dependencies.get(dependencies.indexOf(newDependency)) : null);
            if (existingDependency != null) {
                existingDependency.getFromFiles().add(sourceFileDependency);
            } else {
                dependencies.add(newDependency);
                newDependency.getFromFiles().add(sourceFileDependency);
                if (dependencyIndex != null) {
                    dependencyIndex.put(newDependency.getDependencyString(), newDependency);
                }
            }
        }
    }

    public static int getDependenciesCount(List<ComponentDependency> componentDependencies) {
        int[] count = {0};
        componentDependencies.forEach(d -> count[0] += d.getCount());
        return count[0];
    }

    public static int getCyclicDependencyPlacesCount(List<ComponentDependency> componentDependencies) {
        // For each d1, the original counted every distinct d2 whose edge is the reverse of d1's, then
        // halved the total. Indexing the reverse edges by frequency reproduces that in O(n).
        Map<String, Integer> reverseEdgeCounts = reverseEdgeFrequencies(componentDependencies);
        int count = 0;
        for (ComponentDependency d1 : componentDependencies) {
            Integer reverseCount = reverseEdgeCounts.get(edgeKey(d1.getFromComponent(), d1.getToComponent()));
            if (reverseCount != null) {
                count += reverseCount;
            }
        }
        return count / 2;
    }

    public static int getCyclicDependencyCount(List<ComponentDependency> componentDependencies) {
        Map<String, Integer> reverseEdgeCounts = reverseEdgeFrequencies(componentDependencies);
        int count = 0;
        for (ComponentDependency d1 : componentDependencies) {
            Integer reverseCount = reverseEdgeCounts.get(edgeKey(d1.getFromComponent(), d1.getToComponent()));
            if (reverseCount != null) {
                // The original added d1.getCount() once per matching reverse d2.
                count += d1.getCount() * reverseCount;
            }
        }
        return count;
    }

    // Frequency of each "to -> from" edge, so that for a given d1 the number of d2 that form a cycle
    // with it is reverseEdgeFrequencies.get("d1.from -> d1.to"). A dependency never counts itself
    // because isCyclic requires from != to (a self-edge's reverse is itself and is excluded below).
    private static Map<String, Integer> reverseEdgeFrequencies(List<ComponentDependency> componentDependencies) {
        Map<String, Integer> frequencies = new HashMap<>();
        componentDependencies.forEach(d -> {
            if (!d.getFromComponent().equals(d.getToComponent())) {
                frequencies.merge(edgeKey(d.getToComponent(), d.getFromComponent()), 1, Integer::sum);
            }
        });
        return frequencies;
    }

    private static String edgeKey(String fromComponent, String toComponent) {
        return fromComponent + " -> " + toComponent;
    }

    public static List<ComponentDependency> getComponentDependencies(List<Dependency> dependencies, String groupName) {
        List<ComponentDependency> componentDependencies = new ArrayList<>();
        // O(1) lookups in place of the previous O(n) list scans: dedup of (file -> target component)
        // links and lookup of an existing component dependency to merge into.
        Set<String> fileToComponentLinks = new HashSet<>();
        Map<String, ComponentDependency> componentDependencyIndex = new HashMap<>();
        dependencies.forEach(dependency -> {
            dependency.getFromFiles().forEach(sourceFileDependency -> {
                List<NamedSourceCodeAspect> fromComponents = dependency.getFromComponents(groupName);
                List<NamedSourceCodeAspect> toComponents = dependency.getToComponents(groupName);
                for (NamedSourceCodeAspect toComponent : toComponents) {
                    for (NamedSourceCodeAspect fromComponent : fromComponents) {
                        if (fromComponent.getName().equalsIgnoreCase(toComponent.getName())) {
                            return;
                        }
                    }
                }
                toComponents.forEach(targetComponent -> {
                    String fileToComponentLink = sourceFileDependency.getSourceFile().getFile().getPath() + "::" +
                            targetComponent.getName();
                    if (fileToComponentLinks.add(fileToComponentLink)) {
                        sourceFileDependency.getSourceFile().getLogicalComponents(groupName).forEach(sourceComponent -> {
                            addComponentDependency(sourceFileDependency, componentDependencies, componentDependencyIndex, sourceComponent, targetComponent);
                        });
                    }
                });
            });
        });

        return componentDependencies;
    }

    private static void addComponentDependency(SourceFileDependency sourceFileDependency, List<ComponentDependency> componentDependencies,
            Map<String, ComponentDependency> componentDependencyIndex, NamedSourceCodeAspect sourceComponent, NamedSourceCodeAspect targetComponent) {
        if (!sourceComponent.getName().equalsIgnoreCase(targetComponent.getName())) {
            ComponentDependency componentDependency = new ComponentDependency(sourceComponent.getName(),
                    targetComponent.getName());
            ComponentDependency existing = componentDependencyIndex.get(componentDependency.getDependencyString());
            if (existing != null) {
                componentDependency = existing;
                componentDependency.setCount(componentDependency.getCount() + 1);
            } else {
                componentDependency.setCount(1);
                componentDependencies.add(componentDependency);
                componentDependencyIndex.put(componentDependency.getDependencyString(), componentDependency);
            }

            SourceFile sourceFile = sourceFileDependency.getSourceFile();
            componentDependency.setLocFrom(componentDependency.getLocFrom() + sourceFile.getLinesOfCode());
            componentDependency.getEvidence().add(new DependencyEvidence(sourceFile.getRelativePath(), sourceFileDependency.getCodeFragment()));
        }
    }

    public static void findErrors(List<DependencyAnchor> anchors, List<DependencyError> errors) {
        anchors.forEach(anchor -> {
            Map<String, List<String>> componentsMap = new HashMap<>();
            anchor.getSourceFiles().forEach(sourceFile -> {
                sourceFile.getLogicalComponents().forEach(component -> {
                    List<String> components = componentsMap.computeIfAbsent(component.getFiltering(), k -> new ArrayList<>());
                    if (!components.contains(component.getName())) {
                        components.add(component.getName());
                    }
                });
            });

            componentsMap.keySet().forEach(key -> {
                List<String> components = componentsMap.get(key);
                if (components.size() > 1) {
                    String message = "ERROR: the anchor \'" + anchor.getAnchor() + "\' found in " + components.size() + " '" + key + "'+ components. " + components;
                    errors.add(new DependencyError(message, key));
                }
            });
        });
    }


}
