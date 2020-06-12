/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    public static void stringToZipFile(File zipFile, String entryName, String content) {
        stringToZipFile(zipFile, new String[][]{{entryName, content}});
    }

    public static void stringToZipFile(File zipFile, String[][] entries) {
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            for (String[] entry : entries) {
                if (entry.length == 2) {
                    ZipEntry zipEntry = new ZipEntry(entry[0]);
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(entry[1].getBytes());
                }
            }

            zipOut.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String unzipFirstEntryAsString(File zipFile) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry = zis.getNextEntry();
            StringBuilder stringBuilder = new StringBuilder();
            if (zipEntry != null) {
                int len;
                byte[] buffer = new byte[1024];
                while ((len = zis.read(buffer)) > 0) {
                    for (int i = 0; i < len; i++) {
                        stringBuilder.append((char) buffer[i]);
                    }
                }
            }
            zis.closeEntry();
            zis.close();

            String content = stringBuilder.toString();

            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Map<String, ZipEntryContent> unzipAllEntriesAsStrings(File zipFile) {
        Map<String, ZipEntryContent> entries = new HashMap<>();
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                StringBuilder stringBuilder = new StringBuilder();
                if (zipEntry != null) {
                    int len;
                    byte[] buffer = new byte[1024];
                    while ((len = zis.read(buffer)) > 0) {
                        for (int i = 0; i < len; i++) {
                            stringBuilder.append((char) buffer[i]);
                        }
                    }
                }
                String name = zipEntry.getName();
                entries.put(name, new ZipEntryContent(name, stringBuilder.toString()));
            }
            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return entries;
    }
}
