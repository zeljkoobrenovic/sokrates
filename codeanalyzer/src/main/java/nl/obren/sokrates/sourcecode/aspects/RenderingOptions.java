/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

public class RenderingOptions {
    private String orientation = "TB";
    private boolean renderComponentsWithoutDependencies = true;

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
}
