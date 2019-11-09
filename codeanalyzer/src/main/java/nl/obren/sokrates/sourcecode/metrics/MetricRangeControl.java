package nl.obren.sokrates.sourcecode.metrics;

import nl.obren.sokrates.sourcecode.aspects.Range;

public class MetricRangeControl {
    private String metric = "";
    private String description = "";
    private Range desiredRange = new Range();

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Range getDesiredRange() {
        return desiredRange;
    }

    public void setDesiredRange(Range desiredRange) {
        this.desiredRange = desiredRange;
    }
}
