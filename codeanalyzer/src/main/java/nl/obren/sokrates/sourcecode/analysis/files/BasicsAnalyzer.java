/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicsAnalyzer extends Analyzer {
    private final StringBuffer textSummary;
    private final CodeConfiguration codeConfiguration;
    private final long start;
    private final CodeAnalysisResults results;
    private File codeConfigurationFile;
    private ProgressFeedback progressFeedback;

    public BasicsAnalyzer(CodeAnalysisResults analysisResults, File codeConfigurationFile, ProgressFeedback progressFeedback) {
        this.results = analysisResults;
        this.codeConfiguration = analysisResults.getCodeConfiguration();
        this.start = analysisResults.getAnalysisStartTimeMs();
        this.textSummary = analysisResults.getTextSummary();
        this.codeConfigurationFile = codeConfigurationFile;
        this.progressFeedback = progressFeedback;
    }

    public void analyze() {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();

        sourceCodeFiles.load(new File(CodeConfiguration.getAbsoluteSrcRoot(codeConfiguration.getSrcRoot(), codeConfigurationFile)),
                progressFeedback);

        results.setFilesExcludedByExtension(sourceCodeFiles.getFilesExcludedByExtension());
        results.setIgnoredFilesGroups(sourceCodeFiles.getIgnoredFilesGroups());
        results.setCodeConfiguration(codeConfiguration);
        results.setTotalNumberOfFilesInScope(sourceCodeFiles.getAllFiles().size());

        addTotalNumberOfFilesMetric(sourceCodeFiles);

        codeConfiguration.load(sourceCodeFiles, codeConfigurationFile);
        analyzeScopes();

        List<SourceFile> excludedFiles = sourceCodeFiles.getExcludedFiles();

        results.setNumberOfExcludedFiles(excludedFiles.size());
        results.setExcludedExtensions(getExcludedExtensions(excludedFiles));

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Excluded from analyses " + (excludedFiles.size()) + " files", start);
    }

    private Map<String, Integer> getExcludedExtensions(List<SourceFile> excludedFiles) {
        Map<String, Integer> excludedExtensions = new HashMap<>();

        excludedFiles.forEach(excludedFile -> {
            String extension = excludedFile.getExtension();
            excludedExtensions.put(extension, excludedExtensions.containsKey(extension)
                    ? excludedExtensions.get(extension) + 1 : 1);
        });

        return excludedExtensions;
    }

    private void addTotalNumberOfFilesMetric(SourceCodeFiles sourceCodeFiles) {
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Found " + sourceCodeFiles.getAllFiles().size() + " files", start);

        results.getMetricsList().addMetric()
                .id(AnalysisUtils.getMetricId("TOTAL_NUMBER_OF_FILES"))
                .description("Total number of files in the source folder")
                .value(sourceCodeFiles.getAllFiles().size());
    }

    private void analyzeScopes() {
        AnalysisUtils.analyze("", codeConfiguration.getMain(), null, progressFeedback, results.getMainAspectAnalysisResults(),
                results.getMetricsList(), results.getTextSummary(), start);
        AnalysisUtils.analyze("", codeConfiguration.getTest(), null, progressFeedback, results.getTestAspectAnalysisResults(),
                results.getMetricsList(), results.getTextSummary(), start);
        results.getMetricsList().addMetric()
                .id(AnalysisUtils.getMetricId("TEST_VS_MAIN_LINES_OF_CODE_PERCENTAGE"))
                .description("Test / main code ratio")
                .value(((int) (10000.0 * results.getTestAspectAnalysisResults().getLinesOfCode() / results.getMainAspectAnalysisResults().getLinesOfCode())) / 100.0
                );

        AnalysisUtils.analyze("", codeConfiguration.getGenerated(), null, progressFeedback, results.getGeneratedAspectAnalysisResults(),
                results.getMetricsList(), results.getTextSummary(), start);
        AnalysisUtils.analyze("", codeConfiguration.getBuildAndDeployment(), null, progressFeedback, results.getBuildAndDeployAspectAnalysisResults(),
                results.getMetricsList(), results.getTextSummary(), start);
        AnalysisUtils.analyze("", codeConfiguration.getOther(), null, progressFeedback, results.getOtherAspectAnalysisResults(),
                results.getMetricsList(), results.getTextSummary(), start);
    }
}
