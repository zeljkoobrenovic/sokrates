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
        ClassLoader clazz = this.getClass().getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("templates/" + templateFileName);

        try {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            content = content.replace("${data}", new JsonGenerator().generate(data));

            return content;
        } catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }


}
