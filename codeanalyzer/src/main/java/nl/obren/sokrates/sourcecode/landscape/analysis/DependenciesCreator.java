package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependenciesCreator {
    private List<ComponentDependency> dependencies = new ArrayList<>();
    private Map<String, ComponentDependency> dependenciesMap = new HashMap();

    public DependenciesCreator() {

    }

    public void add(String from, String to) {
        String key1 = from + "::" + to;
        String key2 = to + "::" + from;

        if (dependenciesMap.containsKey(key1)) {
            dependenciesMap.get(key1).increment(1);
        } else if (dependenciesMap.containsKey(key2)) {
            dependenciesMap.get(key2).increment(1);
        } else {
            ComponentDependency dependency = new ComponentDependency(from, to);
            dependenciesMap.put(key1, dependency);
            dependencies.add(dependency);
        }
    }

    public List<ComponentDependency> getDependencies() {
        return dependencies;
    }

    public List<ComponentDependency> getIndirectDependencies() {
        Map<String, List<String>> toMap = new HashMap();

        getDependencies().forEach(dependency -> {
            String to = dependency.getToComponent();
            String from = dependency.getFromComponent();
            if (!toMap.containsKey(to)) {
                toMap.put(to, new ArrayList<>());
            }
            toMap.get(to).add(from);
        });

        DependenciesCreator indirect = new DependenciesCreator();

        toMap.keySet().forEach(key -> {
            toMap.get(key).forEach(from1 -> {
                toMap.get(key).stream().filter(from2 -> !from2.equalsIgnoreCase(from1)).forEach(from2 -> {
                    indirect.add(from1, from2);
                });

            });
        });


        List<ComponentDependency> indirectDependencies = indirect.getDependencies();

        indirectDependencies.forEach(dependency -> dependency.setCount(dependency.getCount() / 2));

        return indirectDependencies;
    }
}
