/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import java.util.ArrayList;
import java.util.List;

public class VisualizationItem {
    private String name;
    private String color = "";
    private Integer size;
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
}
