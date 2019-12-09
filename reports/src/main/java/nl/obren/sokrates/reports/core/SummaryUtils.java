package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SummaryUtils {
    private static final int BAR_WIDTH = 480;
    private static final int BAR_HEIGHT = 12;

    public void summarize(CodeAnalysisResults analysisResults, RichTextReport report) {
        summarizeMainVolume(analysisResults, report);
        summarizeDuplication(analysisResults, report);
        summarizeFileSize(report, analysisResults);
        summarizeUnitSize(analysisResults, report);
        summarizeUnitComplexity(analysisResults, report);
        summarizeComponents(analysisResults, report);
        addSummaryFindings(analysisResults, report);
    }

    private void summarizeFileSize(RichTextReport report, CodeAnalysisResults analysisResults) {
        FilesAnalysisResults filesAnalysisResults = analysisResults.getFilesAnalysisResults();
        if (filesAnalysisResults != null) {
            SourceFileSizeDistribution distribution = filesAnalysisResults.getOveralFileSizeDistribution();
            if (distribution != null) {
                int mainLOC = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
                int veryLongFilesLOC = distribution.getVeryHighRiskValue();
                int shortFilesLOC = distribution.getLowRiskValue();

                report.addParagraph("File Size: <b style='" + (veryLongFilesLOC > 1 ? "color: crimson" : "") + "'>"
                        + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(mainLOC, veryLongFilesLOC))
                        + "%</b> very long (>1000 LOC), <b style='color: green'>"
                        + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(mainLOC, shortFilesLOC))
                        + "%</b> short (<= 200 LOC)", "margin-bottom: 0; margin-top: 6px");
                report.addContentInDiv(getRiskProfileVisual(distribution));
            }
        }
    }

    private void summarizeListOfLocAspects(StringBuilder summary, int totalLoc, int mainLoc, List<NumericMetric> linesOfCodePerAspect) {
        if (linesOfCodePerAspect.size() > 0) {
            summary.append("<span style='color: grey'> = ");
        }
        final boolean[] first = {true};
        linesOfCodePerAspect.forEach(ext -> {
            if (!first[0]) {
                summary.append(" + ");
            } else {
                first[0] = false;
            }
            String language = ext.getName().toUpperCase().replace("*.", "");
            int loc = ext.getValue().intValue();
            double percentage = totalLoc > 0 ? 100.0 * loc / totalLoc : 0;
            String formattedPercentage = FormattingUtils.getFormattedPercentage(percentage) + "%";

            summary.append("<b>" + language + "</b> <span style='color:lightgrey'>(" + formattedPercentage + ")</span>");
        });
        if (linesOfCodePerAspect.size() > 0) {
            summary.append("</span>");
            summary.append("<div>" + getVolumeVisual(linesOfCodePerAspect, totalLoc, mainLoc, "") + "</div>");
        }
    }

    public void summarizeAndCompare(CodeAnalysisResults analysisResults, CodeAnalysisResults refData, RichTextReport report) {
        StringBuilder summary = new StringBuilder("");
        summarizeMainCode(analysisResults, summary);

        summary.append(addDiffDiv(analysisResults.getMainAspectAnalysisResults().getLinesOfCode(),
                refData.getMainAspectAnalysisResults().getLinesOfCode()));
        summary.append("<div style='margin-left: 24px;font-size:90%;margin-bottom:46px'>");
        summarizeMainCode(refData, summary);
        summary.append("</div>");

        summarizeTestCode(analysisResults, summary);

        summary.append(addDiffDiv(analysisResults.getTestAspectAnalysisResults().getLinesOfCode(),
                refData.getTestAspectAnalysisResults().getLinesOfCode()));
        summary.append("<div style='margin-left: 24px;font-size:90%;margin-bottom:46px'>");
        summarizeTestCode(refData, summary);
        summary.append("</div>");

        report.addParagraph(summary.toString());

        report.startDiv("color:black");
        summarizeDuplication(analysisResults, report);
        report.endDiv();

        report.addParagraph(addDiffDiv(analysisResults.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage().doubleValue(),
                refData.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage().doubleValue()));
        report.startDiv("margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeDuplication(refData, report);
        report.endDiv();

        report.startDiv("color:black");
        summarizeFileSize(report, analysisResults);
        report.endDiv();

        report.startDiv("margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeFileSize(report, refData);
        report.endDiv();

        report.startDiv("color:black");
        summarizeUnitSize(analysisResults, report);
        report.endDiv();

        report.startDiv("margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeUnitSize(refData, report);
        report.endDiv();

        report.startDiv("color:black");
        summarizeUnitComplexity(analysisResults, report);
        report.endDiv();

        report.startDiv("margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeUnitComplexity(refData, report);
        report.endDiv();

        report.startDiv("color:black");
        summarizeComponents(analysisResults, report);
        report.endDiv();

        report.startDiv("margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeComponents(refData, report);
        report.endDiv();

        report.startDiv("color:black");
        addSummaryFindings(analysisResults, report);
        report.endDiv();

        report.startDiv("margin-left: 24px;font-size:90%;margin-bottom:46px");
        addSummaryFindings(refData, report);
        report.endDiv();
    }

    private void summarizeMainVolume(CodeAnalysisResults analysisResults, RichTextReport report) {
        StringBuilder summary = new StringBuilder("");
        summarizeMainCode(analysisResults, summary);
        report.addParagraph(summary.toString());
    }

    private void summarizeTestVolume(CodeAnalysisResults analysisResults, RichTextReport report) {
        StringBuilder summary = new StringBuilder("");
        summarizeTestCode(analysisResults, summary);
        report.addParagraph(summary.toString());
    }

    private void summarizeTestCode(CodeAnalysisResults analysisResults, StringBuilder summary) {
        summary.append("<p style='margin-bottom: 0; margin-top: 36px'>Test Code: ");
        int mainLoc = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        int totalLoc = analysisResults.getTestAspectAnalysisResults().getLinesOfCode();
        summary.append(RichTextRenderingUtils.renderNumberStrong(totalLoc) + " LOC");
        List<NumericMetric> linesOfCodePerExtension = analysisResults.getTestAspectAnalysisResults().getLinesOfCodePerExtension();
        summarizeListOfLocAspects(summary, totalLoc, mainLoc, linesOfCodePerExtension);
        summary.append("</p>");
    }

    private void summarizeMainCode(CodeAnalysisResults analysisResults, StringBuilder summary) {
        summary.append("<p style='margin-bottom: 0'>Main Code: ");
        int totalLoc = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        summary.append(RichTextRenderingUtils.renderNumberStrong(totalLoc) + " LOC");
        summarizeListOfLocAspects(summary, totalLoc, totalLoc, analysisResults.getMainAspectAnalysisResults().getLinesOfCodePerExtension());

        summary.append("</p>");
    }

    private void addSummaryFindings(CodeAnalysisResults analysisResults, RichTextReport report) {
        List<String> summaryFindings = analysisResults.getCodeConfiguration().getSummary();
        if (summaryFindings != null && summaryFindings.size() > 0) {
            report.addParagraph("Other findings:");
            report.startUnorderedList();
            summaryFindings.forEach(summaryFinding -> {
                report.addListItem(summaryFinding);
            });
            report.endUnorderedList();
        }
    }

    private void summarizeComponents(CodeAnalysisResults analysisResults, RichTextReport report) {
        report.addHtmlContent("<p style='margin-bottom: 0; margin-top: 30px'>Logical Component Decomposition:");
        boolean first[] = {true};
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(decomposition -> {
            if (!first[0]) {
                report.addHtmlContent(", ");
            } else {
                first[0] = false;
            }
            report.addHtmlContent(decomposition.getKey() + " (" + decomposition.getComponents().size() + " components)");
        });
        report.addHtmlContent("</p>");

        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(decomposition -> {
            int mainLoc = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
            int totalLoc[] = {0};
            List<NumericMetric> linesOfCodePerComponent = decomposition.getLinesOfCodePerComponent();
            linesOfCodePerComponent.forEach(c -> totalLoc[0] += c.getValue().intValue());

            report.addContentInDiv(getVolumeVisual(linesOfCodePerComponent, totalLoc[0], mainLoc, decomposition.getKey() + " (" + decomposition.getComponents().size() + " components)"));
        });

    }

    private void summarizeUnitComplexity(CodeAnalysisResults analysisResults, RichTextReport report) {
        int linesOfCodeInUnits = analysisResults.getUnitsAnalysisResults().getLinesOfCodeInUnits();
        RiskDistributionStats distribution = analysisResults.getUnitsAnalysisResults().getConditionalComplexityRiskDistribution();
        int veryComplexUnitsLOC = distribution.getVeryHighRiskValue();
        int lowComplexUnitsLOC = distribution.getLowRiskValue();

        report.addParagraph("Conditional Complexity: <b style='" + (veryComplexUnitsLOC > 1 ? "color: crimson" : "") + "'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, veryComplexUnitsLOC))
                + "%</b> very complex (McCabe index > 25), <b style='color: green'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, lowComplexUnitsLOC))
                + "%</b> simple (McCabe index <= 5)", "margin-bottom: 0; margin-top: 6px");

        report.addContentInDiv(getRiskProfileVisual(distribution));

    }

    private void summarizeUnitSize(CodeAnalysisResults analysisResults, RichTextReport report) {
        int linesOfCodeInUnits = analysisResults.getUnitsAnalysisResults().getLinesOfCodeInUnits();
        RiskDistributionStats distribution = analysisResults.getUnitsAnalysisResults().getUnitSizeRiskDistribution();
        int veryLongUnitsLOC = distribution.getVeryHighRiskValue();
        int lowUnitsLOC = distribution.getLowRiskValue();

        report.addParagraph("Unit Size: <b style='" + (veryLongUnitsLOC > 1 ? "color: crimson" : "") + "'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, veryLongUnitsLOC))
                + "%</b> very long (>100 LOC), <b style='color: green'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, lowUnitsLOC))
                + "%</b> short (<= 20 LOC)", "margin-bottom: 0; margin-top: 6px");
        report.addContentInDiv(getRiskProfileVisual(distribution));
    }

    private void summarizeDuplication(CodeAnalysisResults analysisResults, RichTextReport report) {
        DuplicationMetric overallDuplication = analysisResults.getDuplicationAnalysisResults().getOverallDuplication();
        Number duplicationPercentage = overallDuplication.getDuplicationPercentage();
        double duplication = duplicationPercentage.doubleValue();
        if (!analysisResults.getCodeConfiguration().getAnalysis().isSkipDuplication()) {
            report.addParagraph("Duplication: <b style='" + (duplication > 5.0 ? "color: crimson" : "") + "'>" + FormattingUtils.getFormattedPercentage(duplication) + "%</b>", "margin-bottom: 0; margin-top: 24px");

            report.addContentInDiv(getDuplicationVisual(duplicationPercentage));
        }
    }

    private String getVolumeVisual(List<NumericMetric> linesOfCodePerExtension, int totalLoc, int mainLoc, String text) {

        int barWidth = (int) ((double) BAR_WIDTH * totalLoc / mainLoc);
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(barWidth + 200);
        chart.setBarHeight(BAR_HEIGHT);
        chart.setMaxBarWidth(barWidth);
        chart.setBarStartXOffset(0);
        chart.setFontSize("small");


        List<Integer> values = linesOfCodePerExtension.stream().map(metric -> metric.getValue().intValue()).collect(Collectors.toList());
        Collections.sort(values);
        Collections.reverse(values);
        return chart.getStackedBarSvg(values, Palette.getDefaultPalette(), "", text);
    }

    private String getRiskProfileVisual(RiskDistributionStats distributionStats) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(BAR_WIDTH + 20);
        chart.setBarHeight(BAR_HEIGHT);
        chart.setMaxBarWidth(BAR_WIDTH);
        chart.setBarStartXOffset(0);

        List<Integer> values = Arrays.asList(distributionStats.getVeryHighRiskValue(),
                distributionStats.getHighRiskValue(), distributionStats.getMediumRiskValue(), distributionStats.getLowRiskValue());

        return chart.getStackedBarSvg(values, Palette.getRiskPalette(), "", "");
    }

    private String getDuplicationVisual(Number duplicationPercentage) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(BAR_WIDTH + 20);
        chart.setBarHeight(BAR_HEIGHT);
        chart.setMaxBarWidth(BAR_WIDTH);
        chart.setBarStartXOffset(2);
        chart.setAlignment(SimpleOneBarChart.Alignment.LEFT);
        chart.setActiveColor("crimson");
        chart.setBackgroundColor("#9DC034");
        return chart.getPercentageSvg(duplicationPercentage.doubleValue(), "", "");
    }

    private String addDiffDiv(double value, double refValue) {
        double diff = value - refValue;
        String diffText = getDiffText(diff, refValue);
        StringBuilder html = new StringBuilder("<div style='margin-left: 24px; margin-top: 0; text-align: left; color: " +
                (diff == 0 ? "lightgrey" : (diff < 0 ? "#b9936c" : "#6b5b95")) + "'>");
        if (diff > 0) {
            html.append("+" + diffText + " ⬆");
        } else if (diff < 0) {
            html.append("" + diffText + " ⬇ ");
        } else {
            html.append("" + diffText + "");
        }

        html.append("</div>");

        return html.toString();
    }

    private String getDiffText(double diff, double refValue) {
        String diffText;
        if (Math.abs(refValue) < 0.0000000000000000000001) {
            diffText = "";
        } else {
            double percentage = 100.0 * diff / refValue;
            diffText = diff + " (" + (percentage > 0 ? "+" : (percentage < 0 ? "-" : ""))
                    + FormattingUtils.getFormattedPercentage(Math.abs(percentage)) + "%)";
        }
        return diffText;
    }

}
