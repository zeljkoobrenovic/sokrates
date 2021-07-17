/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.metrics;

import java.util.ArrayList;
import java.util.List;

public class NumericMetric {
    private String name;
    private Number value;
    private List<NumericMetric> description = new ArrayList<>();

    public NumericMetric() {
    }

    public NumericMetric(String name, Number value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }

    public List<NumericMetric> getDescription() {
        return description;
    }

    public void setDescription(List<NumericMetric> description) {
        this.description = description;
    }
}
