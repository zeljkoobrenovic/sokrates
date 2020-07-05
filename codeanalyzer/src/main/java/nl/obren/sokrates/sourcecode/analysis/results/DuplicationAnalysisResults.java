/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;

import java.util.ArrayList;
import java.util.List;

public class DuplicationAnalysisResults {
    private DuplicationMetric overallDuplication = new DuplicationMetric("system");

    private List<List<DuplicationMetric>> duplicationPerComponent = new ArrayList<>();
    private List<DuplicationMetric> duplicationPerConcern = new ArrayList<>();
    private List<DuplicationMetric> duplicationPerExtension = new ArrayList<>();

    @JsonIgnore
    private List<DuplicationInstance> allDuplicates = new ArrayList<>();
    private List<DuplicationInstance> longestDuplicates = new ArrayList<>();
    private List<DuplicationInstance> mostFrequentDuplicates = new ArrayList<>();

    public DuplicationMetric getOverallDuplication() {
        return overallDuplication;
    }

    public void setOverallDuplication(DuplicationMetric overallDuplication) {
        this.overallDuplication = overallDuplication;
    }

    public List<List<DuplicationMetric>> getDuplicationPerComponent() {
        return duplicationPerComponent;
    }

    public void setDuplicationPerComponent(List<List<DuplicationMetric>> duplicationPerComponent) {
        this.duplicationPerComponent = duplicationPerComponent;
    }

    public List<DuplicationMetric> getDuplicationPerConcern() {
        return duplicationPerConcern;
    }

    public void setDuplicationPerConcern(List<DuplicationMetric> duplicationPerConcern) {
        this.duplicationPerConcern = duplicationPerConcern;
    }

    public List<DuplicationMetric> getDuplicationPerExtension() {
        return duplicationPerExtension;
    }

    public void setDuplicationPerExtension(List<DuplicationMetric> duplicationPerExtension) {
        this.duplicationPerExtension = duplicationPerExtension;
    }

    public List<DuplicationInstance> getLongestDuplicates() {
        return longestDuplicates;
    }

    public void setLongestDuplicates(List<DuplicationInstance> longestDuplicates) {
        this.longestDuplicates = longestDuplicates;
    }

    public List<DuplicationInstance> getMostFrequentDuplicates() {
        return mostFrequentDuplicates;
    }

    public void setMostFrequentDuplicates(List<DuplicationInstance> mostFrequentDuplicates) {
        this.mostFrequentDuplicates = mostFrequentDuplicates;
    }

    @JsonIgnore
    public List<DuplicationInstance> getAllDuplicates() {
        return allDuplicates;
    }

    @JsonIgnore
    public void setAllDuplicates(List<DuplicationInstance> allDuplicates) {
        this.allDuplicates = allDuplicates;
    }
}
