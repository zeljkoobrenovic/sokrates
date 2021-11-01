/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils.charts;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OrgChart {
    private List<OrgChartItem> data = new ArrayList<>();

    public OrgChart(List<OrgChartItem> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder dataString = new StringBuilder();

        data.forEach(row -> {
            dataString.append("["
                    + "'" + row.getItem() + "',"
                    + "'" + row.getParent() + "',"
                    + "'" + row.getTooltip() + "'"
                    + "],");
        });


        String pieChartScript = getScriptString(StringUtils.removeEnd(dataString.toString(), ","));

        return pieChartScript;
    }

    private String getScriptString(String dataString) {
        return "<script type=\"text/javascript\">\n" +
                "      google.charts.load(\"current\", {packages:[\"orgchart\"]});\n" +
                "      google.charts.setOnLoadCallback(drawChart);\n" +
                "      function drawChart() {\n" +
                "var data = new google.visualization.DataTable();\n" +
                "        data.addColumn('string', 'Name');\n" +
                "        data.addColumn('string', 'Manager');\n" +
                "        data.addColumn('string', 'ToolTip');\n" +
                "\n" +
                "        data.addRows([\n" +
                dataString +
                "        ]);\n" +
                "\n" +
                "        // Create the chart.\n" +
                "        var chart = new google.visualization.OrgChart(document.getElementById('chart_div'));\n" +
                "        // Draw the chart, setting the allowHtml option to true for the tooltips.\n" +
                "        chart.draw(data, {allowHtml:true});" +
                "      }\n" +
                "</script>" +
                "<div id=\"chart_div\" style=\"width: 800px; height: 500px;\"></div>";
    }
}
