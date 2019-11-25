package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
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
    private final SourceCodeAspect main;
    private CodeAnalysisResults codeAnalysisResults;
    private ProgressFeedback progressFeedback;

    public LogicalDecompositionAnalyzer(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
        this.logicalDecompositionAnalysisResults = codeAnalysisResults.getLogicalDecompositionsAnalysisResults();
        this.codeConfiguration = codeAnalysisResults.getCodeConfiguration();
        this.metricsList = codeAnalysisResults.getMetricsList();
        this.start = codeAnalysisResults.getAnalysisStartTimeMs();
        this.textSummary = codeAnalysisResults.getTextSummary();
        this.main = codeConfiguration.getMain();
    }

    public void analyze(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
        progressFeedback.start();
        progressFeedback.setDetailedText("");
        AnalysisUtils.info(textSummary, progressFeedback, "Extracting dependencies...", start);
        DependenciesAnalysis dependenciesAnalysis = DependenciesUtils.extractDependencies(codeConfiguration.getMain(), codeAnalysisResults.getCodeConfiguration().getAnalysis().isSkipDependencies());

        boolean shouldGetDependencies = false;

        for (LogicalDecomposition logicalDecomposition : codeConfiguration.getLogicalDecompositions()) {
            if (logicalDecomposition.getDependenciesFinder().isUseBuiltInDependencyFinders()) {
                shouldGetDependencies = true;
                break;
            }
        }

        List<Dependency> allDependencies = shouldGetDependencies ? dependenciesAnalysis.getDependencies() : new ArrayList<>();
        this.codeAnalysisResults.setAllDependencies(allDependencies);
        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            LogicalDecompositionAnalysisResults logicalDecompositionAnalysisResults = new LogicalDecompositionAnalysisResults(logicalDecomposition.getName());
            logicalDecompositionAnalysisResults.setLogicalDecomposition(logicalDecomposition);
            this.logicalDecompositionAnalysisResults.add(logicalDecompositionAnalysisResults);
            logicalDecomposition.getComponents().forEach(component -> {
                AspectAnalysisResults componentAnalysisResults = new AspectAnalysisResults(component.getName());
                logicalDecompositionAnalysisResults.getComponents().add(componentAnalysisResults);
                AnalysisUtils.analyze(component, LogicalDecompositionAnalyzer.this.progressFeedback, componentAnalysisResults,
                        metricsList, textSummary, start);
            });

            List<ComponentDependency> componentDependencies =
                    logicalDecomposition.getDependenciesFinder().isUseBuiltInDependencyFinders()
                    ? DependencyUtils.getComponentDependencies(allDependencies, logicalDecomposition.getName())
                    : new ArrayList<>();
            DependenciesFinderExtractor finder = new DependenciesFinderExtractor(logicalDecomposition);
            List<ComponentDependency> finderDependencies = finder.findComponentDependencies(codeConfiguration.getMain());
            componentDependencies.addAll(finderDependencies);

            logicalDecompositionAnalysisResults.setComponentDependencies(componentDependencies);

            addDependencyMetrics(allDependencies, logicalDecomposition.getName(), componentDependencies);
            List<DependencyError> errors = new ArrayList<>();
            dependenciesAnalysis.getErrors().stream().filter(error -> error.getFiltering().equals(logicalDecomposition.getName())).forEach(errors::add);
            logicalDecompositionAnalysisResults.getComponentDependenciesErrors().addAll(errors);

        });
    }

    private void addDependencyMetrics(List<Dependency> allDependencies, String logicalDecompositionName, List<ComponentDependency> componentDependencies) {
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - \"" + logicalDecompositionName + "\", found " + allDependencies.size() + " dependencies ("
                + componentDependencies.size() + " component dependencies)", start);

        addNumberOfAnchorDependenciesMetric(allDependencies, logicalDecompositionName);
        addNumberOfComponentDependenciesMetric(logicalDecompositionName, componentDependencies);
    }

    private void addNumberOfComponentDependenciesMetric(String logicalDecompositionName, List<ComponentDependency> componentDependencies) {
        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("NUMBER_OF_COMPONENT_DEPENDENCIES"))
                .description("Number of component dependencies")
                .scope(Metric.Scope.LOGICAL_DECOMPOSITION)
                .scopeQualifier(logicalDecompositionName)
                .value(componentDependencies.size());
    }

    private void addNumberOfAnchorDependenciesMetric(List<Dependency> allDependencies, String logicalDecompositionName) {
        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("NUMBER_OF_ANCHOR_DEPENDENCIES"))
                .description("Number of anchor dependencies")
                .scope(Metric.Scope.LOGICAL_DECOMPOSITION)
                .scopeQualifier(logicalDecompositionName)
                .value(allDependencies.size());
    }

}
