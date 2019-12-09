package nl.obren.sokrates.common.renderingutils.charts;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PieChart {
    private static int chartCounter = 1;
    private String title;
    private List<String> labels = new ArrayList<>();
    private List<Number> values = new ArrayList<>();
    private List<String> colors = new ArrayList<>();
    private int width = 800;
    private int height = 500;

    public PieChart(String title) {
        this.title = title;
    }

    public void addItem(String label, Number value, String color) {
        labels.add(label);
        values.add(value);
        colors.add(color);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<Number> getValues() {
        return values;
    }

    public void setValues(List<Number> values) {
        this.values = values;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    @Override
    public String toString() {
        StringBuilder dataString = new StringBuilder();
        dataString.append("['', ''],");
        for (int i = 0; i < labels.size(); i++) {
            dataString.append("['" + labels.get(i) + "', " + values.get(i) + "],");
        }
        StringBuilder colorsString = new StringBuilder();
        colors.forEach(color -> colorsString.append("'" + color + "',"));

        String pieChartScript = getScriptString(dataString, colorsString);

        return pieChartScript;
    }

    private String getScriptString(StringBuilder dataString, StringBuilder colorsString) {
        String id = "pie" + chartCounter++;
        String functionId = "draw_" + id;
        return "<script type=\"text/javascript\">\n" +
                "      google.charts.load(\"current\", {packages:[\"corechart\"]});\n" +
                "      google.charts.setOnLoadCallback(" + functionId + ");\n" +
                "      function " + functionId + "() {\n" +
                "        var data = google.visualization.arrayToDataTable([\n" +
                StringUtils.removeEnd(dataString.toString(), ",") +
                "        ]);\n" +
                "        var options = {\n" +
                (StringUtils.isNotBlank(title) ? "          title: '" + title + "',\n" : "") +
                "          pieHole: 0.5,\n" +
                "          fontName: 'Tahoma', " +
                "          colors: [\n" +
                StringUtils.removeEnd(colorsString.toString(), ",") +
                "          ]\n" +
                "        };\n" +
                "\n" +
                "        var chart = new google.visualization.PieChart(document.getElementById('" + id + "'));\n" +
                "        chart.draw(data, options);\n" +
                "      }\n" +
                "</script>" +
                "<div id=\"" + id + "\" style=\"width: " + width + "px; height: " + height + "px;\"></div>";
    }
}
