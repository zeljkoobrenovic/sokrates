/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.metrics.MetricsWithGoal;

import java.util.ArrayList;
import java.util.List;

public class GoalsAnalysisResults {
    private MetricsWithGoal metricsWithGoal;

    private List<ControlStatus> controlStatuses = new ArrayList<>();

    public MetricsWithGoal getMetricsWithGoal() {
        return metricsWithGoal;
    }

    public void setMetricsWithGoal(MetricsWithGoal metricsWithGoal) {
        this.metricsWithGoal = metricsWithGoal;
    }

    public List<ControlStatus> getControlStatuses() {
        return controlStatuses;
    }

    public void setControlStatuses(List<ControlStatus> controlStatuses) {
        this.controlStatuses = controlStatuses;
    }
}
