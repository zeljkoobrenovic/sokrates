/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SourceFileDuplication {
    private SourceFile sourceFile;
    private int cleanedLinesOfCode;
    // A set (insertion-ordered) so de-duplicating a line index is O(1) instead of a List.contains scan;
    // a hot duplication file can otherwise make addLines O(n^2) in its duplicated-line count.
    private Set<Integer> duplicatedLineIndexes = new LinkedHashSet<>();

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void addLines(int startLine, int endLine) {
        for (int i = startLine; i <= endLine; i++) {
            duplicatedLineIndexes.add(i);
        }
    }

    public List<Integer> getDuplicatedLineIndexes() {
        return new ArrayList<>(duplicatedLineIndexes);
    }

    public void setDuplicatedLineIndexes(List<Integer> duplicatedLineIndexes) {
        this.duplicatedLineIndexes = new LinkedHashSet<>(duplicatedLineIndexes);
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
