/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import nl.obren.sokrates.sourcecode.analysis.AnalyzerOverride;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.ArrayList;
import java.util.List;

public class AnalysisConfig {
    // If set to true, Sokrates skips duplication analysis and reporting
    private boolean skipDuplication = false;
    private boolean skipCorrelations = false;

    // If set to true, Sokrates skips analysis and reporting of component dependencies
    private boolean skipDependencies = false;

    // If set to true, Sokrates creates a copy of source files linked from reports
    private boolean saveSourceFiles = true;

    // If set to true, Sokrates saves code fragments in files linked from reports
    private boolean saveCodeFragments = true;

    // Sokrates will ignore files longer than a given number of bytes
    private int maxFileSizeBytes = 1000000;

    // Sokrates will ignore files with more than a given number of lines of code
    private int maxLines = 10000;

    // Sokrates will ignore files with any line longer than a given number of characters
    private int maxLineLength = 1000;

    // A maximal number of days in source code history used to calculate temporal file dependencies
    private int maxTemporalDependenciesDepthDays = 180;

    // Repositories with more than a given number of lines of main code will skip duplication analyses even if skipDuplication flag is false
    private int locDuplicationThreshold = 10000000;

    // A minimal size of duplicated code block included in duplication analyses
    private int minDuplicationBlockLoc = 6;

    // A limit for lists of code examples in reports
    private int maxTopListSize = 50;

    // Can override default mapping between file path and used source code analysers
    private List<AnalyzerOverride> analyzerOverrides = new ArrayList<>();

    // Thresholds for risk profiles used in file size analyses
    private Thresholds fileSizeThresholds = Thresholds.defaultFileSizeThresholds();

    // Thresholds for risk profiles used in file age analyses
    private Thresholds fileAgeThresholds = Thresholds.defaultFileAgeThresholds();

    // Thresholds for risk profiles used in file update frequency analyses
    private Thresholds fileUpdateFrequencyThresholds = Thresholds.defaultFileUpdateFrequencyThresholds();

    // Thresholds for risk profiles used in file update frequency analyses
    private Thresholds fileContributorsCountThresholds = Thresholds.defaultFileContributorsCountThresholds();

    // Thresholds for risk profiles used in unit size analyses
    private Thresholds unitSizeThresholds = Thresholds.defaultUnitSizeThresholds();

    // Thresholds for risk profiles used in unit conditional complexity analyses
    private Thresholds conditionalComplexityThresholds = Thresholds.defaultConditionalComplexityThresholds();
    // Thresholds for risk profiles used in unit conditional complexity analyses
    private Thresholds fileConditionalComplexityThresholds = Thresholds.defaultConditionalComplexityThresholds();

    // An optional HTML code fragment to be included in a header section of generated HTML reports (e.g. Google Analytics snippet)
    private String customHtmlReportHeaderFragment = "";

    // If true, in feature of interest analyses, additional features of interest will be generated if there is an overlap between defined features (i.e. if several features include the same files)
    private boolean analyzeConcernOverlaps = false;

    public boolean isSkipDuplication() {
        return skipDuplication;
    }

    public void setSkipDuplication(boolean skipDuplication) {
        this.skipDuplication = skipDuplication;
    }

    public boolean isSkipDependencies() {
        return skipDependencies;
    }

    public boolean isSkipCorrelations() {
        return skipCorrelations;
    }

    public void setSkipCorrelations(boolean skipCorrelations) {
        this.skipCorrelations = skipCorrelations;
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

    public int getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(int maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }

    public void setMaxLineLength(int maxLineLength) {
        this.maxLineLength = maxLineLength;
    }

    public boolean isSaveSourceFiles() {
        return saveSourceFiles;
    }

    public void setSaveSourceFiles(boolean saveSourceFiles) {
        this.saveSourceFiles = saveSourceFiles;
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


    public Thresholds getFileContributorsCountThresholds() {
        return fileContributorsCountThresholds;
    }

    public void setFileContributorsCountThresholds(Thresholds fileContributorsCountThresholds) {
        this.fileContributorsCountThresholds = fileContributorsCountThresholds;
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

    public Thresholds getFileConditionalComplexityThresholds() {
        return fileConditionalComplexityThresholds;
    }

    public void setFileConditionalComplexityThresholds(Thresholds fileConditionalComplexityThresholds) {
        this.fileConditionalComplexityThresholds = fileConditionalComplexityThresholds;
    }

    public String getCustomHtmlReportHeaderFragment() {
        return customHtmlReportHeaderFragment;
    }

    public void setCustomHtmlReportHeaderFragment(String customHtmlReportHeaderFragment) {
        this.customHtmlReportHeaderFragment = customHtmlReportHeaderFragment;
    }

    public boolean getAnalyzeConcernOverlaps() {
        return analyzeConcernOverlaps;
    }

    public void setAnalyzeConcernOverlaps(boolean analyzeConcernOverlaps) {
        this.analyzeConcernOverlaps = analyzeConcernOverlaps;
    }

    public int getMaxTemporalDependenciesDepthDays() {
        return maxTemporalDependenciesDepthDays;
    }

    public void setMaxTemporalDependenciesDepthDays(int maxTemporalDependenciesDepthDays) {
        this.maxTemporalDependenciesDepthDays = maxTemporalDependenciesDepthDays;
    }

    public boolean isAnalyzeConcernOverlaps() {
        return analyzeConcernOverlaps;
    }
    public void setCacheSourceFiles(boolean cacheSourceFiles) {
        this.saveSourceFiles = cacheSourceFiles;
    }
}
