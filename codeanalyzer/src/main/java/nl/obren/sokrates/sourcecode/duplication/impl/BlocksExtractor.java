/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

import nl.obren.sokrates.common.utils.ProgressFeedback;

import java.util.HashMap;
import java.util.Map;

public class BlocksExtractor {
    private Files files;
    private Map<Block, Block> uniqueBlocks = new HashMap<>();
    private Map<Block, Block> duplicateBlocks = new HashMap<>();
    private int minDuplicationBlockSize;
    private ProgressFeedback progressFeedback;

    public BlocksExtractor(Files files, int minDuplicationBlockSize, ProgressFeedback progressFeedback) {
        this.files = files;
        this.minDuplicationBlockSize = minDuplicationBlockSize;
        this.progressFeedback = progressFeedback;
    }

    public void extractMinimalBlocks() {
        if (progressFeedback != null) {
            progressFeedback.setText("Extracting minimal duplication blocks");
        }
        int progressValue[] = {0};
        files.getFiles().forEach(file -> {
            if (progressFeedback != null) {
                progressFeedback.progress(progressValue[0]++, files.getFiles().size());
            }
            file.extractBlocks(minDuplicationBlockSize).forEach(blockInFile -> {
                blockInFile.extractAllPossibleSubBlocks(minDuplicationBlockSize).forEach(b -> {
                    addBlockToMaps(b, file);
                });
            });
        });
        if (progressFeedback != null) {
            progressFeedback.progress(0, 0);
        }
    }

    private void addBlockToMaps(Block block, FileInfoForDuplication fileInfoForDuplication) {
        Block originalBlock = uniqueBlocks.get(block);
        if (originalBlock == null) {
            if (!block.getFiles().contains(fileInfoForDuplication.getSourceFile())) {
                block.getFiles().add(fileInfoForDuplication.getSourceFile());
            }
            uniqueBlocks.put(block, block);
        } else {
            duplicateBlocks.put(originalBlock, originalBlock);
            if (!originalBlock.getFiles().contains(fileInfoForDuplication.getSourceFile())) {
                originalBlock.getFiles().add(fileInfoForDuplication.getSourceFile());
            }
        }
    }

    public Files getFiles() {
        return files;
    }

    public void setFiles(Files files) {
        this.files = files;
    }

    public Map<Block, Block> getUniqueBlocks() {
        return uniqueBlocks;
    }

    public void setUniqueBlocks(Map<Block, Block> uniqueBlocks) {
        this.uniqueBlocks = uniqueBlocks;
    }

    public Map<Block, Block> getDuplicateBlocks() {
        return duplicateBlocks;
    }

    public void setDuplicateBlocks(Map<Block, Block> duplicateBlocks) {
        this.duplicateBlocks = duplicateBlocks;
    }

    public int getMinDuplicationBlockSize() {
        return minDuplicationBlockSize;
    }

    public void setMinDuplicationBlockSize(int minDuplicationBlockSize) {
        this.minDuplicationBlockSize = minDuplicationBlockSize;
    }
}
