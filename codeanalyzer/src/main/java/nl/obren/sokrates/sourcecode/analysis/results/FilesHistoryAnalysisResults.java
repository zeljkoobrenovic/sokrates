/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileChangeDistribution;

import java.util.ArrayList;
import java.util.List;

public class FilesHistoryAnalysisResults {
    private SourceFileAgeDistribution overallFileLastModifiedDistribution;
    private SourceFileAgeDistribution overallFileFirstModifiedDistribution;
    private SourceFileChangeDistribution overallFileChangeDistribution;

    private List<RiskDistributionStats> changeDistributionPerExtension = new ArrayList<>();
    private List<RiskDistributionStats> lastModifiedDistributionPerExtension = new ArrayList<>();
    private List<RiskDistributionStats> firstModifiedDistributionPerExtension = new ArrayList<>();

    private List<FilePairChangedTogether> filePairsChangedTogether = new ArrayList<>();

    private List<FileAgeDistributionPerLogicalDecomposition> changeDistributionPerLogicalDecomposition = new ArrayList<>();
    private List<FileAgeDistributionPerLogicalDecomposition> firstModifiedDistributionPerLogicalDecomposition = new ArrayList<>();
    private List<FileAgeDistributionPerLogicalDecomposition> lastModifiedDistributionPerLogicalDecomposition = new ArrayList<>();

    @JsonIgnore
    private List<SourceFile> allFiles = new ArrayList<>();
    @JsonIgnore
    private List<FileModificationHistory> history = new ArrayList<>();

    private List<SourceFile> oldestFiles = new ArrayList<>();
    private List<SourceFile> youngestFiles = new ArrayList<>();

    private List<SourceFile> mostRecentlyChangedFiles = new ArrayList<>();
    private List<SourceFile> mostPreviouslyChangedFiles = new ArrayList<>();
    private List<SourceFile> mostChangedFiles = new ArrayList<>();

    @JsonIgnore
    public List<SourceFile> getAllFiles() {
        return allFiles;
    }

    @JsonIgnore
    public void setAllFiles(List<SourceFile> allFiles) {
        this.allFiles = allFiles;
    }

    public List<SourceFile> getOldestFiles() {
        return oldestFiles;
    }

    public void setOldestFiles(List<SourceFile> oldestFiles) {
        this.oldestFiles = oldestFiles;
    }

    public List<SourceFile> getYoungestFiles() {
        return youngestFiles;
    }

    public void setYoungestFiles(List<SourceFile> youngestFiles) {
        this.youngestFiles = youngestFiles;
    }

    public SourceFileAgeDistribution getOverallFileLastModifiedDistribution() {
        return overallFileLastModifiedDistribution;
    }

    public void setOverallFileLastModifiedDistribution(SourceFileAgeDistribution overallFileLastModifiedDistribution) {
        this.overallFileLastModifiedDistribution = overallFileLastModifiedDistribution;
    }

    public SourceFileAgeDistribution getOverallFileFirstModifiedDistribution() {
        return overallFileFirstModifiedDistribution;
    }

    public void setOverallFileFirstModifiedDistribution(SourceFileAgeDistribution overallFileFirstModifiedDistribution) {
        this.overallFileFirstModifiedDistribution = overallFileFirstModifiedDistribution;
    }

    public List<RiskDistributionStats> getLastModifiedDistributionPerExtension() {
        return lastModifiedDistributionPerExtension;
    }

    public void setLastModifiedDistributionPerExtension(List<RiskDistributionStats> lastModifiedDistributionPerExtension) {
        this.lastModifiedDistributionPerExtension = lastModifiedDistributionPerExtension;
    }


    public List<FileAgeDistributionPerLogicalDecomposition> getLastModifiedDistributionPerLogicalDecomposition() {
        return lastModifiedDistributionPerLogicalDecomposition;
    }

    public void setLastModifiedDistributionPerLogicalDecomposition(List<FileAgeDistributionPerLogicalDecomposition> lastModifiedDistributionPerLogicalDecomposition) {
        this.lastModifiedDistributionPerLogicalDecomposition = lastModifiedDistributionPerLogicalDecomposition;
    }

    public List<RiskDistributionStats> getFirstModifiedDistributionPerExtension() {
        return firstModifiedDistributionPerExtension;
    }

    public void setFirstModifiedDistributionPerExtension(List<RiskDistributionStats> firstModifiedDistributionPerExtension) {
        this.firstModifiedDistributionPerExtension = firstModifiedDistributionPerExtension;
    }

    public List<FileAgeDistributionPerLogicalDecomposition> getFirstModifiedDistributionPerLogicalDecomposition() {
        return firstModifiedDistributionPerLogicalDecomposition;
    }

    public void setFirstModifiedDistributionPerLogicalDecomposition(List<FileAgeDistributionPerLogicalDecomposition> firstModifiedDistributionPerLogicalDecomposition) {
        this.firstModifiedDistributionPerLogicalDecomposition = firstModifiedDistributionPerLogicalDecomposition;
    }

    public List<SourceFile> getMostRecentlyChangedFiles() {
        return mostRecentlyChangedFiles;
    }

    public void setMostRecentlyChangedFiles(List<SourceFile> mostRecentlyChangedFiles) {
        this.mostRecentlyChangedFiles = mostRecentlyChangedFiles;
    }

    public List<SourceFile> getMostPreviouslyChangedFiles() {
        return mostPreviouslyChangedFiles;
    }

    public void setMostPreviouslyChangedFiles(List<SourceFile> mostPreviouslyChangedFiles) {
        this.mostPreviouslyChangedFiles = mostPreviouslyChangedFiles;
    }

    public List<SourceFile> getMostChangedFiles() {
        return mostChangedFiles;
    }

    public void setMostChangedFiles(List<SourceFile> mostChangedFiles) {
        this.mostChangedFiles = mostChangedFiles;
    }

    public SourceFileChangeDistribution getOverallFileChangeDistribution() {
        return overallFileChangeDistribution;
    }

    public void setOverallFileChangeDistribution(SourceFileChangeDistribution overallFileChangeDistribution) {
        this.overallFileChangeDistribution = overallFileChangeDistribution;
    }

    public List<RiskDistributionStats> getChangeDistributionPerExtension() {
        return changeDistributionPerExtension;
    }

    public void setChangeDistributionPerExtension(List<RiskDistributionStats> changeDistributionPerExtension) {
        this.changeDistributionPerExtension = changeDistributionPerExtension;
    }

    public List<FileAgeDistributionPerLogicalDecomposition> getChangeDistributionPerLogicalDecomposition() {
        return changeDistributionPerLogicalDecomposition;
    }

    public void setChangeDistributionPerLogicalDecomposition(List<FileAgeDistributionPerLogicalDecomposition> changeDistributionPerLogicalDecomposition) {
        this.changeDistributionPerLogicalDecomposition = changeDistributionPerLogicalDecomposition;
    }

    public List<FilePairChangedTogether> getFilePairsChangedTogether() {
        return filePairsChangedTogether;
    }

    public void setFilePairsChangedTogether(List<FilePairChangedTogether> filePairsChangedTogether) {
        this.filePairsChangedTogether = filePairsChangedTogether;
    }

    @JsonIgnore
    public List<FileModificationHistory> getHistory() {
        return history;
    }

    @JsonIgnore
    public void setHistory(List<FileModificationHistory> history) {
        this.history = history;
    }
}
