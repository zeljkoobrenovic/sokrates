/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.common.renderingutils.Threshold;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.SimpleCallback;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

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

    public static RiskDistributionStats getUnitSizeRiskDistributionInstance(Thresholds thresholds) {
        return thresholds.toRiskDistribution();
    }

    public static RiskDistributionStats getConditionalComplexityRiskDistributionInstance(Thresholds thresholds) {
        return thresholds.toRiskDistribution();
    }

    public static RiskDistributionStats getUnitSizeDistribution(List<UnitInfo> units, Thresholds thresholds) {
        RiskDistributionStats distribution = getUnitSizeRiskDistributionInstance(thresholds);
        units.forEach(unit -> distribution.update(unit.getLinesOfCode(), unit.getLinesOfCode()));
        return distribution;
    }

    public static RiskDistributionStats getConditionalComplexityDistribution(List<UnitInfo> units, Thresholds thresholds) {
        RiskDistributionStats distribution = getConditionalComplexityRiskDistributionInstance(thresholds);
        units.forEach(unit -> distribution.update(unit.getMcCabeIndex(), unit.getLinesOfCode()));
        return distribution;
    }

    public static List<RiskDistributionStats> getUnitSizeDistributionPerExtension(List<UnitInfo> units, Thresholds thresholds) {
        return getAggregateUnitSizeRiskDistribution(units, thresholds, unit -> unit.getSourceFile().getExtension());
    }

    public static List<RiskDistributionStats> getConditionalComplexityDistributionPerExtension(List<UnitInfo> units, Thresholds thresholds) {
        return getAggregateConditionalComplexityRiskDistribution(units, thresholds, unit -> unit.getSourceFile().getExtension());
    }

    public static List<List<RiskDistributionStats>> getUnitSizeDistributionPerComponent(List<LogicalDecomposition> logicalDecompositions, List<UnitInfo> units, Thresholds thresholds) {
        List<List<RiskDistributionStats>> componentStats = new ArrayList<>();
        logicalDecompositions.forEach(logicalDecomposition -> {
            List<UnitInfo> unitsInScope = units.stream().filter(unit -> logicalDecomposition.isInScope(unit.getSourceFile())).collect(Collectors.toList());
            componentStats.add(getAggregateUnitSizeRiskDistribution(unitsInScope,thresholds,
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

    public static List<List<RiskDistributionStats>> getConditionalComplexityDistributionPerComponent(List<LogicalDecomposition> logicalDecompositions, List<UnitInfo> units, Thresholds thresholds) {
        List<List<RiskDistributionStats>> componentStats = new ArrayList<>();
        logicalDecompositions.forEach(logicalDecomposition -> {
            List<UnitInfo> unitsInScope = units.stream().filter(unit -> logicalDecomposition.isInScope(unit.getSourceFile())).collect(Collectors.toList());
            componentStats.add(getAggregateConditionalComplexityRiskDistribution(unitsInScope, thresholds, unit -> unit.getSourceFile().getLogicalComponents(logicalDecomposition.getName()).get(0).getName()));
        });
        return componentStats;
    }

    public static List<RiskDistributionStats> getAggregateUnitSizeRiskDistribution(List<UnitInfo> units, Thresholds thresholds, SimpleCallback<UnitInfo, String> aggregationKeyCallback) {
        Map<String, RiskDistributionStats> distributionMap = new HashMap<>();
        List<RiskDistributionStats> distributionList = new ArrayList<>();
        units.forEach(unit -> {
            String key = aggregationKeyCallback.call(unit);
            RiskDistributionStats distributionStats = distributionMap.get(key);
            if (distributionStats == null) {
                distributionStats = getUnitSizeRiskDistributionInstance(thresholds);
                distributionMap.put(key, distributionStats);
                distributionStats.setKey(key);
                distributionList.add(distributionStats);
            }
            distributionStats.update(unit.getLinesOfCode(), unit.getLinesOfCode());
        });

        return distributionList;
    }

    public static List<RiskDistributionStats> getAggregateConditionalComplexityRiskDistribution(List<UnitInfo> units, Thresholds thresholds, SimpleCallback<UnitInfo, String> aggregationKeyCallback) {
        Map<String, RiskDistributionStats> distributionMap = new HashMap<>();
        List<RiskDistributionStats> distributionList = new ArrayList<>();
        units.forEach(unit -> {
            String key = aggregationKeyCallback.call(unit);
            RiskDistributionStats distributionStats = distributionMap.get(key);
            if (distributionStats == null) {
                distributionStats = getConditionalComplexityRiskDistributionInstance(thresholds);
                distributionMap.put(key, distributionStats);
                distributionStats.setKey(key);
                distributionList.add(distributionStats);
            }
            distributionStats.update(unit.getMcCabeIndex(), unit.getLinesOfCode());
        });

        return distributionList;
    }
}
