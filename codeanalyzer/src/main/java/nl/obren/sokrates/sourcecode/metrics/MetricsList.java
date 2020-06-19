/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.metrics;

import java.util.ArrayList;
import java.util.List;

public class MetricsList {
    private List<Metric> metrics = new ArrayList<>();

    public Metric addMetric() {
        Metric metric = new Metric();
        metrics.add(metric);
        return metric;
    }

    public void remove(String id) {
        int index = 0;

        for (int i = 0; i < metrics.size(); i++) {
            if (metrics.get(i).getId().equalsIgnoreCase(id)) {
                metrics.remove(i);
                return;
            }
        }
    }

    public Metric addSystemMetric() {
        Metric metric = new Metric();
        metrics.add(metric);
        return metric;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    public Metric getMetricById(String id) {
        for (Metric metric : metrics) {
            if (metric.getId().equalsIgnoreCase(id)) {
                return metric;
            }
        }
        return null;
    }
}
