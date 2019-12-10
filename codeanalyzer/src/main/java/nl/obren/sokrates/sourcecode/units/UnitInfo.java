/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class UnitInfo {
    private String shortName = "";
    private String longName = "";
    private SourceFile sourceFile;
    private int startLine = 0;
    private int endLine = 0;
    private int linesOfCode = 0;
    private int mcCabeIndex = 1;
    private int numberOfParameters = 0;
    private int numberOfLiterals = 0;
    private int numberOfStatements = 0;
    private int numberOfExpressions = 0;
    @JsonIgnore
    private String cleanedBody;
    @JsonIgnore
    private String body;
    private List<UnitInfo> children = new ArrayList<>();

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public int getMcCabeIndex() {
        return mcCabeIndex;
    }

    public void setMcCabeIndex(int mcCabeIndex) {
        this.mcCabeIndex = mcCabeIndex;
    }

    public int getNumberOfParameters() {
        return numberOfParameters;
    }

    public void setNumberOfParameters(int numberOfParameters) {
        this.numberOfParameters = numberOfParameters;
    }

    public int getNumberOfLiterals() {
        return numberOfLiterals;
    }

    public void setNumberOfLiterals(int numberOfLiterals) {
        this.numberOfLiterals = numberOfLiterals;
    }

    public int getNumberOfStatements() {
        return numberOfStatements;
    }

    public void setNumberOfStatements(int numberOfStatements) {
        this.numberOfStatements = numberOfStatements;
    }

    public int getNumberOfExpressions() {
        return numberOfExpressions;
    }

    public void setNumberOfExpressions(int numberOfExpressions) {
        this.numberOfExpressions = numberOfExpressions;
    }

    @JsonIgnore
    public String getCleanedBody() {
        return cleanedBody;
    }

    @JsonIgnore
    public void setCleanedBody(String cleanedBody) {
        this.cleanedBody = cleanedBody;
    }

    @JsonIgnore
    public String getBody() {
        return body;
    }

    @JsonIgnore
    public void setBody(String body) {
        this.body = body;
    }

    public List<UnitInfo> getChildren() {
        return children;
    }

    public void setChildren(List<UnitInfo> children) {
        this.children = children;
    }
}
