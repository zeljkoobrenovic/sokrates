package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Shared renderers for the landscape report "info block" cards (big metric cards, language cards,
 * small commit/contributor cards, and the clean Activity-tab trend cards). Both
 * {@link LandscapeReportGenerator} (Overview tab) and {@link LandscapeReportContributorsTab}
 * (Activity tab) render the same cards, so the HTML lives here once and each caller passes its
 * own {@link RichTextReport}.
 */
public class InfoBlocks {

    // Big metric card (e.g. "lines of main code"): white rounded card, icon on top, the per-metric
    // color used as an accent on the number rather than as a full background fill.
    public static void addInfoBlockWithColor(RichTextReport report, String mainValue, String subtitle, String color, String tooltip, String icon) {
        String style = "border-radius: 12px;";

        style += "border: 1px dashed #f0f0f0;";
        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 160px; height: 120px;";
        style += "background-color: #ffffff; text-align: center; vertical-align: middle; margin-bottom: 16px;";

        String accentColor = mainValue.equals("<b>0</b>") ? "grey" : toAccentColor(color);
        report.startDiv("display: inline-block; text-align: center", tooltip);
        report.addContentInDiv(ReportFileExporter.getIconSvg(icon, 48), "width: 160px; text-align: center; margin-top: 18px; margin-bottom: -12px");
        report.startDiv(style, tooltip);
        report.addHtmlContent("<div style='font-size: 50px; margin-top: 8px; color: " + accentColor + ";'>" + mainValue + "</div>");
        report.addHtmlContent("<div style='color: #434343; font-size: 15px;'>" + subtitle + "</div>");
        report.endDiv();
        report.endDiv();
    }

    // Turns a metric card's (pale) category color into a distinct, text-legible accent for the
    // card's number. The category pastels are nearly indistinguishable once darkened, so each known
    // category color is mapped to its own saturated accent hue; anything else falls back to a generic
    // darken-toward-black. Strips any trailing CSS declaration (e.g. "; opacity: 0.8") and passes
    // named colors through unchanged.
    public static String toAccentColor(String color) {
        if (color == null) {
            return "#434343";
        }
        String value = color.split(";")[0].trim();

        // Distinct accent per landscape metric category (keyed by the pale source constant).
        switch (value.toUpperCase()) {
            case "#EADDCA": // REPOSITORIES_COLOR (beige) -> amber/brown
                return "#b45309";
            case "#D6E4E1": // MAIN_LOC_COLOR (pale teal) -> teal
                return "#0f766e";
            case "#E0FFFF": // MAIN_LOC_FRESH_COLOR (light cyan) -> cyan/blue
                return "#0e7490";
            case "#F0F0F0": // TEST_LOC_COLOR (grey) -> slate
                return "#475569";
            case "#ADD8E6": // PEOPLE_COLOR (light blue) -> indigo
                return "#4338ca";
            default:
                break;
        }

        if (!value.matches("#[0-9a-fA-F]{6}")) {
            return value;
        }
        int r = Integer.parseInt(value.substring(1, 3), 16);
        int g = Integer.parseInt(value.substring(3, 5), 16);
        int b = Integer.parseInt(value.substring(5, 7), 16);
        double factor = 0.45; // scale toward black so the pastel reads as a deep, legible accent
        r = (int) Math.round(r * factor);
        g = (int) Math.round(g * factor);
        b = (int) Math.round(b * factor);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    public static void addLangInfoBlock(RichTextReport report, String value, String lang, String description) {
        String style = "margin: 4px 4px 4px 0px; display: inline-block; " +
                "width: 80px; height: 114px; cursor: help; " +
                "text-align: center; vertical-align: middle; margin-bottom: 16px;";

        report.addHtmlContent("<div class=\"infoBlock\" style=\"" + style + "\" title=\"" + description + "\">");

        report.addContentInDiv("", "margin-top: 8px");
        report.addHtmlContent(DataImageUtils.getLangDataImageDiv42(lang));
        report.addHtmlContent("<div style='font-size: 24px; margin-top: 8px;'>" + value + "</div>");
        report.addHtmlContent("<div class='infoBlockLabel'>" + lang + "</div>");
        report.endDiv();
    }

    public static void addLangInfoBlockExtra(RichTextReport report, String value, String lang, String description, String extra) {
        String style = "margin: 4px 4px 4px 0px; display: inline-block; " +
                "width: 80px; height: 114px; cursor: help; " +
                "text-align: center; vertical-align: middle; margin-bottom: 16px;";

        report.addHtmlContent("<div class=\"infoBlock\" style=\"" + style + "\" title=\"" + description + "\">");

        report.addContentInDiv("", "margin-top: 8px");
        report.addHtmlContent(DataImageUtils.getLangDataImageDiv42(lang));
        report.addHtmlContent("<div style='font-size: 24px; margin-top: 8px;'>" + value + "</div>");
        report.addHtmlContent("<div class='infoBlockLabel'>" + lang + "</div>");
        report.addHtmlContent("<div style='color: #9ca3af; font-size: 9px; margin-top: 1px;'>" + extra + "</div>");
        report.endDiv();
    }

    public static void addSmallInfoBlock(RichTextReport report, String value, String subtitle, String color, String link) {
        String style = "margin: 4px 4px 4px 0px;";
        style += "display: inline-block; width: 80px; height: 76px;";
        style += "text-align: center; vertical-align: middle; margin-bottom: 16px;";

        boolean linked = StringUtils.isNotBlank(link);
        report.addHtmlContent("<div class=\"infoBlock" + (linked ? " linked" : "") + "\" style=\"" + style + "\">");
        if (linked) {
            report.startNewTabLink(link, "text-decoration: none; color: inherit;");
        }
        report.addHtmlContent("<div style='font-size: 24px; margin-top: 8px;'>" + value + "</div>");
        report.addHtmlContent("<div class='infoBlockLabel'>" + subtitle + "</div>");
        if (linked) {
            report.endNewTabLink();
        }
        report.endDiv();
    }

    // Clean card matching the Activity tab look (icon on top, white rounded card, no border/shadow).
    public static void addActivityTrendCard(RichTextReport report, String value, String subtitle, String icon) {
        String style = "border-radius: 12px; margin: 0; display: inline-block; " +
                "width: 96px; background-color: #ffffff; " +
                "text-align: center; vertical-align: middle;";

        report.startDiv("display: inline-block; text-align: center;");
        report.addContentInDiv(ReportFileExporter.getIconSvg(icon, 36), "width: 96px; text-align: center; margin-bottom: -10px;");
        report.addHtmlContent("<div style=\"" + style + "\">");
        report.addHtmlContent("<div style='font-size: 30px; margin-top: 4px;'>" + value + "</div>");
        report.addHtmlContent("<div class='infoBlockLabel'>" + subtitle + "</div>");
        report.endDiv();
        report.endDiv();
    }
}
