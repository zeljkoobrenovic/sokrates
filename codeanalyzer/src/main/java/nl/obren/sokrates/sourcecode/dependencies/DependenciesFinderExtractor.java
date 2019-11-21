package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DependenciesFinderExtractor {
    private static final int MAX_SEARCH_DEPTH_LINES = 200;
    private LogicalDecomposition logicalDecomposition;

    public DependenciesFinderExtractor(LogicalDecomposition logicalDecomposition) {
        this.logicalDecomposition = logicalDecomposition;
    }

    public List<ComponentDependency> findComponentDependencies(SourceCodeAspect aspect) {
        List<ComponentDependency> dependencies = new ArrayList<>();
        Map<String, ComponentDependency> dependenciesMap = new HashMap<>();

        aspect.getSourceFiles().forEach(sourceFile -> findComponentDependencies(dependencies, dependenciesMap, sourceFile));

        return dependencies;
    }

    private void findComponentDependencies(List<ComponentDependency> dependencies, Map<String, ComponentDependency> dependenciesMap, SourceFile sourceFile) {
        List<String> lines = sourceFile.getLines();
        if (lines.size() > MAX_SEARCH_DEPTH_LINES) {
            lines = lines.subList(0, MAX_SEARCH_DEPTH_LINES);
        }

        lines.forEach(line -> {
            logicalDecomposition.getDependenciesFinder().getRules().forEach(rule -> {
                if (RegexUtils.matchesEntirely(rule.getPattern(), line)) {

                    ComponentDependency dependency = new ComponentDependency();
                    String group = logicalDecomposition.getName();
                    SourceCodeAspect firstAspect = sourceFile.getLogicalComponents(group).get(0);
                    dependency.setFromComponent(firstAspect.getName());
                    dependency.setToComponent(rule.getComponent());

                    if (!dependency.getFromComponent().equalsIgnoreCase(dependency.getToComponent())) {
                        String key = dependency.getDependencyString();
                        if (dependenciesMap.containsKey(key)) {
                            dependenciesMap.get(key).setCount(dependenciesMap.get(key).getCount() + 1);
                        } else {
                            dependencies.add(dependency);
                            dependenciesMap.put(key, dependency);
                        }
                    }
                }
            });
        });
    }
}
