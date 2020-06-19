/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.metrics;

import nl.obren.sokrates.common.utils.SystemUtils;

public class Metric {

    private String id;
    private Number value;
    private String description;

    public String getId() {
        return id;
    }

    public Metric id(String name) {
        this.id = SystemUtils.getFileSystemFriendlyName(name).toUpperCase();
        return this;
    }

    public Number getValue() {
        return value;
    }

    public Metric value(Number value) {
        this.value = value;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Metric description(String description) {
        this.description = description;
        return this;
    }
}
