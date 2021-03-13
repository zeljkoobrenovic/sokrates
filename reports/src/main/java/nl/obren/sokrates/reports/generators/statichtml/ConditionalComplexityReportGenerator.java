/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.PieChartUtils;
import nl.obren.sokrates.reports.utils.RiskDistributionStatsReportUtils;
import nl.obren.sokrates.reports.utils.UtilsReportUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.Arrays;
import java.util.List;

public class ConditionalComplexityReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("51+", "26-50", "11-25", "6-10", "1-5");

    public ConditionalComplexityReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addConditionalComplexityToReport(RichTextReport report) {
        boolean cacheFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        UnitsAnalysisResults unitsAnalysisResults = codeAnalysisResults.getUnitsAnalysisResults();
        RiskDistributionStats unitMcCabeDistribution = unitsAnalysisResults.getConditionalComplexityRiskDistribution();

        report.startSection("Intro", "");
        report.startUnorderedList();
        // "\"Conditional complexity\" (also called cyclomatic complexity) is a term used to measure the complexity of software. The term refers to the number of possible paths through a program function; a higher value means higher maintenance and testing costs."
        report.addListItem("Conditional complexity (also called cyclomatic complexity) is a term used to measure the complexity of software. The term refers to the number of possible paths through a program function. A higher value ofter means higher maintenance and testing costs (<a href='https://resources.infosecinstitute.com/conditional-complexity-of-risk-models/#gref'>infosecinstitute.com</a>).");
        report.addListItem("Conditional complexity is calculated by counting all conditions in the program that can affect the execution path (e.g. if statement, loops, switches, and/or operators, try and catch blocks...).");
        report.addListItem("Conditional complexity is measured at the unit level (methods, functions...).");
        report.addListItem("Units are classified in four categories based on the measured McCabe index: " +
                "1-5 (simple units), 6-10 (medium complex units), 11-25 (complex units), 26+ (very complex units).");
        report.endUnorderedList();
        report.startShowMoreBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("To learn more about conditional complexity and techniques for reducing this type of complexity, Sokrates recommends the following resources:");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/refactoring/simplifying-conditional-expressions'>Simplifying Conditional Expressions</a>, sourcemaking.com");
        report.addListItem("<a target='_blank' href='https://en.wikipedia.org/wiki/Cyclomatic_complexity'>Cyclomatic Complexity</a>, wikipedia.org");

        report.endUnorderedList();
        report.endUnorderedList();

        report.endShowMoreBlock();

        report.endSection();

        // "https://www.wrightfully.com/thoughts-on-cyclomatic-complexity/"

        report.startSection("Conditional Complexity Overall", "");
        report.startUnorderedList();
        int linesOfCodeInUnits = unitsAnalysisResults.getLinesOfCodeInUnits();
        report.addListItem("There are <a href='../data/text/units.txt'>" + RichTextRenderingUtils.renderNumberStrong(unitsAnalysisResults.getTotalNumberOfUnits())
                + " units</a> with " + RichTextRenderingUtils.renderNumberStrong(linesOfCodeInUnits)
                + " lines of code in units (" + (RichTextRenderingUtils.renderNumber(100.0 * linesOfCodeInUnits / codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode())) + "% of code)" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getVeryHighRiskCount())
                + " very complex units (" + RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getHighRiskCount())
                + " complex units (" + RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getMediumRiskCount())
                + " medium complex units (" + RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getLowRiskCount())
                + " simple units (" + RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getNegligibleRiskCount())
                + " very simple units (" + RichTextRenderingUtils.renderNumberStrong(unitMcCabeDistribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(unitMcCabeDistribution, labels));
        report.endSection();

        report.startSection("Alternative Visuals", "");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='visuals/units_3d_complexity.html'>3D view of all units</a>");
        report.endUnorderedList();
        report.endSection();

        report.startSection("Conditional Complexity per Extension", "");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(unitsAnalysisResults
                .getConditionalComplexityRiskDistributionPerExtension(), labels).toString());
        report.endSection();

        report.startSection("Conditional Complexity per Logical Component", "");
        List<List<RiskDistributionStats>> conditionalComplexityRiskDistributionPerComponent = unitsAnalysisResults.getConditionalComplexityRiskDistributionPerComponent();
        conditionalComplexityRiskDistributionPerComponent.forEach(stats -> {
            report.startSubSection(getLogicalDecompositionName(conditionalComplexityRiskDistributionPerComponent.indexOf(stats)) + " logical decomposition", "");
            report.startScrollingDiv();
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(stats, labels).toString());
            report.endDiv();
            report.endSection();
        });
        report.endSection();

        List<UnitInfo> mostComplexUnits = unitsAnalysisResults.getMostComplexUnits();
        report.startSection("Most Complex Units", "Top " + mostComplexUnits.size() + " most complex units");
        report.startScrollingDiv();
        report.addHtmlContent(UtilsReportUtils.getUnitsTable(mostComplexUnits, "most_complex_unit", cacheFiles).toString());
        report.endDiv();
        report.endSection();
    }

    private String getLogicalDecompositionName(int index) {
        return codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().get(index).getName();
    }
}
