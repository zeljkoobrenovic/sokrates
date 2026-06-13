/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.Link;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class ReportRenderer {
    private static final Log LOG = LogFactory.getLog(ReportRenderer.class);

    private static String minimize(String html) {
        html = StringUtils.replace(html, "  ", " ");
        html = StringUtils.replace(html, "\n\n", "\n");
        return html;
    }

    public static String renderBreadcrumbsInDiv(List<Link> breadcrumbs) {
        StringBuilder content = new StringBuilder();
        if (breadcrumbs.size() > 0) {
            content.append("<div style='opacity: 0.6; font-size: 80%; margin-bottom: -8px; margin-top: 12px;'>");
            boolean first[] = {true};
            breadcrumbs.forEach(breadcrumb -> {
                if (!first[0]) {
                    content.append(" / ");
                }
                content.append("<a href='" + breadcrumb.getHref() + "'>" + breadcrumb.getLabel() + "</a>");
                first[0] = false;
            });
            content.append("</div>");
        }

        return content.toString();
    }

    public void render(RichTextReport richTextReport, ReportRenderingClient reportRenderingClient) {
        StringBuilder content = new StringBuilder();
        if (StringUtils.isNotBlank(richTextReport.getDisplayName())) {
            renderHeader(richTextReport, content);
        }
        reportRenderingClient.append(content.toString());
        richTextReport.getRichTextFragments().forEach(fragment -> {
            renderFragment(reportRenderingClient, fragment);
        });
    }

    private void renderHeader(RichTextReport richTextReport, StringBuilder content) {
        String parentUrl = richTextReport.getParentUrl();
        String parentUrlHtml = "<a href='" + parentUrl + "' style=\"font-size: 100%;text-decoration:none\">";

        content.append(renderBreadcrumbsInDiv(richTextReport.getBreadcrumbs()));
        content.append("<table>");
        content.append("<tr>");

        content.append("<td style='border: none'>");
        content.append("<div style='margin-top: 15px; padding-bottom: 15px; white-space: nowrap; overflow: hidden;'>");

        if (StringUtils.isNotBlank(parentUrl)) {
            content.append(parentUrlHtml);
        }
        content.append(renderLogo(richTextReport));
        if (StringUtils.isNotBlank(parentUrl)) {
            content.append("</a>");
        }
        content.append("</div>");

        content.append("</td>");

        content.append("<td style='border: none'>");
        if (StringUtils.isNotBlank(parentUrl)) {
            content.append(parentUrlHtml);
        }
        content.append("<div style='font-size: 48px; display: inline-block; vertical-align: middle;'>" +
                richTextReport.getDisplayName() + "</div>");
        if (StringUtils.isNotBlank(richTextReport.getDescription())) {
            content.append("<div style='color: #787878; font-size: 94%; margin-top: 2px; white-space: nowrap; overflow: hidden;'>" + richTextReport.getDescription() + "</div>");
        }
        if (StringUtils.isNotBlank(parentUrl)) {
            content.append("</a>");
        }
        content.append("</div>");
        content.append("</td>");

        content.append("</tr>");
        content.append("</table>");
    }

    private String renderLogo(RichTextReport richTextReport) {
        StringBuilder content = new StringBuilder();
        if (richTextReport.isRenderLogo()) {
            String logoLink = richTextReport.getLogoLink();
            if (StringUtils.isBlank(logoLink)) {
                logoLink = "https://zeljkoobrenovic.github.io/sokrates-media/icons/repository.png";
            }
            boolean complexHeader = richTextReport.getDisplayName().contains("<div");
            String valign = "middle";
            if (StringUtils.isNotBlank(logoLink)) {
                int size = 64;
                content.append("<img style='");
                if (complexHeader) {
                    // content.append("transform: scale(0.8); ");
                }
                content.append("height: " + size + "px' valign='" + valign + "' src='" +
                        logoLink + "'>\n");
            } else {

            }
        }

        return content.toString();
    }

    private void renderFragment(ReportRenderingClient reportRenderingClient, RichTextFragment fragment) {
        if (fragment.getType() == RichTextFragment.Type.GRAPHVIZ) {
            // Fragment content is a Mermaid flowchart definition, rendered client-side by mermaid.js
            // (loaded once via ReportConstants.REPORTS_HTML_HEADER). The definition is also kept in a
            // hidden <script> so the "download .mmd" link can build the file in the browser from the
            // already-embedded text — no .mmd files are written to disk.
            if (fragment.isShow()) {
                reportRenderingClient.append(mermaidBlock(fragment.getId(), fragment.getFragment()));
            }
        } else if (fragment.getType() == RichTextFragment.Type.SVG) {
            if (fragment.isShow()) {
                reportRenderingClient.append(minimize(fragment.getFragment()));
            }
        } else {
            if (fragment.isShow()) {
                reportRenderingClient.append(minimize(fragment.getFragment()));
            }
        }
    }

    // Wraps a Mermaid definition for client-side rendering. The definition is emitted verbatim
    // (NOT through minimize(), which would collapse newlines/spaces and break flowchart syntax).
    // A copy is also kept in a hidden <script> keyed by the graph id: mermaid.js replaces the <pre>
    // content with rendered SVG, so the original source must live somewhere it won't touch for the
    // client-side "download .mmd" link (see ReportConstants.downloadMermaid) to read it back.
    static String mermaidBlock(String id, String mermaidDefinition) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(id)) {
            sb.append("<script type=\"text/plain\" class=\"mermaid-source\" id=\"mermaid-source-")
                    .append(id).append("\">\n")
                    .append(mermaidDefinition).append("\n</script>\n");
        }
        sb.append("<pre class=\"mermaid\">\n").append(mermaidDefinition).append("\n</pre>\n");
        return sb.toString();
    }
}
