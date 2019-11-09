package nl.obren.sokrates.sourcecode.analysis;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.files.*;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.Metric;
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
        start = System.currentTimeMillis();
        results.setAnalysisStartTimeMs(start);
        results.setCodeConfiguration(codeConfiguration);

        AnalysisUtils.detailedInfo(results.getTextSummary(), progressFeedback, "Start of analysis", start);

        new BasicsAnalyzer(results, codeConfigurationFile, progressFeedback).analyze();

        if (shouldAnalyzeLogicalDecomposition()) {
            new LogicalDecompositionAnalyzer(results).analyze(progressFeedback);
        }

        if (shouldAnalyzeCrossCuttingConcerns()) {
            new CrossCuttingConcernsAnalyzer(results, progressFeedback).analyze();
        }

        if (shouldAnalyzeFiles()) {
            new FileSizeAnalyzer(results).analyze();
        }

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


    private boolean shouldAnalyzeCrossCuttingConcerns() {
        return codeAnalyzerSettings.isAnalyzeCrossCuttingConcerns() || codeAnalyzerSettings.isCreateMetricsList() || codeAnalyzerSettings.isAnalyzeControls();
    }

    private boolean shouldAnalyzeFiles() {
        return codeAnalyzerSettings.isAnalyzeFileSize() || codeAnalyzerSettings.isCreateMetricsList() || codeAnalyzerSettings.isAnalyzeControls();
    }

    private boolean shouldAnalyzeUnits() {
        return codeAnalyzerSettings.isAnalyzeUnitSize() || codeAnalyzerSettings.isAnalyzeCyclomaticComplexity()
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
                .scope(Metric.Scope.SYSTEM)
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
