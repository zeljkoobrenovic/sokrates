/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;

public class DuplicationReportUtils {
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
            // Use the guarded accessor so a metric with 0 cleaned lines yields 0%, not NaN.
            double percentage = metric.getDuplicationPercentage().doubleValue();
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

        chart.setMaxBarWidth(200);
        // Use the guarded accessor so 0 cleaned lines yields 0%, not NaN.
        double percentage = metric.getDuplicationPercentage().doubleValue();
        String textRight = StringEscapeUtils.escapeHtml4(
                FormattingUtils.getFormattedPercentage(percentage)
                        + "% (" + FormattingUtils.formatCount(metric.getDuplicatedLinesOfCode()) + " lines)");
        String svg = chart.getPercentageSvg(percentage, metric.getKey(), textRight);
        report.startDiv("width: 100%; overflow-x: auto");
        report.addContentInDiv(svg);
        report.endDiv();
    }

}
