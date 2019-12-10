/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.sourcecode.operations.OperationStatement;

import java.util.ArrayList;
import java.util.List;

public class MetaRule {
    private String pathPattern = "";
    private String contentPattern = "";
    private String use = "content"; // valid values are "content" or "path"
    private boolean ignoreComments = false;
    private List<OperationStatement> nameOperations = new ArrayList<>();

    public MetaRule() {
    }

    public MetaRule(String pathPattern, String contentPattern, String use) {
        this.pathPattern = pathPattern;
        this.contentPattern = contentPattern;
        this.use = use;
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

    public List<OperationStatement> getNameOperations() {
        return nameOperations;
    }

    public void setNameOperations(List<OperationStatement> nameOperations) {
        this.nameOperations = nameOperations;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public boolean isIgnoreComments() {
        return ignoreComments;
    }

    public void setIgnoreComments(boolean ignoreComments) {
        this.ignoreComments = ignoreComments;
    }
}
