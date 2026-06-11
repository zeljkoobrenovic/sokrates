/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

public class VisualizationItem {
    private String name;
    private String color = "";
    private Integer size;
    // Optional hover tooltip text; when blank the chart falls back to the name. Lets the on-hover
    // detail (full name + size + language) differ from the clipped in-bubble label. Not serialized
    // when empty so it adds nothing to charts that don't use it (e.g. the all-files circles).
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String tooltip = "";
    private List<VisualizationItem> children = new ArrayList<>();

    public VisualizationItem(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public VisualizationItem(String name, Integer size, String color) {
        this.name = name;
        this.color = color;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public List<VisualizationItem> getChildren() {
        return children;
    }

    public void setChildren(List<VisualizationItem> children) {
        this.children = children;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
}
