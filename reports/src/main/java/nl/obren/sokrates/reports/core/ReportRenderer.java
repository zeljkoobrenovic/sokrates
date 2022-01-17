/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import nl.obren.sokrates.sourcecode.Link;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ReportRenderer {
    private static String minimize(String html) {
        html = StringUtils.replace(html, "  ", " ");
        html = StringUtils.replace(html, "\n\n", "\n");
        return html;
    }

    public void render(RichTextReport richTextReport, ReportRenderingClient reportRenderingClient) {
        StringBuilder content = new StringBuilder();
        if (StringUtils.isNotBlank(richTextReport.getDisplayName())) {
            renderHeader(richTextReport, content);
        }
        if (StringUtils.isNotBlank(richTextReport.getDescription())) {
            content.append("<p style=\"color: #787878; font-size: 94%; margin-top: 9px;\">" + richTextReport.getDescription() + "</p>\n");
        }
        reportRenderingClient.append(content.toString());
        richTextReport.getRichTextFragments().forEach(fragment -> {
            renderFragment(reportRenderingClient, fragment);
        });
    }

    private void renderHeader(RichTextReport richTextReport, StringBuilder content) {
        content.append(renderBreadcrumbsInDiv(richTextReport.getBreadcrumbs()));
        content.append("<h1>");
        String parentUrl = richTextReport.getParentUrl();
        if (StringUtils.isNotBlank(parentUrl)) {
            content.append("<a href='" + parentUrl + "' style=\"font-size: 110%;text-decoration:none\">\n");
        }
        if (StringUtils.isNotBlank(richTextReport.getLogoLink())) {
            int size = 39;
            String valign = richTextReport.getDisplayName().contains("<div") ? "middle" : "bottom";
            content.append("<img style='height: " + size + "px' valign='" + valign + "' src='" + richTextReport.getLogoLink() + "'>\n");
        }
        content.append(richTextReport.getDisplayName());
        if (StringUtils.isNotBlank(parentUrl)) {
            content.append("</a>\n");
        }
        content.append("</h1>\n");
    }

    public static String renderBreadcrumbsInDiv(List<Link> breadcrumbs) {
        StringBuilder content = new StringBuilder();
        if (breadcrumbs.size() > 0) {
            content.append("<div style='opacity: 0.6; font-size: 80%; margin-bottom: -12px'>");
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

    private void renderFragment(ReportRenderingClient reportRenderingClient, RichTextFragment fragment) {
        if (fragment.getType() == RichTextFragment.Type.GRAPHVIZ) {
            if (shouldExportVisualToFile(reportRenderingClient, fragment)) {
                renderAndSaveVisuals(reportRenderingClient, fragment);
            } else {
                if (fragment.isShow()) {
                    System.out.println("Rendering graphviz content: " + fragment.getId());
                    reportRenderingClient.append(minimize(GraphvizUtil.getSvgFromDot(fragment.getFragment()) + "\n"));
                }
            }
        } else if (fragment.getType() == RichTextFragment.Type.SVG) {
            if (fragment.isShow()) {
                reportRenderingClient.append(minimize(fragment.getFragment() + "\n"));
            }
        } else {
            if (fragment.isShow()) {
                reportRenderingClient.append(minimize(fragment.getFragment() + "\n"));
            }
        }
    }

    private void renderAndSaveVisuals(ReportRenderingClient reportRenderingClient, RichTextFragment fragment) {
        try {
            File folder = reportRenderingClient.getVisualsExportFolder();
            String id = fragment.getId();

            File dotFile = new File(folder, id + ".dot.txt");
            FileUtils.write(dotFile, fragment.getFragment(), StandardCharsets.UTF_8);

            System.out.println("Rendering graphviz file " + fragment.getId());
            String svgContent = minimize(GraphvizUtil.getSvgFromDot(fragment.getFragment()));

            if (fragment.isShow()) {
                reportRenderingClient.append(svgContent + "\n");
            }

            File svgFile = new File(folder, id + ".svg");
            FileUtils.write(svgFile, svgContent, StandardCharsets.UTF_8);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean shouldExportVisualToFile(ReportRenderingClient reportRenderingClient, RichTextFragment fragment) {
        return reportRenderingClient.getVisualsExportFolder() != null && StringUtils.isNotBlank(fragment.getId());
    }
}
