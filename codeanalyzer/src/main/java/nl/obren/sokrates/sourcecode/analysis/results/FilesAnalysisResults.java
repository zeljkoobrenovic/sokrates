/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;

import java.util.ArrayList;
import java.util.List;

public class FilesAnalysisResults {
    private SourceFileSizeDistribution overallFileSizeDistribution;
    private List<RiskDistributionStats> fileSizeDistributionPerExtension = new ArrayList<>();
    private List<FileDistributionPerLogicalDecomposition> fileSizeDistributionPerLogicalDecomposition = new ArrayList<>();

    @JsonIgnore
    private List<SourceFile> allFiles = new ArrayList<>();
    private List<SourceFile> longestFiles = new ArrayList<>();

    private List<SourceFile> filesWithMostUnits = new ArrayList<>();
    // File complexity (sum of the McCabe indexes of a file's units; files with units only), filled by the
    // UnitsAnalyzer once the units are known.
    private RiskDistributionStats overallFileComplexityDistribution;
    private List<RiskDistributionStats> fileComplexityDistributionPerExtension = new ArrayList<>();
    private List<FileComplexityDistributionPerLogicalDecomposition> fileComplexityDistributionPerLogicalDecomposition = new ArrayList<>();
    private List<SourceFile> mostComplexFiles = new ArrayList<>();

    @JsonIgnore
    public List<SourceFile> getAllFiles() {
        return allFiles;
    }

    @JsonIgnore
    public SourceFile getSourceFileByRelativePath(String relativePath) {
        return allFiles.stream().filter(f -> f.getRelativePath().equalsIgnoreCase(relativePath)).findAny().orElse(null);
    }

    @JsonIgnore
    public void setAllFiles(List<SourceFile> allFiles) {
        this.allFiles = allFiles;
    }

    public List<SourceFile> getLongestFiles() {
        return longestFiles;
    }

    public void setLongestFiles(List<SourceFile> longestFiles) {
        this.longestFiles = longestFiles;
    }

    public List<SourceFile> getFilesWithMostUnits() {
        return filesWithMostUnits;
    }

    public void setFilesWithMostUnits(List<SourceFile> filesWithMostUnits) {
        this.filesWithMostUnits = filesWithMostUnits;
    }

    public SourceFileSizeDistribution getOverallFileSizeDistribution() {
        return overallFileSizeDistribution;
    }

    public void setOveralFileSizeDistribution(SourceFileSizeDistribution overallFileSizeDistribution) {
        this.overallFileSizeDistribution = overallFileSizeDistribution;
    }

    public void setOverallFileSizeDistribution(SourceFileSizeDistribution overallFileSizeDistribution) {
        this.overallFileSizeDistribution = overallFileSizeDistribution;
    }

    public List<RiskDistributionStats> getFileSizeDistributionPerExtension() {
        return fileSizeDistributionPerExtension;
    }

    public void setFileSizeDistributionPerExtension(List<RiskDistributionStats> fileSizeDistributionPerExtension) {
        this.fileSizeDistributionPerExtension = fileSizeDistributionPerExtension;
    }


    public List<FileDistributionPerLogicalDecomposition> getFileSizeDistributionPerLogicalDecomposition() {
        return fileSizeDistributionPerLogicalDecomposition;
    }

    public void setFileSizeDistributionPerLogicalDecomposition(List<FileDistributionPerLogicalDecomposition> fileSizeDistributionPerLogicalDecomposition) {
        this.fileSizeDistributionPerLogicalDecomposition = fileSizeDistributionPerLogicalDecomposition;
    }

    public RiskDistributionStats getOverallFileComplexityDistribution() {
        return overallFileComplexityDistribution;
    }

    public void setOverallFileComplexityDistribution(RiskDistributionStats overallFileComplexityDistribution) {
        this.overallFileComplexityDistribution = overallFileComplexityDistribution;
    }

    public List<RiskDistributionStats> getFileComplexityDistributionPerExtension() {
        return fileComplexityDistributionPerExtension;
    }

    public void setFileComplexityDistributionPerExtension(List<RiskDistributionStats> fileComplexityDistributionPerExtension) {
        this.fileComplexityDistributionPerExtension = fileComplexityDistributionPerExtension;
    }

    public List<FileComplexityDistributionPerLogicalDecomposition> getFileComplexityDistributionPerLogicalDecomposition() {
        return fileComplexityDistributionPerLogicalDecomposition;
    }

    public void setFileComplexityDistributionPerLogicalDecomposition(List<FileComplexityDistributionPerLogicalDecomposition> fileComplexityDistributionPerLogicalDecomposition) {
        this.fileComplexityDistributionPerLogicalDecomposition = fileComplexityDistributionPerLogicalDecomposition;
    }

    public List<SourceFile> getMostComplexFiles() {
        return mostComplexFiles;
    }

    public void setMostComplexFiles(List<SourceFile> mostComplexFiles) {
        this.mostComplexFiles = mostComplexFiles;
    }
}
