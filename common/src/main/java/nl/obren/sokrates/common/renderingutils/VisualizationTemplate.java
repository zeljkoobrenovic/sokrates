/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import nl.obren.sokrates.common.io.JsonGenerator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class VisualizationTemplate {
    private static final Log LOG = LogFactory.getLog(VisualizationTemplate.class);

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

    public String renderZoomableSunburst(List<VisualizationItem> items) {
        return render("zoomable_sunburst.html", items).replace(",\"children\":[]", "");
    }
}
