package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
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
        RiskDistributionStats distributionStats = new RiskDistributionStats(20, 50, 100);
        distributionStats.setLowRiskLabel("1-20");
        distributionStats.setMediumRiskLabel("21-50");
        distributionStats.setHighRiskLabel("51-100");
        distributionStats.setVeryHighRiskLabel("101+");
        return distributionStats;
    }

    public static RiskDistributionStats getCyclomaticComplexityRiskDistributionInstance() {
        RiskDistributionStats distributionStats = new RiskDistributionStats(5, 10, 25);
        distributionStats.setLowRiskLabel("1-5");
        distributionStats.setMediumRiskLabel("6-10");
        distributionStats.setHighRiskLabel("11-15");
        distributionStats.setVeryHighRiskLabel("26+");
        return distributionStats;
    }

    public static RiskDistributionStats getUnitSizeDistribution(List<UnitInfo> units) {
        RiskDistributionStats distribution = getUnitSizeRiskDistributionInstance();
        units.forEach(unit -> distribution.update(unit.getLinesOfCode(), unit.getLinesOfCode()));
        return distribution;
    }

    public static RiskDistributionStats getCyclomaticComplexityDistribution(List<UnitInfo> units) {
        RiskDistributionStats distribution = getCyclomaticComplexityRiskDistributionInstance();
        units.forEach(unit -> distribution.update(unit.getMcCabeIndex(), unit.getLinesOfCode()));
        return distribution;
    }

    public static List<RiskDistributionStats> getUnitSizeDistributionPerExtension(List<UnitInfo> units) {
        return getAggregateUnitSizeRiskDistribution(units, unit -> unit.getSourceFile().getExtension());
    }

    public static List<RiskDistributionStats> getCyclomaticComplexityDistributionPerExtension(List<UnitInfo> units) {
        return getAggregateCyclomaticComplexityRiskDistribution(units, unit -> unit.getSourceFile().getExtension());
    }

    public static List<List<RiskDistributionStats>> getUnitSizeDistributionPerComponent(List<LogicalDecomposition> logicalDecompositions, List<UnitInfo> units) {
        List<List<RiskDistributionStats>> componentStats = new ArrayList<>();
        logicalDecompositions.forEach(logicalDecomposition -> {
            List<UnitInfo> unitsInScope = units.stream().filter(unit -> logicalDecomposition.isInScope(unit.getSourceFile())).collect(Collectors.toList());
            componentStats.add(getAggregateUnitSizeRiskDistribution(unitsInScope,
                    unit -> unit.getSourceFile().getLogicalComponents(logicalDecomposition.getName()).get(0).getName()));
        });
        return componentStats;
    }

    public static List<List<RiskDistributionStats>> getCyclomaticComplexityDistributionPerComponent(List<LogicalDecomposition> logicalDecompositions, List<UnitInfo> units) {
        List<List<RiskDistributionStats>> componentStats = new ArrayList<>();
        logicalDecompositions.forEach(logicalDecomposition -> {
            List<UnitInfo> unitsInScope = units.stream().filter(unit -> logicalDecomposition.isInScope(unit.getSourceFile())).collect(Collectors.toList());
            componentStats.add(getAggregateCyclomaticComplexityRiskDistribution(unitsInScope, unit -> unit.getSourceFile().getLogicalComponents(logicalDecomposition.getName()).get(0).getName()));
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

    public static List<RiskDistributionStats> getAggregateCyclomaticComplexityRiskDistribution(List<UnitInfo> units, SimpleCallback<UnitInfo, String> aggregationKeyCallback) {
        Map<String, RiskDistributionStats> distributionMap = new HashMap<>();
        List<RiskDistributionStats> distributionList = new ArrayList<>();
        units.forEach(unit -> {
            String key = aggregationKeyCallback.call(unit);
            RiskDistributionStats distributionStats = distributionMap.get(key);
            if (distributionStats == null) {
                distributionStats = getCyclomaticComplexityRiskDistributionInstance();
                distributionMap.put(key, distributionStats);
                distributionStats.setKey(key);
                distributionList.add(distributionStats);
            }
            distributionStats.update(unit.getMcCabeIndex(), unit.getLinesOfCode());
        });

        return distributionList;
    }
}
