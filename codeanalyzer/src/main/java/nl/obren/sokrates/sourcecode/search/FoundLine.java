/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.search;

public class FoundLine {
    private int lineNumber;
    private String line;
    private String foundText;

    public FoundLine() {
    }

    public FoundLine(int lineNumber, String line, String foundText) {
        this.lineNumber = lineNumber;
        this.line = line;
        this.foundText = foundText;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getFoundText() {
        return foundText;
    }

    public void setFoundText(String foundText) {
        this.foundText = foundText;
    }

    @Override
    public String toString() {
        return "Line " + lineNumber + ": " + line.trim();
    }
}
