/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.text.DecimalFormat;

public class RichTextRenderingUtils {
    @JsonIgnore
    private static int showMoreBlockId = 1;

    public static double getPercentage(double total, double count) {
        return (int) (count * 100 * 100 / total) / 100.0;
    }

    public static String renderNumber(int number) {
        DecimalFormat decimalFormat = new DecimalFormat("###,###");
        return decimalFormat.format(number);
    }

    public static String renderNumberStrong(int number) {
        DecimalFormat decimalFormat = new DecimalFormat("###,###");
        return "<b>" + decimalFormat.format(number) + "</b>";
    }

    public static String renderNumber(double number) {
        DecimalFormat format = new DecimalFormat("0.0");
        return format.format(number);
    }

    public static String renderNumberStrong(double number) {
        DecimalFormat format = new DecimalFormat("0.0");
        return "<b>" + format.format(number) + "</b>";
    }

    public static String renderNumberStrongWithSuffix(int number, String singularSuffix, String pluralSuffix) {
        DecimalFormat format = new DecimalFormat("###,###");
        return "<b>" + format.format(number) + "</b> " + (number == 1 ? singularSuffix : pluralSuffix);
    }

    public static String link(String title, String link) {
        return "<a href='" + link + "'>" + title + "</a>";
    }

    public static String getShowMoreParagraph(String visibleContent, String hiddenContent, String linkLabel) {
        StringBuilder content = new StringBuilder();
        String id = "showMoreBlock_" + showMoreBlockId++;
        content.append(visibleContent + " " + "<a href=\"javascript:showHide('" + id + "');\")>" + linkLabel + "</a>");
        content.append("<span id=\"" + id + "\" style=\"display: none;\">");
        content.append(hiddenContent);
        content.append("</span>");

        return content.toString();
    }

    public static String getStartShowMoreParagraph(String visibleContent, String linkLabel) {
        StringBuilder content = new StringBuilder();
        String id = "showMoreBlock_" + showMoreBlockId++;
        content.append(visibleContent + " " + "<a href=\"javascript:showHide('" + id + "');\")>" + linkLabel + "</a>");
        content.append("<span id=\"" + id + "\" style=\"display: none;\">");

        return content.toString();
    }

    public static String getStartShowMoreParagraphDisappear(String visibleContent, String linkLabel) {
        StringBuilder content = new StringBuilder();
        String id = "showMoreBlock_" + showMoreBlockId++;
        content.append("<span id='" + id + "_trigger' style='display: inline-block;'>"
                + visibleContent + " " + "<a href=\"javascript:showHideDisappear('" + id + "');\")>"
                + linkLabel + "</a></span>");
        content.append("<span id='" + id + "' style='display: none;'>");

        return content.toString();
    }

    public static String getEndShowMoreParagraph() {
        return "</span>";
    }

    public static String getLinkWikiStyle(String link) {
        link = link.trim();
        String title, url;
        if (link.contains(" ")) {
            title = link.substring(link.indexOf(" ") + 1).trim();
            url = link.substring(0, link.indexOf(" ")).trim();
        } else {
            title = link;
            url = link;
        }
        return "<a href='" + url + "'>" + title + "</a>";
    }

    public static String getStyledContent(String content, String style) {
        return "<span style=\"" + style + "\">" + content + "</style>";
    }

    public static String bold(String text) {
        return "<b>" + text + "</b>";
    }

    public static String bold(int number) {
        return bold(number + "");
    }

    public static String italic(String text) {
        return "<i>" + text + "</i>";
    }

    public static String paragraph(String context) {
        return "<p>" + context + "</p>";
    }
}
