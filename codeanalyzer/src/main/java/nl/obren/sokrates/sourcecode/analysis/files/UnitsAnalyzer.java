/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import nl.obren.sokrates.sourcecode.units.UnitUtils;
import nl.obren.sokrates.sourcecode.units.UnitsExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static nl.obren.sokrates.sourcecode.analysis.AnalysisUtils.getMetricId;

public class UnitsAnalyzer extends Analyzer {
    private final StringBuffer textSummary;
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final long start;
    private final UnitsAnalysisResults unitsAnalysisResults;
    private final NamedSourceCodeAspect main;
    private final FilesAnalysisResults filesAnalysisResults;
    private ProgressFeedback progressFeedback;

    private UnitCategoryNames unitSizeCategoryNames = new UnitCategoryNames("1_10", "11_20", "21_50", "51_100", "101_PLUS");

    private UnitCategoryNames conditionalComplexityCategoryNames = new UnitCategoryNames("1_5", "6_10", "10_25", "26_50", "51_PLUS");
    private List<UnitInfo> allUnits;

    public UnitsAnalyzer(CodeAnalysisResults analysisResults, ProgressFeedback progressFeedback) {
        this.filesAnalysisResults = analysisResults.getFilesAnalysisResults();
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
        AnalysisUtils.info(textSummary, progressFeedback, "Analysing units...", start);
        this.allUnits = new UnitsExtractor().getUnits(main.getSourceFiles(), progressFeedback);

        int linesOfCode = UnitUtils.getLinesOfCode(allUnits);
        addBasicUnitMetrics(allUnits, linesOfCode);

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "  - found " + allUnits.size() + " units (" + linesOfCode + " lines of code in units)", start);

        unitsAnalysisResults.setTotalNumberOfUnits(allUnits.size());
        unitsAnalysisResults.setLinesOfCodeInUnits(linesOfCode);

        Thresholds unitSizeThresholds = codeConfiguration.getAnalysis().getUnitSizeThresholds();
        Thresholds conditionalComplexityThresholds = codeConfiguration.getAnalysis().getConditionalComplexityThresholds();

        RiskDistributionStats unitSizeDistribution = UnitUtils.getUnitSizeDistribution(allUnits, unitSizeThresholds);
        unitsAnalysisResults.setUnitSizeRiskDistribution(unitSizeDistribution);
        printRiskDistributionStats(unitSizeDistribution, "Unit size ");

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Unit size per component:", start);
        UnitUtils.getUnitSizeDistributionPerComponent(codeConfiguration.getLogicalDecompositions(), allUnits, unitSizeThresholds).forEach(group -> {
            List<RiskDistributionStats> componentUnitSizeDistributionStats = new ArrayList<>();
            unitsAnalysisResults.getUnitSizeRiskDistributionPerComponent().add(componentUnitSizeDistributionStats);
            group.forEach(componentUnitSizeDistribution -> {
                componentUnitSizeDistributionStats.add(componentUnitSizeDistribution);
                printRiskDistributionStats(componentUnitSizeDistribution, "Unit Size Component " + componentUnitSizeDistribution.getKey() + ": ");
            });
        });

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Unit size per extension:", start);
        UnitUtils.getUnitSizeDistributionPerExtension(allUnits, unitSizeThresholds).forEach(extensionUnitSizeDistribution -> {
            unitsAnalysisResults.getUnitSizeRiskDistributionPerExtension().add(extensionUnitSizeDistribution);
            printRiskDistributionStats(extensionUnitSizeDistribution, "Unit Size Extension " + extensionUnitSizeDistribution.getKey() + ": ");
        });

        RiskDistributionStats conditionalComplexityDistributionAllUnits = UnitUtils.getConditionalComplexityDistribution(allUnits, conditionalComplexityThresholds);
        RiskDistributionStats conditionalComplexityDistribution = conditionalComplexityDistributionAllUnits;
        unitsAnalysisResults.setConditionalComplexityRiskDistribution(conditionalComplexityDistribution);
        printRiskDistributionStats(conditionalComplexityDistributionAllUnits, "Conditional complexity: ");

        metricsList.addMetric()
                .id(safeId(getMetricId("CONDITIONAL_COMPLEXITY_HIGH_PLUS_RISK_COUNT")))
                .value(conditionalComplexityDistributionAllUnits.getHighRiskCount() + conditionalComplexityDistributionAllUnits.getVeryHighRiskCount());

        metricsList.addMetric()
                .id(safeId(getMetricId("CONDITIONAL_COMPLEXITY_HIGH_PLUS_RISK_LOC")))
                .value(conditionalComplexityDistributionAllUnits.getHighRiskValue() + conditionalComplexityDistributionAllUnits.getVeryHighRiskValue());


        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Conditional complexity per component:", start);
        UnitUtils.getConditionalComplexityDistributionPerComponent(codeConfiguration.getLogicalDecompositions(), allUnits, conditionalComplexityThresholds).forEach(group -> {
            List<RiskDistributionStats> componentConditionalComplexityDistributionStats = new ArrayList<>();
            unitsAnalysisResults.getConditionalComplexityRiskDistributionPerComponent().add(componentConditionalComplexityDistributionStats);
            group.forEach(componentConditionalComplexityDistribution -> {
                componentConditionalComplexityDistributionStats.add(componentConditionalComplexityDistribution);
                printRiskDistributionStats(componentConditionalComplexityDistribution, "Conditional Complexity Component " + componentConditionalComplexityDistribution.getKey() + " ");
            });
        });
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Conditional complexity per extension:", start);
        UnitUtils.getConditionalComplexityDistributionPerExtension(allUnits, conditionalComplexityThresholds).forEach(extensionUnitSizeDistribution -> {
            unitsAnalysisResults.getConditionalComplexityRiskDistributionPerExtension().add(extensionUnitSizeDistribution);
            printRiskDistributionStats(extensionUnitSizeDistribution, "Conditional Complexity Component " + extensionUnitSizeDistribution.getKey() + ": ");
        });

