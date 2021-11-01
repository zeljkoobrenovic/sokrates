/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import nl.obren.sokrates.common.utils.BasicColorInfo;

public class Threshold {
    private String title;
    private Number threshold;
    private BasicColorInfo color;

    public Threshold(String title, Number threshold, BasicColorInfo color) {
        this.title = title;
        this.threshold = threshold;
        this.color = color;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Number getThreshold() {
        return threshold;
    }

    public void setThreshold(Number threshold) {
        this.threshold = threshold;
    }

    public BasicColorInfo getColor() {
        return color;
    }

    public void setColor(BasicColorInfo color) {
        this.color = color;
    }
}
