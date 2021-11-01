/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

public class RenderingOptions {
    // A rendering orientation used in Graphviz based dependency diagrams (see https://graphviz.org/docs/attr-types/rankdir/)
    private String orientation = "TB";

    // A maximal number of links between components displayed in dependency diagrams
    private int maxNumberOfDependencies = 100;

    // If true, components without dependencies will be visible in diagrams, otherwise they will be hidden.
    private boolean renderComponentsWithoutDependencies = true;

    // If true, additional diagrams will be generated to show indirect components dependencies (via other components)
    private boolean renderIndirectDependencies = false;

    // If true, in additional diagrams internal dependecies will be shown
    private boolean renderInternalIndirectDependencies = false;

    // If true, all dependency will be visualized with an arrow from target to source
    private boolean reverseDirection = false;

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public boolean isRenderComponentsWithoutDependencies() {
        return renderComponentsWithoutDependencies;
    }

    public void setRenderComponentsWithoutDependencies(boolean renderComponentsWithoutDependencies) {
        this.renderComponentsWithoutDependencies = renderComponentsWithoutDependencies;
    }

    public boolean isRenderIndirectDependencies() {
        return renderIndirectDependencies;
    }

    public void setRenderIndirectDependencies(boolean renderIndirectDependencies) {
        this.renderIndirectDependencies = renderIndirectDependencies;
    }

    public boolean isRenderInternalIndirectDependencies() {
        return renderInternalIndirectDependencies;
    }

    public void setRenderInternalIndirectDependencies(boolean renderInternalIndirectDependencies) {
        this.renderInternalIndirectDependencies = renderInternalIndirectDependencies;
    }

    public int getMaxNumberOfDependencies() {
        return maxNumberOfDependencies;
    }

    public void setMaxNumberOfDependencies(int maxNumberOfDependencies) {
        this.maxNumberOfDependencies = maxNumberOfDependencies;
    }

    public boolean isReverseDirection() {
        return reverseDirection;
    }

    public void setReverseDirection(boolean reverseDirection) {
        this.reverseDirection = reverseDirection;
    }
}
