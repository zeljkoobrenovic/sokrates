/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.renderingutils.charts.BarChart;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.renderingutils.charts.PieChart;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Arrays;
import java.util.List;

public class DuplicationReportUtils {
    public static PieChart getSystemDuplicationPieChart(int totalNumberOfCleanedLines, int numberOfDuplicatedLines) {
        PieChart pieChart = new PieChart("System Duplication");
        Palette duplicationPalette = Palette.getDuplicationPalette();

        int nonDuplicatedLinesOfCode = totalNumberOfCleanedLines - numberOfDuplicatedLines;

        pieChart.addItem("Not-Duplicated Lines", nonDuplicatedLinesOfCode, duplicationPalette.nextColor());
        pieChart.addItem("Duplicated Lines", numberOfDuplicatedLines, duplicationPalette.nextColor());
        return pieChart;
    }


    public static BarChart getDuplicationPerAspect(List<DuplicationMetric> duplicationMetrics, String title) {
        BarChart barChart = new BarChart(title);
        barChart.setLabels(Arrays.asList("Component", "Not-Duplicated Lines", "Duplicated Lines"));
        Palette duplicationPalette = Palette.getDuplicationPalette();
        barChart.setColors(Arrays.asList(duplicationPalette.nextColor(), duplicationPalette.nextColor()));
        barChart.setHeight(50 + duplicationMetrics.size() * 50);

        duplicationMetrics.forEach(duplication -> {
            int duplicatedLinesOfCode = duplication.getDuplicatedLinesOfCode();
            int nonDuplicatedLinesOfCode = duplication.getCleanedLinesOfCode() - duplicatedLinesOfCode;

            barChart.addRow(duplication.getKey(), Arrays.asList(nonDuplicatedLinesOfCode, duplicatedLinesOfCode));
        });

        return barChart;
    }

    public static void addDuplicationPerAspect(RichTextReport report, List<DuplicationMetric> duplicationMetrics) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);
        chart.setBarHeight(20);
        chart.setAlignment(SimpleOneBarChart.Alignment.LEFT);
        chart.setActiveColor("crimson");
        chart.setBackgroundColor("green");

        final int[] maxCleanedLinesOfCode = {1};

        duplicationMetrics.forEach(metric -> maxCleanedLinesOfCode[0] = Math.max(maxCleanedLinesOfCode[0], metric.getCleanedLinesOfCode()));

        report.startDiv("width: 100%; overflow-x: auto; max-height: 300px; overflow-y: scroll;");
        duplicationMetrics.stream().sorted((o1, o2) -> o2.getDuplicatedLinesOfCode() - o1.getDuplicatedLinesOfCode()).forEach(metric -> {
            chart.setMaxBarWidth((int) (200.0 * metric.getCleanedLinesOfCode() / maxCleanedLinesOfCode[0]));
            double percentage = 100.0 * metric.getDuplicatedLinesOfCode() / metric.getCleanedLinesOfCode();
            String textRight = StringEscapeUtils.escapeHtml4(
                    FormattingUtils.getFormattedPercentage(percentage)
                            + "% (" + FormattingUtils.formatCount(metric.getDuplicatedLinesOfCode()) + " lines)");
            String svg = chart.getPercentageSvg(percentage, metric.getKey(), textRight);
            report.addContentInDiv(svg);
        });
        report.endDiv();
    }

    public static void addOverallDuplication(RichTextReport report, DuplicationMetric metric) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);
        chart.setBarHeight(40);
        chart.setAlignment(SimpleOneBarChart.Alignment.LEFT);
        chart.setActiveColor("crimson");
        chart.setBackgroundColor("green");

        chart.setMaxBarWidth((int) (200.0 * metric.getCleanedLinesOfCode() / metric.getCleanedLinesOfCode()));
        double percentage = 100.0 * metric.getDuplicatedLinesOfCode() / metric.getCleanedLinesOfCode();
        String textRight = StringEscapeUtils.escapeHtml4(
                FormattingUtils.getFormattedPercentage(percentage)
                        + "% (" + FormattingUtils.formatCount(metric.getDuplicatedLinesOfCode()) + " lines)");
        String svg = chart.getPercentageSvg(percentage, metric.getKey(), textRight);
        report.startDiv("width: 100%; overflow-x: auto");
        report.addContentInDiv(svg);
        report.endDiv();
    }

}
