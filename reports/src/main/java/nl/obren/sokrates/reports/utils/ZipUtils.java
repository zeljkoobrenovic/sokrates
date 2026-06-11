/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
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

    /**
     * Zips a single source file into {@code zipFile} under {@code entryName}, streaming its bytes
     * (never loading the whole file into memory). Use this instead of reading a file into a String
     * and calling {@link #stringToZipFile} when the file may be large — a String/byte[] cannot
     * exceed ~2 GB (Integer.MAX_VALUE), which is reachable e.g. by git-history.txt on huge repos.
     */
    public static void fileToZipFile(File zipFile, String entryName, File sourceFile) {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(sourceFile)) {
            zipOut.putNextEntry(new ZipEntry(entryName));
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zipOut.write(buffer, 0, len);
            }
            zipOut.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Zips every file under {@code sourceDir} (recursively) into {@code zipFile}, storing each with
     * its path relative to {@code sourceDir} (forward slashes) as the entry name and its raw bytes
     * (so binary files such as nested zips survive). The output {@code zipFile} is itself excluded
     * even if it lives under {@code sourceDir}.
     */
    public static void zipFolder(File sourceDir, File zipFile) {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {
            List<File> files = new ArrayList<>();
            collectFiles(sourceDir, files);
            String basePath = sourceDir.getCanonicalPath();
            String zipPath = zipFile.getCanonicalPath();
            for (File file : files) {
                if (file.getCanonicalPath().equals(zipPath)) {
                    continue;
                }
                String entryName = file.getCanonicalPath().substring(basePath.length() + 1).replace(File.separatorChar, '/');
                zipOut.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, len);
                    }
                }
                zipOut.closeEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void collectFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectFiles(child, out);
            } else {
                out.add(child);
            }
        }
    }

    /**
     * (Re)packages {@code sourceDir} into {@code zipFile}, merging with any pre-existing
     * {@code zipFile}: every loose file under {@code sourceDir} is added (overwriting same-named
     * entries), then entries from the previous zip that have no loose counterpart are carried over.
     * Everything is streamed byte-by-byte (via a temp zip that replaces {@code zipFile}), so no
     * entry is ever held in memory as a String/byte[] — safe for entries near the ~2 GB array limit.
     */
    public static void zipFolderMergingExistingZip(File sourceDir, File zipFile) {
        try {
            File tempZip = File.createTempFile("sokrates_data_", ".zip", sourceDir);
            java.util.Set<String> added = new java.util.HashSet<>();
            try (FileOutputStream fos = new FileOutputStream(tempZip);
                 ZipOutputStream zipOut = new ZipOutputStream(fos)) {
                // 1) loose files (recursive), excluding the existing zip and the temp zip.
                List<File> files = new ArrayList<>();
                collectFiles(sourceDir, files);
                String basePath = sourceDir.getCanonicalPath();
                String zipPath = zipFile.getCanonicalPath();
                String tempPath = tempZip.getCanonicalPath();
                byte[] buffer = new byte[8192];
                for (File file : files) {
                    String cp = file.getCanonicalPath();
                    if (cp.equals(zipPath) || cp.equals(tempPath)) {
                        continue;
                    }
                    String entryName = cp.substring(basePath.length() + 1).replace(File.separatorChar, '/');
                    zipOut.putNextEntry(new ZipEntry(entryName));
                    try (FileInputStream fis = new FileInputStream(file)) {
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, len);
                        }
                    }
                    zipOut.closeEntry();
                    added.add(entryName);
                }
                // 2) carry over previous zip entries not replaced by a loose file.
                if (zipFile.exists()) {
                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            if (entry.isDirectory() || added.contains(entry.getName())) {
                                continue;
                            }
                            zipOut.putNextEntry(new ZipEntry(entry.getName()));
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, len);
                            }
                            zipOut.closeEntry();
                            added.add(entry.getName());
                        }
                    }
                }
            }
            // Swap the temp zip over the target.
            if (zipFile.exists() && !zipFile.delete()) {
                throw new IOException("Could not replace " + zipFile.getPath());
            }
            if (!tempZip.renameTo(zipFile)) {
                throw new IOException("Could not rename " + tempZip.getPath() + " to " + zipFile.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    /**
     * In-memory equivalent of {@link #stringToZipFile(File, String[][])}: zips the {@code entries}
     * (each {@code {name, content}}) and returns the raw zip bytes, for embedding base64-inline in a
     * template (decoded client-side by {@code sokratesUnzip}) instead of writing a sibling .zip.
     */
    public static byte[] stringEntriesToZipBytes(String[][] entries) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(bos)) {
            for (String[] entry : entries) {
                if (entry.length == 2) {
                    zipOut.putNextEntry(new ZipEntry(entry[0]));
                    zipOut.write(entry[1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    zipOut.closeEntry();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bos.toByteArray();
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
