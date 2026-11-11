/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class SourceCodeAspectUtils {
    private static final Log LOG = LogFactory.getLog(SourceCodeAspectUtils.class);
    public static final int MAX_SEARCH_DEPTH = 20;

    public static int getMaxLinesOfCode(List<? extends NamedSourceCodeAspect> aspects) {
        int maxFileLinesOfCode = 0;

        for (NamedSourceCodeAspect aspect : aspects) {
            maxFileLinesOfCode = Math.max(maxFileLinesOfCode, aspect.getLinesOfCode());
        }

        return maxFileLinesOfCode;
    }

    public static int getMaxFileCount(List<? extends NamedSourceCodeAspect> aspects) {
        int maxFileCount = 0;

        for (NamedSourceCodeAspect aspect : aspects) {
            maxFileCount = Math.max(maxFileCount, aspect.getSourceFiles().size());
        }

        return maxFileCount;
    }

    public static List<NamedSourceCodeAspect> getSourceCodeAspectBasedOnFolderDepth(String srcRoot, List<SourceFile>
            sourceFiles, int depth, int minComponentCount) {

        List<NamedSourceCodeAspect> aspects = new ArrayList<>();
        for (int currentDepth = Math.max(1, depth); currentDepth <= MAX_SEARCH_DEPTH; currentDepth++) {
            aspects = getComponentsBasedOnFolderDepth(srcRoot, sourceFiles, currentDepth);
            int count = aspects.size();
            if (count >= minComponentCount) {
                break;
            }
        }
        return aspects;
    }

    public static List<NamedSourceCodeAspect> getComponentsBasedOnFolderDepth(String srcRoot, List<SourceFile>
            sourceFiles, int depth) {
        List<String> paths = getUniquePaths(sourceFiles, depth);

        String greatestCommonPrefix = greatestCommonPrefix(paths);

        List<NamedSourceCodeAspect> aspects = new ArrayList<>();

        paths.forEach(path -> {
            String aspectName = path;
            if (!aspectName.equals(greatestCommonPrefix)) {
                aspectName = path.substring(greatestCommonPrefix.length());
            }
            aspectName = StringUtils.defaultIfBlank(aspectName, "ROOT");
            NamedSourceCodeAspect aspect = new NamedSourceCodeAspect(aspectName);
            aspect.getSourceFileFilters().add(new SourceFileFilter(folderPathPattern(path), ""));

            paths.forEach(otherPath -> {
                if (!path.equals(otherPath)) {
                    addExclusiveFilterIfNeeded(path, otherPath, srcRoot, aspect);
                }
            });

            aspects.add(aspect);
        });

        return aspects;
    }

    private static void addExclusiveFilterIfNeeded(String path, String otherPath, String srcRoot, NamedSourceCodeAspect
            aspect) {
        if (otherPath.startsWith(path)) {
            SourceFileFilter otherSourceFileFilter = new SourceFileFilter(folderPathPattern(otherPath), "");
            otherSourceFileFilter.setException(true);
            aspect.getSourceFileFilters().add(otherSourceFileFilter);
        }
    }

    /**
     * The filter of a folder-based component: filters match the path below the source root with a leading
     * separator ({@link SourceFileFilter#matchingPath}), so the pattern starts at the folder — the source
     * root (an unescaped, possibly absolute path) is no longer part of it.
     */
    static String folderPathPattern(String folderPath) {
        return ("/" + folderPath + "/.*").replace("//", "/");
    }

    public static List<String> getUniquePaths(List<SourceFile> sourceFiles, int depth) {
        // LinkedHashSet preserves first-seen order (as the previous list did) with O(1) dedup
        // instead of an O(n) contains() scan per file.
        Set<String> paths = new LinkedHashSet<>();
        sourceFiles.forEach(sourceFile -> paths.add(getFolderBasedComponentName(sourceFile, depth)));
        return new ArrayList<>(paths);
    }

    public static String getFolderBasedComponentName(SourceFile sourceFile, int depth) {
        String relativePath = sourceFile.getRelativePath().replace("\\", "/");
        String[] subFolders = relativePath.split("/");
        StringBuilder aspectPath = new StringBuilder();
        for (int i = 0; i < Math.min(depth, subFolders.length - 1); i++) {
            aspectPath.append(subFolders[i] + "/");
        }
        String componentName = aspectPath.toString().trim();
        if (componentName.length() > 1) {
            componentName = componentName.substring(0, componentName.lastIndexOf("/"));
        }
        return componentName;
    }

    public static String greatestCommonPrefix(List<String> strings) {
        if (strings.size() > 0) {
            int minLength[] = {Integer.MAX_VALUE};
            strings.forEach(string -> minLength[0] = Math.min(minLength[0], string.length()));

            for (int i = 0; i < minLength[0]; i++) {
                char c = strings.get(0).charAt(i);
                for (String string : strings) {
                    if (string.charAt(i) != c) {
                        return getWholeFolderNamesString(strings.get(0).substring(0, i));
                    }
                }
            }
            String commonPrefix = getWholeFolderNamesString(strings.get(0).substring(0, minLength[0]));
            return commonPrefix;
        }
        return "";
    }

    private static String getWholeFolderNamesString(String commonPrefix) {
        if (!StringUtils.containsAny(commonPrefix, "/", "\\")) {
            return "";
        }

        if (!StringUtils.endsWithAny(commonPrefix, "/", "\\") && StringUtils.containsAny(commonPrefix, "/", "\\")) {
            int lastIndexOfSeparator1 = commonPrefix.lastIndexOf("/");
            int lastIndexOfSeparator2 = commonPrefix.lastIndexOf("\\");
            int lastIndexOfSeparator = Math.max(lastIndexOfSeparator1, lastIndexOfSeparator2);
            commonPrefix = commonPrefix.substring(0, lastIndexOfSeparator + 1);
        }
        return commonPrefix;
    }

    // Strips the legacy "  *." decoration older analyses wrote into per-extension metric names
    // (e.g. "  *.java" -> "java"), so data produced before and after that change compares equal.
    public static String extensionName(String perExtensionName) {
        return perExtensionName == null ? "" : perExtensionName.replace("*.", "").trim().toLowerCase();
    }

    // One aspect per (lower-cased) file extension, named by the bare extension ("java", "ts"), sorted by LOC desc.
    @JsonIgnore
    public static List<NamedSourceCodeAspect> getAspectsPerExtensions(NamedSourceCodeAspect aspect) {
        Map<String, NamedSourceCodeAspect> map = new HashMap<>();

        aspect.getSourceFiles().forEach(sourceFile -> {
            String extension = ExtensionGroupExtractor.getExtension(sourceFile.getFile().getPath()).toLowerCase();
            NamedSourceCodeAspect extensionAspect = map.get(extension);
            if (extensionAspect == null) {
                extensionAspect = new NamedSourceCodeAspect(extension);
                map.put(extension, extensionAspect);
            }
            extensionAspect.getSourceFiles().add(sourceFile);
        });

        List<NamedSourceCodeAspect> list = new ArrayList<>();
        map.values().forEach(list::add);
        Collections.sort(list, (o1, o2) -> o1.getLinesOfCode() > o2.getLinesOfCode() ? -1 : (o1.getLinesOfCode() < o2.getLinesOfCode() ? 1 : 0));

        return list;
    }
}
