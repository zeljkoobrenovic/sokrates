/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.trends;

import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.util.ArrayList;
import java.util.List;

public class MetricsTrendInfo {
    private String id;
    private String description;

    private List<ValueSnapshotPair> values = new ArrayList<>();

    public MetricsTrendInfo() {
    }

    public MetricsTrendInfo(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public MetricsTrendInfo(Metric metric) {
        this(metric.getId(), metric.getDescription());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ValueSnapshotPair> getValues() {
        return values;
    }

    public void setValues(List<ValueSnapshotPair> values) {
        this.values = values;
    }
}
