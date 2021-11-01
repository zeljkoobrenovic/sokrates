/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.cleaners;

import java.util.ArrayList;
import java.util.List;

public class CleanedContent {
    private String cleanedContent;
    private List<Integer> fileLineIndexes = new ArrayList<>();

    public CleanedContent() {
    }

    public int getCleanedLinesCount() {
        return fileLineIndexes.size();
    }

    public CleanedContent(String cleanedContent) {
        this.cleanedContent = cleanedContent;
    }

    public String getCleanedContent() {
        return cleanedContent;
    }

    public void setCleanedContent(String cleanedContent) {
        this.cleanedContent = cleanedContent;
    }

    public List<Integer> getFileLineIndexes() {
        return fileLineIndexes;
    }

    public void setFileLineIndexes(List<Integer> fileLineIndexes) {
        this.fileLineIndexes = fileLineIndexes;
    }

    public List<String> getLines() {
        return SourceCodeCleanerUtils.splitInLines(cleanedContent);
    }
}
