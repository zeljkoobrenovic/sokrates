package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.SourceFile;

public class DuplicatedFileBlock {
    private SourceFile sourceFile;
    private int sourceFileCleanedLinesOfCode;
    private int startLine;
    private int endLine;
    private int cleanedStartLine;
    private int cleanedEndLine;

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
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

    public int getBlockSize() {
        return cleanedEndLine - cleanedStartLine + 1;
    }

    public double getPercentage() {
        return sourceFileCleanedLinesOfCode > 0 ? 100.0 * getBlockSize() / sourceFileCleanedLinesOfCode : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof DuplicatedFileBlock)) {
            return false;
        }

        DuplicatedFileBlock duplicatedFileBlock = (DuplicatedFileBlock) obj;

        return duplicatedFileBlock.getSourceFile().equals(this.getSourceFile())
                && duplicatedFileBlock.getCleanedStartLine() == this.getCleanedStartLine()
                && duplicatedFileBlock.getCleanedEndLine() == this.getCleanedEndLine();
    }

}
