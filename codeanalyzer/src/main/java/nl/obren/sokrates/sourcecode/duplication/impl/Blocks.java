/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Blocks {
    private Files files;
    private int minDuplicationBlockSize;
    private String progressText = "";
    private int currentProgressValue = 0;
    private int endProgressValue = 0;
    private boolean optimize = false;

    private Map<String, Pair<SourceFile, SourceFile>> filePairMap = new HashMap<>();

    private List<Block> blocksDuplicatedAmongFiles = new ArrayList<>();
    private List<Block> duplicatedFilePairs = new ArrayList<>();
    private List<Block> duplicateBlocks = new ArrayList<>();
    private ProgressFeedback progressFeedback;
    private Map<String, DuplicateRangePairs> fileRangePairs = new HashMap<>();

    private Map<String, DuplicationInstance> duplicationInstances = new HashMap<>();

    public Blocks(Files files, int minDuplicationBlockSize) {
        this.files = files;
        this.minDuplicationBlockSize = minDuplicationBlockSize;
    }

    public List<DuplicationInstance> extractDuplicatedBlocks(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
        BlocksExtractor blocksExtractor = new BlocksExtractor(files, minDuplicationBlockSize, progressFeedback);
        blocksExtractor.extractMinimalBlocks();

        this.duplicateBlocks = new ArrayList<>();
        blocksExtractor.getDuplicateBlocks().values().forEach(duplicateBlocks::add);

        findDuplicatedSharedAmongFiles();

        findDuplicatesAmongFiles();

        findDuplicatesWithinFiles();

        reportProgress("Populating duplication list");
        List<DuplicationInstance> result = new ArrayList<>();
        duplicationInstances.values().forEach(result::add);

        resetProgressValues(0);

        return result;
    }

    private void findDuplicatedSharedAmongFiles() {
        reportProgress("Identifying files with duplication");
        duplicateBlocks.stream().filter(b -> b.getFiles().size() > 1).forEach(blocksDuplicatedAmongFiles::add);
        createDuplicatedFilesMap();
        resetProgressValues(0);
    }

    private void createDuplicatedFilesMap() {
        resetProgressValues(blocksDuplicatedAmongFiles.size());
        reportProgress("Creating a map of duplicated files");
        blocksDuplicatedAmongFiles.forEach(db -> {
            reportProgressNextStep();
            if (db.getFiles().size() == 2) {
                duplicatedFilePairs.add(db);
                SourceFile sourceFile1 = db.getFiles().get(0);
                SourceFile sourceFile2 = db.getFiles().get(1);
                String pairKey1 = getPairKey(sourceFile1.getFile().getPath(), sourceFile2.getFile().getPath());
                String pairKey2 = getPairKey(sourceFile2.getFile().getPath(), sourceFile1.getFile().getPath());

                if (!(filePairMap.containsKey(pairKey1) || filePairMap.containsKey(pairKey2))) {
                    filePairMap.put(pairKey1, new ImmutablePair<>(sourceFile1, sourceFile2));
                }
            } else if (db.getFiles().size() > 2) {
                db.getFiles().forEach(f1 -> {
                    db.getFiles().stream().filter(f2 -> f1 != f2).forEach(f2 -> {
                        Block newBlock = new Block();
                        duplicatedFilePairs.add(newBlock);
                        newBlock.setLineIndexes(db.getLineIndexes());
                        newBlock.getFiles().add(f1);
                        newBlock.getFiles().add(f2);

                        String pairKey1 = getPairKey(f1.getFile().getPath(), f2.getFile().getPath());
                        String pairKey2 = getPairKey(f2.getFile().getPath(), f1.getFile().getPath());

                        if (!(filePairMap.containsKey(pairKey1) || filePairMap.containsKey(pairKey2))) {
                            filePairMap.put(pairKey1, new ImmutablePair(f1, f2));
                        }
                    });
                });
            }
        });
        resetProgressValues(0);
    }

    private String getPairKey(String path1, String path2) {
        return new StringBuilder()
                .append(path1)
                .append("::")
                .append(path2)
                .toString();
    }

    private void findDuplicatesAmongFiles() {
        resetProgressValues(filePairMap.size());
        reportProgress("Finding duplicates among files");

        filePairMap.values().forEach(pair -> {
            if (progressFeedback != null && progressFeedback.canceled()) {
                return;
            }
            reportProgressNextStep();
            SourceFile f1 = pair.getLeft();
            FileInfoForDuplication fileInfoForDuplication1 = files.getFilesMap().get(f1);

            SourceFile f2 = pair.getRight();


            FileInfoForDuplication fileInfoForDuplication2Copy = new FileInfoForDuplication();
            fileInfoForDuplication2Copy.setSourceFile(files.getFilesMap().get(f2).getSourceFile());
            fileInfoForDuplication2Copy.setBlocks(files.getFilesMap().get(f2).getBlocks());
            fileInfoForDuplication2Copy.getLineIDs().addAll(files.getFilesMap().get(f2).getLineIDs());

            int startBlockSize = optimize
                    ? minDuplicationBlockSize
                    : Math.min(fileInfoForDuplication1.getBiggestBlockSize(), fileInfoForDuplication2Copy.getBiggestBlockSize());

            for (int blockSize = startBlockSize; blockSize >= minDuplicationBlockSize; blockSize--) {
                addDuplicationInstances(fileInfoForDuplication1, fileInfoForDuplication2Copy, blockSize);
            }
        });
        resetProgressValues(0);
    }

    private void addDuplicationInstances(FileInfoForDuplication fileInfoForDuplication1,
                                        FileInfoForDuplication fileInfoForDuplication2,
                                        final int blockSize) {
        final List<Block> blocks = fileInfoForDuplication1.extractBlocks(blockSize);
        blocks.forEach(block1 -> {
            List<Block> allPossibleSubBlocks = block1.extractAllPossibleSubBlocks(blockSize);
            allPossibleSubBlocks.forEach(subBlock1 -> {
                DuplicationInstance instance = duplicationInstances.get(subBlock1.getStringKey());
                if (instance == null) {
                    instance = new DuplicationInstance();
                    instance.setBlockSize(blockSize);
                }

                Integer cleanedStartLine1 = fileInfoForDuplication1.indexesOf(subBlock1).get(0);
                addFileToDuplicationInstance(instance, fileInfoForDuplication1.getSourceFile(), cleanedStartLine1 + 1, blockSize);

                final DuplicationInstance currentInstance = instance;
                List<Integer> foundBlockIDs = fileInfoForDuplication2.indexesOf(subBlock1);
                if (foundBlockIDs.size() > 0) {
                    Integer cleanedStartLine2 = foundBlockIDs.get(0);

                    DuplicateRange range1 = new DuplicateRange(cleanedStartLine1, cleanedStartLine1 + blockSize - 1);
                    DuplicateRange range2 = new DuplicateRange(cleanedStartLine2, cleanedStartLine2 + blockSize - 1);

                    DuplicateRangePair pair1 = new DuplicateRangePair(range1, range2);
                    DuplicateRangePair pair2 = new DuplicateRangePair(range2, range1);

                    File file1 = fileInfoForDuplication1.getSourceFile().getFile();
                    File file2 = fileInfoForDuplication2.getSourceFile().getFile();
                    String key1 = getPairKey(file1.getPath(), file2.getPath());
                    String key2 = getPairKey(file2.getPath(), file1.getPath());

                    boolean alreadyIncluded = false;
                    DuplicateRangePairs duplicateRangePairs1 = fileRangePairs.get(key1);
                    DuplicateRangePairs duplicateRangePairs2 = fileRangePairs.get(key2);

                    if (duplicateRangePairs1 != null && duplicateRangePairs1.includes(pair1)) {
                        alreadyIncluded = true;
                    }

                    if (!alreadyIncluded && duplicateRangePairs2 != null && duplicateRangePairs2.includes(pair2)) {
                        alreadyIncluded = true;
                    }

                    if (!alreadyIncluded) {
                        duplicationInstances.put(subBlock1.getStringKey(), currentInstance);
                        addFileToDuplicationInstance(currentInstance, fileInfoForDuplication2.getSourceFile(), cleanedStartLine2 + 1, blockSize);

                        if (duplicateRangePairs1 == null) {
                            duplicateRangePairs1 = new DuplicateRangePairs();
                            fileRangePairs.put(key1, duplicateRangePairs1);
                        }
                        duplicateRangePairs1.getRanges().add(pair1);

                        if (duplicateRangePairs2 == null) {
                            duplicateRangePairs2 = new DuplicateRangePairs();
                            fileRangePairs.put(key2, duplicateRangePairs2);
                        }
                        duplicateRangePairs2.getRanges().add(pair2);
                    }
                }
            });
        });
    }

    private void findDuplicatesWithinFiles() {
        resetProgressValues(files.getFiles().size());
        reportProgress("Finding duplicates within files");
        files.getFiles().forEach(fileLineIndexes -> {
            if (progressFeedback != null && progressFeedback.canceled()) {
                return;
            }
            reportProgressNextStep();

            List<DuplicateRange> ranges = new ArrayList<>();

            FileInfoForDuplication copy = new FileInfoForDuplication();
            copy.setSourceFile(fileLineIndexes.getSourceFile());
            copy.setBlocks(fileLineIndexes.getBlocks());
            copy.getLineIDs().addAll(fileLineIndexes.getLineIDs());

            for (int blockSize = optimize ? minDuplicationBlockSize : copy.getBiggestBlockSize(); blockSize >= minDuplicationBlockSize; blockSize--) {
                final int currentBlockSize = blockSize;
                final List<Block> blocks = copy.extractBlocks(currentBlockSize);
                blocks.forEach(block -> {
                    block.extractAllPossibleSubBlocks(currentBlockSize).forEach(subBlock -> {
                        List<Integer> indexesOf = copy.indexesOf(subBlock);
                        if (indexesOf.size() > 1) {
                            DuplicationInstance instance = duplicationInstances.get(subBlock.getStringKey());
                            if (instance == null) {
                                instance = new DuplicationInstance();
                                instance.setBlockSize(currentBlockSize);
                            }
                            final DuplicationInstance currentInstance = instance;
                            indexesOf.forEach(index -> {
                                DuplicateRange range = new DuplicateRange(index, index + currentBlockSize - 1);
                                if (!alreadyIncludedInRange(ranges, range)) {
                                    addFileToDuplicationInstance(currentInstance, copy.getSourceFile(), index + 1, currentBlockSize);
                                    ranges.add(range);
                                }
                            });
                            if (instance.getDuplicatedFileBlocks().size() > 1) {
                                duplicationInstances.put(subBlock.getStringKey(), instance);
                            }
                        }
                        // copy.clearSubBlock(subBlock);
                    });
                });
            }
        });
        resetProgressValues(0);
    }

    private boolean alreadyIncludedInRange(List<DuplicateRange> ranges, DuplicateRange range) {
        for (DuplicateRange testRange : ranges) {
            if (testRange.includes(range)) {
                return true;
            }
        }

        return false;
    }

    private void addFileToDuplicationInstance(DuplicationInstance instance, SourceFile sourceFile, int cleanedStartLine, int blockSize) {
        DuplicatedFileBlock duplicatedFileBlock = new DuplicatedFileBlock();
        duplicatedFileBlock.setSourceFile(sourceFile);

        duplicatedFileBlock.setCleanedStartLine(cleanedStartLine);
        int cleanedEndLine = cleanedStartLine + blockSize - 1;
        duplicatedFileBlock.setCleanedEndLine(cleanedEndLine);

        CleanedContent cleanedContent = files.getPathToCleanedContent().get(sourceFile);
        duplicatedFileBlock.setStartLine(cleanedContent.getFileLineIndexes().get(cleanedStartLine - 1) + 1);
        duplicatedFileBlock.setEndLine(cleanedContent.getFileLineIndexes().get(cleanedEndLine - 1) + 1);

        duplicatedFileBlock.setSourceFileCleanedLinesOfCode(cleanedContent.getFileLineIndexes().size());

        if (!instance.getDuplicatedFileBlocks().contains(duplicatedFileBlock)) {
            instance.getDuplicatedFileBlocks().add(duplicatedFileBlock);
        }
    }

    private void reportProgressNextStep() {
        currentProgressValue++;
        reportProgress(currentProgressValue);
    }

    private void reportProgress(int currentValue) {
        currentProgressValue = currentValue;
        reportProgress(progressText);
    }

    private void reportProgress(String text) {
        this.progressText = text;
        if (progressFeedback != null) {
            progressFeedback.progress(currentProgressValue, endProgressValue);
            if (endProgressValue > 0) {
                progressFeedback.setText(progressText + " (" + currentProgressValue + " / " + endProgressValue + ")");
            } else {
                progressFeedback.setText(progressText);
            }
        }
    }

    private void resetProgressValues(int endValue) {
        currentProgressValue = 0;
        endProgressValue = endValue;
        reportProgress("");
    }
}
