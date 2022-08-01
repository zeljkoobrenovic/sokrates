package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.*;

public class CommitTrendsExtractors {
    private CodeAnalysisResults analysisResults;

    public CommitTrendsExtractors(CodeAnalysisResults analysisResults) {
        this.analysisResults = analysisResults;
    }

    public Map<String, Map<String, Integer>> getCommitsPerYear(String logicalDecompositionKey) {
        Map<String, Map<String, Integer>> componentsMap = new HashMap<>();
        List<SourceFile> allFiles = analysisResults.getFilesHistoryAnalysisResults().getAllFiles();
        allFiles.stream().filter(item -> item.getFileModificationHistory() != null).forEach(item -> {
            List<NamedSourceCodeAspect> logicalComponents = item.getLogicalComponents(logicalDecompositionKey);
            if (logicalComponents.size() > 0) {
                String componentName = logicalComponents.get(0).getName();

                Map<String, Integer> componentYears;
                if (componentsMap.containsKey(componentName)) {
                    componentYears = componentsMap.get(componentName);
                } else {
                    componentYears = new HashMap<>();
                    componentsMap.put(componentName, componentYears);
                }
                item.getFileModificationHistory().getCommits().forEach(commit -> {
                    String year = DateUtils.getYear(commit.getDate());
                    int prevValue = componentYears.containsKey(year) ? componentYears.get(year) : 0;
                    componentYears.put(year, prevValue + 1);
                });
            }
        });

        return componentsMap;
    }

    public List<NumericMetric> getTotalCommits(String logicalDecompositionKey) {
        Map<String, Set<String>> commitsMap = new HashMap<>();
        List<SourceFile> allFiles = analysisResults.getFilesHistoryAnalysisResults().getAllFiles();
        allFiles.stream().filter(item -> item.getFileModificationHistory() != null).forEach(item -> {
            List<NamedSourceCodeAspect> logicalComponents = item.getLogicalComponents(logicalDecompositionKey);
            if (logicalComponents.size() > 0) {
                String component = logicalComponents.get(0).getName();

                item.getFileModificationHistory().getCommits().forEach(commit -> {
                    if (!commitsMap.containsKey(component)) {
                        commitsMap.put(component, new HashSet<>());
                    }
                    commitsMap.get(component).add(commit.getId());
                });
            }
        });

        List<NumericMetric> metrics = new ArrayList<>();

        commitsMap.keySet().forEach(key -> {
            metrics.add(new NumericMetric(key, commitsMap.get(key).size()));
        });

        return metrics;
    }
}
