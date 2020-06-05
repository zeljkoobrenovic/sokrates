/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.SimpleCallback;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnitUtils {

    public static int getLinesOfCode(List<UnitInfo> units) {
        int linesOfCodeCount = 0;
        for (UnitInfo unit : units) {
            linesOfCodeCount += unit.getLinesOfCode();
        }
        return linesOfCodeCount;
    }

    public static RiskDistributionStats getUnitSizeRiskDistributionInstance() {
        RiskDistributionStats distributionStats = new RiskDistributionStats(10, 20, 50, 100);
        distributionStats.setNegligibleRiskLabel("1-10");
        distributionStats.setLowRiskLabel("10-20");
        distributionStats.setMediumRiskLabel("21-50");
        distributionStats.setHighRiskLabel("51-100");
        distributionStats.setVeryHighRiskLabel("101+");
        return distributionStats;
    }

    public static RiskDistributionStats getConditionalComplexityRiskDistributionInstance() {
        RiskDistributionStats distributionStats = new RiskDistributionStats(5, 10, 25, 50);
        distributionStats.setNegligibleRiskLabel("1-5");
        distributionStats.setLowRiskLabel("6-10");
        distributionStats.setMediumRiskLabel("11-25");
        distributionStats.setHighRiskLabel("26-50");
        distributionStats.setVeryHighRiskLabel("51+");
        return distributionStats;
    }

    public static RiskDistributionStats getUnitSizeDistribution(List<UnitInfo> units) {
        RiskDistributionStats distribution = getUnitSizeRiskDistributionInstance();
        units.forEach(unit -> distribution.update(unit.getLinesOfCode(), unit.getLinesOfCode()));
        return distribution;
    }

    public static RiskDistributionStats getConditionalComplexityDistribution(List<UnitInfo> units) {
        RiskDistributionStats distribution = getConditionalComplexityRiskDistributionInstance();
        units.forEach(unit -> distribution.update(unit.getMcCabeIndex(), unit.getLinesOfCode()));
        return distribution;
    }

    public static List<RiskDistributionStats> getUnitSizeDistributionPerExtension(List<UnitInfo> units) {
        return getAggregateUnitSizeRiskDistribution(units, unit -> unit.getSourceFile().getExtension());
    }

    public static List<RiskDistributionStats> getConditionalComplexityDistributionPerExtension(List<UnitInfo> units) {
        return getAggregateConditionalComplexityRiskDistribution(units, unit -> unit.getSourceFile().getExtension());
    }

    public static List<List<RiskDistributionStats>> getUnitSizeDistributionPerComponent(List<LogicalDecomposition> logicalDecompositions, List<UnitInfo> units) {
        List<List<RiskDistributionStats>> componentStats = new ArrayList<>();
        logicalDecompositions.forEach(logicalDecomposition -> {
            List<UnitInfo> unitsInScope = units.stream().filter(unit -> logicalDecomposition.isInScope(unit.getSourceFile())).collect(Collectors.toList());
            componentStats.add(getAggregateUnitSizeRiskDistribution(unitsInScope,
                    unit -> {
                        List<NamedSourceCodeAspect> logicalComponents = unit.getSourceFile().getLogicalComponents(logicalDecomposition.getName());
                        if (logicalComponents.size() > 0)
                            return logicalComponents.get(0).getName();
                        else {
                            return "";
                        }
                    }
            ));
        });
        return componentStats;
    }

    public static List<List<RiskDistributionStats>> getConditionalComplexityDistributionPerComponent(List<LogicalDecomposition> logicalDecompositions, List<UnitInfo> units) {
        List<List<RiskDistributionStats>> componentStats = new ArrayList<>();
        logicalDecompositions.forEach(logicalDecomposition -> {
            List<UnitInfo> unitsInScope = units.stream().filter(unit -> logicalDecomposition.isInScope(unit.getSourceFile())).collect(Collectors.toList());
            componentStats.add(getAggregateConditionalComplexityRiskDistribution(unitsInScope, unit -> unit.getSourceFile().getLogicalComponents(logicalDecomposition.getName()).get(0).getName()));
        });
        return componentStats;
    }

    public static List<RiskDistributionStats> getAggregateUnitSizeRiskDistribution(List<UnitInfo> units, SimpleCallback<UnitInfo, String> aggregationKeyCallback) {
        Map<String, RiskDistributionStats> distributionMap = new HashMap<>();
        List<RiskDistributionStats> distributionList = new ArrayList<>();
        units.forEach(unit -> {
            String key = aggregationKeyCallback.call(unit);
            RiskDistributionStats distributionStats = distributionMap.get(key);
            if (distributionStats == null) {
                distributionStats = getUnitSizeRiskDistributionInstance();
                distributionMap.put(key, distributionStats);
                distributionStats.setKey(key);
                distributionList.add(distributionStats);
            }
            distributionStats.update(unit.getLinesOfCode(), unit.getLinesOfCode());
        });

        return distributionList;
    }

    public static List<RiskDistributionStats> getAggregateConditionalComplexityRiskDistribution(List<UnitInfo> units, SimpleCallback<UnitInfo, String> aggregationKeyCallback) {
        Map<String, RiskDistributionStats> distributionMap = new HashMap<>();
        List<RiskDistributionStats> distributionList = new ArrayList<>();
        units.forEach(unit -> {
            String key = aggregationKeyCallback.call(unit);
            RiskDistributionStats distributionStats = distributionMap.get(key);
            if (distributionStats == null) {
                distributionStats = getConditionalComplexityRiskDistributionInstance();
                distributionMap.put(key, distributionStats);
                distributionStats.setKey(key);
                distributionList.add(distributionStats);
            }
            distributionStats.update(unit.getMcCabeIndex(), unit.getLinesOfCode());
        });

        return distributionList;
    }
}
