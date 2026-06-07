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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExplorerTemplate {
    private static final Log LOG = LogFactory.getLog(ExplorerTemplate.class);

    public String render(String templateFileName, Object data) {
        return render(templateFileName, data, "{}");
    }

    /**
     * Renders an explorer template, substituting the <code>${data}</code> placeholder with the JSON
     * serialization of <code>data</code> and the <code>${langIcons}</code> placeholder with the given
     * pre-built JSON object literal mapping languages/extensions to base64 image data URIs.
     */
    public String render(String templateFileName, Object data, String langIconsJson) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("langIcons", langIconsJson != null ? langIconsJson : "{}");
        return render(templateFileName, data, placeholders);
    }

    /**
     * Renders an explorer template, substituting the <code>${data}</code> placeholder with the JSON
     * serialization of <code>data</code> plus each entry of <code>extraPlaceholders</code> as a
     * <code>${key}</code> -&gt; value substitution (e.g. <code>langIcons</code>, <code>features</code>,
     * <code>options</code>). Values are inserted verbatim, so they should already be valid JSON/HTML.
     */
    public String render(String templateFileName, Object data, Map<String, String> extraPlaceholders) {
        ClassLoader clazz = this.getClass().getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("templates/" + templateFileName);

        try {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            // Embed the data compressed (deflate+base64) and inflate it in-browser, instead of
            // inlining raw JSON — the same self-contained, file://-friendly scheme used by the
            // visualization templates. The ${data} site becomes a sokratesInflate("...") call;
            // ${sokrates-inflate-lib} injects the fflate + helper head block.
            String dataExpr = "sokratesInflate(\"" + VisualizationTemplate.deflateBase64(new JsonGenerator().generateCompressed(data)) + "\")";
            content = content.replace("${data}", dataExpr);
            content = content.replace("${sokrates-inflate-lib}", VisualizationTemplate.inflateLib());
            if (extraPlaceholders != null) {
                for (Map.Entry<String, String> entry : extraPlaceholders.entrySet()) {
                    content = content.replace("${" + entry.getKey() + "}",
                            entry.getValue() != null ? entry.getValue() : "");
                }
            }

            return content;
        } catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }


}
