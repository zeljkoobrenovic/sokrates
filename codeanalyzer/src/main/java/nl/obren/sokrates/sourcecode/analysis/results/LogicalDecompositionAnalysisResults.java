/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.aspects.DependenciesFinder;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyError;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.List;

public class LogicalDecompositionAnalysisResults {
    private String key;
    private List<AspectAnalysisResults> components = new ArrayList<>();
    private List<ComponentDependency> componentDependencies = new ArrayList<>();
    private List<DependencyError> componentDependenciesErrors = new ArrayList<>();
    private LogicalDecomposition logicalDecomposition;
    @JsonIgnore
    private List<Dependency> allDependencies;

    public LogicalDecompositionAnalysisResults() {
    }

    public LogicalDecompositionAnalysisResults(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<AspectAnalysisResults> getComponents() {
        return components;
    }

    public void setComponents(List<AspectAnalysisResults> components) {
        this.components = components;
    }

    public List<NumericMetric> getFileCountPerComponent() {
        List<NumericMetric> fileCount = new ArrayList<>();
        components.forEach(component -> fileCount.add(new NumericMetric(component.getName(), component.getFilesCount())));
        return fileCount;
    }

    public List<NumericMetric> getLinesOfCodePerComponent() {
        List<NumericMetric> fileCount = new ArrayList<>();
        components.forEach(component -> fileCount.add(new NumericMetric(component.getName(), component.getLinesOfCode())));
        return fileCount;
    }

    public LogicalDecomposition getLogicalDecomposition() {
        return logicalDecomposition;
    }

    public void setLogicalDecomposition(LogicalDecomposition logicalDecomposition) {
        this.logicalDecomposition = logicalDecomposition;
    }

    public List<ComponentDependency> getComponentDependencies() {
        return componentDependencies;
    }

    public void setComponentDependencies(List<ComponentDependency> componentDependencies) {
        this.componentDependencies = componentDependencies;
    }

    public List<DependencyError> getComponentDependenciesErrors() {
        return componentDependenciesErrors;
    }

    public void setComponentDependenciesErrors(List<DependencyError> componentDependenciesErrors) {
        this.componentDependenciesErrors = componentDependenciesErrors;
    }

    @JsonIgnore
    public void setAllDependencies(List<Dependency> allDependencies) {
        this.allDependencies = allDependencies;
    }

    @JsonIgnore
    public List<Dependency> getAllDependencies() {
        return allDependencies;
    }
}
