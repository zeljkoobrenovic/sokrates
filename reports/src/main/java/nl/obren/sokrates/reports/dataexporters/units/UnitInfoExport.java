/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.units;

import java.util.List;

public class UnitInfoExport {
    private String shortName = "";
    private String longName = "";
    private String relativeFileName;
    private int fileLinesCount;
    private List<String> components;
    private int startLine = 0;
    private int endLine = 0;
    private int linesOfCode = 0;
    private int mcCabeIndex = 1;
    private int numberOfParameters = 0;
    private int numberOfLiterals = 0;
    private int numberOfStatements = 0;
    private int numberOfExpressions = 0;

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

    public String getRelativeFileName() {
        return relativeFileName;
    }

    public void setRelativeFileName(String relativeFileName) {
        this.relativeFileName = relativeFileName;
    }

    public int getFileLinesCount() {
        return fileLinesCount;
    }

    public void setFileLinesCount(int fileLinesCount) {
        this.fileLinesCount = fileLinesCount;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
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
}
