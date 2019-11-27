package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import nl.obren.sokrates.common.renderingutils.PlantUmlUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ReportRenderer {
    public void render(RichTextReport richTextReport, ReportRenderingClient reportRenderingClient) {
        StringBuilder content = new StringBuilder();
        content.append("<h1>");
        content.append("<a href='index.html' style=\"text-decoration:none\">\n");
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
            reportRenderingClient.append(GraphvizUtil.getSvgExternal(fragment.getFragment()) + "\n");
        } else if (fragment.getType() == RichTextFragment.Type.PLANTUML) {
            reportRenderingClient.append(PlantUmlUtil.getSvg(fragment.getFragment()) + "\n");
        } else if (fragment.getType() == RichTextFragment.Type.SVG) {
            reportRenderingClient.append(fragment.getFragment() + "\n");
        } else {
            reportRenderingClient.append(fragment.getFragment() + "\n");
        }
    }

    public List<Figure> getFigures(RichTextReport richTextReport) {
        List<Figure> figures = new ArrayList<>();
        richTextReport.getRichTextFragments().forEach(fragment -> {
            if (fragment.getType() == RichTextFragment.Type.GRAPHVIZ) {
                Figure figure = new Figure(fragment.getDescription(), GraphvizUtil.getSvgExternal(fragment.getFragment()) + "\n");
                figure.setSource(fragment.getFragment());
                figures.add(figure);
            } else if (fragment.getType() == RichTextFragment.Type.PLANTUML) {
                Figure figure = new Figure(fragment.getDescription(), PlantUmlUtil.getSvg(fragment.getFragment()) + "\n");
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
