package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicationDependenciesUtils {
    public static List<ComponentDependency> extractDependencies(String group, List<DuplicationInstance> duplicationInstances) {
        List<ComponentDependency> componentDependencies = new ArrayList<>();
        Map<String, ComponentDependency> componentDependenciesMap = new HashMap<>();

        duplicationInstances.forEach(instance -> {
            instance.getDuplicatedFileBlocks().forEach(file1 -> {
                List<SourceCodeAspect> logicalComponents1 = file1.getSourceFile().getLogicalComponents(group);
                if (logicalComponents1.size() > 0) {
                    instance.getDuplicatedFileBlocks().forEach(file2 -> {
                        if (file1 != file2) {
                            List<SourceCodeAspect> logicalComponents2 = file2.getSourceFile().getLogicalComponents(group);
                            if (logicalComponents2.size() > 0) {
                                String name1 = logicalComponents1.get(0).getName();
                                String name2 = logicalComponents2.get(0).getName();
                                if (!name1.equalsIgnoreCase(name2)) {
                                    ComponentDependency dependency = getDependency(name1, name2, componentDependencies, componentDependenciesMap);
                                    dependency.setCount(dependency.getCount() + instance.getBlockSize());
                                }
                            }
                        }
                    });
                }
            });
        });

        componentDependencies.forEach(d -> d.setCount(d.getCount() / 2));

        return componentDependencies;
    }

    private static ComponentDependency getDependency(String name1, String name2, List<ComponentDependency> componentDependencies, Map<String, ComponentDependency> componentDependenciesMap) {
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
