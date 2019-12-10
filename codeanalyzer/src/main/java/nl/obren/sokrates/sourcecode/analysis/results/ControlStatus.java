/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;

public class ControlStatus {
    private MetricRangeControl control;
    private Metric metric;
    private String status;

    public MetricRangeControl getControl() {
        return control;
    }

    public void setControl(MetricRangeControl control) {
        this.control = control;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
