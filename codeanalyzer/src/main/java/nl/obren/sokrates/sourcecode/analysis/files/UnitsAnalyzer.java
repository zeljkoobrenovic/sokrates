package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
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
    private ProgressFeedback progressFeedback;
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final long start;
    private final UnitsAnalysisResults unitsAnalysisResults;
    private final SourceCodeAspect main;

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
        printRiskDistributionStats(unitSizeDistribution, "Unit size distribution: ");

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Unit size distribution per component:", start);
        UnitUtils.getUnitSizeDistributionPerComponent(codeConfiguration.getLogicalDecompositions(), allUnits).forEach(group -> {
            List<RiskDistributionStats> componentUnitSizeDistributionStats = new ArrayList<>();
            unitsAnalysisResults.getUnitSizeRiskDistributionPerComponent().add(componentUnitSizeDistributionStats);
            group.forEach(componentUnitSizeDistribution -> {
                componentUnitSizeDistributionStats.add(componentUnitSizeDistribution);
                printRiskDistributionStats(componentUnitSizeDistribution, "  - " + componentUnitSizeDistribution.getKey() + ": ");
            });
        });

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Unit size distribution per extension:", start);
        UnitUtils.getUnitSizeDistributionPerExtension(allUnits).forEach(extensionUnitSizeDistribution -> {
            unitsAnalysisResults.getUnitSizeRiskDistributionPerExtension().add(extensionUnitSizeDistribution);
            printRiskDistributionStats(extensionUnitSizeDistribution, "  - " + extensionUnitSizeDistribution.getKey() + ": ");
        });

        RiskDistributionStats cyclomaticComplexityDistribution = UnitUtils.getCyclomaticComplexityDistribution(allUnits);
        unitsAnalysisResults.setCyclomaticComplexityRiskDistribution(cyclomaticComplexityDistribution);
        printRiskDistributionStats(UnitUtils.getCyclomaticComplexityDistribution(allUnits), "Cyclomatic complexity distribution: ");
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Cyclomatic complexity distribution per component:", start);
        UnitUtils.getCyclomaticComplexityDistributionPerComponent(codeConfiguration.getLogicalDecompositions(), allUnits).forEach(group -> {
            List<RiskDistributionStats> componentCyclomaticComplexityDistributionStats = new ArrayList<>();
            unitsAnalysisResults.getCyclomaticComplexityRiskDistributionPerComponent().add(componentCyclomaticComplexityDistributionStats);
            group.forEach(componentCyclomaticComplexityDistribution -> {
                componentCyclomaticComplexityDistributionStats.add(componentCyclomaticComplexityDistribution);
                printRiskDistributionStats(componentCyclomaticComplexityDistribution, "  - " + componentCyclomaticComplexityDistribution.getKey() + ": ");
            });
        });
        AnalysisUtils.detailedInfo(textSummary, progressFeedback, "Cyclomatic complexity distribution per extension:", start);
        UnitUtils.getCyclomaticComplexityDistributionPerExtension(allUnits).forEach(extensionUnitSizeDistribution -> {
            unitsAnalysisResults.getCyclomaticComplexityRiskDistributionPerExtension().add(extensionUnitSizeDistribution);
            printRiskDistributionStats(extensionUnitSizeDistribution, "  - " + extensionUnitSizeDistribution.getKey() + ": ");
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

    private void printRiskDistributionStats(RiskDistributionStats riskDistributionStats, String prefix) {
        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("LOW_RISK_VALUE"))
                .description("Low risk value - " + riskDistributionStats.getKey())
                .scope(Metric.Scope.SYSTEM)
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getLowRiskValue());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("MEDIUM_RISK_VALUE"))
                .description("Medium risk value - " + riskDistributionStats.getKey())
                .scope(Metric.Scope.SYSTEM)
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getMediumRiskValue());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("HIGH_RISK_VALUE"))
                .description("High risk value - " + riskDistributionStats.getKey())
                .scope(Metric.Scope.SYSTEM)
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getHighRiskValue());

        metricsList.addMetric()
                .id(AnalysisUtils.getMetricId("VERY_HIGH_RISK_VALUE"))
                .description("Very high risk value - " + riskDistributionStats.getKey())
                .scope(Metric.Scope.SYSTEM)
                .scopeQualifier(prefix)
                .value(riskDistributionStats.getVeryHighRiskValue());

        AnalysisUtils.detailedInfo(textSummary, progressFeedback, prefix + riskDistributionStats.getLowRiskValue() + " / "
                + riskDistributionStats.getMediumRiskValue() + " / "
                + riskDistributionStats.getHighRiskValue() + " / "
                + riskDistributionStats.getVeryHighRiskValue(), start);
    }


}
