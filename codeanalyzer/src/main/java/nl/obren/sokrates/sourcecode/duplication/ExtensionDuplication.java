/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

public class ExtensionDuplication {
    private String extension;
    private int cleanedLinesOfCode;
    private int duplicatedLinesOfCode;

    public ExtensionDuplication(int duplicatedLinesOfCode, int cleanedLinesCount) {
        this.duplicatedLinesOfCode = duplicatedLinesOfCode;
        this.cleanedLinesOfCode = cleanedLinesCount;
    }

    public ExtensionDuplication() {
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getCleanedLinesOfCode() {
        return cleanedLinesOfCode;
    }

    public void setCleanedLinesOfCode(int cleanedLinesOfCode) {
        this.cleanedLinesOfCode = cleanedLinesOfCode;
    }

    public int getDuplicatedLinesOfCode() {
        return duplicatedLinesOfCode;
    }

    public void setDuplicatedLinesOfCode(int duplicatedLinesOfCode) {
        this.duplicatedLinesOfCode = duplicatedLinesOfCode;
    }
}
