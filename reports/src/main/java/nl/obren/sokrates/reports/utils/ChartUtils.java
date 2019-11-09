package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.renderingutils.googlecharts.BarChart;
import nl.obren.sokrates.common.renderingutils.googlecharts.Palette;
import nl.obren.sokrates.common.renderingutils.googlecharts.PieChart;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChartUtils {

    public static PieChart getAspectsVolumePieChart(String title, List<NumericMetric> linesOfCodePerAspect) {
        PieChart pieChart = new PieChart("");
        pieChart.setWidth(800);
        pieChart.setHeight(400);
        pieChart.setTitle(title);

        Palette palette = Palette.getDefaultPalette();
        linesOfCodePerAspect.forEach(scope -> {
            pieChart.addItem(scope.getName(), scope.getValue().intValue(), palette.nextColor());
        });
        return pieChart;
    }

    public static BarChart getVolumeBarChart(String title, List<NumericMetric> data) {
        List<NumericMetric> temp = new ArrayList<>();
        data.stream().filter(item -> item.getValue().intValue() > 0).forEach(temp::add);
        data = temp;

        BarChart barChart = new BarChart(title);
        barChart.setLabels(Arrays.asList("lines of code", "lines of code"));
        barChart.setColors(Palette.getDefaultPalette().getColors());

        barChart.setHeight(50 + data.size() * 50);

        data.forEach(metric -> {
            barChart.addRow(metric.getName(), Arrays.asList(metric.getValue()));
        });

        return barChart;
    }

}
