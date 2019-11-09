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

public class CyclomaticComplexityReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("26+", "11-25", "6-10", "1-5");

    public CyclomaticComplexityReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addCyclomaticComplexityToReport(RichTextReport report) {
        UnitsAnalysisResults unitsAnalysisResults = codeAnalysisResults.getUnitsAnalysisResults();
        RiskDistributionStats unitMcCabeDistribution = unitsAnalysisResults.getCyclomaticComplexityRiskDistribution();

        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("Cyclomatic complexity is a software metric (measurement), used to indicate the complexity of a program. It is a quantitative measure of the number of linearly " +
                "independent paths through a program's source code.");
        report.addListItem("Cyclomatic complexity is measured at the unit level (methods, functions...).");
        report.addListItem("Units are classified in four categories based on the measured McCabe index: " +
                "1-5 (simple units), 6-10 (medium complex units), 11-25 (complex units), 26+ (very complex units).");
        report.endUnorderedList();
        report.addHorizontalLine();
        report.endSection();

        report.startSection("Cyclomatic Complexity Overall", "");
        report.startUnorderedList();
        int linesOfCodeInUnits = unitsAnalysisResults.getLinesOfCodeInUnits();
        report.addListItem("There are " + RichTextRenderingUtils.renderNumberStrong(unitsAnalysisResults.getTotalNumberOfUnits())
                + " units with " + RichTextRenderingUtils.renderNumberStrong(linesOfCodeInUnits)
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
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(unitMcCabeDistribution, labels));
        report.endSection();

        report.startSection("Cyclomatic Complexity per Extension", "");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(unitsAnalysisResults
                .getCyclomaticComplexityRiskDistributionPerExtension(), labels).toString());
        report.endSection();

        report.startSection("Cyclomatic Complexity per Logical Component", "");
        List<List<RiskDistributionStats>> cyclomaticComplexityRiskDistributionPerComponent = unitsAnalysisResults.getCyclomaticComplexityRiskDistributionPerComponent();
        cyclomaticComplexityRiskDistributionPerComponent.forEach(stats -> {
            report.startSubSection(getLogicalDecompositionName(cyclomaticComplexityRiskDistributionPerComponent.indexOf(stats)) + " logical decomposition", "");
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(stats, labels).toString());
            report.endSection();
        });
        report.endSection();


        report.startSection("Alternative Visuals", "");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='visuals/units_3d.html'>3D view of all units</a>");
        report.endUnorderedList();
        report.endSection();

        List<UnitInfo> mostComplexUnits = unitsAnalysisResults.getMostComplexUnits();
        report.startSection("Most Complex Units", "Top " + mostComplexUnits.size() + " most complex units");
        report.addHtmlContent(UtilsReportUtils.getUnitsTable(mostComplexUnits, "most_complex_unit").toString());
    }

    private String getLogicalDecompositionName(int index) {
        return codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().get(index).getName();
    }
}
