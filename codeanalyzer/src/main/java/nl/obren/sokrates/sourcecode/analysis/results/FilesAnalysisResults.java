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
    private SourceFileSizeDistribution overalFileSizeDistribution;
    private List<RiskDistributionStats> fileSizeDistributionPerExtension = new ArrayList<>();
    private List<FileDistributionPerLogicalDecomposition> fileSizeDistributionPerLogicalDecomposition = new ArrayList<>();

    @JsonIgnore
    private List<SourceFile> allFiles = new ArrayList<>();
    private List<SourceFile> longestFiles = new ArrayList<>();

    @JsonIgnore
    public List<SourceFile> getAllFiles() {
        return allFiles;
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

    public SourceFileSizeDistribution getOveralFileSizeDistribution() {
        return overalFileSizeDistribution;
    }

    public void setOveralFileSizeDistribution(SourceFileSizeDistribution overalFileSizeDistribution) {
        this.overalFileSizeDistribution = overalFileSizeDistribution;
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
}
