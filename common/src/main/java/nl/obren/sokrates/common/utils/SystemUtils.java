/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

public class SystemUtils {
    private static final Log LOG = LogFactory.getLog(SystemUtils.class);

    private SystemUtils() {
    }

    public static String getSafeFileName(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        for (char c : string.toCharArray()) {
            if (c == '.' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            } else {
                sb.append("_");
            }
        }
        return sb.toString();
    }
    public static void openFile(String path) {
        exec(new String[]{"open", path}, null);
    }

    public static void openFile(File file) {
        openFile(file.getPath());
    }

    public static Process exec(final String args[], final File directory) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            processBuilder.directory(directory);
            processBuilder.redirectErrorStream(false);
            final Process process = processBuilder.start();

            return process;
        } catch (IOException e) {
            LOG.error(e);
        }

        return null;
    }

    public static void createAndOpenTempFile(String content) {
        try {
            File file = File.createTempFile("temp_", ".html");
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
            openFile(file);
        } catch (IOException e) {
            LOG.error(e);
        }
    }


}
