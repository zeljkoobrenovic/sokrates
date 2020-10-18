/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class FileInfoForDuplication {
    public static final int IGNORE_LINE_INDEX = -1;
    private SourceFile sourceFile;
    private List<Integer> lineIDs = new ArrayList();
    private List<Block> blocks = new ArrayList();

    public List<Block> extractBlocks(int minBlockSize) {
        blocks.clear();

        Block block = new Block();
        for (int i = 0; i < lineIDs.size(); i++) {
            Integer id = lineIDs.get(i);
            if (id == IGNORE_LINE_INDEX) {
                if (block.getLineIndexes().size() >= minBlockSize) {
                    blocks.add(block);
                }
                block = new Block();
            } else {
                block.getLineIndexes().add(id);
            }
        }
        if (block.getLineIndexes().size() >= minBlockSize) {
            blocks.add(block);
        }

        return blocks;
    }

    public int getBiggestBlockSize() {
        int blockSize = 0;

        for (Block block : blocks) {
            blockSize = Math.max(blockSize, block.getLineIndexes().size());
        }

        return blockSize;
    }

    public int getSmallestBlockSize(int minDuplicationBlockSize) {
        int blockSize = minDuplicationBlockSize;

        for (Block block : blocks) {
            if (block.getLineIndexes().size() >= minDuplicationBlockSize) {
                blockSize = Math.min(blockSize, block.getLineIndexes().size());
            }
        }

        return blockSize;
    }

    public List<Integer> indexesOf(Block block) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < lineIDs.size() - block.getLineIndexes().size() + 1; i++) {
            if (lineIDs.get(i).intValue() == block.getLineIndexes().get(0).intValue()) {
                boolean containsBlock = true;
                for (int j = 0; j < block.getLineIndexes().size(); j++) {
                    if (lineIDs.get(i + j).intValue() != block.getLineIndexes().get(j).intValue()) {
                        containsBlock = false;
                        break;
                    }
                }
                if (containsBlock) {
                    indexes.add(i);
                    i += block.getLineIndexes().size() - 1;
                }
            }
        }

        return indexes;
    }

    public void clearSubBlock(Block block) {
        List<Integer> indexes = indexesOf(block);

        indexes.forEach(index -> {
            for (int i = index; i < index + block.getLineIndexes().size(); i++) {
                getLineIDs().set(i, IGNORE_LINE_INDEX);
            }
        });
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public List<Integer> getLineIDs() {
        return lineIDs;
    }

    public void setLineIDs(List<Integer> lineIDs) {
        this.lineIDs = lineIDs;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        lineIDs.forEach(id -> stringBuilder.append(id).append(";"));

        return stringBuilder.toString();
    }
}
