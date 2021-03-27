/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import nl.obren.sokrates.sourcecode.analysis.AnalyzerOverride;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.ArrayList;
import java.util.List;

public class AnalysisConfig {
    private boolean skipDuplication = false;
    private boolean skipDependencies = false;
    private boolean cacheSourceFiles = true;
    private boolean saveCodeFragments = true;
    private int maxLineLength = 1000;
    private int locDuplicationThreshold = 10000000;
    private int minDuplicationBlockLoc = 6;
    private int maxTopListSize = 50;
    private List<AnalyzerOverride> analyzerOverrides = new ArrayList<>();
    private Thresholds fileSizeThresholds = Thresholds.defaultFileSizeThresholds();
    private Thresholds fileAgeThresholds = Thresholds.defaultFileAgeThresholds();
    private Thresholds fileUpdateFrequencyThresholds = Thresholds.defaultFileUpdateFrequencyThresholds();
    private Thresholds unitSizeThresholds = Thresholds.defaultUnitSizeThresholds();
    private Thresholds conditionalComplexityThresholds = Thresholds.defaultConditionalComplexityThresholds();

    public boolean isSkipDuplication() {
        return skipDuplication;
    }

    public void setSkipDuplication(boolean skipDuplication) {
        this.skipDuplication = skipDuplication;
    }

    public boolean isSkipDependencies() {
        return skipDependencies;
    }

    public void setSkipDependencies(boolean skipDependencies) {
        this.skipDependencies = skipDependencies;
    }

    public List<AnalyzerOverride> getAnalyzerOverrides() {
        return analyzerOverrides;
    }

    public void setAnalyzerOverrides(List<AnalyzerOverride> analyzerOverrides) {
        this.analyzerOverrides = analyzerOverrides;
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }

    public void setMaxLineLength(int maxLineLength) {
        this.maxLineLength = maxLineLength;
    }

    public boolean isCacheSourceFiles() {
        return cacheSourceFiles;
    }

    public void setCacheSourceFiles(boolean cacheSourceFiles) {
        this.cacheSourceFiles = cacheSourceFiles;
    }

    public int getLocDuplicationThreshold() {
        return locDuplicationThreshold;
    }

    public void setLocDuplicationThreshold(int locDuplicationThreshold) {
        this.locDuplicationThreshold = locDuplicationThreshold;
    }

    public int getMinDuplicationBlockLoc() {
        return minDuplicationBlockLoc;
    }

    public void setMinDuplicationBlockLoc(int minDuplicationBlockLoc) {
        this.minDuplicationBlockLoc = minDuplicationBlockLoc;
    }

    public int getMaxTopListSize() {
        return maxTopListSize;
    }

    public void setMaxTopListSize(int maxTopListSize) {
        this.maxTopListSize = maxTopListSize;
    }

    public boolean isSaveCodeFragments() {
        return saveCodeFragments;
    }

    public void setSaveCodeFragments(boolean saveCodeFragments) {
        this.saveCodeFragments = saveCodeFragments;
    }

    public Thresholds getFileSizeThresholds() {
        return fileSizeThresholds;
    }

    public void setFileSizeThresholds(Thresholds fileSizeThresholds) {
        this.fileSizeThresholds = fileSizeThresholds;
    }

    public Thresholds getFileAgeThresholds() {
        return fileAgeThresholds;
    }

    public void setFileAgeThresholds(Thresholds fileAgeThresholds) {
        this.fileAgeThresholds = fileAgeThresholds;
    }

    public Thresholds getFileUpdateFrequencyThresholds() {
        return fileUpdateFrequencyThresholds;
    }

    public void setFileUpdateFrequencyThresholds(Thresholds fileUpdateFrequencyThresholds) {
        this.fileUpdateFrequencyThresholds = fileUpdateFrequencyThresholds;
    }

    public Thresholds getUnitSizeThresholds() {
        return unitSizeThresholds;
    }

    public void setUnitSizeThresholds(Thresholds unitSizeThresholds) {
        this.unitSizeThresholds = unitSizeThresholds;
    }

    public Thresholds getConditionalComplexityThresholds() {
        return conditionalComplexityThresholds;
    }

    public void setConditionalComplexityThresholds(Thresholds conditionalComplexityThresholds) {
        this.conditionalComplexityThresholds = conditionalComplexityThresholds;
    }
}
