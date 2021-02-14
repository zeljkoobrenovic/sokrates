/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
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
    private String name = "";
    private String scope = "main";
    private List<SourceFileFilter> filters = new ArrayList<>();
    private int componentsFolderDepth = 1;
    private List<NamedSourceCodeAspect> components = new ArrayList<>();
    private List<MetaRule> metaComponents = new ArrayList<>();
    private List<GroupingRule> groups = new ArrayList<>();
    private boolean includeRemainingFiles = true;
    private DependenciesFinder dependenciesFinder = new DependenciesFinder();
    private RenderingOptions renderingOptions = new RenderingOptions();
    private boolean includeExternalComponents = true;
    private int dependencyLinkThreshold = 1;
    private int duplicationLinkThreshold = 50;
    private int temporalLinkThreshold = 2;
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
                    filteredSourceFiles, componentsFolderDepth));
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
                return;
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
