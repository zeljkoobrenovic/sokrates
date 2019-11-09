package nl.obren.sokrates.reports.utils;

public class ReportUtils {

    public static String getSvgBar(int linesOfCode, int maxLinesOfCode, String color) {
        int width = 100;
        return "<svg width='" + width + "' y='15' height='10'>" + "<rect fill='" + color + "' width='" + Math.max(1, width * linesOfCode / maxLinesOfCode) + "' height='30'/>" + "</svg";
    }

    public static String getSvgCircle(String color) {
        String svg = "<svg height=\"22\" width=\"22\">\n";
        svg += "<circle cx=\"11\" cy=\"11\" r=\"10\" ";
        svg += "fill=\"" + color + "\" />";
        svg += "</svg>";

        return svg;
    }
}
