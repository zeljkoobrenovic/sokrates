/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class Range {
    // A minimal value (has to be <= max)
    private String min = "";

    // A maximal value (has to be >= min)
    private String max = "";

    // If bigger than zero, the valid range is from [min - tolerance] to [max + tolerance]
    private String tolerance = "0";

    public Range() {
    }

    public Range(String min, String max) {
        this.min = min;
        this.max = max;
    }

    public Range(String min, String max, String tolerance) {
        this.min = min;
        this.max = max;
        this.tolerance = tolerance;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getTolerance() {
        return tolerance;
    }

    public void setTolerance(String tolerance) {
        this.tolerance = tolerance;
    }

    @JsonIgnore
    public boolean isInRange(double value) {
        return isBiggerThan(min, value, "0") && isSmallerThan(max, value, "0");
    }

    @JsonIgnore
    public boolean isInRangeWithTolerance(double value) {
        return isBiggerThan(min, value, tolerance) && isSmallerThan(max, value, tolerance);
    }

    @JsonIgnore
    private boolean isBiggerThan(String testValue, double value, String tolerance) {
        if (StringUtils.isBlank(testValue)) {
            return true;
        }
        String testValueString = testValue.replace(" ", "").trim();
        if (NumberUtils.isCreatable(testValueString) && NumberUtils.isCreatable(tolerance)) {
            return value >= Double.parseDouble(testValueString) - Double.parseDouble(tolerance);
        }

        return false;
    }

    @JsonIgnore
    private boolean isSmallerThan(String testValue, double value, String tolerance) {
        if (StringUtils.isBlank(testValue)) {
            return true;
        }
        String testValueString = testValue.replace(" ", "").trim();
        if (NumberUtils.isCreatable(testValueString) && NumberUtils.isCreatable(tolerance)) {
            return value <= Double.parseDouble(testValueString) + Double.parseDouble(tolerance);
        }

        return false;
    }

    @JsonIgnore
    public String getTextDescription() {
        return "[" + min + " - " + max + "] ±" + tolerance;
    }
}
