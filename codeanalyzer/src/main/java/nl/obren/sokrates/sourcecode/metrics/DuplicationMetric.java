/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.metrics;

public class DuplicationMetric {
    private String key;
    private int numberOfDuplicates;
    private int cleanedLinesOfCode;
    private int duplicatedLinesOfCode;
    private int numberOfFilesWithDuplicates;

    public DuplicationMetric() {
    }

    public DuplicationMetric(String key) {
        this.key = key;
    }

    public DuplicationMetric(String key, int cleanedLinesOfCode, int duplicatedLinesOfCode) {
        this.key = key;
        this.cleanedLinesOfCode = cleanedLinesOfCode;
        this.duplicatedLinesOfCode = duplicatedLinesOfCode;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getNumberOfDuplicates() {
        return numberOfDuplicates;
    }

    public void setNumberOfDuplicates(int numberOfDuplicates) {
        this.numberOfDuplicates = numberOfDuplicates;
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

    public Number getDuplicationPercentage() {
        return cleanedLinesOfCode > 0 ? (100.0 * duplicatedLinesOfCode / cleanedLinesOfCode) : 0;
    }

    public void setNumberOfFilesWithDuplicates(int numberOfFilesWithDuplicates) {
        this.numberOfFilesWithDuplicates = numberOfFilesWithDuplicates;
    }

    public int getNumberOfFilesWithDuplicates() {
        return numberOfFilesWithDuplicates;
    }
}
