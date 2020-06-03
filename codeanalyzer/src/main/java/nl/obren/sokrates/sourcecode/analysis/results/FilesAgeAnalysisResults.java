/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;

import java.util.ArrayList;
import java.util.List;

public class FilesAgeAnalysisResults {
    private SourceFileAgeDistribution overallFileAgeDistribution;
    private List<RiskDistributionStats> fileAgeDistributionPerExtension = new ArrayList<>();
    private List<FileAgeDistributionPerLogicalDecomposition> fileAgeDistributionPerLogicalDecomposition = new ArrayList<>();

    @JsonIgnore
    private List<SourceFile> allFiles = new ArrayList<>();

    private List<SourceFile> oldestFiles = new ArrayList<>();
    private List<SourceFile> youngestFiles = new ArrayList<>();

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

    public SourceFileAgeDistribution getOverallFileAgeDistribution() {
        return overallFileAgeDistribution;
    }

    public void setOverallFileAgeDistribution(SourceFileAgeDistribution overallFileAgeDistribution) {
        this.overallFileAgeDistribution = overallFileAgeDistribution;
    }

    public List<RiskDistributionStats> getFileAgeDistributionPerExtension() {
        return fileAgeDistributionPerExtension;
    }

    public void setFileAgeDistributionPerExtension(List<RiskDistributionStats> fileAgeDistributionPerExtension) {
        this.fileAgeDistributionPerExtension = fileAgeDistributionPerExtension;
    }


    public List<FileAgeDistributionPerLogicalDecomposition> getFileAgeDistributionPerLogicalDecomposition() {
        return fileAgeDistributionPerLogicalDecomposition;
    }

    public void setFileAgeDistributionPerLogicalDecomposition(List<FileAgeDistributionPerLogicalDecomposition> fileAgeDistributionPerLogicalDecomposition) {
        this.fileAgeDistributionPerLogicalDecomposition = fileAgeDistributionPerLogicalDecomposition;
    }
}
