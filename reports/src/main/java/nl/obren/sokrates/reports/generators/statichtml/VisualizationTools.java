package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;

public class VisualizationTools {
    public static void addDownloadLinks(RichTextReport report, String graphId) {
        report.startDiv("");
        report.addHtmlContent("Download: ");
        report.addNewTabLink("SVG", "visuals/" + graphId + ".svg");
        report.addHtmlContent(" ");
        report.addNewTabLink("DOT", "visuals/" + graphId + ".dot.txt");
        report.addHtmlContent(" ");
        report.addNewTabLink("(open online Graphviz editor)", "https://obren.io/tools/graphviz/");
        report.endDiv();
    }


}
