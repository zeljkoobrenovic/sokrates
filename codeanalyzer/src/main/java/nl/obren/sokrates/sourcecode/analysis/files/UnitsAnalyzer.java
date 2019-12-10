/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import nl.obren.sokrates.sourcecode.units.UnitUtils;
import nl.obren.sokrates.sourcecode.units.UnitsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnitsAnalyzer extends Analyzer {
    private final StringBuffer textSummary;
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final long start;
    private final UnitsAnalysisResults unitsAnalysisResults;
    private final NamedSourceCodeAspect main;
    private ProgressFeedback progressFeedback;

    private UnitCategoryNames unitSizeCategoryNames = new UnitCategoryNames("1_20", "21_50", "51_100", "101_PLUS");

    private UnitCategoryNames conditionalComplexityCategoryNames = new UnitCategoryNames("1_5", "6_10", "10_25", "26_PLUS");

    public UnitsAnalyzer(CodeAnalysisResults analysisResults, ProgressFeedback progressFeedback) {
        this.unitsAnalysisResults = analysisResults.getUnitsAnalysisResults();
        this.codeConfiguration = analysisResults.getCodeConfiguration();
        this.metricsList = analysisResults.getMetricsList();
        this.start = analysisResults.getAnalysisStartTimeMs();
        this.textSummary = analysisResults.getTextSummary();
        this.progressFeedback = progressFeedback;
        this.main = codeConfiguration.getMain();
    }

    public void analyze() {
        progressFeedback.start();
        AnalysisUtils.info(textSummary, progressFeedback, "Extracting units...", start);
        List<UnitInfo> allUnits = new UnitsExtractor().getUnits(main.getSourceFiles(), progressFeedback);

        int linesOfCode = UnitUtils.getLinesOfCode(allUnits);
        addBasicUnitMetrics(allUnits, linesOfCode);

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - found " + allUnits.size() + " units (" + linesOfCode + " lines of code in units)", start);

        unitsAnalysisResults.setTotalNumberOfUnits(allUnits.size());
        unitsAnalysisResults.setLinesOfCodeInUnits(linesOfCode);

        RiskDistributionStats unitSizeDistribution = UnitUtils.getUnitSizeDistribution(allUnits);
        unitsAnalysisResults.setUnitSizeRiskDistribution(unitSizeDistribution);
        printRiskDistributionStats(unitSizeDistribution, unitSizeCategoryNames, "Unit size distribution ");

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Unit size distribution per component:", start);
        UnitUtils.getUnitSizeDistributionPerComponent(codeConfiguration.getLogicalDecompositions(), allUnits).forEach(group -> {
            List<RiskDistributionStats> componentUnitSizeDistributionStats = new ArrayList<>();
            unitsAnalysisResults.getUnitSizeRiskDistributionPerComponent().add(componentUnitSizeDistributionStats);
            group.forEach(componentUnitSizeDistribution -> {
                componentUnitSizeDistributionStats.add(componentUnitSizeDistribution);
                printRiskDistributionStats(componentUnitSizeDistribution, unitSizeCategoryNames, "Unit Size Component " + componentUnitSizeDistribution.getKey() + ": ");
            });
        });

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Unit size distribution per extension:", start);
        UnitUtils.getUnitSizeDistributionPerExtension(allUnits).forEach(extensionUnitSizeDistribution -> {
            unitsAnalysisResults.getUnitSizeRiskDistributionPerExtension().add(extensionUnitSizeDistribution);
            printRiskDistributionStats(extensionUnitSizeDistribution, unitSizeCategoryNames, "Unit Size Extension " + extensionUnitSizeDistribution.getKey() + ": ");
        });

        RiskDistributionStats conditionalComplexityDistribution = UnitUtils.getConditionalComplexityDistribution(allUnits);
        unitsAnalysisResults.setConditionalComplexityRiskDistribution(conditionalComplexityDistribution);
        printRiskDistributionStats(UnitUtils.getConditionalComplexityDistribution(allUnits), conditionalComplexityCategoryNames, "Conditional complexity distribution: ");
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Conditional complexity distribution per component:", start);
        UnitUtils.getConditionalComplexityDistributionPerComponent(codeConfiguration.getLogicalDecompositions(), allUnits).forEach(group -> {
            List<RiskDistributionStats> componentConditionalComplexityDistributionStats = new ArrayList<>();
            unitsAnalysisResults.getConditionalComplexityRiskDistributionPerComponent().add(componentConditionalComplexityDistributionStats);
            group.forEach(componentConditionalComplexityDistribution -> {
                componentConditionalComplexityDistributionStats.add(componentConditionalComplexityDistribution);
                printRiskDistributionStats(componentConditionalComplexityDistribution, conditionalComplexityCategoryNames, "Conditional Complexity Component " + componentConditionalComplexityDistribution.getKey() + " ");
            });
        });
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Conditional complexity distribution per extension:", start);
        UnitUtils.getConditionalComplexityDistributionPerExtension(allUnits).forEach(extensionUnitSizeDistribution -> {
            unitsAnalysisResults.getConditionalComplexityRiskDistributionPerExtension().add(extensionUnitSizeDistribution);
            printRiskDistributionStats(extensionUnitSizeDistribution, conditionalComplexityCategoryNames, "Conditional Complexity Component " + extensionUnitSizeDistribution.getKey() + ": ");
        });

        int sampleSize = 50;
        addAllUnits(allUnits, unitsAnalysisResults);
        addLongestUnits(allUnits, unitsAnalysisResults, sampleSize);
        addMostComplexUnits(allUnits, unitsAnalysisResults, sampleSize);
    }

    private void addBasicUnitMetrics(List<UnitInfo> allUnits, int linesOfCode) {
        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("NUMBER_OF_UNITS"))
                .description("Number of units")
                .scope(Metric.Scope.SYSTEM)
                .value(allUnits.size());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("LINES_OF_CODE_IN_UNITS"))
                .description("Lines of code in units")
                .scope(Metric.Scope.SYSTEM)
                .value(linesOfCode);

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("LINES_OF_CODE_OUTSIDE_UNITS"))
                .description("Lines of code in units")
                .scope(Metric.Scope.SYSTEM)
                .value(main.getLinesOfCode() - linesOfCode);
    }

    private void addAllUnits(List<UnitInfo> allUnits, UnitsAnalysisResults unitsAnalysisResults) {
        List<UnitInfo> units = new ArrayList<>(allUnits);
        Collections.sort(units, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        units.forEach(unitInfo -> {
            unitsAnalysisResults.getAllUnits().add(unitInfo);
        });
    }

    private void addLongestUnits(List<UnitInfo> allUnits, UnitsAnalysisResults unitsAnalysisResults, int sampleSize) {
        List<UnitInfo> units = new ArrayList<>(allUnits);
        Collections.sort(units, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        int index[] = {0};
        units.forEach(unitInfo -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            unitsAnalysisResults.getLongestUnits().add(unitInfo);
        });
    }

    private void addMostComplexUnits(List<UnitInfo> allUnits, UnitsAnalysisResults unitsAnalysisResults, int sampleSize) {
        Collections.sort(allUnits, (o1, o2) -> Integer.compare(o2.getMcCabeIndex(), o1.getMcCabeIndex()));
        int index[] = {0};
        allUnits.forEach(unitInfo -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            unitsAnalysisResults.getMostComplexUnits().add(unitInfo);
        });
    }

    private void printRiskDistributionStats(RiskDistributionStats riskDistributionStats, UnitCategoryNames categoryNames, String prefix) {
        String namePrefix = SystemUtils.getFileSystemFriendlyName(prefix.toUpperCase().replace(":", "") + "_").toUpperCase();

        addLowRiskMetrics(riskDistributionStats, categoryNames, prefix, namePrefix);
        addMediumRIskMetrics(riskDistributionStats, categoryNames, prefix, namePrefix);
        addHighRiskMetrics(riskDistributionStats, categoryNames, prefix, namePrefix);
        addVeryHighRiskMetrics(riskDistributionStats, categoryNames, prefix, namePrefix);

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, prefix + riskDistributionStats.getLowRiskValue() + " / "
                + riskDistributionStats.getMediumRiskValue() + " / "
                + riskDistributionStats.getHighRiskValue() + " / "
                + riskDistributionStats.getVeryHighRiskValue(), start);
    }

    private void addVeryHighRiskMetrics(RiskDistributionStats riskDistributionStats, UnitCategoryNames categoryNames, String prefix, String namePrefix) {
        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getVeryHighRisk() + "_LOC")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getVeryHighRiskValue());

        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getVeryHighRisk() + "_PERCENTAGE")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getVeryHighRiskPercentage());

        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getVeryHighRisk() + "_COUNT")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getVeryHighRiskCount());
    }

    private void addHighRiskMetrics(RiskDistributionStats riskDistributionStats, UnitCategoryNames categoryNames, String prefix, String namePrefix) {
        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getHighRisk() + "_LOC")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getHighRiskValue());

        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getHighRisk() + "_PERCENTAGE")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getHighRiskPercentage());

        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getHighRisk() + "_COUNT")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getHighRiskCount());
    }

    private void addMediumRIskMetrics(RiskDistributionStats riskDistributionStats, UnitCategoryNames categoryNames, String prefix, String namePrefix) {
        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getMediumRisk() + "_LOC")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getMediumRiskValue());

        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getMediumRisk() + "_PERCENTAGE")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getMediumRiskPercentage());

        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getMediumRisk() + "_COUNT")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getMediumRiskCount());
    }

    private String safeId(String id) {
        return SystemUtils.getFileSystemFriendlyName(id).toUpperCase();
    }

    private void addLowRiskMetrics(RiskDistributionStats riskDistributionStats, UnitCategoryNames categoryNames, String prefix, String namePrefix) {
        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getLowRisk() + "_LOC")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getLowRiskValue());

        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getLowRisk() + "_PERCENTAGE")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getLowRiskPercentage());

        metricsList.addMetric()
                .id(safeId(AnalysisUtils.getMetricId(namePrefix + categoryNames.getLowRisk() + "_COUNT")))
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getLowRiskCount());
    }


}
