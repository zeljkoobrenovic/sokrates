/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.files.*;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScoresAnalyzer;
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

        // Apply an optional _sokrates/config-people.json (same model as landscapes) before any analyzer
        // parses git history: it groups a person's emails under one identity and overrides their
        // userName in the per-repository reports. Set here (not in ContributorsAnalyzer) because
        // FileHistoryAnalyzer parses history first and the parsed history is cached for the whole run.
        // Missing/unparseable file -> null -> ignored.
        codeConfiguration.getFileHistoryAnalysis().setPeopleConfig(
                ContributorsAnalyzer.loadPeopleConfig(codeConfigurationFile.getParentFile()));

        AnalysisUtils.detailedInfo(results.getTextSummary(), progressFeedback, "Start of analysis", start);

        timed("analysis/basic", () -> new BasicsAnalyzer(results, codeConfigurationFile, progressFeedback).analyze());
        if (shouldAnalyzeLogicalDecomposition()) {
            timed("analysis/logical decomposition", () -> new LogicalDecompositionAnalyzer(results).analyze(progressFeedback));
        }
        if (shouldAnalyzeConcerns()) {
            timed("analysis/features of interest", () -> new ConcernsAnalyzer(results, progressFeedback).analyze());
        }
        if (shouldAnalyzeFileSize()) {
            timed("analysis/file size", () -> new FileSizeAnalyzer(results).analyze());
        }
        if (shouldAnalyzeUnits()) {
            timed("analysis/units", () -> new UnitsAnalyzer(results, progressFeedback).analyze());
        }
        if (shouldAnalyzeFileHistory()) {
            timed("analysis/file history", () -> new FileHistoryAnalyzer(results, codeConfigurationFile.getParentFile()).analyze());
            timed("analysis/contributors", () -> new ContributorsAnalyzer(results, codeConfigurationFile.getParentFile()).analyze());
        }
        if (shouldAnalyzeDuplication()) {
            timed("analysis/duplication", () -> new DuplicationAnalyzer(results).analyze(progressFeedback));
        }
        timed("analysis/maintainability scores", () -> new MaintainabilityScoresAnalyzer(results).analyze());
        if (shouldAnalyzeControls()) {
            timed("analysis/controls", () -> new ControlsAnalyzer(results, progressFeedback).analyze());
        }

        addTotalAnalysisTimeMetric();

        ProcessingStopwatch.end("analysis");

        return results;
    }

    /** Runs one analysis step between the matching ProcessingStopwatch start/end marks. */
    private static void timed(String step, Runnable work) {
        ProcessingStopwatch.start(step);
        work.run();
        ProcessingStopwatch.end(step);
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
