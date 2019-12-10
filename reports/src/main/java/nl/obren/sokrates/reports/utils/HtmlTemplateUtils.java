/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HtmlTemplateUtils {
    public static String getResource(String resourcePath) {
        InputStream in = HtmlTemplateUtils.class.getResourceAsStream(resourcePath);
        if (in == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line;
        StringBuilder body = new StringBuilder();
        while (true) {
            try {
                if (!((line = reader.readLine()) != null)) break;
                body.append(line + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return body.toString();
    }
}
