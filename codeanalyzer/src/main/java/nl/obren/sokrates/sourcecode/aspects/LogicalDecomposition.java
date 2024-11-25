/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LogicalDecomposition {
    // A name of logical decomposition
    private String name = "";

    // A scope from which files are taken
    private String scope = "main";

    // An optional list of source code filter applied the scoped files (if empty, all files from the scope are used)
    private List<SourceFileFilter> filters = new ArrayList<>();

    // A folder depth used to automatically group files in components
    private int componentsFolderDepth = 1;

    // If bigger than zero, the Sokrates will, if feasible, automatically look for the folder depth at which there are at least a given number of components
    private int minComponentsCount = 0;

    // A list of explicitly defined components with regex-based rules for source files they should include
    private List<NamedSourceCodeAspect> components = new ArrayList<>();

    // A list of rules used to identify components based on file paths or content patterns
    private List<MetaRule> metaComponents = new ArrayList<>();

    // A list of regex-based rules used to group components (used in diagrams to display components in groups)
    private List<GroupingRule> groups = new ArrayList<>();

    // If true and source filters are used, it will display in dependency diagram files not included via source file filters as one "Unclassified" component
    private boolean includeRemainingFiles = true;

    // A configuration of dependency finders, used to identify links between components
    private DependenciesFinder dependenciesFinder = new DependenciesFinder();

    // Rendering options for component and dependency visualizations
    private RenderingOptions renderingOptions = new RenderingOptions();

    // If true, components that are not a part of source code will be displayed in diagrams
    private boolean includeExternalComponents = true;

    // A minimal number of dependencies between components to be included in the duplication dependency diagrams
    private int dependencyLinkThreshold = 1;

    // A minimal number of duplicated lines of code between components to be included in the duplication dependency diagrams
    private int duplicationLinkThreshold = 50;

    // A minimal number of links between files to be included in the temporal dependency diagrams
    private int temporalLinkThreshold = 1;

    // A maximal depth of files (lines of code from the beginning) used for dependency analyses
    private int maxSearchDepthLines = 200;

    public LogicalDecomposition() {
    }

    public LogicalDecomposition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getComponentsFolderDepth() {
        return componentsFolderDepth;
    }

    public void setComponentsFolderDepth(int componentsFolderDepth) {
        this.componentsFolderDepth = componentsFolderDepth;
    }

    public int getMinComponentsCount() {
        return minComponentsCount;
    }

    public void setMinComponentsCount(int minComponentsCount) {
        this.minComponentsCount = minComponentsCount;
    }

    public List<NamedSourceCodeAspect> getComponents() {
        return components;
    }

    public void setComponents(List<NamedSourceCodeAspect> components) {
        this.components = components;
    }

    @JsonIgnore
    public void updateLogicalComponentsFiles(SourceCodeFiles sourceCodeFiles, CodeConfiguration codeConfiguration, File codeConfigurationFile) {
        List<SourceFile> allSourceFiles = codeConfiguration.getScope(scope).getSourceFiles();
        List<SourceFile> filteredSourceFiles = getSourceFiles(codeConfiguration);
        if (componentsFolderDepth > 0) {
            components.addAll(SourceCodeAspectUtils.getSourceCodeAspectBasedOnFolderDepth(
                    CodeConfiguration.getAbsoluteSrcRoot(codeConfiguration.getSrcRoot(), codeConfigurationFile),
                    filteredSourceFiles, componentsFolderDepth, minComponentsCount));
        }

        for (NamedSourceCodeAspect aspect : components) {
            sourceCodeFiles.getSourceFiles(aspect, filteredSourceFiles);
            aspect.getSourceFiles().forEach(sourceFile -> {
                if (!sourceFile.getLogicalComponents().contains(aspect))
                    sourceFile.getLogicalComponents().add(aspect);
            });
        }

        MetaRulesProcessor helper = MetaRulesProcessor.getLogicalDecompositionInstance();
        List<NamedSourceCodeAspect> metaComponents = helper.extractAspects(getSourceFiles(codeConfiguration), this.metaComponents);
        components.addAll(metaComponents);
        CodeConfigurationUtils.populateUnclassifiedAndMultipleAspectsFiles(components,
                (includeRemainingFiles ? allSourceFiles : filteredSourceFiles),
                sourceFileAspectPair -> {
                    NamedSourceCodeAspect namedSourceCodeAspect = sourceFileAspectPair.getRight();
                    namedSourceCodeAspect.setFiltering(name);
                    sourceFileAspectPair.getLeft().getLogicalComponents().add(namedSourceCodeAspect);
                    return null;
                });
        CodeConfigurationUtils.removeEmptyAspects(components);
        components.forEach(component -> component.setFiltering(name));
    }

    @JsonIgnore
    public boolean isInScope(SourceFile sourceFile) {
        final boolean[] inScope = {false};
        components.forEach(component -> component.getSourceFiles().forEach(compSourceFile -> {
            if (compSourceFile.equals(sourceFile)) {
                inScope[0] = true;
            }
        }));

        return inScope[0];
    }

    private List<SourceFile> getSourceFiles(CodeConfiguration codeConfiguration) {
        Predicate<SourceFile> sourceFileFilter = sourceFile -> {
            List<SourceFileFilter> filters = getFilters();
            if (filters == null || filters.size() == 0) {
                return true;
            }

            boolean[] inScope = {false};
            filters.forEach(filter -> {
                if (filter.matches(sourceFile)) {
                    if (!filter.getException()) {
                        inScope[0] = true;
                    } else {
                        inScope[0] = false;
                        return;
                    }
                }
            });

            return inScope[0];
        };
        return codeConfiguration.getScope(scope).getSourceFiles().stream()
                .filter(sourceFileFilter).collect(Collectors.toList());
    }

    public boolean isIncludeRemainingFiles() {
        return includeRemainingFiles;
    }

    public void setIncludeRemainingFiles(boolean includeRemainingFiles) {
        this.includeRemainingFiles = includeRemainingFiles;
    }

    public List<SourceFileFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<SourceFileFilter> filters) {
        this.filters = filters;
    }

    public List<MetaRule> getMetaComponents() {
        return metaComponents;
    }

    public void setMetaComponents(List<MetaRule> metaComponents) {
        this.metaComponents = metaComponents;
    }

    public RenderingOptions getRenderingOptions() {
        return renderingOptions;
    }

    public void setRenderingOptions(RenderingOptions renderingOptions) {
        this.renderingOptions = renderingOptions;
    }

    public DependenciesFinder getDependenciesFinder() {
        return dependenciesFinder;
    }

    public void setDependenciesFinder(DependenciesFinder dependenciesFinder) {
        this.dependenciesFinder = dependenciesFinder;
    }

    public NamedSourceCodeAspect getComponentByName(String name) {
        Optional<NamedSourceCodeAspect> first = this.components.stream().filter(c -> c.getName().equalsIgnoreCase(name)).findFirst();
        return first.isPresent() ? first.get() : null;
    }

    public int getDependencyLinkThreshold() {
        return dependencyLinkThreshold;
    }

    public void setDependencyLinkThreshold(int dependencyLinkThreshold) {
        this.dependencyLinkThreshold = dependencyLinkThreshold;
    }

    public int getDuplicationLinkThreshold() {
        return duplicationLinkThreshold;
    }

    public void setDuplicationLinkThreshold(int duplicationLinkThreshold) {
        this.duplicationLinkThreshold = duplicationLinkThreshold;
    }

    public boolean isIncludeExternalComponents() {
        return includeExternalComponents;
    }

    public void setIncludeExternalComponents(boolean includeExternalComponents) {
        this.includeExternalComponents = includeExternalComponents;
    }

    public int getTemporalLinkThreshold() {
        return temporalLinkThreshold;
    }

    public void setTemporalLinkThreshold(int temporalLinkThreshold) {
        this.temporalLinkThreshold = temporalLinkThreshold;
    }

    public int getMaxSearchDepthLines() {
        return maxSearchDepthLines;
    }

    public void setMaxSearchDepthLines(int maxSearchDepthLines) {
        this.maxSearchDepthLines = maxSearchDepthLines;
    }

    public List<GroupingRule> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupingRule> groups) {
        this.groups = groups;
    }
}
