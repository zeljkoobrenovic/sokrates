/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.charts;

import nl.obren.sokrates.common.renderingutils.charts.Palette;

import java.util.List;

public class SimpleOneBarChart {
    private String backgroundColor = "#ffffff";
    private String activeColor = "#00aced";
    private Alignment alignment = Alignment.LEFT;
    private String backgroundStyle = "fill-opacity:0.4;stroke:#a0a0a0;stroke-width:0.5px;";
    private String activeStyle = "fill:#c0deed;stroke:#898989;stroke-width:0.5;";
    private int width = 800;
    private int maxBarWidth = 200;
    private int barHeight = 20;
    private int barStartXOffset = 300;
    private String fontSize = "medium";

    public void calculateBarOffsetFromTexts(List<String> leftTexts) {

        barStartXOffset = 0;

        leftTexts.forEach(text -> barStartXOffset = Math.max(barStartXOffset, 20 + text.length() * 8));

        barStartXOffset = Math.min(400, barStartXOffset);
        barStartXOffset = Math.max(100, barStartXOffset);
    }

    public String getStackedBarSvg(List<Integer> values, Palette palette, String textLeft, String textRight) {
        String svg = "<svg width='" + width + "' height='" + (barHeight + 4) + "'>";

        double sum = values.stream().mapToDouble(n -> n.doubleValue()).sum();

        svg += getBackgroundBarSvg(maxBarWidth);

        int x = barStartXOffset;

        for (int i = 0; i < values.size(); i++) {
            double value = values.get(i).doubleValue();
            String color = palette.nextColor();
            if (value > 0.0000000001) {
                int barSize = (int) (maxBarWidth * (value / sum));
                if (barSize == 0 && value > 0) {
                    barSize = 1;
                }
                if (x < maxBarWidth + barStartXOffset) {
                    if (x + barSize <= maxBarWidth + barStartXOffset) {
                        svg += getBarSvg(x, barSize, color);
                    } else {
                        svg += getBarSvg(x, maxBarWidth + barStartXOffset - x, color);
                    }
                }
                x += barSize;
            }
        }


        svg += getRightAlignedTextSvg(textLeft, barStartXOffset - 4);
        svg += getTextSvg(textRight, (barStartXOffset + 8 + maxBarWidth));

        svg += "</svg>";

        return svg;
    }

    public String getLegend(List<String> labels, Palette palette) {
        String html = "<div style='display: inline-block'>";

        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            String color = palette.nextColor();
            html += "<div style='margin: 4px; vertical-align:middle;display:inline-block;width:14px;height:14px;background-color:" + color + "; border: 1px solid #a0a0a0;'></div>";
            html += "<div style='margin-right:10px; display:inline-block'>" + label + "</div>";
        }

        html += "</div>";
        return html;
    }

    private String getBarSvg(int x, int activeBarSize, String color) {
        return "<rect" +
                " x='" + x + "'" +
                " y='2'" +
                " width='" + activeBarSize + "'" +
                " height='" + barHeight + "'" +
                " style='" + activeStyle + "fill:" + color + "'" +
                "/>";
    }

    public String getPercentageSvg(double percentage, String textLeft, String textRight) {
        double activeBarSize = Math.max(1, percentage * maxBarWidth / 100.0);
        return getSvg(activeBarSize, textLeft, textRight);
    }

    public String getSvg(double activeBarSize, String textLeft, String textRight) {
        String svg = "<svg width='" + width + "' height='" + (barHeight + 4) + "'>";

        svg += getBackgroundBarSvg(maxBarWidth);
        svg += getBarSvg((int) (activeBarSize));
        svg += getRightAlignedTextSvg(textLeft, barStartXOffset - 4);
        svg += getTextSvg(textRight, barStartXOffset + 8 + maxBarWidth);

        svg += "</svg>";

        return svg;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    public void setSmallerFontSize() {
        this.fontSize = "smaller";
    }

    private String getRightAlignedTextSvg(String text, int x) {
        return "<text text-anchor='end' " +
                " font-size='" + this.fontSize + "' " +
                " x='" + x + "'" +
                " y='" + (barHeight / 2 + 7) + "'>" +
                text +
                "</text>";
    }

    private String getTextSvg(String text, int x) {
        return "<text" +
                " font-size='" + this.fontSize + "' " +
                " x='" + x + "'" +
                " y='" + (barHeight / 2 + 7) + "'>" +
                text +
                "</text>";
    }

    private String getBarSvg(int activeBarSize) {
        int x = barStartXOffset + (alignment == Alignment.LEFT ? 0 : maxBarWidth - activeBarSize);
        return "<rect" +
                " x='" + x + "'" +
                " y='2'" +
                " width='" + activeBarSize + "'" +
                " height='" + barHeight + "'" +
                " style='" + activeStyle + "fill:" + activeColor + "'" +
                "/>";
    }

    private String getBackgroundBarSvg(double backgroundBarSize) {
        return "<rect" +
                " x='" + barStartXOffset + "'" +
                " y='2'" +
                " width='" + ((int) backgroundBarSize) + "'" +
                " height='" + barHeight + "'" +
                " style='" + backgroundStyle + "fill:" + backgroundColor + "'" +
                "/>";
    }

    public String getBackgroundStyle() {
        return backgroundStyle;
    }

    public void setBackgroundStyle(String backgroundStyle) {
        this.backgroundStyle = backgroundStyle;
    }

    public String getActiveStyle() {
        return activeStyle;
    }

    public void setActiveStyle(String activeStyle) {
        this.activeStyle = activeStyle;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getMaxBarWidth() {
        return maxBarWidth;
    }

    public void setMaxBarWidth(int maxBarWidth) {
        this.maxBarWidth = maxBarWidth;
    }

    public int getBarHeight() {
        return barHeight;
    }

    public void setBarHeight(int barHeight) {
        this.barHeight = barHeight;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getActiveColor() {
        return activeColor;
    }

    public void setActiveColor(String activeColor) {
        this.activeColor = activeColor;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public int getBarStartXOffset() {
        return barStartXOffset;
    }

    public void setBarStartXOffset(int barStartXOffset) {
        this.barStartXOffset = barStartXOffset;
    }

    public enum Alignment {LEFT, RIGHT}
}
