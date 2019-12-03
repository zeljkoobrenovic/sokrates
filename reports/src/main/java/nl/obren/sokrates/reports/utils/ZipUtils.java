package nl.obren.sokrates.reports.utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    public static void stringToZipFile(File zipFile, String entryName, String content) {
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOut.putNextEntry(zipEntry);
            zipOut.write(content.getBytes());

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
}
