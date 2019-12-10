/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class SourceFileDuplication {
    private SourceFile sourceFile;
    private int cleanedLinesOfCode;
    private List<Integer> duplicatedLineIndexes = new ArrayList<>();

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void addLines(int startLine, int endLine) {
        for (int i = startLine; i <= endLine; i++) {
            addLineIndex(i);
        }
    }

    private void addLineIndex(int lineIndex) {
        if (!duplicatedLineIndexes.contains(lineIndex)) {
            duplicatedLineIndexes.add(lineIndex);
        }
    }

    public List<Integer> getDuplicatedLineIndexes() {
        return duplicatedLineIndexes;
    }

    public void setDuplicatedLineIndexes(List<Integer> duplicatedLineIndexes) {
        this.duplicatedLineIndexes = duplicatedLineIndexes;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public int getCleanedLinesOfCode() {
        return cleanedLinesOfCode;
    }

    public void setCleanedLinesOfCode(int cleanedLinesOfCode) {
        this.cleanedLinesOfCode = cleanedLinesOfCode;
    }

    public int getDuplicatedLinesOfCode() {
        return duplicatedLineIndexes.size();
    }
}
