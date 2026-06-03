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
        // A single git commit touching several files in the same component appears in each of those
        // files' per-file histories with the same commit id. Track seen ids per (component, year) so
        // a commit is counted once, matching getTotalCommits' set-based dedup (otherwise the counts
        // are inflated by file fan-out).
        Map<String, Map<String, Set<String>>> seenCommitIds = new HashMap<>();
        List<SourceFile> allFiles = analysisResults.getFilesHistoryAnalysisResults().getAllFiles();
        allFiles.stream().filter(item -> item.getFileModificationHistory() != null).forEach(item -> {
            List<NamedSourceCodeAspect> logicalComponents = item.getLogicalComponents(logicalDecompositionKey);
            if (logicalComponents.size() > 0) {
                String componentName = logicalComponents.get(0).getName();

                Map<String, Integer> componentYears = componentsMap.computeIfAbsent(componentName, k -> new HashMap<>());
                Map<String, Set<String>> componentSeenIds = seenCommitIds.computeIfAbsent(componentName, k -> new HashMap<>());
                item.getFileModificationHistory().getCommits().forEach(commit -> {
                    String year = DateUtils.getYear(commit.getDate());
                    Set<String> yearSeenIds = componentSeenIds.computeIfAbsent(year, k -> new HashSet<>());
                    if (yearSeenIds.add(commit.getId())) {
                        componentYears.merge(year, 1, Integer::sum);
                    }
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
