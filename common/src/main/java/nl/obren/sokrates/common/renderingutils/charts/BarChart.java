/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils.charts;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BarChart {
    private static int chartCounter = 1;
    private String title;
    private int width = 900;
    private int height = 500;
    private List<String> labels = new ArrayList<>();
    private List<String> rowLabels = new ArrayList<>();
    private List<List<Number>> rows = new ArrayList<>();
    private List<String> colors = new ArrayList<>();

    public BarChart(String title) {
        this.title = title;
    }

    public void addRow(String label, List<Number> values) {
        rowLabels.add(label);
        rows.add(values);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<List<Number>> getRows() {
        return rows;
    }

    public void setRows(List<List<Number>> rows) {
        this.rows = rows;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    @Override
    public String toString() {
        StringBuilder labelsString = new StringBuilder();
        labels.forEach(label -> labelsString.append("'" + label + "',"));
        StringBuilder dataString = new StringBuilder();
        dataString.append("[" + StringUtils.removeEnd(labelsString.toString(), ",") + "],");
        for (int i = 0; i < rowLabels.size(); i++) {
            dataString.append("['" + rowLabels.get(i) + "', ");
            rows.get(i).forEach(value -> dataString.append("" + value + ","));
            dataString.append("],");
        }

        StringBuilder colorsString = new StringBuilder();
        colors.forEach(color -> colorsString.append("'" + color + "',"));

        String pieChartScript = getScriptString(dataString, colorsString);

        return pieChartScript;
    }

    private String getScriptString(StringBuilder dataString, StringBuilder colorsString) {
        String id = "bar" + chartCounter++;
        String functionId = "draw_" + id;
        return "<script type=\"text/javascript\">\n" +
                "      google.charts.load(\"current\", {packages:[\"corechart\"]});\n" +
                "      google.charts.setOnLoadCallback(" + functionId + ");\n" +
                "      function " + functionId + "() {\n" +
                "        var data = google.visualization.arrayToDataTable([\n" +
                StringUtils.removeEnd(dataString.toString().replace(",]", "]"), ",") +
                "        ]);\n" +
                "        var options = {\n" +
                "          title: '" + title + "',\n" +
                "          fontName: 'Tahoma', " +
                "          isStacked: true," +
                "          height: " + height + "," +
                "          legend: { position: 'top', maxLines: 3 }, " +
                "          colors: [\n" +
                StringUtils.removeEnd(colorsString.toString(), ",") +
                "          ]\n" +
                "        };\n" +
                "\n" +
                "        var chart = new google.visualization.BarChart(document.getElementById('" + id + "'));\n" +
                "        chart.draw(data, options);\n" +
                "      }\n" +
                "</script>" +
                "<div id=\"" + id + "\" style=\"width: " + width + "px; height: " + height + "px;\"></div>";
    }
}
