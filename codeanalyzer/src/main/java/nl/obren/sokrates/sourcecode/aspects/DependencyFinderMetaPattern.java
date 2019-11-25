package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.sourcecode.operations.OperationStatement;

import java.util.ArrayList;
import java.util.List;

public class DependencyFinderMetaPattern {
    private String pathPattern = "";
    private String contentPattern = "";

    private List<OperationStatement> nameOperations = new ArrayList<>();

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
}
