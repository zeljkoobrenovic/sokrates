/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependencyUtils {

    public static void addDependency(List<Dependency> dependencies, SourceFileDependency sourceFileDependency, DependencyAnchor sourceAnchor, DependencyAnchor targetAnchor) {
        String targetKey = targetAnchor.getAnchor();
        String sourceKey = sourceAnchor.getAnchor();
        if (!targetKey.equalsIgnoreCase(sourceKey)) {
            Dependency newDependency = new Dependency(sourceAnchor, targetAnchor);
            if (dependencies.contains(newDependency)) {
                Dependency existingDependency = dependencies.get(dependencies.indexOf(newDependency));
                existingDependency.getFromFiles().add(sourceFileDependency);
            } else {
                dependencies.add(newDependency);
                newDependency.getFromFiles().add(sourceFileDependency);
            }
        }
    }

    public static int getDependenciesCount(List<ComponentDependency> componentDependencies) {
        int[] count = {0};
        componentDependencies.forEach(d -> count[0] += d.getCount());
        return count[0];
    }

    public static int getCyclicDependencyPlacesCount(List<ComponentDependency> componentDependencies) {
        int[] count = {0};
        componentDependencies.forEach(d1 -> {
            componentDependencies.stream().filter(d2 -> d1 != d2 && isCyclic(d1, d2)).forEach(d2 -> {
                count[0]++;
            });
        });
        return count[0] / 2;
    }

    private static boolean isCyclic(ComponentDependency d1, ComponentDependency d2) {
        return d1.getFromComponent().equals(d2.getToComponent()) && d1.getToComponent().equals(d2.getFromComponent());
    }

    public static int getCyclicDependencyCount(List<ComponentDependency> componentDependencies) {
        int[] count = {0};
        componentDependencies.forEach(d1 -> {
            componentDependencies.stream().filter(d2 -> d1 != d2 && isCyclic(d1, d2)).forEach(d2 -> {
                count[0] += d1.getCount();
            });
        });
        return count[0];
    }

    public static List<ComponentDependency> getComponentDependencies(List<Dependency> dependencies, String groupName) {
        List<ComponentDependency> componentDependencies = new ArrayList<>();
        List<String> fileToComponentLinks = new ArrayList<>();
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
                    if (!fileToComponentLinks.contains(fileToComponentLink)) {
                        fileToComponentLinks.add(fileToComponentLink);
                        sourceFileDependency.getSourceFile().getLogicalComponents(groupName).forEach(sourceComponent -> {
                            addComponentDependency(sourceFileDependency, componentDependencies, sourceComponent, targetComponent);
                        });
                    }
                });
            });
        });

        return componentDependencies;
    }

    private static void addComponentDependency(SourceFileDependency sourceFileDependency, List<ComponentDependency> componentDependencies, NamedSourceCodeAspect
            sourceComponent, NamedSourceCodeAspect targetComponent) {
        if (!sourceComponent.getName().equalsIgnoreCase(targetComponent.getName())) {
            ComponentDependency componentDependency = new ComponentDependency(sourceComponent.getName(),
                    targetComponent.getName());
            if (componentDependencies.contains(componentDependency)) {
                componentDependency = componentDependencies.get(componentDependencies.indexOf(componentDependency));
                componentDependency.setCount(componentDependency.getCount() + 1);
            } else {
                componentDependency.setCount(1);
                componentDependencies.add(componentDependency);
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
