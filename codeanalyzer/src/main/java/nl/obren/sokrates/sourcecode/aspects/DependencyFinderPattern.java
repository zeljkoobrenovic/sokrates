/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

public class DependencyFinderPattern {
    private String component = "";
    private String pathPattern = "";
    private String contentPattern = "";
    private boolean reverseDirection = false;
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
