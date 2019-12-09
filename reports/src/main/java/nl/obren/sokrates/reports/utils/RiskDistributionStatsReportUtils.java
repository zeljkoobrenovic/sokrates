package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RiskDistributionStatsReportUtils {
    public static List<Integer> getRowData(RiskDistributionStats riskDistributionStats) {
        List<Integer> rowData = new ArrayList<>();
        rowData.add(riskDistributionStats.getVeryHighRiskValue());
        rowData.add(riskDistributionStats.getHighRiskValue());
        rowData.add(riskDistributionStats.getMediumRiskValue());
        rowData.add(riskDistributionStats.getLowRiskValue());
        return rowData;
    }

    public static String getRiskDistributionPerKeySvgBarChart(List<RiskDistributionStats> distributions, List<String> labels) {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(800);

        final String[] html = {""};

        int[] maxTotalValue = {0};
        distributions.forEach(distribution -> {
            maxTotalValue[0] = Math.max(maxTotalValue[0], distribution.getTotalValue());
        });

        Palette palette = Palette.getRiskPalette();

        chart.calculateBarOffsetFromTexts(distributions.stream().map(d -> d.getKey()).collect(Collectors.toList()));

        distributions.stream()
                .sorted((o1,o2) -> o2.getLowRiskValue() - o1.getLowRiskValue())
                .sorted((o1,o2) -> o2.getMediumRiskValue() - o1.getMediumRiskValue())
                .sorted((o1,o2) -> o2.getHighRiskValue() - o1.getHighRiskValue())
                .sorted((o1,o2) -> o2.getVeryHighRiskValue() - o1.getVeryHighRiskValue())
                .forEach(distribution -> {
            int totalValue = distribution.getTotalValue();
            chart.setMaxBarWidth(Math.max(1, (int) Math.round(300.0 * totalValue / maxTotalValue[0])));

            List<Integer> values = getRowData(distribution);

            String joinedValues = values.stream().map(v -> FormattingUtils.getFormattedPercentage(100.0 * v.doubleValue() / totalValue) + "%").collect(Collectors.joining(" | "));
            String stackedBarSvg = chart.getStackedBarSvg(values, palette, distribution.getKey(), joinedValues);
            html[0] += "<div>" + stackedBarSvg + "</div>";
        });

        html[0] += "<div style='font-size:90%;margin-top:20px;width:100%;text-alight:right'>Legend: " + chart.getLegend(labels, palette) + "</div>";

        return html[0];
    }
}
