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

public class UnitsSizeReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("101+", "51-100", "21-50", "1-20");

    public UnitsSizeReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addUnitsSizeToReport(RichTextReport report) {
        UnitsAnalysisResults unitsAnalysisResults = codeAnalysisResults.getUnitsAnalysisResults();
        RiskDistributionStats unitSizeDistribution = unitsAnalysisResults.getUnitSizeRiskDistribution();

        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("Unit size measurements show the distribution of size of units of code (methods, functions...).");
        report.addListItem("Units are classified in four categories based on their size (lines of code): " +
                "1-20 (small units), 20-50 (medium size units), 51-100 (long units), 101+ (very long units).");
        report.endUnorderedList();
        report.endSection();

        report.startSection("Unit Size Overall", "");
        report.startUnorderedList();
        int linesOfCodeInUnits = unitsAnalysisResults.getLinesOfCodeInUnits();
        report.addListItem("There are " + RichTextRenderingUtils.renderNumberStrong(unitsAnalysisResults.getTotalNumberOfUnits())
                + " units with " + RichTextRenderingUtils.renderNumberStrong(linesOfCodeInUnits)
                + " lines of code in units (" + (RichTextRenderingUtils.renderNumber(100.0 * linesOfCodeInUnits / codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode())) + "% of code)" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getVeryHighRiskCount())
                + " very long units (" + RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getHighRiskCount())
                + " long units (" + RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getMediumRiskCount())
                + " medium size units (" + RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getLowRiskCount())
                + " small units (" + RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getLowRiskValue())
                + " lines of code)");

        report.endUnorderedList();
        report.endUnorderedList();

        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(unitSizeDistribution, labels));
        report.endSection();

        report.startSection("Unit Size per Extension", "");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(unitsAnalysisResults.getUnitSizeRiskDistributionPerExtension(), labels).toString());
        report.endSection();

        report.startSection("Unit Size per Logical Component", "");
        List<List<RiskDistributionStats>> unitSizeRiskDistributionPerComponent = unitsAnalysisResults.getUnitSizeRiskDistributionPerComponent();
        unitSizeRiskDistributionPerComponent.forEach(stats -> {
            report.startSubSection(getLogicalDecompositionName(unitSizeRiskDistributionPerComponent.indexOf(stats)) + " logical decomposition", "");
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(stats, labels)
                    .toString());
            report.endSection();
        });
        report.endSection();

        report.startSection("Alternative Visuals", "");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='visuals/units_3d.html'>3D view of all units</a>");
        report.endUnorderedList();
        report.endSection();

        List<UnitInfo> longestUnits = unitsAnalysisResults.getLongestUnits();
        report.startSection("Longest Units", "Top " + longestUnits.size() + " longest units");
        report.addHtmlContent(UtilsReportUtils.getUnitsTable(longestUnits, "longest_unit").toString());
        report.endSection();
    }

    private String getLogicalDecompositionName(int index) {
        return codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().get(index).getName();
    }
}
