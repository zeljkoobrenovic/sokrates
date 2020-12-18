/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.core.SummaryUtils;
import nl.obren.sokrates.reports.dataexporters.trends.ReferenceResultsLoader;
import nl.obren.sokrates.reports.utils.ReportUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.ReferenceAnalysisResult;
import nl.obren.sokrates.sourcecode.core.TrendAnalysisConfig;
import nl.obren.sokrates.sourcecode.metrics.Metric;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrendReportGenerator {
    private RichTextReport report;
    private File codeConfigurationFile;

    public TrendReportGenerator(File codeConfigurationFile) {
        this.codeConfigurationFile = codeConfigurationFile;
    }

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport report) {
        this.report = report;
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("Trend report shows difference in metric between the latest measurements and previous reference measurements.");
        report.endUnorderedList();

        report.startShowMoreBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("For more insights in the value of trend analysis, Sokrates recommends reading the section \"Favor tracking trends over absolute numbers\" in the article <a href='https://martinfowler.com/articles/useOfMetrics.html#FavorTrackingTrendsOverAbsoluteNumbers' target='_blank'>An Appropriate Use of Metrics</a>, (MartinFowler.com).");
        report.endUnorderedList();
        report.endShowMoreBlock();
        report.endSection();

        TrendAnalysisConfig trendAnalysis = codeAnalysisResults.getCodeConfiguration().getTrendAnalysis();
        List<ReferenceAnalysisResult> referenceResults = trendAnalysis.getReferenceAnalyses(codeConfigurationFile.getParentFile());

        if (referenceResults.size() == 0) {
            report.addParagraph("No reference analysis results have been defined.");
            return report;
        }

        summarize(codeAnalysisResults, report, referenceResults);

        referenceResults.forEach(result -> {
            CodeAnalysisResults refData = new ReferenceResultsLoader().getRefData(result.getAnalysisResultsZipFile());
            processReferenceResults(codeAnalysisResults, refData, report, result);
        });

        return report;
    }

    private void summarize(CodeAnalysisResults currentAnalysisResults, RichTextReport report, List<ReferenceAnalysisResult> referenceResults) {
        List<CodeAnalysisResults> analysisResultsList = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        analysisResultsList.add(currentAnalysisResults);
        labels.add("Current");
        int maxMainLoc[] = {currentAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode()};
        int maxTestLoc[] = {currentAnalysisResults.getTestAspectAnalysisResults().getLinesOfCode()};
        int maxTotalLoc[] = {maxMainLoc[0] + maxTestLoc[0]};
        referenceResults.forEach(result -> {
            CodeAnalysisResults refData = new ReferenceResultsLoader().getRefData(result.getAnalysisResultsZipFile());
            if (refData != null && refData.getCodeConfiguration() != null) {
                analysisResultsList.add(refData);
                labels.add(result.getLabel());

                int refLocMain = refData.getMainAspectAnalysisResults().getLinesOfCode();
                int refLocTest = refData.getTestAspectAnalysisResults().getLinesOfCode();

                maxMainLoc[0] = Math.max(refLocMain, maxMainLoc[0]);
                maxTestLoc[0] = Math.max(refLocTest, maxTestLoc[0]);
                maxTotalLoc[0] = Math.max(refLocMain + refLocTest, maxTotalLoc[0]);
            }
        });
        addCodeVolumeSummarySection(report, analysisResultsList, labels, maxTotalLoc[0]);
    }

    private void addCodeVolumeSummarySection(RichTextReport report, List<CodeAnalysisResults> analysisResultsList, List<String> labels, int maxTotalLoc) {
        report.startSection("Summary: Code Volume Change", "");
        report.startDiv("width: 100%; overflow-x: auto");
        report.startTable();
        report.addTableHeader("", "Main LOC", "", "Test LOC", "Duplication");

        int index[] = {0};
        analysisResultsList.forEach(refData -> {
            int mainLoc = refData.getMainAspectAnalysisResults().getLinesOfCode();
            int testLoc = refData.getTestAspectAnalysisResults().getLinesOfCode();
            double duplicationPercentage = refData.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage().doubleValue();

            report.startTableRow();
            report.addTableCell(labels.get(index[0]));
            report.addTableCell(FormattingUtils.formatCount(mainLoc), "text-align: right");
            report.addTableCell(getVolumeSvgBarChart(maxTotalLoc, mainLoc, testLoc));
            report.addTableCell(FormattingUtils.formatCount(testLoc), "text-align: right");
            report.addTableCell(getDuplicationChart(duplicationPercentage));
            report.endTableRow();
            index[0]++;
        });
        report.endTable();
        report.addLineBreak();
        report.addParagraph("See the details (TXT):");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("all metrics", "../data/text/metrics_trend.txt");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("lines of code per extension", "../data/text/metrics_trend_loc_per_extension.txt");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("lines of code per logical component", "../data/text/metrics_trend_loc_logical_decompositions.txt");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("duplicated lines", "../data/text/metrics_trend_loc_duplication.txt");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("lines of code per file size category", "../data/text/metrics_trend_loc_file_size.txt");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("lines of code per unit size category", "../data/text/metrics_trend_unit_size_loc.txt");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("lines of code per conditional complexity catagory", "../data/text/metrics_trend_conditional_complexity_loc.txt");
        report.endListItem();
        report.endUnorderedList();
        report.endDiv();
        report.endSection();
    }

    private String getVolumeSvgBarChart(int maxTotalLoc, int mainLoc, int testLoc) {
        if (maxTotalLoc == 0) return "";

        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(310);
        chart.setMaxBarWidth((int) (300.0 * (mainLoc + testLoc) / maxTotalLoc));
        chart.setBarHeight(20);
        chart.setBarStartXOffset(2);
        return chart.getStackedBarSvg(Arrays.asList(mainLoc, testLoc), Palette.getDefaultPalette(), "", "");
    }

    private String getDuplicationChart(double duplicationPercentage) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(110);
        chart.setMaxBarWidth(60);
        chart.setBarHeight(20);
        chart.setBarStartXOffset(2);
        chart.setActiveColor("crimson");
        return chart.getPercentageSvg(duplicationPercentage, "", FormattingUtils.getFormattedPercentage(duplicationPercentage) + "%");
    }

    private void processReferenceResults(CodeAnalysisResults codeAnalysisResults, CodeAnalysisResults refData, RichTextReport report, ReferenceAnalysisResult result) {
        String analysisResultsPath = result.getAnalysisResultsZipFile().getPath();
        if (refData != null) {
            report.startSection("Current vs. " + result.getLabel(), analysisResultsPath);
            report.startShowMoreBlock("Comparison summary...");
            new SummaryUtils().summarizeAndCompare(codeAnalysisResults, refData, report);
            report.endShowMoreBlock();

            report.addLineBreak();
            report.addLineBreak();

            report.startShowMoreBlock("Detailed comparison of all metrics...");
            report.startDiv("width: 100%; overflow-x: auto");
            report.startTable();
            report.addTableHeader("Metric", "Reference Value", "Current Value", "Difference");
            CodeAnalysisResults finalRefData = refData;
            codeAnalysisResults.getMetricsList().getMetrics().forEach(metric -> {
                addRow(metric, finalRefData);
            });
            report.endTable();
            report.endDiv();
            report.endShowMoreBlock();
            report.endSection();
        } else {
            report.addParagraph("ERROR: could not find the reference result file '" + analysisResultsPath + "'.");
        }
    }

    private void addRow(Metric metric, CodeAnalysisResults refData) {
        Metric refMetric = refData.getMetricsList().getMetricById(metric.getId());
        double currentValue = metric.getValue().doubleValue();
        double refValue = refMetric != null ? refMetric.getValue().doubleValue() : 0;

        report.startTableRow();
        report.startTableCell();
        report.addHtmlContent("<b>" + metric.getId() + "</b><br/>");
        report.endTableCell();


        report.startTableCell("text-align: center; color: lightgrey");
        report.addHtmlContent("" + ReportUtils.formatNumber(refValue));
        report.endTableCell();
        report.startTableCell("text-align: center");
        report.addHtmlContent("<b>" + ReportUtils.formatNumber(currentValue) + "</b>");
        report.endTableCell();

        addDiffCell(currentValue, refValue);

        report.endTableRow();
    }

    private void addDiffCell(double currentValue, double refValue) {
        double diff = currentValue - refValue;
        String diffText = getDiffText(diff, refValue);
        report.startTableCell("text-align: right; color: " +
                (diff == 0 ? "lightgrey" : (diff < 0 ? "#b9936c" : "#6b5b95")));
        if (diff > 0) {
            report.addHtmlContent("+" + diffText + " ⬆");
        } else if (diff < 0) {
            report.addHtmlContent("" + diffText + " ⬇ ");
        } else {
            report.addHtmlContent("" + diffText + "");
        }
        report.endTableCell();
    }

    private String getDiffText(double diff, double refValue) {
        double roundingError = 0.0000000000000000000001;
        String diffText;
        diff = ((int) diff * 100.0) / 100;
        if (Math.abs(diff) < roundingError) {
            diffText = "0 (0%)";
        } else if (Math.abs(refValue) < roundingError) {
            diffText = ReportUtils.formatNumber(diff) + " (NEW)";
        } else {
            double percentage = 100.0 * diff / refValue;
            diffText = ReportUtils.formatNumber(diff) + " (" + (percentage > 0 ? "+" : (percentage < 0 ? "-" : ""))
                    + FormattingUtils.getFormattedPercentage(Math.abs(percentage)) + "%)";
        }
        return diffText;
    }
}
