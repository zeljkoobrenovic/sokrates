/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.Concern;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicationAggregator {

    public static List<SourceFileDuplication> getDuplicationPerSourceFile(List<DuplicationInstance> duplicates) {
        List<SourceFileDuplication> duplications = new ArrayList<>();
        Map<String, SourceFileDuplication> map = new HashMap<>();

        duplicates.forEach(duplicate -> {
            duplicate.getDuplicatedFileBlocks().forEach(block -> {
                SourceFile sourceFile = block.getSourceFile();

                String path = sourceFile.getFile().getPath();
                SourceFileDuplication sourceFileDuplication = map.get(path);
                if (sourceFileDuplication == null) {
                    sourceFileDuplication = new SourceFileDuplication();
                    sourceFileDuplication.setSourceFile(sourceFile);
                    sourceFileDuplication.setCleanedLinesOfCode(block.getSourceFileCleanedLinesOfCode());
                    map.put(path, sourceFileDuplication);
                    duplications.add(sourceFileDuplication);
                }
                sourceFileDuplication.addLines(block.getCleanedStartLine(), block.getCleanedEndLine());
            });
        });

        return duplications;
    }

    public static List<AspectDuplication> getDuplicationPerLogicalComponent(List<LogicalDecomposition> logicalDecompositions,
                                                                            List<SourceFile> sourceFiles, List<SourceFileDuplication> duplicates) {
        List<AspectDuplication> duplications = new ArrayList<>();
        Map<String, AspectDuplication> map = new HashMap<>();

        duplicates.forEach(sourceFileDuplication -> {
            sourceFileDuplication.getSourceFile().getLogicalComponents().forEach(aspect -> {
                String displayName = aspect.getName();
                AspectDuplication aspectDuplication = map.get(displayName);
                if (aspectDuplication == null) {
                    aspectDuplication = new AspectDuplication();
                    aspectDuplication.setAspect(aspect);
                    aspectDuplication.setDuplicatedLinesOfCode(sourceFileDuplication.getDuplicatedLinesOfCode());
                    map.put(displayName, aspectDuplication);
                    duplications.add(aspectDuplication);
                } else {
                    aspectDuplication.setDuplicatedLinesOfCode(aspectDuplication.getDuplicatedLinesOfCode() +
                            sourceFileDuplication.getDuplicatedLinesOfCode());
                }
            });
        });

        map.keySet().forEach(component -> {
            map.get(component).setCleanedLinesOfCode(DuplicationUtils.getTotalNumberOfCleanedLinesForLogicalComponent(sourceFiles, component));
        });

        addLogicalComponentsWithoutDuplicates(logicalDecompositions, sourceFiles, duplications, map);


        return duplications;
    }

    private static void addLogicalComponentsWithoutDuplicates(List<LogicalDecomposition> logicalDecompositions, List<SourceFile> sourceFiles, List<AspectDuplication> duplications, Map<String,
            AspectDuplication> map) {
        logicalDecompositions.forEach(logicalDecomposition -> {
            logicalDecomposition.getComponents().forEach(logicalComponent -> {
                String displayName = logicalComponent.getName();
                if (!map.containsKey(displayName)) {
                    AspectDuplication aspectDuplication = new AspectDuplication();
                    aspectDuplication.setAspect(logicalComponent);
                    aspectDuplication.setDuplicatedLinesOfCode(0);
                    aspectDuplication.setCleanedLinesOfCode(DuplicationUtils.getTotalNumberOfCleanedLinesForLogicalComponent(sourceFiles, displayName));
                    duplications.add(aspectDuplication);
                }
            });
        });
    }

    private static void addConcernsWithoutDuplicates(List<Concern> concerns,
                                                     List<SourceFile> sourceFiles,
                                                     List<AspectDuplication> duplications,
                                                     Map<String, AspectDuplication> map) {
        concerns.forEach(concern -> {
            String displayName = concern.getName();
            if (!map.containsKey(displayName)) {
                AspectDuplication aspectDuplication = new AspectDuplication();
                aspectDuplication.setAspect(concern);
                aspectDuplication.setDuplicatedLinesOfCode(0);
                aspectDuplication.setCleanedLinesOfCode(DuplicationUtils.getTotalNumberOfCleanedLines(sourceFiles));
                duplications.add(aspectDuplication);
            }
        });
    }


    public static List<ExtensionDuplication> getDuplicationPerExtension(List<SourceFile> sourceFiles, List<SourceFileDuplication> duplicates) {
        List<ExtensionDuplication> duplications = new ArrayList<>();
        Map<String, ExtensionDuplication> map = new HashMap<>();

        duplicates.forEach(sourceFileDuplication -> {
            String extension = FilenameUtils.getExtension(sourceFileDuplication.getSourceFile().getFile().getPath());
            ExtensionDuplication extensionDuplication = map.get(extension);
            if (extensionDuplication == null) {
                extensionDuplication = new ExtensionDuplication();
                extensionDuplication.setExtension(extension);
                extensionDuplication.setDuplicatedLinesOfCode(sourceFileDuplication.getDuplicatedLinesOfCode());
                map.put(extension, extensionDuplication);
                duplications.add(extensionDuplication);
            } else {
                extensionDuplication.setDuplicatedLinesOfCode(extensionDuplication.getDuplicatedLinesOfCode() +
                        sourceFileDuplication.getDuplicatedLinesOfCode());
            }
        });

        Map<String, Integer> cleanedLinesMap = DuplicationUtils.getTotalNumberOfCleanedLinesPerExtension(sourceFiles);

        cleanedLinesMap.keySet().forEach(extension -> {
            Integer cleanedLinesCount = cleanedLinesMap.get(extension);
            if (map.containsKey(extension)) {
                map.get(extension).setCleanedLinesOfCode(cleanedLinesCount);
            } else {
                map.put(extension, new ExtensionDuplication(0, cleanedLinesCount));
            }
        });

        return duplications;
    }
}

