/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.files.*;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.text.DecimalFormat;

public class CodeAnalyzer {
    private static final Log LOG = LogFactory.getLog(CodeAnalyzer.class);
    private long start;
    private CodeAnalyzerSettings codeAnalyzerSettings;
    private CodeConfiguration codeConfiguration;
    private File codeConfigurationFile;
    private CodeAnalysisResults results;
    private ProgressFeedback progressFeedback;

    public CodeAnalyzer(CodeAnalyzerSettings codeAnalyzerSettings, CodeConfiguration codeConfiguration, File codeConfigurationFile) {
        this.codeAnalyzerSettings = codeAnalyzerSettings;
        this.codeConfiguration = codeConfiguration;
        this.codeConfigurationFile = codeConfigurationFile;
    }

    public CodeAnalyzerSettings getCodeAnalyzerSettings() {
        return codeAnalyzerSettings;
    }

    public void setCodeAnalyzerSettings(CodeAnalyzerSettings codeAnalyzerSettings) {
        this.codeAnalyzerSettings = codeAnalyzerSettings;
    }

    public CodeAnalysisResults analyze(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
        results = new CodeAnalysisResults();
        results.setMetadata(codeConfiguration.getMetadata());
        start = System.currentTimeMillis();
        results.setAnalysisStartTimeMs(start);
        results.setCodeConfiguration(codeConfiguration);
        StringBuffer textSummary = results.getTextSummary();

        AnalysisUtils.detailedInfo(results.getTextSummary(), progressFeedback, "Start of analysis", start);

        new BasicsAnalyzer(results, codeConfigurationFile, progressFeedback).analyze();

        if (shouldAnalyzeLogicalDecomposition()) {
            AnalysisUtils.info(textSummary, progressFeedback, "Analysing logical decompositions...", start);
            new LogicalDecompositionAnalyzer(results).analyze(progressFeedback);
        }

        if (shouldAnalyzeConcerns()) {
            AnalysisUtils.info(textSummary, progressFeedback, "Analysing features of interest...", start);
            new ConcernsAnalyzer(results, progressFeedback).analyze();
        }

        if (shouldAnalyzeFileSize()) {
            AnalysisUtils.info(textSummary, progressFeedback, "Analysing file size...", start);
            new FileSizeAnalyzer(results).analyze();
        }

        if (shouldAnalyzeFileHistory()) {
            AnalysisUtils.info(textSummary, progressFeedback, "Analysing commits history...", start);
            new FileHistoryAnalyzer(results, codeConfigurationFile.getParentFile()).analyze();
        }

        new ContributorsAnalyzer(results, codeConfigurationFile.getParentFile()).analyze();

        if (shouldAnalyzeUnits()) {
            new UnitsAnalyzer(results, progressFeedback).analyze();
        }

        if (shouldAnalyzeDuplication()) {
            new DuplicationAnalyzer(results).analyze(progressFeedback);
        }

        if (shouldAnalyzeControls()) {
            new ControlsAnalyzer(results, progressFeedback).analyze();
        }

        addTotalAnalysisTimeMetric();

        return results;
    }


    private boolean shouldAnalyzeConcerns() {
        return codeAnalyzerSettings.isAnalyzeConcerns() || codeAnalyzerSettings.isCreateMetricsList() || codeAnalyzerSettings.isAnalyzeControls();
    }

    private boolean shouldAnalyzeFileSize() {
        return codeAnalyzerSettings.isAnalyzeFileSize() || codeAnalyzerSettings.isCreateMetricsList() || codeAnalyzerSettings.isAnalyzeControls();
    }

    private boolean shouldAnalyzeFileHistory() {
        return codeAnalyzerSettings.isAnalyzeFileHistory();
    }

    private boolean shouldAnalyzeUnits() {
        return codeAnalyzerSettings.isAnalyzeUnitSize() || codeAnalyzerSettings.isAnalyzeConditionalComplexity()
                || codeAnalyzerSettings.isCreateMetricsList() || codeAnalyzerSettings.isAnalyzeControls();
    }

    private boolean shouldAnalyzeDuplication() {
        return codeAnalyzerSettings.isAnalyzeDuplication() || codeAnalyzerSettings.isCreateMetricsList() || codeAnalyzerSettings.isAnalyzeControls();
    }

    private boolean shouldAnalyzeControls() {
        return codeAnalyzerSettings.isAnalyzeControls();
    }

    private boolean shouldAnalyzeLogicalDecomposition() {
        return codeAnalyzerSettings.isAnalyzeLogicalDecomposition() || codeAnalyzerSettings.isCreateMetricsList() || codeAnalyzerSettings.isAnalyzeControls();
    }

    private void addTotalAnalysisTimeMetric() {
        results.getMetricsList().addMetric()
                .id(AnalysisUtils.getMetricId("TOTAL_ANALYSIS_TIME_IN_MILLIS"))
                .description("Total analysis time in milliseconds")
                .value(System.currentTimeMillis() - start);

        AnalysisUtils.info(results.getTextSummary(), progressFeedback, "Total analysis time: " + new DecimalFormat("#.00").format(((System.currentTimeMillis() - start) / 10) * 0.01) + "s", start);
        AnalysisUtils.info(results.getTextSummary(), progressFeedback, "", start);
    }

    public CodeConfiguration getCodeConfiguration() {
        return codeConfiguration;
    }

    public void setCodeConfiguration(CodeConfiguration codeConfiguration) {
        this.codeConfiguration = codeConfiguration;
    }

}
