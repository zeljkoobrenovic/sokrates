/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.reports.utils.ReportUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import nl.obren.sokrates.sourcecode.metrics.MetricsWithGoal;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SummaryUtils {
    private static final int BAR_WIDTH = 260;
    private static final int BAR_HEIGHT = 20;
    private String reportRoot = "";

    public String getReportRoot() {
        return reportRoot;
    }

    public void setReportRoot(String reportRoot) {
        this.reportRoot = reportRoot;
    }

    public void summarize(CodeAnalysisResults analysisResults, RichTextReport report) {
        CodeConfiguration config = analysisResults.getCodeConfiguration();

        boolean mainExists = analysisResults.getMainAspectAnalysisResults().getFilesCount() > 0;
        boolean showDuplication = mainExists && !config.getAnalysis().isSkipDuplication();
        boolean showControls = mainExists && config.getGoalsAndControls().size() > 0;
        boolean showUnits = mainExists && analysisResults.getUnitsAnalysisResults().getTotalNumberOfUnits() > 0;

        report.startDiv("width: 100%; overflow-x: auto");
        report.startTable("border: none; min-width: 800px; ");
        summarizeMainVolume(analysisResults, report);
        if (mainExists) {
            if (showDuplication) {
                summarizeDuplication(analysisResults, report);
            }
            summarizeFileSize(report, analysisResults);
            if (showUnits) {
                summarizeUnitSize(analysisResults, report);
                summarizeUnitComplexity(analysisResults, report);
            }
            summarizeComponents(analysisResults, report);
        }
        if (analysisResults.getFilesHistoryAnalysisResults().getHistory().size() > 0) {
            summarizeFileChangeHistory(analysisResults, report);
        }
        if (showControls) {
            summarizeGoals(analysisResults, report);
        }
        addSummaryFindings(analysisResults, report);
        report.endTable();
        report.endDiv();
    }

    private void summarizeFileSize(RichTextReport report, CodeAnalysisResults analysisResults) {
        FilesAnalysisResults filesAnalysisResults = analysisResults.getFilesAnalysisResults();
        if (filesAnalysisResults != null) {
            SourceFileSizeDistribution distribution = filesAnalysisResults.getOveralFileSizeDistribution();
            if (distribution != null) {
                int mainLOC = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
                int veryLongFilesLOC = distribution.getVeryHighRiskValue();
                int shortFilesLOC = distribution.getLowRiskValue() + distribution.getNegligibleRiskValue();

                report.startTableRow();
                report.addTableCell(getIconSvg("file_size"), "border: none");
                report.addTableCell(getRiskProfileVisual(distribution), "border: none");
                report.addTableCell("File Size: <b>"
                        + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(mainLOC, veryLongFilesLOC))
                        + "%</b> long (>1000 LOC), <b>"
                        + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(mainLOC, shortFilesLOC))
                        + "%</b> short (<= 200 LOC)", "border: none; vertical-align: top; padding-top: 11px;");
                report.addTableCell("<a href='" + reportRoot + "FileSize.html'  title='file size details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "border: none");
                report.endTableRow();
            }
        }
    }

    private String getIconSvg(String icon, int w, int h) {
        String svg = HtmlTemplateUtils.getResource("/icons/" + icon + ".svg");
        svg = svg.replaceAll("height='.*?'", "height='" + w + "px'");
        svg = svg.replaceAll("width='.*?'", "width='" + h + "px'");
        return svg;
    }

    private String getIconSvg(String icon) {
        return getIconSvg(icon, 40, 40);
    }

    public void summarizeListOfLocAspects(StringBuilder summary, int totalLoc, List<NumericMetric> linesOfCodePerAspect) {
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
        }
    }

    public void summarizeAndCompare(CodeAnalysisResults analysisResults, CodeAnalysisResults refData, RichTextReport report) {
        StringBuilder summary = new StringBuilder("");
        summarizeMainCode(analysisResults, summary);

        summary.append(addDiffDiv(analysisResults.getMainAspectAnalysisResults().getLinesOfCode(),
                refData.getMainAspectAnalysisResults().getLinesOfCode()));
        summary.append("<div style='margin-top: 24px;font-size:80%;margin-bottom:46px;opacity: 0.5;'>");
        summarizeMainCode(refData, summary);
        summary.append("</div>");

        report.addParagraph(summary.toString());

        report.addHorizontalLine();
        report.addLineBreak();

        report.startDiv("color:black");
        summarizeDuplication(analysisResults, report);
        report.endDiv();

        report.addParagraph(addDiffDiv(analysisResults.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage().doubleValue(),
                refData.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage().doubleValue()));
        report.startDiv("margin-top: 24px;font-size:80%;margin-bottom:46px;opacity: 0.5;");
        summarizeDuplication(refData, report);
        report.endDiv();
        report.addHorizontalLine();
        report.addLineBreak();

        report.startDiv("color:black");
        summarizeFileSize(report, analysisResults);
        report.endDiv();

        report.startDiv("margin-top: 24px;font-size:80%;margin-bottom:46px;opacity: 0.5;;");
        summarizeFileSize(report, refData);
        report.endDiv();
        report.addHorizontalLine();
        report.addLineBreak();

        report.startDiv("color:black");
        summarizeUnitSize(analysisResults, report);
        report.endDiv();

        report.startDiv("margin-top: 24px;font-size:80%;margin-bottom:46px;opacity: 0.5;");
        summarizeUnitSize(refData, report);
        report.endDiv();
        report.addHorizontalLine();
        report.addLineBreak();

        report.startDiv("color:black");
        summarizeUnitComplexity(analysisResults, report);
        report.endDiv();

        report.startDiv("margin-top: 24px;font-size:80%;margin-bottom:46px;opacity: 0.5;");
        summarizeUnitComplexity(refData, report);
        report.endDiv();
        report.addHorizontalLine();
        report.addLineBreak();

        // components
        report.startDiv("color:black");
        summarizeComponents(analysisResults, report);
        report.endDiv();

        report.startDiv("margin-top: 24px;font-size:80%;margin-bottom:46px;opacity: 0.5;");
        summarizeComponents(refData, report);
        report.endDiv();
        report.addHorizontalLine();
        report.addLineBreak();

        // goals
        report.startDiv("color: black");
        summarizeGoals(analysisResults, report);
        report.endDiv();

        report.startDiv("margin-top: 24px;font-size:80%;margin-bottom:46px;opacity: 0.5;");
        summarizeGoals(refData, report);
        report.endDiv();
        report.addHorizontalLine();
        report.addLineBreak();
    }

    private void summarizeMainVolume(CodeAnalysisResults analysisResults, RichTextReport report) {
        report.startTableRow();
        StringBuilder summary = new StringBuilder("");
        summarizeMainCode(analysisResults, summary);
        report.addHtmlContent(summary.toString());
        report.addTableCell("<a href='" + reportRoot + "SourceCodeOverview.html'  title='volume details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "border: none");
        report.endTableRow();
    }

    private String getDetailsIcon() {
        return getIconSvg("details", 22, 22);
    }

    private void summarizeMainCode(CodeAnalysisResults analysisResults, StringBuilder summary) {
        summary.append("<td style='border: none'>" + getIconSvg("codebase") + "</td>");
        summary.append("<td style='border: none'>");
        int totalLoc = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        List<NumericMetric> linesOfCodePerExtension = analysisResults.getMainAspectAnalysisResults().getLinesOfCodePerExtension();
        summary.append("<div>" + getVolumeVisual(linesOfCodePerExtension, totalLoc, totalLoc, "") + "</div>");
        summary.append("</td>");
        summary.append("<td style='border: none; vertical-align: top; padding-top: 11px;'>Main Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(totalLoc) + " LOC");
        summarizeListOfLocAspects(summary, totalLoc, linesOfCodePerExtension);
        summary.append("</td>");
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
        report.startTableRow();
        report.addTableCell(getIconSvg("dependencies"), "border: none");

        report.startTableCell("border: none");
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(decomposition -> {
            int mainLoc = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
            int totalLoc[] = {0};
            List<NumericMetric> linesOfCodePerComponent = decomposition.getLinesOfCodePerComponent();
            linesOfCodePerComponent.forEach(c -> totalLoc[0] += c.getValue().intValue());

            report.addContentInDiv(getVolumeVisual(linesOfCodePerComponent, totalLoc[0], mainLoc, ""));
        });
        report.endTableCell();

        report.startTableCell("border: none; vertical-align: top; padding-top: 11px;");
        report.addHtmlContent("Logical Component Decomposition:");
        boolean first[] = {true};
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(decomposition -> {
            if (!first[0]) {
                report.addHtmlContent(", ");
            } else {
                first[0] = false;
            }
            report.addHtmlContent(decomposition.getKey() + " (" + decomposition.getComponents().size() + " components)");
        });
        report.endTableCell();
        report.addTableCell("<a href='" + reportRoot + "Components.html'  title='logical decomposition details'>" + getDetailsIcon() + "</a>",
                "border: none;");

        report.endTableRow();
    }

    private void summarizeFileChangeHistory(CodeAnalysisResults analysisResults, RichTextReport report) {
        report.startTableRow();
        report.addTableCell(getIconSvg("file_history"), "border: none;  vertical-align: top");

        FilesHistoryAnalysisResults results = analysisResults.getFilesHistoryAnalysisResults();
        report.startTableCell("border: none; padding-top: 4px; vertical-align: top");
        SourceFileAgeDistribution age = results.getOverallFileFirstModifiedDistribution();
        report.addContentInDiv(getRiskProfileVisual(age, Palette.getAgePalette()));
        SourceFileAgeDistribution changes = results.getOverallFileLastModifiedDistribution();
        report.addContentInDiv(getRiskProfileVisual(changes, Palette.getFreshnessPalette()));
        report.endTableCell();

        report.startTableCell("border: none; padding-top: 4px;");
        String ageSummary = FormattingUtils.formatPeriod(results.getAgeInDays()) + " old";
        report.addParagraph(ageSummary);
        report.startUnorderedList();
        report.addListItem("File Age Distribution: "
                + FormattingUtils.getFormattedPercentage(age.getVeryHighRiskPercentage())
                + "% older than a year, "
                + FormattingUtils.getFormattedPercentage(age.getNegligibleRiskPercentage()) + "% less than a month");
        report.addListItem("File Changes Distribution: "
                + FormattingUtils.getFormattedPercentage(changes.getVeryHighRiskPercentage())
                + "% more than a year ago, "
                + FormattingUtils.getFormattedPercentage(changes.getNegligibleRiskPercentage()) + "% past month");
        report.endUnorderedList();
        report.endTableCell();
        report.addTableCell("<a href='" + reportRoot + "FileAge.html'  title='file change history details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "border: none;  vertical-align: top");

        report.endTableRow();
    }

    private String getControlColor(String status) {
        String upperCaseStatus = status.toUpperCase();
        return upperCaseStatus.equals("OK")
                ? "darkgreen"
                : upperCaseStatus.equals("FAILED")
                ? "crimson"
                : (upperCaseStatus.startsWith("IGNORE") ? "grey" : "orange");
    }


    private void summarizeGoals(CodeAnalysisResults analysisResults, RichTextReport report) {
        report.startTableRow();
        report.addTableCell(getIconSvg("goal"), "border: none");

        report.startTableCell("border: none");
        analysisResults.getControlResults().getGoalsAnalysisResults().forEach(goalsAnalysisResults -> {
            goalsAnalysisResults.getControlStatuses().forEach(controlStatus -> {
                report.addHtmlContent(ReportUtils.getSvgCircle(getControlColor(controlStatus.getStatus())) + " ");
            });
        });
        report.endTableCell();

        report.startTableCell("border: none; vertical-align: top; padding-top: 11px;");
        report.addHtmlContent("Goals:");
        boolean first[] = {true};
        analysisResults.getControlResults().getGoalsAnalysisResults().forEach(goalsAnalysisResults -> {
            if (!first[0]) {
                report.addHtmlContent(", ");
            } else {
                first[0] = false;
            }
            MetricsWithGoal metricsWithGoal = goalsAnalysisResults.getMetricsWithGoal();
            report.addHtmlContent(metricsWithGoal.getGoal() + " (" + metricsWithGoal.getControls().size() + ")");
        });
        report.endTableCell();
        report.addTableCell("<a href='" + reportRoot + "Controls.html'  title='metrics &amp; goals details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "border: none");

        report.endTableRow();
    }

    private void summarizeUnitComplexity(CodeAnalysisResults analysisResults, RichTextReport report) {
        int linesOfCodeInUnits = analysisResults.getUnitsAnalysisResults().getLinesOfCodeInUnits();
        RiskDistributionStats distribution = analysisResults.getUnitsAnalysisResults().getConditionalComplexityRiskDistribution();
        int veryComplexUnitsLOC = distribution.getHighRiskValue() + distribution.getVeryHighRiskValue();
        int lowComplexUnitsLOC = distribution.getNegligibleRiskValue();

        report.startTableRow();
        report.addTableCell(getIconSvg("conditional"), "border: none");
        report.addTableCell(getRiskProfileVisual(distribution), "border: none");
        report.addTableCell("Conditional Complexity: <b>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, veryComplexUnitsLOC))
                + "%</b> complex (McCabe index > 25), <b>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, lowComplexUnitsLOC))
                + "%</b> simple (McCabe index <= 5)", "border: none; vertical-align: top; padding-top: 11px;");

        report.addTableCell("<a href='" + reportRoot + "ConditionalComplexity.html'  title='conditional complexity details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "border: none");
        report.endTableRow();
    }

    private void summarizeUnitSize(CodeAnalysisResults analysisResults, RichTextReport report) {
        int linesOfCodeInUnits = analysisResults.getUnitsAnalysisResults().getLinesOfCodeInUnits();
        RiskDistributionStats distribution = analysisResults.getUnitsAnalysisResults().getUnitSizeRiskDistribution();
        int veryLongUnitsLOC = distribution.getVeryHighRiskValue();
        int lowUnitsLOC = distribution.getLowRiskValue() + distribution.getNegligibleRiskValue();

        report.startTableRow();
        report.addTableCell(getIconSvg("unit_size"), "border: none");
        report.addTableCell(getRiskProfileVisual(distribution), "border: none");
        report.addTableCell("Unit Size: <b>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, veryLongUnitsLOC))
                + "%</b> long (>100 LOC), <b>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, lowUnitsLOC))
                + "%</b> short (<= 20 LOC)", "border: none; vertical-align: top; padding-top: 11px;");
        report.addTableCell("<a href='" + reportRoot + "UnitSize.html'  title='unit size details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "border: none");
        report.endTableRow();
    }

    private void summarizeDuplication(CodeAnalysisResults analysisResults, RichTextReport report) {
        DuplicationMetric overallDuplication = analysisResults.getDuplicationAnalysisResults().getOverallDuplication();
        Number duplicationPercentage = overallDuplication.getDuplicationPercentage();
        double duplication = duplicationPercentage.doubleValue();
        if (analysisResults.getCodeConfiguration() == null || !analysisResults.getCodeConfiguration().getAnalysis().isSkipDuplication()) {
            report.startTableRow();
            report.addTableCell(getIconSvg("duplication"), "border: none");
            report.addTableCell(getDuplicationVisual(duplicationPercentage), "border: none");
            report.addTableCell("Duplication: <b>" + FormattingUtils.getFormattedPercentage(duplication) + "%</b>", "margin-bottom: 0;  border: none; vertical-align: top; padding-top: 11px;");
            report.addTableCell("<a href='" + reportRoot + "Duplication.html'  title='duplication details' style='vertical-align: top'>" + getDetailsIcon() + "</a>", "border: none");
            report.endTableRow();
        }
    }

    private String getVolumeVisual(List<NumericMetric> linesOfCodePerExtension, int totalLoc, int mainLoc, String text) {
        int barWidth = Math.min(BAR_WIDTH, (int) ((double) BAR_WIDTH * totalLoc / mainLoc));
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(barWidth);
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
        return getRiskProfileVisual(distributionStats, Palette.getRiskPalette());
    }

    private String getRiskProfileVisual(RiskDistributionStats distributionStats, Palette palette) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(BAR_WIDTH + 20);
        chart.setBarHeight(BAR_HEIGHT);
        chart.setMaxBarWidth(BAR_WIDTH);
        chart.setBarStartXOffset(0);

        List<Integer> values = Arrays.asList(
                distributionStats.getVeryHighRiskValue(),
                distributionStats.getHighRiskValue(),
                distributionStats.getMediumRiskValue(),
                distributionStats.getLowRiskValue(),
                distributionStats.getNegligibleRiskValue());

        return chart.getStackedBarSvg(values, palette, "", "");
    }

    private String getDuplicationVisual(Number duplicationPercentage) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(BAR_WIDTH + 20);
        chart.setBarHeight(BAR_HEIGHT);
        chart.setMaxBarWidth(BAR_WIDTH);
        chart.setBarStartXOffset(2);
        chart.setActiveColor("crimson");
        chart.setBackgroundColor("#9DC034");
        chart.setBackgroundStyle("");

        return chart.getPercentageSvg(duplicationPercentage.doubleValue(), "", "");
    }

    private String addDiffDiv(double value, double refValue) {
        double diff = value - refValue;
        String diffText = getDiffText(diff, refValue);
        StringBuilder html = new StringBuilder("<div style='margin-top: 24px; margin-top: 0; text-align: left; color: " +
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
