/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import java.text.DecimalFormat;

public class ReportUtils {

    public static String getSvgBar(int value, int maxValue, String color) {
        maxValue = Math.max(value, maxValue);
        int width = 200;
        return "<svg width='" + width + "' y='15' height='20'>"
                + "<rect fill='" + "lightgrey" + "' width='" + width + "' height='30'/>"
                + "<rect fill='" + color + "' width='" + Math.max(1, width * value / maxValue) + "' height='30'/>"
                + "</svg>";
    }

    public static String getSvgCircle(String color) {
        String svg = "<svg height=\"22\" width=\"22\">\n";
        svg += "<circle cx=\"11\" cy=\"11\" r=\"10\" ";
        svg += "fill=\"" + color + "\" />";
        svg += "</svg>";

        return svg;
    }

    public static String formatNumber(Number number) {
        try {
            DecimalFormat df = new DecimalFormat("###,###,###.##");
            String formattedValue = df.format(number);
            if (formattedValue.equalsIgnoreCase("NaN")) {
                formattedValue = "-";
            }

            return formattedValue;
        } catch (IllegalArgumentException e) {
            return "-";
        }
    }
}
