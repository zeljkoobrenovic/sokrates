package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.ToStringFunction;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public class CorrelationDiagramGenerator<T> {
    private RichTextReport report;
    private List<T> items;
    private int height = 300;
    private int width = 600;
    private int r = 9;
    private int margin = 5;
    private int maxNumberOfPointsOnDiagram = 2000;

    public CorrelationDiagramGenerator(RichTextReport report, List<T> items) {
        this.report = report;
        this.items = items;
    }

    public void addCorrelations(String title, String xLabel, String yLabel, ToDoubleFunction<T> xValueFunction, ToDoubleFunction<T> yValueFunction, ToStringFunction<T> nameFunction) {
        DescriptiveStatistics xStats = new DescriptiveStatistics();
        DescriptiveStatistics yStats = new DescriptiveStatistics();
        items.forEach(repository -> {
            double xValue = xValueFunction.applyAsDouble(repository);
            double yValue = yValueFunction.applyAsDouble(repository);
            if (xValue > 0 && yValue > 0) {
                xStats.addValue(xValue);
                yStats.addValue(yValue);
            }
        });
        report.startDiv("display: inline-block; vertical-align: top; margin-right: 40px;");
        report.addLevel2Header(title + ": <span style='color: grey;'>" + xStats.getN() + " points</span>");
        if (xStats.getN() == 0) {
            report.addParagraph("No data for \"" + xLabel + "\" vs. \"" + yLabel + "\".");
            report.endDiv();
            return;
        }
        report.startTable("");

        report.startTableRow();
        report.startTableCellColSpan(2, "border: none");

        report.startDiv("width: 600px; text-align: center; ");
        report.addHtmlContent("<svg width=\"" + width + "\" height=\"" + height + "\">");
        report.addHtmlContent(" <rect width=\"" + width + "\" height=\"" + height + "\" style=\"fill:rgb(200,200,200);stroke-width:1;stroke:rgb(100,100,100)\" />");
        Map<String, String> sameLocationMap = new HashMap<>();
        int count[] = {1};
        items.forEach(item -> {
            if (count[0] > maxNumberOfPointsOnDiagram) {
                return;
            }
            double xValue = xValueFunction.applyAsDouble(item);
            double yValue = yValueFunction.applyAsDouble(item);
            if (xValue > 0 && yValue > 0) {
                double x = getX(xStats.getMax(), xValue);
                double y = getY(yStats.getMax(), yValue);
                String key = (int) x + "::" + (int) y;
                if (sameLocationMap.containsKey(key)) {
                    return;
                }
                sameLocationMap.put(key, key);
                report.addHtmlContent(" <circle cx=\"" + (int) x + "\" cy=\"" + (int) y + "\" r=\"" + r + "\" fill=\"black\" fill-opacity=\"0.2\">");
                report.addHtmlContent(" <title>" + nameFunction.toString(item)
                        + "\n  x: " + (int) xValue + " " + xLabel
                        + "\n  y: " + (int) yValue + " " + yLabel + "</title>");
                report.addHtmlContent(" </circle>");
                count[0] += 1;
            }
        });

        renderDistributionLines(xStats, yStats);

        report.addHtmlContent("</svg>");

        report.endDiv();
        report.endTableCell();
        report.addTableCell(yStats.getMax() + "", "padding: 0; padding-top: 10px; font-size: 70%; border: none; vertical-align: top");
        report.addTableCell("<div style='font-size: 130%; margin-bottom: 4px; font-weight: bold'>" + yLabel + "</div>"
                + "&nbsp;&nbsp;min: " + yStats.getMin()
                + "<br>&nbsp;&nbsp;average: " + Math.round(yStats.getMean() * 100) / 100.0
                + "<br>&nbsp;&nbsp;25th percentile: " + yStats.getPercentile(25)
                + "<br>&nbsp;&nbsp;median: " + yStats.getPercentile(50)
                + "<br>&nbsp;&nbsp;75th percentile: " + yStats.getPercentile(75)
                + "<br>&nbsp;&nbsp;max: " + yStats.getMax(), "font-size: 70%; border: none;");

        report.endTableRow();

        report.startTableRow();
        report.addTableCell("0", "padding-left: 21px; text-align: left; font-size: 70%; border: none; height: 6px;");
        report.addTableCell(xStats.getMax() + "", "padding-right: 22px; text-align: right; font-size: 70%; border: none; height: 6px;");
        report.endTableRow();

        report.startTableRow();
        report.startTableCellColSpan("vertical-align: top; border: none; height: 6px;", 2);
        report.addContentInDiv("<div style='font-size: 130%; margin-bottom: 4px; font-weight: bold'>" + xLabel + "</div>"
                        + "min: " + xStats.getMin()
                        + " | average: " + Math.round(xStats.getMean() * 100) / 100.0
                        + " | 25th percentile: " + xStats.getPercentile(25)
                        + " | median: " + xStats.getPercentile(50)
                        + " | 75th percentile: " + xStats.getPercentile(75)
                        + " | max: " + xStats.getMax()
                , "text-align: center; border: none; font-size: 70%; height: 6px; margin-bottom: 26px;");
        report.endTableCell();
        report.endTableRow();

        report.endTable();
        report.endDiv();
    }

    private void renderDistributionLines(DescriptiveStatistics xStats, DescriptiveStatistics yStats) {
        double x1MinZero = getX(xStats.getMax(), 0) - r;
        double x1Min = getX(xStats.getMax(), xStats.getMin()) - r;
        double y1Min = getY(yStats.getMax(), yStats.getMin()) + r;
        double x1Max = getX(xStats.getMax(), xStats.getMax()) + r;
        double y1Max = getY(yStats.getMax(), yStats.getMax()) - r;

        double x1Median = getX(xStats.getMax(), xStats.getPercentile(50)) + r;
        double y1Median = getY(yStats.getMax(), yStats.getPercentile(50)) - r;
        double x1P25 = getX(xStats.getMax(), xStats.getPercentile(25)) + r;
        double y1P25 = getY(yStats.getMax(), yStats.getPercentile(25)) - r;
        double x1P75 = getX(xStats.getMax(), xStats.getPercentile(75)) + r;
        double y1P75 = getY(yStats.getMax(), yStats.getPercentile(75)) - r;

        double x1Outlier1 = Math.max(x1Min, x1P25 - Math.abs(x1P75 - x1P25) * 1.5);
        double x1Outlier2 = Math.min(x1Max, x1P75 + Math.abs(x1P75 - x1P25) * 1.5);

        double y1Outlier1 = Math.min(y1Min, y1P25 + Math.abs(y1P75 - y1P25) * 1.5);
        double y1Outlier2 = Math.max(y1Max, y1P75 - Math.abs(y1P75 - y1P25) * 1.5);

        report.addHtmlContent(" <line x1='" + (int) x1Median + "' y1='0' x2='" + (int) x1Median + "' y2='" + height + "' stroke='blue' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='0' y1='" + (int) y1Median + "' x2='" + width + "' y2='" + (int) y1Median + "' stroke='blue' stroke-opacity='0.5'/>");

        report.addHtmlContent(" <line x1='" + (int) x1P25 + "' y1='0' x2='" + (int) x1P25 + "' y2='" + height + "' stroke='grey' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='0' y1='" + (int) y1P25 + "' x2='" + width + "' y2='" + (int) y1P25 + "' stroke='grey' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='" + (int) x1P75 + "' y1='0' x2='" + (int) x1P75 + "' y2='" + height + "' stroke='grey' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='0' y1='" + (int) y1P75 + "' x2='" + width + "' y2='" + (int) y1P75 + "' stroke='grey' stroke-opacity='0.5'/>");

        report.addHtmlContent(" <line x1='" + (int) x1MinZero + "' y1='0' x2='" + (int) x1MinZero + "' y2='" + height + "' stroke='black' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='0' y1='" + (int) y1Min + "' x2='" + width + "' y2='" + (int) y1Min + "' stroke='black' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='" + (int) x1Max + "' y1='0' x2='" + (int) x1Max + "' y2='" + height + "' stroke='black' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='0' y1='" + (int) y1Max + "' x2='" + width + "' y2='" + (int) y1Max + "' stroke='black' stroke-opacity='0.5'/>");

        report.addHtmlContent(" <line x1='" + (int) x1Outlier1 + "' y1='0' x2='" + (int) x1Outlier1 + "' y2='" + height + "' stroke='red' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='" + (int) x1Outlier2 + "' y1='0' x2='" + (int) x1Outlier2 + "' y2='" + height + "' stroke='red' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='0' y1='" + (int) y1Outlier1 + "' x2='" + width + "' y2='" + (int) y1Outlier1 + "' stroke='red' stroke-opacity='0.5'/>");
        report.addHtmlContent(" <line x1='0' y1='" + (int) y1Outlier2 + "' x2='" + width + "' y2='" + (int) y1Outlier2 + "' stroke='red' stroke-opacity='0.5'/>");
    }

    public double getY(Number yMax, double yValue) {
        return (height - 2 * r - margin) - (height - 4 * r - 2 * margin) * yValue / yMax.doubleValue();
    }

    public double getX(Number xMax, double xValue) {
        return margin + 2 * r + (width - 4 * r - 2 * margin) * xValue / xMax.doubleValue();
    }


}
