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

public class UnitsSizeReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("101+", "51-100", "21-50", "11-20", "1-10");

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
        report.addListItem("You should aim at keeping units small (< 20 lines). Long units may become \"bloaters\", code that have increased to such gargantuan proportions that they are hard to work with.");
        report.endUnorderedList();

        report.startShowMoreBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("To learn more about unit size, the Sokrates recommends the following resources:");
        report.startUnorderedList();
        report.addListItem("<a href='https://martinfowler.com/bliki/FunctionLength.html'>Function Length</a>, MartinFowler.com");
        report.startUnorderedList();
        report.addListItem("<i>\"If you have to spend effort into looking at a fragment of code to figure out what it's doing, then you should extract it into a function and name the function after that 'what'.\"</i>");
        report.endUnorderedList();
        report.addListItem("<a href='https://softwareengineering.stackexchange.com/questions/133404/what-is-the-ideal-length-of-a-method-for-you'>Stack Overflow Discussion on Unit Size</a>, stackoverflow.com");
        report.startUnorderedList();
        report.addListItem("<i>\"Use common sense, stick to small function sizes in most instances but don't be dogmatic about it if you have a genuinely good reason to make an unusually big function.\"</i>");
        report.endUnorderedList();
        report.addListItem("<a href='https://blog.codinghorror.com/code-smells/'>Coding Smells</a>, codinghorror.com");
        report.startUnorderedList();
        report.addListItem("<i>\"All other things being equal, a shorter method is easier to read, easier to understand, and easier to troubleshoot. Refactor long methods into smaller methods if you can.\"</i>");
        report.endUnorderedList();
        report.addListItem("<a href='https://www.amazon.com/dp/0132350882/'>Clean Code: A Handbook of Agile Software Craftsmanship</a>, by Robert Martin");
        report.startUnorderedList();
        report.addListItem("<i>\"The first rule of functions is that they should be small. The second rule of functions is that they should be smaller than that. Functions should not be 100 lines long. Functions should hardly ever be 20 lines long.\"</i>");
        report.endUnorderedList();

        report.endUnorderedList();
        report.endUnorderedList();
        report.endShowMoreBlock();
        report.endSection();

        report.startSection("Unit Size Overall", "");
        report.startUnorderedList();
        int linesOfCodeInUnits = unitsAnalysisResults.getLinesOfCodeInUnits();
        report.addListItem("There are <a href='../data/text/units.txt'>" + RichTextRenderingUtils.renderNumberStrong(unitsAnalysisResults.getTotalNumberOfUnits())
                + " units</a> with " + RichTextRenderingUtils.renderNumberStrong(linesOfCodeInUnits)
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
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getNegligibleRiskCount())
                + " very small units (" + RichTextRenderingUtils.renderNumberStrong(unitSizeDistribution.getNegligibleRiskValue())
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
            report.startScrollingDiv();
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(stats, labels)
                    .toString());
            report.endDiv();
            report.endSection();
        });
        report.endSection();

        report.startSection("Alternative Visuals", "");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='visuals/units_3d_size.html'>3D view of all units</a>");
        report.endUnorderedList();
        report.endSection();

        List<UnitInfo> longestUnits = unitsAnalysisResults.getLongestUnits();
        report.startSection("Longest Units", "Top " + longestUnits.size() + " longest units");
        boolean cacheFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.startScrollingDiv();
        report.addHtmlContent(UtilsReportUtils.getUnitsTable(longestUnits, "longest_unit", cacheFiles).toString());
        report.endDiv();
        report.endSection();
    }

    private String getLogicalDecompositionName(int index) {
        return codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().get(index).getName();
    }
}
