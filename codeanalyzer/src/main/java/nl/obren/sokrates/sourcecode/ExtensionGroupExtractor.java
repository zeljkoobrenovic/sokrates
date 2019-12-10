/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;

public class ExtensionGroupExtractor {
    private static final Log LOG = LogFactory.getLog(ExtensionGroupExtractor.class);

    private static final List<String> knownSourceExtensions = Arrays.asList(
            "java", "js", "jse", "jad", "abap", "scala",
            "ts", "go",
            "bas", "asm", "fs",
            "cs", "asmx", "m", "mm",
            "c", "h", "hh", "cpp", "cxx", "hpp",
            "html", "htm", "hta", "php", "phpt", "jsp", "jsf", "jspx", "tld", "asp", "aspx",
            "css", "cls", "less",
            "xml", "fmw", "as", "mxml", "springbeans", "xsd", "wsdl", "dtd", "cfm",
            "groovy", "rb", "py", "pyc", "pl", "do",
            "sql", "md", "properties", "yml", "yaml", "scss", "svg",
            "f", "f03", "f40", "f77", "f90", "f95",
            "bat", "sh", "bsh", "cql",
            "plist", "json", "thrift", "feature", "txt", "gradle", "xsl",
            "meta", "mat", "prefab", "asset", "unity",
            "shader", "cginc", "controller", "physicmaterial", "rendertexture",
            "cubemap", "mixer");
    private static final List<String> knownBinaryExtensions = Arrays.asList("3ds", "3g2", "3gp", "7z", "a", "aac", "adp",
            "ai", "aif", "aiff", "alz", "ape", "apk", "ar", "arj", "asf", "au", "avi", "bak", "bh", "bin", "bk", "bmp",
            "btif", "bz2", "bzip2", "cab", "caf", "cgm", "class", "cmx", "cpio", "cr2", "csv", "cur", "dat", "deb", "dex",
            "djvu", "dll", "dmg", "dng", "doc", "docm", "docx", "dot", "dotm", "dra", "DS_Store", "dsk", "dts", "dtshd",
            "dvb", "dwg", "dxf", "ecelp4800", "ecelp7470", "ecelp9600", "egg", "eol", "eot", "epub", "exe", "f4v", "fbs",
            "fh", "fla", "flac", "fli", "flv", "fpx", "fst", "fvt", "g3", "gif", "graffle", "gz", "gzip", "h261", "h263",
            "h264", "ico", "ief", "img", "ipa", "iso", "jar", "jpeg", "jpg", "jpgv", "jpm", "jxr", "key", "ktx", "lha",
            "lvp", "lz", "lzh", "lzma", "lzo", "m3u", "m4a", "m4v", "mar", "mdi", "mht", "mid", "midi", "mj2", "mka",
            "mkv", "mmr", "mng", "mobi", "mov", "movie", "mp3", "mp4", "mp4a", "mpeg", "mpg", "mpga", "mxu", "nef", "npx",
            "numbers", "o", "oga", "ogg", "ogv", "otf", "pages", "pbm", "pcx", "pdf", "pea", "pgm", "pic", "png", "pnm",
            "pot", "potm", "potx", "ppa", "ppam", "ppm", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", "psd", "pya", "pyc",
            "pyo", "pyv", "qt", "rar", "ras", "raw", "rgb", "rip", "rlc", "rmf", "rmvb", "rtf", "rz", "s3m", "s7z", "scpt",
            "sgi", "shar", "sil", "slk", "smv", "so", "sub", "swf", "tar", "tbz", "tbz2", "tga", "tgz", "thmx", "tif",
            "tiff", "tlz", "ttc", "ttf", "txz", "udf", "uvh", "uvi", "uvm", "uvp", "uvs", "uvu", "viv", "vob",
            "war", "wav", "wax", "wbmp", "wdp", "weba", "webm", "webp", "whl", "wim", "wm", "wma", "wmv", "wmx", "woff",
            "woff2", "wvx", "xbm", "xif", "xla", "xlam", "xls", "xlsb", "xlsm", "xlsx", "xlt", "xltm", "xltx", "xm", "xmind",
            "xpi", "xpm", "xwd", "xz", "z", "zip", "zipx");

    private static final List<String> knownIgnorableExtension = Arrays.asList(
            "ds_store", "iml", "ser");

    private Map<String, ExtensionGroup> extensionsMap = new HashMap<>();

    public static boolean isKnownSourceCodeExtension(String extension) {
        return LanguageAnalyzerFactory.getInstance().getAnalyzersMap().containsKey(extension.toLowerCase())
                || knownSourceExtensions.contains(extension.toLowerCase());
    }

    public static boolean isKnownBinaryExtension(String extension) {
        return knownBinaryExtensions.contains(extension.toLowerCase());
    }

    public static boolean isKnownIgnorableExtension(String extension) {
        return knownIgnorableExtension.contains(extension.toLowerCase());
    }

    public void extractExtensionsInfo(File root) {
        if (root.isDirectory()) {
            if (!root.getName().startsWith(".")) {
                for (File file : root.listFiles()) {
                    extractExtensionsInfo(file);
                }
            }
        } else {
            updateExtensionInfo(root);
        }
    }

    private void updateExtensionInfo(File file) {
        String extension = FilenameUtils.getExtension(file.getPath());
        if (!extension.isEmpty() && !isKnownBinaryExtension(extension) && !isKnownIgnorableExtension(extension)) {
            updateMap(file, extension);
        }
    }

    private void updateMap(File file, String extension) {
        if (extensionsMap.containsKey(extension)) {
            ExtensionGroup extensionGroup = extensionsMap.get(extension);
            extensionGroup.setNumberOfFiles(extensionGroup.getNumberOfFiles() + 1);
            extensionGroup.setTotalSizeInBytes((int) (extensionGroup.getTotalSizeInBytes() + FileUtils.sizeOf(file)));
        } else {
            int fileSizeInBytes = getFileSizeInBytes(file);
            if (fileSizeInBytes > 0) {
                ExtensionGroup extensionGroup = new ExtensionGroup(extension);
                extensionGroup.setNumberOfFiles(1);
                extensionGroup.setTotalSizeInBytes(fileSizeInBytes);
                extensionsMap.put(extension, extensionGroup);
            }
        }
    }

    private int getFileSizeInBytes(File file) {
        try {
            return (int) FileUtils.sizeOf(file);
        } catch (IllegalArgumentException e) {
            LOG.error(e);
        }
        return 0;
    }

    public List<ExtensionGroup> getExtensionsList() {
        List<ExtensionGroup> list = new ArrayList<>();
        extensionsMap.keySet().forEach(key -> {
            ExtensionGroup extensionGroup = extensionsMap.get(key);
            list.add(extensionGroup);
        });
        sort(list);

        return list;
    }

    private void sort(List<ExtensionGroup> list) {
        Collections.sort(list, (o1, o2) -> {
            if (isKnownSourceCodeExtension(o1.getExtension()) && !isKnownSourceCodeExtension(o2.getExtension())) {
                return -1;
            } else if (!isKnownSourceCodeExtension(o1.getExtension()) && isKnownSourceCodeExtension(o2.getExtension())) {
                return 1;
            }
            return Integer.compare(o2.getNumberOfFiles(), o1.getNumberOfFiles());
        });
    }

    public Map<String, ExtensionGroup> getExtensionsMap() {
        return extensionsMap;
    }

    public void setExtensionsMap(Map<String, ExtensionGroup> extensionsMap) {
        this.extensionsMap = extensionsMap;
    }
}

