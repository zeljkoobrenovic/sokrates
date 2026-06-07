/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.Link;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        if (StringUtils.isNotBlank(richTextReport.getDescription())) {
            content.append("<p style=\"color: #787878; font-size: 94%; margin-top: 9px; white-space: nowrap; overflow: hidden;\">" + richTextReport.getDescription() + "</p>\n");
        }
        reportRenderingClient.append(content.toString());
        richTextReport.getRichTextFragments().forEach(fragment -> {
            renderFragment(reportRenderingClient, fragment);
        });
    }

    private void renderHeader(RichTextReport richTextReport, StringBuilder content) {
        content.append(renderBreadcrumbsInDiv(richTextReport.getBreadcrumbs()));
        content.append("<div style='font-size: 36px; margin-top: 15px; padding-bottom: 15px; white-space: nowrap; overflow: hidden;'>");

        String parentUrl = richTextReport.getParentUrl();
        if (StringUtils.isNotBlank(parentUrl)) {
            content.append("<a href='" + parentUrl + "' style=\"font-size: 100%;text-decoration:none\">\n");
        }

        content.append(renderLogo(richTextReport));

        content.append("<div style='margin-left: 4px; display: inline-block; vertical-align: middle; font-size: 120%'>" +
                richTextReport.getDisplayName() + "</div></div>\n");

        if (StringUtils.isNotBlank(parentUrl)) {
            content.append("</a>\n");
        }

        content.append("</div>\n");
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
            // Fragment content is now a Mermaid flowchart definition (rendered client-side by
            // mermaid.js, loaded once via ReportConstants.REPORTS_HTML_HEADER). When a visuals
            // folder + id are available, also write the definition as a downloadable .mmd file.
            if (shouldExportVisualToFile(reportRenderingClient, fragment)) {
                renderAndSaveVisuals(reportRenderingClient, fragment);
            } else if (fragment.isShow()) {
                reportRenderingClient.append(mermaidBlock(fragment.getFragment()));
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

    private void renderAndSaveVisuals(ReportRenderingClient reportRenderingClient, RichTextFragment fragment) {
        try {
            File folder = reportRenderingClient.getVisualsExportFolder();
            String id = fragment.getId();

            // Save the Mermaid definition so it can be downloaded / opened in the Mermaid editor.
            File mmdFile = new File(folder, id + ".mmd");
            FileUtils.write(mmdFile, fragment.getFragment(), StandardCharsets.UTF_8);

            if (fragment.isShow()) {
                reportRenderingClient.append(mermaidBlock(fragment.getFragment()) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Wraps a Mermaid definition for client-side rendering. The definition is emitted verbatim
    // (NOT through minimize(), which would collapse newlines/spaces and break flowchart syntax).
    private static String mermaidBlock(String mermaidDefinition) {
        return "<pre class=\"mermaid\">\n" + mermaidDefinition + "\n</pre>\n";
    }

    private boolean shouldExportVisualToFile(ReportRenderingClient reportRenderingClient, RichTextFragment fragment) {
        return reportRenderingClient.getVisualsExportFolder() != null && StringUtils.isNotBlank(fragment.getId());
    }
}
