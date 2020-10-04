/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.IgnoredFilesGroup;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodeAnalysisResults {
    @JsonIgnore
    private CodeConfiguration codeConfiguration;

    private Metadata metadata = new Metadata();

    private MetricsList metricsList = new MetricsList();
    private ControlsAnalysisResults controlResults = new ControlsAnalysisResults();
    private StringBuffer textSummary = new StringBuffer();

    private int totalNumberOfFilesInScope;
    private AspectAnalysisResults mainAspectAnalysisResults = new AspectAnalysisResults();
    private AspectAnalysisResults testAspectAnalysisResults = new AspectAnalysisResults();
    private AspectAnalysisResults generatedAspectAnalysisResults = new AspectAnalysisResults();
    private AspectAnalysisResults buildAndDeployAspectAnalysisResults = new AspectAnalysisResults();
    private AspectAnalysisResults otherAspectAnalysisResults = new AspectAnalysisResults();

    private List<LogicalDecompositionAnalysisResults> logicalDecompositionsAnalysisResults = new ArrayList<>();
    private List<ConcernsAnalysisResults> concernsAnalysisResults = new ArrayList<>();

    @JsonIgnore
    private List<Dependency> allDependencies = new ArrayList<>();

    private FilesAnalysisResults filesAnalysisResults = new FilesAnalysisResults();
    private FilesHistoryAnalysisResults filesHistoryAnalysisResults = new FilesHistoryAnalysisResults();
    private UnitsAnalysisResults unitsAnalysisResults = new UnitsAnalysisResults();

    private DuplicationAnalysisResults duplicationAnalysisResults = new DuplicationAnalysisResults();
    private ContributorsAnalysisResults contributorsAnalysisResults = new ContributorsAnalysisResults();

    private int numberOfExcludedFiles;
    private Map<String, Integer> excludedExtensions;
    private long analysisStartTimeMs;
    @JsonIgnore
    private List<SourceFile> filesExcludedByExtension;
    @JsonIgnore
    private Map<String, IgnoredFilesGroup> ignoredFilesGroups;

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public MetricsList getMetricsList() {
        return metricsList;
    }

    public void setMetricsList(MetricsList metricsList) {
        this.metricsList = metricsList;
    }

    public StringBuffer getTextSummary() {
        return textSummary;
    }

    public void setTextSummary(StringBuffer textSummary) {
        this.textSummary = textSummary;
    }

    @JsonIgnore
    public CodeConfiguration getCodeConfiguration() {
        return codeConfiguration;
    }

    @JsonIgnore
    public void setCodeConfiguration(CodeConfiguration codeConfiguration) {
        this.codeConfiguration = codeConfiguration;
    }

    public int getTotalNumberOfFilesInScope() {
        return totalNumberOfFilesInScope;
    }

    public void setTotalNumberOfFilesInScope(int totalNumberOfFilesInScope) {
        this.totalNumberOfFilesInScope = totalNumberOfFilesInScope;
    }

    public AspectAnalysisResults getMainAspectAnalysisResults() {
        return mainAspectAnalysisResults;
    }

    public void setMainAspectAnalysisResults(AspectAnalysisResults mainAspectAnalysisResults) {
        this.mainAspectAnalysisResults = mainAspectAnalysisResults;
    }

    public AspectAnalysisResults getTestAspectAnalysisResults() {
        return testAspectAnalysisResults;
    }

    public void setTestAspectAnalysisResults(AspectAnalysisResults testAspectAnalysisResults) {
        this.testAspectAnalysisResults = testAspectAnalysisResults;
    }

    public AspectAnalysisResults getGeneratedAspectAnalysisResults() {
        return generatedAspectAnalysisResults;
    }

    public void setGeneratedAspectAnalysisResults(AspectAnalysisResults generatedAspectAnalysisResults) {
        this.generatedAspectAnalysisResults = generatedAspectAnalysisResults;
    }

    public AspectAnalysisResults getBuildAndDeployAspectAnalysisResults() {
        return buildAndDeployAspectAnalysisResults;
    }

    public void setBuildAndDeployAspectAnalysisResults(AspectAnalysisResults buildAndDeployAspectAnalysisResults) {
        this.buildAndDeployAspectAnalysisResults = buildAndDeployAspectAnalysisResults;
    }

    public AspectAnalysisResults getOtherAspectAnalysisResults() {
        return otherAspectAnalysisResults;
    }

    public void setOtherAspectAnalysisResults(AspectAnalysisResults otherAspectAnalysisResults) {
        this.otherAspectAnalysisResults = otherAspectAnalysisResults;
    }

    public List<LogicalDecompositionAnalysisResults> getLogicalDecompositionsAnalysisResults() {
        return logicalDecompositionsAnalysisResults;
    }

    public void setLogicalDecompositionsAnalysisResults(List<LogicalDecompositionAnalysisResults> logicalDecompositionsAnalysisResults) {
        this.logicalDecompositionsAnalysisResults = logicalDecompositionsAnalysisResults;
    }

    public List<ConcernsAnalysisResults> getConcernsAnalysisResults() {
        return concernsAnalysisResults;
    }

    public void setConcernsAnalysisResults(List<ConcernsAnalysisResults> concernsAnalysisResults) {
        this.concernsAnalysisResults = concernsAnalysisResults;
    }

    @JsonIgnore
    public List<Dependency> getAllDependencies() {
        return allDependencies;
    }

    @JsonIgnore
    public void setAllDependencies(List<Dependency> allDependencies) {
        this.allDependencies = allDependencies;
    }

    public FilesAnalysisResults getFilesAnalysisResults() {
        return filesAnalysisResults;
    }

    public void setFilesAnalysisResults(FilesAnalysisResults filesAnalysisResults) {
        this.filesAnalysisResults = filesAnalysisResults;
    }

    public FilesHistoryAnalysisResults getFilesHistoryAnalysisResults() {
        return filesHistoryAnalysisResults;
    }

    public void setFilesHistoryAnalysisResults(FilesHistoryAnalysisResults filesHistoryAnalysisResults) {
        this.filesHistoryAnalysisResults = filesHistoryAnalysisResults;
    }

    public UnitsAnalysisResults getUnitsAnalysisResults() {
        return unitsAnalysisResults;
    }

    public void setUnitsAnalysisResults(UnitsAnalysisResults unitsAnalysisResults) {
        this.unitsAnalysisResults = unitsAnalysisResults;
    }

    public DuplicationAnalysisResults getDuplicationAnalysisResults() {
        return duplicationAnalysisResults;
    }

    public void setDuplicationAnalysisResults(DuplicationAnalysisResults duplicationAnalysisResults) {
        this.duplicationAnalysisResults = duplicationAnalysisResults;
    }

    public int getMaxLinesOfCode() {
        int maxLinesOfCode = 0;
        maxLinesOfCode = Math.max(mainAspectAnalysisResults.getLinesOfCode(), maxLinesOfCode);
        maxLinesOfCode = Math.max(testAspectAnalysisResults.getLinesOfCode(), maxLinesOfCode);
        maxLinesOfCode = Math.max(generatedAspectAnalysisResults.getLinesOfCode(), maxLinesOfCode);
        maxLinesOfCode = Math.max(buildAndDeployAspectAnalysisResults.getLinesOfCode(), maxLinesOfCode);
        maxLinesOfCode = Math.max(otherAspectAnalysisResults.getLinesOfCode(), maxLinesOfCode);

        return maxLinesOfCode;
    }

    public int getMaxFileCount() {
        int maxFileCount = 0;
        maxFileCount = Math.max(mainAspectAnalysisResults.getFilesCount(), maxFileCount);
        maxFileCount = Math.max(testAspectAnalysisResults.getFilesCount(), maxFileCount);
        maxFileCount = Math.max(generatedAspectAnalysisResults.getFilesCount(), maxFileCount);
        maxFileCount = Math.max(buildAndDeployAspectAnalysisResults.getFilesCount(), maxFileCount);
        maxFileCount = Math.max(otherAspectAnalysisResults.getFilesCount(), maxFileCount);

        return maxFileCount;
    }

    public int getNumberOfExcludedFiles() {
        return numberOfExcludedFiles;
    }

    public void setNumberOfExcludedFiles(int numberOfExcludedFiles) {
        this.numberOfExcludedFiles = numberOfExcludedFiles;
    }

    public Map<String, Integer> getExcludedExtensions() {
        return excludedExtensions;
    }

    public void setExcludedExtensions(Map<String, Integer> excludedExtensions) {
        this.excludedExtensions = excludedExtensions;
    }

    public ControlsAnalysisResults getControlResults() {
        return controlResults;
    }

    public void setControlResults(ControlsAnalysisResults controlResults) {
        this.controlResults = controlResults;
    }

    public long getAnalysisStartTimeMs() {
        return analysisStartTimeMs;
    }

    public void setAnalysisStartTimeMs(long analysisStartTimeMs) {
        this.analysisStartTimeMs = analysisStartTimeMs;
    }

    @JsonIgnore
    public void setFilesExcludedByExtension(List<SourceFile> filesExcludedByExtension) {
        this.filesExcludedByExtension = filesExcludedByExtension;
    }

    @JsonIgnore
    public List<SourceFile> getFilesExcludedByExtension() {
        return filesExcludedByExtension;
    }

    @JsonIgnore
    public void setIgnoredFilesGroups(Map<String, IgnoredFilesGroup> ignoredFilesGroups) {
        this.ignoredFilesGroups = ignoredFilesGroups;
    }

    @JsonIgnore
    public Map<String, IgnoredFilesGroup> getIgnoredFilesGroups() {
        return ignoredFilesGroups;
    }

    public ContributorsAnalysisResults getContributorsAnalysisResults() {
        return contributorsAnalysisResults;
    }

    public void setContributorsAnalysisResults(ContributorsAnalysisResults contributorsAnalysisResults) {
        this.contributorsAnalysisResults = contributorsAnalysisResults;
    }
}
