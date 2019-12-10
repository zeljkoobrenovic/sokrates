/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicationUtils {
    public static String getLinesAsString(List<String> lines, int startLine, int blockSize) {
        StringBuilder block = new StringBuilder();
        for (int lineIndex = startLine; lineIndex < startLine + blockSize; lineIndex++) {
            if (block.length() > 0) {
                block.append("\n");
            }
            block.append(lines.get(lineIndex));
        }
        return block.toString();
    }

    public static int indexOf(List<String> lines, List<String> linesToLookFor) {
        return indexOf(lines, linesToLookFor, 0);
    }

    public static int indexOf(List<String> lines, List<String> linesToLookFor, int startIndex) {
        for (int i = startIndex; i < lines.size() - linesToLookFor.size() + 1; i++) {
            boolean found = true;
            for (int j = 0; j < linesToLookFor.size(); j++) {
                if (!lines.get(i + j).equalsIgnoreCase(linesToLookFor.get(j))) {
                    found = false;
                    break;
                }
            }

            if (found) {
                return i;
            }
        }

        return -1;
    }

    public static int getNumberOfDuplicatedLines(List<DuplicationInstance> duplicationInstances) {
        Map<String, Integer> lines = new HashMap<>();

        duplicationInstances.forEach(duplicationInstance -> {
            duplicationInstance.getDuplicatedFileBlocks().forEach(block -> {
                int start = block.getCleanedStartLine();
                int end = block.getCleanedEndLine();
                for (int i = start; i <= end; i++) {
                    lines.put(block.getSourceFile().getRelativePath() + "::" + i, 1);
                }
            });
        });

        return lines.size();
    }

    public static int getTotalNumberOfCleanedLines(List<SourceFile> sourceFiles) {
        int count[] = {0};

        sourceFiles.forEach(sourceFile -> {
            count[0] += sourceFile.getCleanedLinesForDuplication().size();
        });

        return count[0];
    }

    public static Map<String, Integer> getTotalNumberOfCleanedLinesPerExtension(List<SourceFile> sourceFiles) {
        Map<String, Integer> map = new HashMap<>();

        sourceFiles.forEach(sourceFile -> {
            String extension = sourceFile.getExtension();
            int lineCount = sourceFile.getCleanedLinesForDuplication().size();
            map.put(extension, map.containsKey(extension) ? map.get(extension) + lineCount : lineCount);
        });

        return map;
    }

    public static int getTotalNumberOfCleanedLinesForLogicalComponent(List<SourceFile> sourceFiles, String componentName) {
        int count[] = {0};

        sourceFiles.forEach(sourceFile -> {
            sourceFile.getLogicalComponents().forEach(component -> {
                if (component.getName().equalsIgnoreCase(componentName)) {
                    count[0] += sourceFile.getCleanedLinesForDuplication().size();
                }
            });
        });

        return count[0];
    }
}
