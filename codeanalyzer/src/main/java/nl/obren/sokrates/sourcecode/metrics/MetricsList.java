/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MetricsList {
    private List<Metric> metrics = new ArrayList<>();
    // Lazily built lower-cased id -> Metric index for O(1) getMetricById, instead of scanning the
    // whole list per lookup. Invalidated (set to null) whenever the list is structurally changed.
    // Built on first query, by which point metric ids (set via the builder after addMetric) are final.
    private transient Map<String, Metric> idIndex = null;

    public Metric addMetric() {
        Metric metric = new Metric();
        metrics.add(metric);
        idIndex = null;
        return metric;
    }

    public void remove(String id) {
        for (int i = 0; i < metrics.size(); i++) {
            if (metrics.get(i).getId().equalsIgnoreCase(id)) {
                metrics.remove(i);
                idIndex = null;
                return;
            }
        }
    }

    public Metric addSystemMetric() {
        Metric metric = new Metric();
        metrics.add(metric);
        idIndex = null;
        return metric;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
        idIndex = null;
    }

    public Metric getMetricById(String id) {
        if (idIndex == null) {
            Map<String, Metric> index = new HashMap<>();
            // First id wins, matching the previous first-match-in-list-order scan.
            for (Metric metric : metrics) {
                if (metric.getId() != null) {
                    index.putIfAbsent(metric.getId().toLowerCase(Locale.ROOT), metric);
                }
            }
            idIndex = index;
        }
        return id != null ? idIndex.get(id.toLowerCase(Locale.ROOT)) : null;
    }
}
