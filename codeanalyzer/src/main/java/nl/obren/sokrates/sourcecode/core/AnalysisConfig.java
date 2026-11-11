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
    private int maxTemporalDependenciesDepthDays = 365;

    // Commits touching more main files than this are left out of temporal file dependencies (a commit with N files
    // yields N*(N-1)/2 file pairs, and such commits are mostly mass edits such as reformatting); 0 or less = no limit
    private int maxFilesPerCommitForTemporalDependencies = 100;

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
    // Thresholds for risk profiles used in the file complexity analysis (sum of the McCabe indexes of a
    // file's units). Replaces "fileConditionalComplexityThresholds", which nothing read and which init
    // wrote with the unit bands; that key is now ignored when a configuration is loaded.
    private Thresholds fileComplexityThresholds = Thresholds.defaultFileComplexityThresholds();

    // The File Size report's "Large Files That Change Often" section: window, list size, tokens per line
    private FileReadsForChangesConfig fileReadsForChanges = new FileReadsForChangesConfig();

    // The Human and AI maintainability scores (Highlights tiles, Maintainability Scores section, landscape columns)
    private MaintainabilityScoresConfig maintainabilityScores = new MaintainabilityScoresConfig();

    // Configured starting values of the estimate pages' assumptions (applied only when enabled)
    private EstimateAssumptionsConfig estimateAssumptions = new EstimateAssumptionsConfig();

    // Thresholds for risk profiles used in commit analysis
    private Thresholds commitFilesCountThresholds = Thresholds.defaultCommitFilesCountThresholds();

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

    public Thresholds getFileComplexityThresholds() {
        return fileComplexityThresholds;
    }

    public void setFileComplexityThresholds(Thresholds fileComplexityThresholds) {
        this.fileComplexityThresholds = fileComplexityThresholds;
    }

    public FileReadsForChangesConfig getFileReadsForChanges() {
        return fileReadsForChanges;
    }

    // An explicit null ("fileReadsForChanges": null) falls back to the defaults.
    public void setFileReadsForChanges(FileReadsForChangesConfig fileReadsForChanges) {
        this.fileReadsForChanges = fileReadsForChanges != null ? fileReadsForChanges : new FileReadsForChangesConfig();
    }

    public MaintainabilityScoresConfig getMaintainabilityScores() {
        return maintainabilityScores;
    }

    // An explicit null ("maintainabilityScores": null) falls back to the defaults.
    public void setMaintainabilityScores(MaintainabilityScoresConfig maintainabilityScores) {
        this.maintainabilityScores = maintainabilityScores != null ? maintainabilityScores : new MaintainabilityScoresConfig();
    }

    public EstimateAssumptionsConfig getEstimateAssumptions() {
        return estimateAssumptions;
    }

    // An explicit null ("estimateAssumptions": null) falls back to the defaults.
    public void setEstimateAssumptions(EstimateAssumptionsConfig estimateAssumptions) {
        this.estimateAssumptions = estimateAssumptions != null ? estimateAssumptions : new EstimateAssumptionsConfig();
    }

    public Thresholds getCommitFilesCountThresholds() {
        return commitFilesCountThresholds;
    }

    public void setCommitFilesCountThresholds(Thresholds commitFilesCountThresholds) {
        this.commitFilesCountThresholds = commitFilesCountThresholds;
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

    public int getMaxFilesPerCommitForTemporalDependencies() {
        return maxFilesPerCommitForTemporalDependencies;
    }

    public void setMaxFilesPerCommitForTemporalDependencies(int maxFilesPerCommitForTemporalDependencies) {
        this.maxFilesPerCommitForTemporalDependencies = maxFilesPerCommitForTemporalDependencies;
    }

    public boolean isAnalyzeConcernOverlaps() {
        return analyzeConcernOverlaps;
    }
    public void setCacheSourceFiles(boolean cacheSourceFiles) {
        this.saveSourceFiles = cacheSourceFiles;
    }
}
