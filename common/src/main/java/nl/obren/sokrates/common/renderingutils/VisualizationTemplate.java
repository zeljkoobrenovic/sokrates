/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class VisualizationTemplate {
    private static final Log LOG = LogFactory.getLog(VisualizationTemplate.class);

    // Returns a visualization template verbatim (no ${...} substitution). Used for the static,
    // data-fetching templates (e.g. zoomable_circles.html / zoomable_sunburst.html) that load
    // their per-view data from a sibling .zip at runtime instead of embedding it at generation.
    public String rawTemplate(String templateFileName) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("vis_templates/" + templateFileName);
        try {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }

    public String render(String templateFileName, List<VisualizationItem> data) {
        ClassLoader clazz = this.getClass().getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("vis_templates/" + templateFileName);

        try {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            content = content.replace("${data}", new JsonGenerator().generate(data));

            return content;
        } catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }

    public String renderBubbleChart(List<VisualizationItem> items) {
        return render("bubble_chart.html", items).replace(",\"children\":[]", "");
    }

    public String renderTreeMap(List<VisualizationItem> items) {
        return render("treemap.html", items).replace(",\"children\":[]", "");
    }

    public String renderZoomableCircles(List<VisualizationItem> items) {
        return render("zoomable_circles.html", items).replace(",\"children\":[]", "");
    }

    // The JSON payload that the zoomable circles/sunburst templates embed as the tree's
    // "children". Circles and sunburst share the same item JSON; this is the value stored as a
    // zip entry when the per-view HTML files are collapsed into one template + one family zip.
    public static String zoomableItemsJson(List<VisualizationItem> items) {
        try {
            return new JsonGenerator().generate(items).replace(",\"children\":[]", "");
        } catch (IOException e) {
            LOG.error(e);
            return "[]";
        }
    }

    public String renderZoomableCirclesColored(List<VisualizationItem> items) {
        return render("zoomable_circles_colored.html", items).replace(",\"children\":[]", "");
    }

    public String renderZoomableSunburst(List<VisualizationItem> items) {
        return render("zoomable_sunburst.html", items).replace(",\"children\":[]", "");
    }

    public String render2DForceGraph(Force3DObject data) {
        ClassLoader clazz = this.getClass().getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("vis_templates/force_2d.html");

        try {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            content = content.replace("${data}", new JsonGenerator().generate(data));

            return content;
        } catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }

    public String render3DForceGraph(Force3DObject data) {
        ClassLoader clazz = this.getClass().getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("vis_templates/force_3d.html");

        try {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            content = content.replace("${data}", new JsonGenerator().generate(data));

            return content;
        } catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }
    public String renderRacingCharts(List<RacingChartItem> items, String startYear, String title) {
        ClassLoader clazz = this.getClass().getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("vis_templates/bar_chart_races.html");

        try {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            content = content.replace("${title}", title);
            content = content.replace("${startYear}", startYear + "");
            content = content.replace("${data}", new JsonGenerator().generateCompressed(items));

            return content;
        } catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }
}
