/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.duplication;

import nl.obren.sokrates.reports.dataexporters.files.FileExportInfo;

public class DuplicateFileBlockExportInfo {
    private FileExportInfo file = new FileExportInfo();
    private int sourceFileCleanedLinesOfCode;
    private int startLine;
    private int endLine;
    private int cleanedStartLine;
    private int cleanedEndLine;

    public FileExportInfo getFile() {
        return file;
    }

    public void setFile(FileExportInfo file) {
        this.file = file;
    }

    public int getSourceFileCleanedLinesOfCode() {
        return sourceFileCleanedLinesOfCode;
    }

    public void setSourceFileCleanedLinesOfCode(int sourceFileCleanedLinesOfCode) {
        this.sourceFileCleanedLinesOfCode = sourceFileCleanedLinesOfCode;
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

    public int getCleanedStartLine() {
        return cleanedStartLine;
    }

    public void setCleanedStartLine(int cleanedStartLine) {
        this.cleanedStartLine = cleanedStartLine;
    }

    public int getCleanedEndLine() {
        return cleanedEndLine;
    }

    public void setCleanedEndLine(int cleanedEndLine) {
        this.cleanedEndLine = cleanedEndLine;
    }
}
