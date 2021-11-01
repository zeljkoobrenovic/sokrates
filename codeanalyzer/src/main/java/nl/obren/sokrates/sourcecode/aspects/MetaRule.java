/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.sourcecode.operations.OperationStatement;

import java.util.ArrayList;
import java.util.List;

public class MetaRule {
    // A regex rule for file paths included in analyses (if empty all paths are included)
    private String pathPattern = "";

    // A regex rule for content (any source code loune) included in analyses (if empty all files are included)
    private String contentPattern = "";

    // The source of data (for each file) used as a starting value for transformations. Valid values are "content" (a source code line) or "path".
    private String use = "content";

    // If true, only content outside comments will be used for content-based analyses
    private boolean ignoreComments = false;

    // A list of string transformations, applied in a pipeline, to get a component name based on path or content. If empty, the original content is used.
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
