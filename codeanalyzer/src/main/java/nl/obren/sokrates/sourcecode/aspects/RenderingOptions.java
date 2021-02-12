/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

public class RenderingOptions {
    private String orientation = "TB";
    private int maxNumberOfDependencies = 100;
    private boolean renderComponentsWithoutDependencies = true;
    private boolean renderIndirectDependencies = false;
    private boolean renderInternalIndirectDependencies = false;
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
