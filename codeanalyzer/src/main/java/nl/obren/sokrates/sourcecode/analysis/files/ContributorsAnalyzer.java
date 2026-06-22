/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributorsImport;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.io.File;
import java.util.List;

import static nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils.getPeopleDependencies;
import static nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils.getPeopleFileDependencies;

public class ContributorsAnalyzer extends Analyzer {
    private CodeConfiguration codeConfiguration;
    private MetricsList metricsList;
    private CodeAnalysisResults codeAnalysisResults;
    private File sokratesFolder;

    private ContributorsAnalysisResults analysisResults;

    public ContributorsAnalyzer(CodeAnalysisResults results, File sokratesFolder) {
        this.analysisResults = results.getContributorsAnalysisResults();
        this.codeConfiguration = results.getCodeConfiguration();
        this.metricsList = results.getMetricsList();
        codeAnalysisResults = results;
        this.sokratesFolder = sokratesFolder;
    }

    public void analyze() {
        FileHistoryAnalysisConfig fileHistoryAnalysisConfig = codeConfiguration.getFileHistoryAnalysis();
        if (fileHistoryAnalysisConfig.filesHistoryImportPathExists(sokratesFolder)) {
            ProcessingStopwatch.start("analysis/contributors/loading");
            // Relative paths per scope (main/test/build/generated/other), so the activity diagrams can
            // offer a scope tab per scope alongside "All". Built from the already-scoped aspects; a
            // scope with no files is omitted (so only present scopes get a tab).
            java.util.Map<String, java.util.Set<String>> pathsByScope = getPathsByScope();
            ContributorsImport contributorsImport = fileHistoryAnalysisConfig.getContributors(sokratesFolder, fileHistoryAnalysisConfig, pathsByScope);
            analysisResults.setLatestCommitDate(contributorsImport.getLatestCommitDate());
            analysisResults.setContributors(contributorsImport.getContributors());
            analysisResults.setContributorsPerYear(contributorsImport.getContributorsPerYear());
            analysisResults.setContributorsPerMonth(contributorsImport.getContributorsPerMonth());
            analysisResults.setContributorsPerWeek(contributorsImport.getContributorsPerWeek());
            analysisResults.setContributorsPerDay(contributorsImport.getContributorsPerDay());
            analysisResults.setContributorsPerYearByScope(contributorsImport.getContributorsPerYearByScope());
            analysisResults.setContributorsPerMonthByScope(contributorsImport.getContributorsPerMonthByScope());
            analysisResults.setContributorsPerWeekByScope(contributorsImport.getContributorsPerWeekByScope());
            analysisResults.setContributorsPerDayByScope(contributorsImport.getContributorsPerDayByScope());
            ProcessingStopwatch.end("analysis/contributors/loading");
            ProcessingStopwatch.start("analysis/contributors/per extension");
            analysisResults.setCommitsPerExtensions(fileHistoryAnalysisConfig.getCommitsPerExtension(sokratesFolder, fileHistoryAnalysisConfig));
            analysisResults.setUnscopedExtensionFileCounts(computeUnscopedExtensionFileCounts(fileHistoryAnalysisConfig, pathsByScope));
            ProcessingStopwatch.end("analysis/contributors/per extension");

            ProcessingStopwatch.start("analysis/contributors/get people file dependencies");
            analysisResults.setPeopleFileDependencies30Days(getPeopleFileDependencies(codeAnalysisResults, 30));
            analysisResults.setPeopleFileDependencies90Days(getPeopleFileDependencies(codeAnalysisResults, 90));
            analysisResults.setPeopleFileDependencies180Days(getPeopleFileDependencies(codeAnalysisResults, 180));
            analysisResults.setPeopleFileDependencies365Days(getPeopleFileDependencies(codeAnalysisResults, 365));
            ProcessingStopwatch.end("analysis/contributors/get people file dependencies");

            ProcessingStopwatch.start("analysis/contributors/get people dependencies");
            analysisResults.setPeopleDependencies30Days(getPeopleDependencies(codeAnalysisResults, 30));
            analysisResults.setPeopleDependencies90Days(getPeopleDependencies(codeAnalysisResults, 90));
            analysisResults.setPeopleDependencies180Days(getPeopleDependencies(codeAnalysisResults, 180));
            analysisResults.setPeopleDependencies365Days(getPeopleDependencies(codeAnalysisResults, 365));
            ProcessingStopwatch.end("analysis/contributors/get people dependencies");

            addMetrics();
        }
    }

