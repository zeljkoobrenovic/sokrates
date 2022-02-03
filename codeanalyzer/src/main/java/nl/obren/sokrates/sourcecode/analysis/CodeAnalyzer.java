/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.files.*;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
        ProcessingStopwatch.start("analysis");

        this.progressFeedback = progressFeedback;
        results = new CodeAnalysisResults();
        results.setMetadata(codeConfiguration.getMetadata());
        start = System.currentTimeMillis();
        results.setAnalysisStartTimeMs(start);
        results.setCodeConfiguration(codeConfiguration);

        AnalysisUtils.detailedInfo(results.getTextSummary(), progressFeedback, "Start of analysis", start);

        ProcessingStopwatch.start("analysis/basic");
        new BasicsAnalyzer(results, codeConfigurationFile, progressFeedback).analyze();
        ProcessingStopwatch.end("analysis/basic");

        if (shouldAnalyzeLogicalDecomposition()) {
            ProcessingStopwatch.start("analysis/logical decomposition");
            new LogicalDecompositionAnalyzer(results).analyze(progressFeedback);
            ProcessingStopwatch.end("analysis/logical decomposition");
        }

        if (shouldAnalyzeConcerns()) {
            ProcessingStopwatch.start("analysis/features of interest");
            new ConcernsAnalyzer(results, progressFeedback).analyze();
            ProcessingStopwatch.end("analysis/features of interest");
        }

        if (shouldAnalyzeFileSize()) {
            ProcessingStopwatch.start("analysis/file size");
            new FileSizeAnalyzer(results).analyze();
            ProcessingStopwatch.end("analysis/file size");
        }

        if (shouldAnalyzeUnits()) {
            ProcessingStopwatch.start("analysis/units");
            new UnitsAnalyzer(results, progressFeedback).analyze();
            ProcessingStopwatch.end("analysis/units");
        }

        if (shouldAnalyzeFileHistory()) {
            ProcessingStopwatch.start("analysis/file history");
            new FileHistoryAnalyzer(results, codeConfigurationFile.getParentFile()).analyze();
            ProcessingStopwatch.end("analysis/file history");

            ProcessingStopwatch.start("analysis/contributors");
            new ContributorsAnalyzer(results, codeConfigurationFile.getParentFile()).analyze();
            ProcessingStopwatch.end("analysis/contributors");
        }

        if (shouldAnalyzeDuplication()) {
            ProcessingStopwatch.start("analysis/duplication");
            new DuplicationAnalyzer(results).analyze(progressFeedback);
            ProcessingStopwatch.end("analysis/duplication");
        }

        if (shouldAnalyzeControls()) {
            ProcessingStopwatch.start("analysis/controls");
            new ControlsAnalyzer(results, progressFeedback).analyze();
            ProcessingStopwatch.end("analysis/controls");
        }

        addTotalAnalysisTimeMetric();

        ProcessingStopwatch.end("analysis");

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

        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
        AnalysisUtils.info(results.getTextSummary(), progressFeedback, "Total analysis time: " + decimalFormat.format(((System.currentTimeMillis() - start) / 10) * 0.01) + "s", start);
        AnalysisUtils.info(results.getTextSummary(), progressFeedback, "", start);
    }

    public CodeConfiguration getCodeConfiguration() {
        return codeConfiguration;
    }

    public void setCodeConfiguration(CodeConfiguration codeConfiguration) {
        this.codeConfiguration = codeConfiguration;
    }

}
