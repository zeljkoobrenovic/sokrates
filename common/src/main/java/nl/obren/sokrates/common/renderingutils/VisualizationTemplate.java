/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;

public class VisualizationTemplate {
    private static final Log LOG = LogFactory.getLog(VisualizationTemplate.class);

    // Injected into a template's <head> (via the ${sokrates-inflate-lib} placeholder) so the
    // page can decode the inline, deflate+base64-compressed JSON that replaces ${data}. The
    // diagram data stays embedded in the single HTML file (works from file://), just compressed.
    // fflate.unzlibSync handles the zlib stream produced by java.util.zip.Deflater (default).
    private static final String INFLATE_LIB =
            "<script src=\"https://cdn.jsdelivr.net/npm/fflate@0.8.2/umd/index.js\"></script>\n" +
            "<script>\n" +
            "  function sokratesInflate(b64) {\n" +
            "    var bin = atob(b64);\n" +
            "    var bytes = new Uint8Array(bin.length);\n" +
            "    for (var i = 0; i < bin.length; i++) { bytes[i] = bin.charCodeAt(i); }\n" +
            "    return JSON.parse(fflate.strFromU8(fflate.unzlibSync(bytes)));\n" +
            "  }\n" +
            "</script>";

    // The head block (fflate + sokratesInflate helper) needed by any page that embeds compressed
    // inline JSON. Exposed for non-template producers (e.g. X3DomExporter) that build HTML directly.
    public static String inflateLib() {
        return INFLATE_LIB;
    }

    // Injected into a template's <head> (via ${sokrates-unzip-lib}) so a page can decode a whole
    // ZIP archive embedded inline as base64 — the same multi-entry archives the viewers used to
    // fetch() at runtime, now embedded so the report opens from file:// with no web server.
    // sokratesUnzip returns the { entryName: Uint8Array } map fflate.unzipSync produces, which the
    // viewers already consume (they key into it by ?file=/?key=). The page base64-decodes to bytes
    // and unzips; reuses the same fflate CDN script as INFLATE_LIB (load only one of the two libs).
    private static final String UNZIP_LIB =
            "<script src=\"https://cdn.jsdelivr.net/npm/fflate@0.8.2/umd/index.js\"></script>\n" +
            "<script>\n" +
            "  function sokratesUnzip(b64) {\n" +
            "    var bin = atob(b64);\n" +
            "    var bytes = new Uint8Array(bin.length);\n" +
            "    for (var i = 0; i < bin.length; i++) { bytes[i] = bin.charCodeAt(i); }\n" +
            "    return fflate.unzipSync(bytes);\n" +
            "  }\n" +
            "</script>";

    // The head block (fflate + sokratesUnzip helper) for pages embedding a whole zip archive inline.
    public static String embedZipLib() {
        return UNZIP_LIB;
    }

    // Base64-encode raw bytes (e.g. a built zip archive) for inline embedding in a template; the
    // page decodes them with the sokratesUnzip helper above.
    public static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    // Deflate (zlib) + base64 a JSON string for inline embedding; the page decodes it with the
    // sokratesInflate helper above.
    public static String deflateBase64(String json) {
        byte[] input = json.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, input.length / 4));
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            int n = deflater.deflate(buffer);
            out.write(buffer, 0, n);
        }
        deflater.end();
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    // Substitutes the data + inflate-lib placeholders: ${data} becomes a sokratesInflate("...")
    // call returning the (decompressed) data, and ${sokrates-inflate-lib} becomes the head lib.
    private static String embedCompressed(String content, String json) {
        String dataExpr = "sokratesInflate(\"" + deflateBase64(json) + "\")";
        return content.replace("${data}", dataExpr).replace("${sokrates-inflate-lib}", INFLATE_LIB);
    }

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

    // Renders a ${data}-template with the item JSON embedded compressed (deflate+base64) rather
    // than raw. The empty-children strip is applied to the JSON before compressing so the decoded
    // payload matches the previous raw output exactly.
    private String renderCompressed(String templateFileName, List<VisualizationItem> items) {
        try {
            String json = new JsonGenerator().generate(items).replace(",\"children\":[]", "");
            String content = IOUtils.toString(
                    this.getClass().getClassLoader().getResourceAsStream("vis_templates/" + templateFileName),
                    StandardCharsets.UTF_8);
            return embedCompressed(content, json);
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }

    public String renderBubbleChart(List<VisualizationItem> items) {
        return renderCompressed("bubble_chart.html", items);
    }

    public String renderTreeMap(List<VisualizationItem> items) {
        return renderCompressed("treemap.html", items);
    }

    // Renders a self-contained (inline-data) zoomable circles/sunburst page: the data is embedded
    // directly via SOKRATES_INLINE_DATA so the page does NOT fetch a zip. Used by callers that
    // render a single view (e.g. landscape sub-landscape circles), as opposed to the per-repository
    // family pages which embed a per-view archive (one JSON per view) and extract by ?key=.
    private String renderZoomableInline(String templateFileName, List<VisualizationItem> items) {
        // Embed the data compressed (deflate+base64), decoded in-browser by sokratesInflate — same
        // scheme as the rest of the report suite. The template defines sokratesInflate (fflate is
        // already loaded there). The embedded-archive placeholder is cleared (inline mode never
        // touches SOKRATES_ARCHIVE), so the literal placeholder never leaks into the page.
        String inline = "var SOKRATES_INLINE_DATA = sokratesInflate(\"" + deflateBase64(zoomableItemsJson(items)) + "\");";
        return rawTemplate(templateFileName)
                .replace("${sokrates-inline-data}", inline)
                .replace("${embedded-archive}", "");
    }

    public String renderZoomableCircles(List<VisualizationItem> items) {
        return renderZoomableInline("zoomable_circles.html", items);
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
        return renderCompressed("zoomable_circles_colored.html", items);
    }

    public String renderZoomableSunburst(List<VisualizationItem> items) {
        return renderZoomableInline("zoomable_sunburst.html", items);
    }

    public String render2DForceGraph(Force3DObject data) {
        try {
            String content = IOUtils.toString(
                    this.getClass().getClassLoader().getResourceAsStream("vis_templates/force_2d.html"),
                    StandardCharsets.UTF_8);
            return embedCompressed(content, new JsonGenerator().generate(data));
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }

    public String render3DForceGraph(Force3DObject data) {
        try {
            String content = IOUtils.toString(
                    this.getClass().getClassLoader().getResourceAsStream("vis_templates/force_3d.html"),
                    StandardCharsets.UTF_8);
            return embedCompressed(content, new JsonGenerator().generate(data));
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }

    public String renderRacingCharts(List<RacingChartItem> items, String startYear, String title) {
        try {
            String content = IOUtils.toString(
                    this.getClass().getClassLoader().getResourceAsStream("vis_templates/bar_chart_races.html"),
                    StandardCharsets.UTF_8);
            content = content.replace("${title}", title);
            content = content.replace("${startYear}", startYear + "");
            return embedCompressed(content, new JsonGenerator().generateCompressed(items));
        } catch (IOException e) {
            LOG.error(e);
        }
        return null;
    }
}