    // Lowercased relative paths of each scope's source files, keyed by scope name (main, test, build,
    // generated, other). A scope with no files is omitted, so only present scopes get an activity-
    // diagram tab. Returns null when there is no configuration to read (then only "All" is produced).
    private java.util.Map<String, java.util.Set<String>> getPathsByScope() {
        java.util.Map<String, java.util.Set<String>> pathsByScope = new java.util.LinkedHashMap<>();
        addScopePaths(pathsByScope, "main", codeConfiguration.getMain());
        addScopePaths(pathsByScope, "test", codeConfiguration.getTest());
        addScopePaths(pathsByScope, "build", codeConfiguration.getBuildAndDeployment());
        addScopePaths(pathsByScope, "generated", codeConfiguration.getGenerated());
        addScopePaths(pathsByScope, "other", codeConfiguration.getOther());
        return pathsByScope.isEmpty() ? null : pathsByScope;
    }

    // Distinct file count per extension for git-history files that fall in NO scope (the union of all
    // scope path sets is the "in scope" set; everything else is unscoped — deleted/renamed-away or
    // excluded from every aspect). These files are not analyzed, so there is no lines-of-code; the value
    // is the distinct-file count, used by the report only to order the icons (it shows "-" for the
    // number). Returns an empty list when there is no scope info (pathsByScope null) — then there is no
    // unscoped tab at all — or no unscoped activity. Mirrors the unscoped time-slot residual in
    // GitContributorsUtil so the icon set matches that tab's data.
    private java.util.List<nl.obren.sokrates.sourcecode.metrics.NumericMetric> computeUnscopedExtensionFileCounts(
            FileHistoryAnalysisConfig config, java.util.Map<String, java.util.Set<String>> pathsByScope) {
        java.util.List<nl.obren.sokrates.sourcecode.metrics.NumericMetric> result = new java.util.ArrayList<>();
        if (pathsByScope == null || pathsByScope.isEmpty()) {
            return result;
        }
        java.util.Set<String> allScopePaths = new java.util.HashSet<>();
        pathsByScope.values().forEach(allScopePaths::addAll);

        // extension -> distinct unscoped file paths
        java.util.Map<String, java.util.Set<String>> filesByExtension = new java.util.LinkedHashMap<>();
        java.io.File historyFile = config.getContributorsFile(sokratesFolder);
        nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils.getHistoryFromFile(historyFile, config).forEach(fileUpdate -> {
            String path = fileUpdate.getPath();
            if (path == null || allScopePaths.contains(path.toLowerCase())) {
                return; // in a scope (or no path) — not unscoped
            }
            String extension = fileUpdate.getExtension();
            if (extension == null || extension.trim().isEmpty()) {
                return;
            }
            filesByExtension.computeIfAbsent(extension, k -> new java.util.HashSet<>()).add(path);
        });

        filesByExtension.forEach((extension, files) ->
                result.add(new nl.obren.sokrates.sourcecode.metrics.NumericMetric(extension, files.size())));
        // Order by distinct file count, descending (the report renders them in this order).
        result.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
        return result;
    }

    // Adds one scope's source-file paths to the map, lowercased so matching against git-history paths
    // is case-insensitive (mirrors FileHistoryAnalyzer.enrichFilesWithAge). A source file's
    // relativePath is taken from the analysis srcRoot, which may sit one directory above the git root
    // (e.g. srcRoot ".." makes paths "<repo>/main/..." while git-history stores "main/..."). To match
    // in either case we add BOTH the full path and the path with its leading segment stripped;
    // GitContributorsUtil tests a git path against this set directly. Empty/missing aspects are
    // skipped so they don't produce an empty tab.
    private void addScopePaths(java.util.Map<String, java.util.Set<String>> pathsByScope, String scope,
                               nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect aspect) {
        if (aspect == null || aspect.getSourceFiles() == null || aspect.getSourceFiles().isEmpty()) {
            return;
        }
        java.util.Set<String> paths = new java.util.HashSet<>();
        aspect.getSourceFiles().forEach(sourceFile -> {
            if (sourceFile.getRelativePath() != null) {
                String path = sourceFile.getRelativePath().toLowerCase();
                paths.add(path);
                int slash = path.indexOf('/');
                if (slash > 0 && slash < path.length() - 1) {
                    paths.add(path.substring(slash + 1));
                }
            }
        });
        if (!paths.isEmpty()) {
            pathsByScope.put(scope, paths);
        }
    }

    private void addMetrics() {
        metricsList.addSystemMetric().id("NUMBER_OF_CONTRIBUTORS")
                .value(analysisResults.getContributors().size())
                .description("Number of contributors");
    }

}
