/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ControlStatus;
import nl.obren.sokrates.sourcecode.analysis.results.ControlsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.GoalsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

public class ControlsAnalyzer extends Analyzer {
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final ControlsAnalysisResults controlsAnalysisResults;

    public ControlsAnalyzer(CodeAnalysisResults analysisResults, ProgressFeedback progressFeedback) {
        this.controlsAnalysisResults = analysisResults.getControlResults();
        this.codeConfiguration = analysisResults.getCodeConfiguration();
        this.metricsList = analysisResults.getMetricsList();
    }

    public void analyze() {
        codeConfiguration.getGoalsAndControls().forEach(goal -> {
            GoalsAnalysisResults goalsAnalysisResults = new GoalsAnalysisResults();
            goalsAnalysisResults.setMetricsWithGoal(goal);

            goal.getControls().forEach(control -> {
                Metric metric = metricsList.getMetricById(control.getMetric());
                ControlStatus controlStatus = new ControlStatus();
                controlStatus.setControl(control);
                if (metric == null) {
                    controlStatus.setMetric(new Metric().id(control.getMetric()));
                    controlStatus.setStatus("IGNORED: the metric not found");
                } else {
                    controlStatus.setMetric(metric);
                    boolean inRange = control.getDesiredRange().isInRange(metric.getValue().doubleValue());
                    boolean inRangeWithTolerance = control.getDesiredRange().isInRangeWithTolerance(metric.getValue().doubleValue());
                    controlStatus.setStatus(inRange ? "OK" : (inRangeWithTolerance ? "TOLERANT" : "FAILED"));
                }
                goalsAnalysisResults.getControlStatuses().add(controlStatus);
            });

            controlsAnalysisResults.getGoalsAnalysisResults().add(goalsAnalysisResults);
        });
    }
}
