/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Blocks {
    private Files files;
    private int minDuplicationBlockSize;
    private String progressText = "";
    private AtomicInteger currentProgressValue = new AtomicInteger(0);
    private int endProgressValue = 0;
    private boolean optimize = true;

    private Map<String, Pair<SourceFile, SourceFile>> filePairMap = new HashMap<>();

    private List<Block> blocksDuplicatedAmongFiles = new ArrayList<>();
    private List<Block> duplicatedFilePairs = new ArrayList<>();
    private List<Block> duplicateBlocks = new ArrayList<>();
    private ProgressFeedback progressFeedback;
    // The two find loops below run in parallel; these maps are shared across worker threads.
    // ConcurrentHashMap gives safe publication, and the per-instance / per-key synchronisation in the
    // loop bodies guards the read-modify-write sequences that a concurrent map alone does not cover.
    private Map<String, DuplicateRangePairs> fileRangePairs = new ConcurrentHashMap<>();

    private Map<Block, DuplicationInstance> duplicationInstances = new ConcurrentHashMap<>();

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

        filePairMap.values().parallelStream().forEach(pair -> {
            if (progressFeedback != null && progressFeedback.canceled()) {
                return;
            }
            reportProgressNextStep();
            SourceFile f1 = pair.getLeft();
            SourceFile f2 = pair.getRight();

            // Both files must be thread-local copies: addDuplicationInstances calls extractBlocks() on
            // file1, which mutates that FileInfoForDuplication's internal blocks list. The shared originals
            // in files.getFilesMap() are read concurrently by every file-pair task, so mutating one in place
            // would corrupt the others (ConcurrentModificationException / lost blocks).
            FileInfoForDuplication fileInfoForDuplication1Copy = copyOf(files.getFilesMap().get(f1));
            FileInfoForDuplication fileInfoForDuplication2Copy = copyOf(files.getFilesMap().get(f2));

            addDuplicationInstances(fileInfoForDuplication1Copy, fileInfoForDuplication2Copy, minDuplicationBlockSize);
        });
        resetProgressValues(0);
    }

    // A thread-local copy with its own lineIDs and (crucially) its own blocks list, so per-task
    // extractBlocks() mutations never touch the shared original held in files.getFilesMap().
    private FileInfoForDuplication copyOf(FileInfoForDuplication original) {
        FileInfoForDuplication copy = new FileInfoForDuplication();
        copy.setSourceFile(original.getSourceFile());
        copy.getLineIDs().addAll(original.getLineIDs());
        return copy;
    }

    private void addDuplicationInstances(FileInfoForDuplication fileInfoForDuplication1,
                                         FileInfoForDuplication fileInfoForDuplication2,
                                         final int blockSize) {
        final List<Block> blocks = fileInfoForDuplication1.extractBlocks(blockSize);
        blocks.forEach(block1 -> {
            List<Block> allPossibleSubBlocks = block1.extractAllPossibleSubBlocks(blockSize);
            allPossibleSubBlocks.forEach(subBlock1 -> {
                List<Integer> foundBlockIDs = fileInfoForDuplication2.indexesOf(subBlock1);
                if (foundBlockIDs.isEmpty()) {
                    // No cross-file match: the original code discarded the instance it briefly built here,
                    // so there is nothing to record (and nothing to publish to the map).
                    return;
                }

                Integer cleanedStartLine1 = fileInfoForDuplication1.indexesOf(subBlock1).get(0);
                Integer cleanedStartLine2 = foundBlockIDs.get(0);

                DuplicateRange range1 = new DuplicateRange(cleanedStartLine1, cleanedStartLine1 + blockSize - 1);
                DuplicateRange range2 = new DuplicateRange(cleanedStartLine2, cleanedStartLine2 + blockSize - 1);

                DuplicateRangePair pair1 = new DuplicateRangePair(range1, range2);
                DuplicateRangePair pair2 = new DuplicateRangePair(range2, range1);

                File file1 = fileInfoForDuplication1.getSourceFile().getFile();
                File file2 = fileInfoForDuplication2.getSourceFile().getFile();
                String key1 = getPairKey(file1.getPath(), file2.getPath());
                String key2 = getPairKey(file2.getPath(), file1.getPath());

                // Atomic get-or-create keyed by block: a block is only ever recorded once a real cross-file
                // match exists, so creating the instance here (rather than earlier) preserves the original
                // semantics of never publishing match-less instances. Two threads sharing a block resolve to
                // the same instance and then serialise on it below.
                DuplicationInstance instance = duplicationInstances.computeIfAbsent(subBlock1, k -> {
                    DuplicationInstance created = new DuplicationInstance();
                    created.setBlockSize(blockSize);
                    return created;
                });

                // Serialise the read-modify-write on this block's instance and its range bookkeeping.
                // Different blocks lock on different instances and proceed concurrently.
                synchronized (instance) {
                    addFileToDuplicationInstance(instance, fileInfoForDuplication1.getSourceFile(), cleanedStartLine1 + 1, blockSize);

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
                        addFileToDuplicationInstance(instance, fileInfoForDuplication2.getSourceFile(), cleanedStartLine2 + 1, blockSize);

                        if (duplicateRangePairs1 == null) {
                            duplicateRangePairs1 = fileRangePairs.computeIfAbsent(key1, k -> new DuplicateRangePairs());
                        }
                        duplicateRangePairs1.getRanges().add(pair1);

                        if (duplicateRangePairs2 == null) {
                            duplicateRangePairs2 = fileRangePairs.computeIfAbsent(key2, k -> new DuplicateRangePairs());
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
        files.getFiles().parallelStream().forEach(fileLineIndexes -> {
            if (progressFeedback != null && progressFeedback.canceled()) {
                return;
            }
            reportProgressNextStep();

            // ranges is per-file local state, so it stays confined to this worker thread.
            List<DuplicateRange> ranges = new ArrayList<>();

            // Own blocks list (via copyOf) so extractBlocks() below does not mutate the shared original.
            FileInfoForDuplication copy = copyOf(fileLineIndexes);

            for (int blockSize = optimize ? minDuplicationBlockSize : copy.getBiggestBlockSize(); blockSize >= minDuplicationBlockSize; blockSize--) {
                final int currentBlockSize = blockSize;
                final List<Block> blocks = copy.extractBlocks(currentBlockSize);
                blocks.forEach(block -> {
                    block.extractAllPossibleSubBlocks(currentBlockSize).forEach(subBlock -> {
                        List<Integer> indexesOf = copy.indexesOf(subBlock);
                        if (indexesOf.size() > 1) {
                            // Mirror the original "keep only if it ends up with >1 blocks" gate: build on a
                            // local instance (or the one the cross-file pass already published for this block),
                            // then publish only if it crosses that threshold.
                            DuplicationInstance existing = duplicationInstances.get(subBlock);
                            DuplicationInstance instance = existing != null ? existing : new DuplicationInstance();
                            if (existing == null) {
                                instance.setBlockSize(currentBlockSize);
                            }
                            synchronized (instance) {
                                indexesOf.forEach(index -> {
                                    DuplicateRange range = new DuplicateRange(index, index + currentBlockSize - 1);
                                    if (!alreadyIncludedInRange(ranges, range)) {
                                        addFileToDuplicationInstance(instance, copy.getSourceFile(), index + 1, currentBlockSize);
                                        ranges.add(range);
                                    }
                                });
                                if (instance.getDuplicatedFileBlocks().size() > 1) {
                                    // putIfAbsent: if a concurrent thread published a rival instance for this
                                    // block meanwhile, fold our blocks into theirs so nothing is lost.
                                    DuplicationInstance published = duplicationInstances.putIfAbsent(subBlock, instance);
                                    if (published != null && published != instance) {
                                        mergeInto(published, instance);
                                    }
                                }
                            }
                        }
                        // copy.clearSubBlock(subBlock);
                    });
                });
            }
        });
        resetProgressValues(0);
    }

    // Folds the file blocks of a rival (never-published) instance into the one that won publication,
    // de-duplicating against what is already there. Locks the published instance to stay consistent with
    // the per-instance synchronisation used everywhere else; the source instance is thread-confined.
    private void mergeInto(DuplicationInstance published, DuplicationInstance source) {
        synchronized (published) {
            source.getDuplicatedFileBlocks().forEach(block -> {
                if (!published.getDuplicatedFileBlocks().contains(block)) {
                    published.getDuplicatedFileBlocks().add(block);
                }
            });
        }
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
        reportProgress(currentProgressValue.incrementAndGet());
    }

    private void reportProgress(int currentValue) {
        currentProgressValue.set(currentValue);
        reportProgress(progressText);
    }

    private void reportProgress(String text) {
        this.progressText = text;
        if (progressFeedback != null) {
            int current = currentProgressValue.get();
            progressFeedback.progress(current, endProgressValue);
            if (endProgressValue > 0) {
                progressFeedback.setText(progressText + " (" + current + " / " + endProgressValue + ")");
            } else {
                progressFeedback.setText(progressText);
            }
        }
    }

    private void resetProgressValues(int endValue) {
        currentProgressValue.set(0);
        endProgressValue = endValue;
        reportProgress("");
    }
}
