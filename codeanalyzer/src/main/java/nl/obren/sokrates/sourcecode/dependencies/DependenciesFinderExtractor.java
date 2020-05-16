/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.MetaRule;
import nl.obren.sokrates.sourcecode.aspects.MetaRulesProcessor;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DependenciesFinderExtractor {
    private static final int MAX_SEARCH_DEPTH_LINES = 200;
    private LogicalDecomposition logicalDecomposition;
    private List<ComponentDependency> dependencies = new ArrayList<>();
    private Set<String> fileComponentDependencies = new HashSet<>();
    private Map<String, ComponentDependency> dependenciesMap = new HashMap<>();
    private List<Dependency> allDependencies = new ArrayList<>();

    public DependenciesFinderExtractor(LogicalDecomposition logicalDecomposition) {
        this.logicalDecomposition = logicalDecomposition;
    }

    public List<Dependency> getAllDependencies() {
        return allDependencies;
    }

    public void setAllDependencies(List<Dependency> allDependencies) {
        this.allDependencies = allDependencies;
    }

    public List<ComponentDependency> findComponentDependencies(NamedSourceCodeAspect aspect) {
        dependencies = new ArrayList<>();
        dependenciesMap = new HashMap<>();

        aspect.getSourceFiles().forEach(sourceFile -> findComponentDependenciesViaSimpleRules(sourceFile));
        aspect.getSourceFiles().forEach(sourceFile -> findComponentDependenciesViaMetaRules(sourceFile));

        return dependencies;
    }

    private void findComponentDependenciesViaSimpleRules(SourceFile sourceFile) {
        logicalDecomposition.getDependenciesFinder().getRules().forEach(rule -> {
            SourceFileFilter sourceFileFilter = new SourceFileFilter(rule.getPathPattern(), "");

            if (sourceFileFilter.pathMatches(sourceFile.getRelativePath())) {
                getSimpleLines(sourceFile).forEach(line -> {
                    if (RegexUtils.matchesEntirely(rule.getContentPattern(), line)) {
                        addDependency(dependencies, dependenciesMap, sourceFile, rule.getComponent(), line);
                    }
                });
            }
        });
    }

    private void findComponentDependenciesViaMetaRules(SourceFile sourceFile) {
        logicalDecomposition.getDependenciesFinder().getMetaRules().forEach(metaRule -> {
            SourceFileFilter sourceFileFilter = new SourceFileFilter(metaRule.getPathPattern(), "");

            if (sourceFileFilter.pathMatches(sourceFile.getRelativePath())) {
                getLines(sourceFile, metaRule).forEach(line -> {
                    if (RegexUtils.matchesEntirely(metaRule.getContentPattern(), line)) {
                        String component = new ComplexOperation(metaRule.getNameOperations()).exec(line);
                        addDependency(dependencies, dependenciesMap, sourceFile, component, line);
                    }
                });
            }
        });
    }

    private List<String> getLines(SourceFile sourceFile, MetaRule metaRule) {
        if (metaRule.getUse().equalsIgnoreCase("path")) {
            return Arrays.asList(sourceFile.getRelativePath());
        }
        List<String> lines = metaRule.isIgnoreComments() ? sourceFile.getCleanedLines() : sourceFile.getLines();
        if (lines.size() > MAX_SEARCH_DEPTH_LINES) {
            lines = lines.subList(0, MAX_SEARCH_DEPTH_LINES);
        }
        return lines;
    }

    private List<String> getSimpleLines(SourceFile sourceFile) {
        List<String> lines = sourceFile.getLines();
        if (lines.size() > MAX_SEARCH_DEPTH_LINES) {
            lines = lines.subList(0, MAX_SEARCH_DEPTH_LINES);
        }
        return lines;
    }

    private void addDependency(List<ComponentDependency> dependencies, Map<String, ComponentDependency> dependenciesMap, SourceFile sourceFile, String toComponent, String line) {
        if (StringUtils.isBlank(toComponent)) {
            return;
        }

        if (!logicalDecomposition.isIncludeExternalComponents() && logicalDecomposition.getComponentByName(toComponent) == null) {
            return;
        }

        if (isInDuplicatedDependecies(sourceFile, toComponent)) return;

        ComponentDependency componentDependency = new ComponentDependency();
        String group = logicalDecomposition.getName();
        List<NamedSourceCodeAspect> logicalComponents = sourceFile.getLogicalComponents(group);

        if (logicalComponents.size() == 0) {
            return;
        }

        NamedSourceCodeAspect firstAspect = logicalComponents.get(0);
        String fromComponent = firstAspect.getName();
        componentDependency.setFromComponent(fromComponent);
        componentDependency.setToComponent(toComponent);

        addFileDepedency(sourceFile, fromComponent, toComponent, line, firstAspect);

        if (!componentDependency.getFromComponent().equalsIgnoreCase(componentDependency.getToComponent())) {
            String key = componentDependency.getDependencyString();
            if (dependenciesMap.containsKey(key)) {
                componentDependency = dependenciesMap.get(key);
                componentDependency.setCount(componentDependency.getCount() + 1);
            } else {
                dependencies.add(componentDependency);
                dependenciesMap.put(key, componentDependency);
            }

            componentDependency.setLocFrom(componentDependency.getLocFrom() + sourceFile.getLinesOfCode());
            componentDependency.getPathsFrom().add(sourceFile.getRelativePath());
        }
    }

    private void addFileDepedency(SourceFile sourceFile, String fromComponent, String toComponent, String line, NamedSourceCodeAspect firstAspect) {
        Dependency fileDependency = new Dependency(new DependencyAnchor(""), new DependencyAnchor(toComponent));
        SourceFileDependency sourceFileDependency = new SourceFileDependency(sourceFile);
        sourceFileDependency.setCodeFragment(line);
        fileDependency.getFromFiles().add(sourceFileDependency);
        fileDependency.setFromComponentName(fromComponent);
        fileDependency.setToComponentName(toComponent);
        sourceFile.getLogicalComponents().add(firstAspect);
        allDependencies.add(fileDependency);
    }

    private boolean isInDuplicatedDependecies(SourceFile sourceFile, String toComponent) {
        String duplicationKey = sourceFile.getRelativePath() + " -> " + toComponent;
        if (fileComponentDependencies.contains(duplicationKey)) {
            return true;
        }

        fileComponentDependencies.add(duplicationKey);
        return false;
    }
}
