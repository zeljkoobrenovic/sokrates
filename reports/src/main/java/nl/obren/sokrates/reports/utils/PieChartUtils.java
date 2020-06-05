/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.List;
import java.util.stream.Collectors;

public class PieChartUtils {
    public static String getRiskDistributionPieChart(RiskDistributionStats distribution, List<String> labels) {
        return getRiskDistributionPieChart(distribution, labels, Palette.getRiskPalette());
    }

    public static String getRiskDistributionPieChart(RiskDistributionStats distribution, List<String> labels, Palette palette) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);
        chart.setBarHeight(100);
        chart.setMaxBarWidth(400);
        chart.setBarStartXOffset(0);

        int totalValue = distribution.getTotalValue();

        List<Integer> values = RiskDistributionStatsReportUtils.getRowData(distribution);

        String joinedValues = values.stream().map(v -> FormattingUtils.getFormattedPercentage(100.0 * v.doubleValue() / totalValue) + "%").collect(Collectors.joining(" | "));
        String stackedBarSvg = chart.getStackedBarSvg(values, palette, distribution.getKey(), joinedValues);

        String html = "<div style='width: 100%; overflow-x: auto'>";
        html += "<div>" + stackedBarSvg + "</div>";
        html += "<div style='font-size:90%;margin-top:20px;width:100%;text-alight:right'>";
        html += "Legend: " + chart.getLegend(labels, palette);
        html += "</div>";
        html += "</div>";

        return html;
    }
}
