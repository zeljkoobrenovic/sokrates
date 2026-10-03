package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.ReportTheme;
import nl.obren.sokrates.reports.core.ReportRenderer;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;

public class VisualizationTools {
    public static void addDownloadLinks(RichTextReport report, String graphId) {
        report.startDiv("");
        report.addHtmlContent("Download: ");
        // The .mmd is built in the browser from the embedded diagram source (no file on disk).
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadMermaid('" + graphId + "');\">Mermaid (.mmd)</a>");
        report.addHtmlContent(" ");
        report.addNewTabLink("(open online Mermaid editor)", "https://obren.io/tools/mermaid/");
        report.endDiv();
    }

    // A self-contained HTML page that renders a Mermaid diagram client-side. Used for standalone
    // graph pages that were previously written as .svg files (and opened via a "new tab" link).
    public static String standaloneMermaidPage(String title, String mermaidDefinition) {
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<title>" + HtmlEscapeUtils.escape(title) + "</title>\n"
                + ReportTheme.headBlock()
                + "<style>pre.mermaid {background: var(--sk-canvas); border-radius: 8px; padding: 8px;}</style>\n"
                + "<script type=\"module\">\n"
                + "import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';\n"
                + "mermaid.initialize({ startOnLoad: true, securityLevel: 'strict', maxEdges: 1000, flowchart: { useMaxWidth: true } });\n"
                + "</script>\n</head>\n<body>\n"
                + "<pre class=\"mermaid\">\n" + ReportRenderer.escapeMermaidText(mermaidDefinition) + "\n</pre>\n"
                + "</body>\n</html>\n";
    }
}