        int sampleSize = codeConfiguration.getAnalysis().getMaxTopListSize();
        addAllUnits(allUnits, unitsAnalysisResults);
        addLongestUnits(allUnits, unitsAnalysisResults, sampleSize);
        addMostComplexUnits(allUnits, unitsAnalysisResults, sampleSize);
    }

    private void addBasicUnitMetrics(List<UnitInfo> allUnits, int linesOfCode) {
        metricsList.addMetric()
                .id(getMetricId("NUMBER_OF_UNITS"))
                .description("Number of units")
                .value(allUnits.size());

        metricsList.addMetric()
                .id(getMetricId("LINES_OF_CODE_IN_UNITS"))
                .description("Lines of code in units")
                .value(linesOfCode);

        metricsList.addMetric()
                .id(getMetricId("LINES_OF_CODE_OUTSIDE_UNITS"))
                .description("Lines of code in units")
                .value(main.getLinesOfCode() - linesOfCode);

        updateFilesWithUnitInfo();
    }

    private void updateFilesWithUnitInfo() {
        filesAnalysisResults.getAllFiles().forEach(sourceFile -> {
            final int[] unitsLoc = {0};
            final int[] unitsCount = {0};
            final int[] mcCabeIndexSum = {0};
            allUnits.stream().filter(u -> u.getSourceFile().getFile() == sourceFile.getFile()).forEach(unitInfo -> {
                unitsCount[0] += 1;
                mcCabeIndexSum[0] += unitInfo.getMcCabeIndex();
                unitsLoc[0] += unitInfo.getLinesOfCode();
            });
            sourceFile.setUnitsCount(unitsCount[0]);
            sourceFile.setUnitsMcCabeIndexSum(mcCabeIndexSum[0]);
            sourceFile.setLinesOfCodeInUnits(unitsLoc[0]);
        });
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

    private void printRiskDistributionStats(RiskDistributionStats riskDistributionStats, String prefix) {
        String namePrefix = SystemUtils.getFileSystemFriendlyName(prefix.toUpperCase().replace(":", "")).toUpperCase();

        addNegligibleRiskMetrics(riskDistributionStats, namePrefix);
        addLowRiskMetrics(riskDistributionStats, namePrefix);
        addMediumRiskMetrics(riskDistributionStats, namePrefix);
        addHighRiskMetrics(riskDistributionStats, namePrefix);
        addVeryHighRiskMetrics(riskDistributionStats, namePrefix);

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, prefix
                + riskDistributionStats.getNegligibleRiskValue() + " / "
                + riskDistributionStats.getLowRiskValue() + " / "
                + riskDistributionStats.getMediumRiskValue() + " / "
                + riskDistributionStats.getHighRiskValue() + " / "
                + riskDistributionStats.getVeryHighRiskValue(), start);
    }

    private void addRiskMetrics(String riskCategory, String namePrefix, int value, double percentage, int count) {
        String prefix = (namePrefix + "_" + riskCategory + "_RISK_").toUpperCase();

        metricsList.addMetric().id(safeId(getMetricId(prefix + "LOC"))).value(value);
        metricsList.addMetric().id(safeId(getMetricId(prefix + "PERCENTAGE"))).value(percentage);
        metricsList.addMetric().id(safeId(getMetricId(prefix + "COUNT"))).value(count);
    }

    private void addVeryHighRiskMetrics(RiskDistributionStats stats, String namePrefix) {
        addRiskMetrics("VERY_HIGH", namePrefix,
                stats.getVeryHighRiskValue(), stats.getVeryHighRiskPercentage(), stats.getVeryHighRiskCount());
    }

    private void addHighRiskMetrics(RiskDistributionStats stats, String namePrefix) {
        addRiskMetrics("HIGH", namePrefix,
                stats.getHighRiskValue(), stats.getHighRiskPercentage(), stats.getHighRiskCount());
    }

    private void addMediumRiskMetrics(RiskDistributionStats stats, String namePrefix) {
        addRiskMetrics("MEDIUM", namePrefix,
                stats.getMediumRiskValue(), stats.getMediumRiskPercentage(), stats.getMediumRiskCount());
    }

    private void addLowRiskMetrics(RiskDistributionStats stats, String namePrefix) {
        addRiskMetrics("LOW", namePrefix,
                stats.getLowRiskValue(), stats.getLowRiskPercentage(), stats.getLowRiskCount());
    }

    private void addNegligibleRiskMetrics(RiskDistributionStats stats, String namePrefix) {
        addRiskMetrics("NEGLIGIBLE", namePrefix,
                stats.getNegligibleRiskValue(), stats.getNegligibleRiskPercentage(), stats.getNegligibleRiskCount());
    }

    private String safeId(String id) {
        return SystemUtils.getFileSystemFriendlyName(id).toUpperCase();
    }



}
