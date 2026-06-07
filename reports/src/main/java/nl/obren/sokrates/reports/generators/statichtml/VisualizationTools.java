package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;

public class VisualizationTools {
    public static void addDownloadLinks(RichTextReport report, String graphId) {
        report.startDiv("");
        report.addHtmlContent("Download: ");
        report.addNewTabLink("Mermaid (.mmd)", "visuals/" + graphId + ".mmd");
        report.addHtmlContent(" ");
        report.addNewTabLink("(open online Mermaid editor)", "https://obren.io/tools/mermaid/");
        report.endDiv();
    }

    // A self-contained HTML page that renders a Mermaid diagram client-side. Used for standalone
    // graph pages that were previously written as .svg files (and opened via a "new tab" link).
    public static String standaloneMermaidPage(String title, String mermaidDefinition) {
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<title>" + title + "</title>\n"
                + "<script type=\"module\">\n"
                + "import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';\n"
                + "mermaid.initialize({ startOnLoad: true, securityLevel: 'loose', maxEdges: 1000, flowchart: { useMaxWidth: true } });\n"
                + "</script>\n</head>\n<body>\n"
                + "<pre class=\"mermaid\">\n" + mermaidDefinition + "\n</pre>\n"
                + "</body>\n</html>\n";
    }
}
