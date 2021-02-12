/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.dependencies.*;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.util.ArrayList;
import java.util.List;

public class LogicalDecompositionAnalyzer extends Analyzer {
    private final StringBuffer textSummary;
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final long start;
    private final List<LogicalDecompositionAnalysisResults> logicalDecompositionAnalysisResults;
    private CodeAnalysisResults codeAnalysisResults;
    private ProgressFeedback progressFeedback;

    public LogicalDecompositionAnalyzer(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
        this.logicalDecompositionAnalysisResults = codeAnalysisResults.getLogicalDecompositionsAnalysisResults();
        this.codeConfiguration = codeAnalysisResults.getCodeConfiguration();
        this.metricsList = codeAnalysisResults.getMetricsList();
        this.start = codeAnalysisResults.getAnalysisStartTimeMs();
        this.textSummary = codeAnalysisResults.getTextSummary();
    }

    public void analyze(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
        progressFeedback.start();
        progressFeedback.setDetailedText("");
        boolean skipDependencies = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSkipDependencies();
        DependenciesAnalysis dependenciesAnalysis = new DependenciesAnalysis();
        boolean shouldGetDependencies = false;
        if (!skipDependencies) {
            AnalysisUtils.info(textSummary, progressFeedback, "Analysing dependencies...", start);
            dependenciesAnalysis = DependenciesUtils.extractDependencies(codeConfiguration.getMain(), skipDependencies);
            for (LogicalDecomposition logicalDecomposition : codeConfiguration.getLogicalDecompositions()) {
                if (logicalDecomposition.getDependenciesFinder().isUseBuiltInDependencyFinders()) {
                    shouldGetDependencies = true;
                    break;
                }
            }
        }

        List<Dependency> allDependencies = shouldGetDependencies ? dependenciesAnalysis.getDependencies() : new ArrayList<>();
        this.codeAnalysisResults.setAllDependencies(allDependencies);
        DependenciesAnalysis finalDependenciesAnalysis = dependenciesAnalysis;
        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            LogicalDecompositionAnalysisResults logicalDecompositionAnalysisResults = new LogicalDecompositionAnalysisResults(logicalDecomposition.getName());
            logicalDecompositionAnalysisResults.setLogicalDecomposition(logicalDecomposition);
            this.logicalDecompositionAnalysisResults.add(logicalDecompositionAnalysisResults);
            logicalDecomposition.getComponents().forEach(component -> {
                AspectAnalysisResults componentAnalysisResults = new AspectAnalysisResults(component.getName());
                logicalDecompositionAnalysisResults.getComponents().add(componentAnalysisResults);
                AnalysisUtils.analyze("DECOMPOSITION_" + logicalDecomposition.getName(), component, null,
                        LogicalDecompositionAnalyzer.this.progressFeedback, componentAnalysisResults,
                        metricsList, textSummary, start);
            });

            List<ComponentDependency> componentDependencies =
                    logicalDecomposition.getDependenciesFinder().isUseBuiltInDependencyFinders()
                    ? DependencyUtils.getComponentDependencies(allDependencies, logicalDecomposition.getName())
                    : new ArrayList<>();
            DependenciesFinderExtractor finder = new DependenciesFinderExtractor(logicalDecomposition);
            List<ComponentDependency> finderDependencies = finder.findComponentDependencies(codeConfiguration.getMain());
            componentDependencies.addAll(finderDependencies);
            allDependencies.addAll(finder.getAllDependencies());

            logicalDecompositionAnalysisResults.setComponentDependencies(componentDependencies);
            logicalDecompositionAnalysisResults.setAllDependencies(allDependencies);

            addDependencyMetrics(allDependencies, logicalDecomposition.getName(), componentDependencies);
            List<DependencyError> errors = new ArrayList<>();
            finalDependenciesAnalysis.getErrors().stream().filter(error -> error.getFiltering().equals(logicalDecomposition.getName())).forEach(errors::add);
            logicalDecompositionAnalysisResults.getComponentDependenciesErrors().addAll(errors);

        });
    }

    private void addDependencyMetrics(List<Dependency> allDependencies, String logicalDecompositionName, List<ComponentDependency> componentDependencies) {
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - \"" + logicalDecompositionName + "\", found " + allDependencies.size() + " dependencies ("
                + componentDependencies.size() + " component dependencies)", start);

        addNumberOfAnchorDependenciesMetric(allDependencies, logicalDecompositionName);
        addNumberOfComponentDependenciesMetric(logicalDecompositionName, componentDependencies);
    }

    private String getMetricFriendlyName(String logicalDecompositionName) {
        return logicalDecompositionName.toUpperCase().replace(" ", "_").replace("-", "_");
    }

    private void addNumberOfComponentDependenciesMetric(String logicalDecompositionName, List<ComponentDependency> componentDependencies) {
        String name = getMetricFriendlyName(logicalDecompositionName);

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("NUMBER_OF_DEPENDENCIES_DECOMPOSITION_" + name))
                .value(componentDependencies.size());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("NUMBER_OF_PLACES_WITH_CYCLIC_DEPENDENCIES_DECOMPOSITION_" + name))
                .value(DependencyUtils.getCyclicDependencyPlacesCount(componentDependencies));
    }

    private void addNumberOfAnchorDependenciesMetric(List<Dependency> allDependencies, String logicalDecompositionName) {
        String name = getMetricFriendlyName(logicalDecompositionName);
        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("NUMBER_OF_DEPENDENCY_LINKS_DECOMPOSITION_" + name))
                .description("Number of anchor dependencies")
                .value(allDependencies.size());
    }

}
