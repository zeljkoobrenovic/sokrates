/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReportRenderer {
    public void render(RichTextReport richTextReport, ReportRenderingClient reportRenderingClient) {
        StringBuilder content = new StringBuilder();
        content.append("<h1>");
        String parentUrl = StringUtils.isNotBlank(richTextReport.getParentUrl()) ? richTextReport.getParentUrl() : "index.html";
        content.append("<a href='" + parentUrl + "' style=\"text-decoration:none\">\n");
        if (StringUtils.isNotBlank(richTextReport.getLogoLink())) {
            int size = richTextReport.getDisplayName().contains("<div") ? 24 : 36;
            String valign = richTextReport.getDisplayName().contains("<div") ? "middle" : "bottom";
            content.append("<img style='height: " + size + "px' valign='" + valign + "' src='" + richTextReport.getLogoLink() + "'>\n");
        }
        content.append(richTextReport.getDisplayName());
        content.append("</a>\n");
        content.append("</h1>\n");
        content.append("<p style=\"font-style: italic; color: gray\">" + richTextReport.getDescription() + "</p>\n");
        reportRenderingClient.append(content.toString());
        richTextReport.getRichTextFragments().forEach(fragment -> {
            renderFragment(reportRenderingClient, fragment);
        });
    }

    private void renderFragment(ReportRenderingClient reportRenderingClient, RichTextFragment fragment) {
        if (fragment.getType() == RichTextFragment.Type.GRAPHVIZ) {
            if (shouldExportVisualToFile(reportRenderingClient, fragment)) {
                renderAndSaveVisuals(reportRenderingClient, fragment);
            } else {
                if (fragment.isShow()) {
                    reportRenderingClient.append(GraphvizUtil.getSvgFromDot(fragment.getFragment()) + "\n");
                }
            }
        } else if (fragment.getType() == RichTextFragment.Type.SVG) {
            if (fragment.isShow()) {
                reportRenderingClient.append(fragment.getFragment() + "\n");
            }
        } else {
            if (fragment.isShow()) {
                reportRenderingClient.append(fragment.getFragment() + "\n");
            }
        }
    }

    private void renderAndSaveVisuals(ReportRenderingClient reportRenderingClient, RichTextFragment fragment) {
        try {
            File folder = reportRenderingClient.getVisualsExportFolder();
            String id = fragment.getId();

            File dotFile = new File(folder, id + ".dot.txt");
            FileUtils.write(dotFile, fragment.getFragment(), StandardCharsets.UTF_8);

            String svgContent = GraphvizUtil.getSvgFromDot(fragment.getFragment());

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

    public List<Figure> getFigures(RichTextReport richTextReport) {
        List<Figure> figures = new ArrayList<>();
        richTextReport.getRichTextFragments().forEach(fragment -> {
            if (fragment.getType() == RichTextFragment.Type.GRAPHVIZ) {
                Figure figure = new Figure(fragment.getDescription(), GraphvizUtil.getSvgFromDot(fragment.getFragment()) + "\n");
                figure.setSource(fragment.getFragment());
                figures.add(figure);
            } else if (fragment.getType() == RichTextFragment.Type.SVG) {
                Figure figure = new Figure(fragment.getDescription(), fragment.getFragment() + "\n");
                figure.setSource(fragment.getFragment());
                figures.add(figure);
            }
        });

        return figures;
    }
}
