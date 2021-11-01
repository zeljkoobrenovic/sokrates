/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;

public class Templates {
    private static final Log LOG = LogFactory.getLog(Templates.class);

    private static String RESOURCES_ROOT = "file:" + getResourceFolder().getPath();
    private static String TEMPLATES_ROOT = RESOURCES_ROOT + FileSystems.getDefault().getSeparator() + "templates" + FileSystems.getDefault().getSeparator();
    public static String EDITOR_TEMPLATE_PATH = TEMPLATES_ROOT + "editor.html";
    public static String CODE_PREVIEW_TEMPLATE_PATH = TEMPLATES_ROOT + "codepreview.html";

    private static File getResourceFolder() {
        File tempFolder = null;
        try {
            tempFolder = createTempFolder();
            RESOURCES_ROOT = "file:" + tempFolder.getPath();
            ClassLoader clazz = Templates.class.getClassLoader();
            InputStream inputStream = clazz.getResourceAsStream("editor.zip");
            UnzipToTempFilesUtil.unzip(inputStream, tempFolder);
        } catch (IOException e) {
            LOG.error(e);
        }

        return tempFolder;
    }

    private static File createTempFolder() throws IOException {
        File tempFolder = File.createTempFile("editor", "");
        tempFolder.delete();
        tempFolder.mkdirs();
        tempFolder.deleteOnExit();
        return tempFolder;
    }
}
