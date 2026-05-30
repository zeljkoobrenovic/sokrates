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
        ClassLoader clazz = this.getClass().getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("templates/" + templateFileName);

        try {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            content = content.replace("${data}", new JsonGenerator().generateCompressed(data));
            content = content.replace("${langIcons}", langIconsJson != null ? langIconsJson : "{}");

            return content;
        } catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }


}
