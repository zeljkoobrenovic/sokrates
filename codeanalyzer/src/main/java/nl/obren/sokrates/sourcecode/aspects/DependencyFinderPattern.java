/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

public class DependencyFinderPattern {
    // A name of the target component
    private String component = "";

    // A regex expression applied on a file path used use to identify files with dependencies to a target component
    private String pathPattern = "";

    // A regex expression applied on a file content (any lis of code) used use to identify files with dependencies to a target component
    private String contentPattern = "";

    // If true, identified dependency will be visualized with an arrow from target to source
    private boolean reverseDirection = false;

    // An optional link color used in dependency diagrams
    private String color = "";

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String getContentPattern() {
        return contentPattern;
    }

    public void setContentPattern(String contentPattern) {
        this.contentPattern = contentPattern;
    }

    public boolean isReverseDirection() {
        return reverseDirection;
    }

    public void setReverseDirection(boolean reverseDirection) {
        this.reverseDirection = reverseDirection;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
