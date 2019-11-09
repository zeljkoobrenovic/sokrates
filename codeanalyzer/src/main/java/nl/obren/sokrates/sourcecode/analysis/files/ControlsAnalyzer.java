package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ControlStatus;
import nl.obren.sokrates.sourcecode.analysis.results.ControlsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

public class ControlsAnalyzer extends Analyzer {
    private final StringBuffer textSummary;
    private ProgressFeedback progressFeedback;
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final long start;
    private final ControlsAnalysisResults controlsAnalysisResults;
    private final SourceCodeAspect main;

    public ControlsAnalyzer(CodeAnalysisResults analysisResults, ProgressFeedback progressFeedback) {
        this.controlsAnalysisResults = analysisResults.getControlResults();
        this.codeConfiguration = analysisResults.getCodeConfiguration();
        this.metricsList = analysisResults.getMetricsList();
        this.start = analysisResults.getAnalysisStartTimeMs();
        this.textSummary = analysisResults.getTextSummary();
        this.progressFeedback = progressFeedback;
        this.main = codeConfiguration.getMain();
    }

    public void analyze() {
        codeConfiguration.getControls().forEach(control -> {
            Metric metric = metricsList.getMetricById(control.getMetric());
            ControlStatus controlStatus = new ControlStatus();
            controlStatus.setControl(control);
            if (metric == null) {
                controlStatus.setMetric(new Metric().id(control.getMetric()));
                controlStatus.setStatus("ERROR: metric does not exist");
            } else {
                controlStatus.setMetric(metric);
                boolean inRange = control.getDesiredRange().isInRange(metric.getValue().doubleValue());
                boolean inRangeWithTolerance = control.getDesiredRange().isInRangeWithTolerance(metric.getValue().doubleValue());
                controlStatus.setStatus(inRange ? "OK" : (inRangeWithTolerance ? "TOLERANT" : "FAILED"));
            }
            controlsAnalysisResults.getControlStatuses().add(controlStatus);
        });
    }




}
