/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public class UnitsAnalysisResults {
    private int totalNumberOfUnits;
    private int linesOfCodeInUnits;

    private RiskDistributionStats unitSizeRiskDistribution = new RiskDistributionStats("system");
    private RiskDistributionStats conditionalComplexityRiskDistribution = new RiskDistributionStats("system");

    private List<RiskDistributionStats> unitSizeRiskDistributionPerExtension = new ArrayList<>();
    private List<List<RiskDistributionStats>> unitSizeRiskDistributionPerComponent = new ArrayList<>();
    private List<UnitInfo> longestUnits = new ArrayList<>();

    private List<RiskDistributionStats> conditionalComplexityRiskDistributionPerExtension = new ArrayList<>();
    private List<List<RiskDistributionStats>> conditionalComplexityRiskDistributionPerComponent = new ArrayList<>();
    private List<UnitInfo> mostComplexUnits = new ArrayList<>();

    @JsonIgnore
    private List<UnitInfo> allUnits = new ArrayList<>();

    public int getTotalNumberOfUnits() {
        return totalNumberOfUnits;
    }

    public void setTotalNumberOfUnits(int totalNumberOfUnits) {
        this.totalNumberOfUnits = totalNumberOfUnits;
    }

    public int getLinesOfCodeInUnits() {
        return linesOfCodeInUnits;
    }

    public void setLinesOfCodeInUnits(int linesOfCodeInUnits) {
        this.linesOfCodeInUnits = linesOfCodeInUnits;
    }

    public RiskDistributionStats getUnitSizeRiskDistribution() {
        return unitSizeRiskDistribution;
    }

    public void setUnitSizeRiskDistribution(RiskDistributionStats unitSizeRiskDistribution) {
        this.unitSizeRiskDistribution = unitSizeRiskDistribution;
    }

    public RiskDistributionStats getConditionalComplexityRiskDistribution() {
        return conditionalComplexityRiskDistribution;
    }

    public void setConditionalComplexityRiskDistribution(RiskDistributionStats conditionalComplexityRiskDistribution) {
        this.conditionalComplexityRiskDistribution = conditionalComplexityRiskDistribution;
    }

    public List<RiskDistributionStats> getUnitSizeRiskDistributionPerExtension() {
        return unitSizeRiskDistributionPerExtension;
    }

    public void setUnitSizeRiskDistributionPerExtension(List<RiskDistributionStats> unitSizeRiskDistributionPerExtension) {
        this.unitSizeRiskDistributionPerExtension = unitSizeRiskDistributionPerExtension;
    }

    public List<List<RiskDistributionStats>> getUnitSizeRiskDistributionPerComponent() {
        return unitSizeRiskDistributionPerComponent;
    }

    public void setUnitSizeRiskDistributionPerComponent(List<List<RiskDistributionStats>> unitSizeRiskDistributionPerComponent) {
        this.unitSizeRiskDistributionPerComponent = unitSizeRiskDistributionPerComponent;
    }

    public List<RiskDistributionStats> getConditionalComplexityRiskDistributionPerExtension() {
        return conditionalComplexityRiskDistributionPerExtension;
    }

    public void setConditionalComplexityRiskDistributionPerExtension(List<RiskDistributionStats> conditionalComplexityRiskDistributionPerExtension) {
        this.conditionalComplexityRiskDistributionPerExtension = conditionalComplexityRiskDistributionPerExtension;
    }

    public List<List<RiskDistributionStats>> getConditionalComplexityRiskDistributionPerComponent() {
        return conditionalComplexityRiskDistributionPerComponent;
    }

    public void setConditionalComplexityRiskDistributionPerComponent(List<List<RiskDistributionStats>> conditionalComplexityRiskDistributionPerComponent) {
        this.conditionalComplexityRiskDistributionPerComponent = conditionalComplexityRiskDistributionPerComponent;
    }

    public List<UnitInfo> getLongestUnits() {
        return longestUnits;
    }

    public void setLongestUnits(List<UnitInfo> longestUnits) {
        this.longestUnits = longestUnits;
    }

    public List<UnitInfo> getMostComplexUnits() {
        return mostComplexUnits;
    }

    public void setMostComplexUnits(List<UnitInfo> mostComplexUnits) {
        this.mostComplexUnits = mostComplexUnits;
    }

    @JsonIgnore
    public List<UnitInfo> getAllUnits() {
        return allUnits;
    }

    @JsonIgnore
    public void setAllUnits(List<UnitInfo> allUnits) {
        this.allUnits = allUnits;
    }
}
