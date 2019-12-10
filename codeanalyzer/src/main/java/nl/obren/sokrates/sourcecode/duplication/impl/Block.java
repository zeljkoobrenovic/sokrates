/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class Block {
    private List<Integer> lineIndexes = new ArrayList<>();
    private List<Block> allPossibleLineBlocks = new ArrayList<>();
    private List<SourceFile> files = new ArrayList<>();
    private String keyCache = null;

    public Block() {
    }

    public Block(List<Integer> lineIndexes) {
        this.lineIndexes = lineIndexes;
        getStringKey();
    }

    public List<SourceFile> getFiles() {
        return files;
    }

    public void setFiles(List<SourceFile> files) {
        this.files = files;
    }

    public List<Integer> getLineIndexes() {
        return lineIndexes;
    }

    public void setLineIndexes(List<Integer> lineIndexes) {
        this.lineIndexes = lineIndexes;
        keyCache = null;
        getStringKey();
    }

    public List<Block> extractAllPossibleSubBlocks(int subBlockSize) {
        if (subBlockSize < 1) {
            throw new IllegalArgumentException("The sub-block size has to be bigger than 0.");
        }

        allPossibleLineBlocks = new ArrayList<>();

        for (int i = 0; i <= lineIndexes.size() - subBlockSize; i++) {
            Block block = new Block();
            block.lineIndexes.addAll(lineIndexes.subList(i, i + subBlockSize));
            allPossibleLineBlocks.add(block);
        }


        return allPossibleLineBlocks;
    }

    public List<Block> getAllPossibleLineBlocks() {
        return allPossibleLineBlocks;
    }

    public void setAllPossibleLineBlocks(List<Block> allPossibleLineBlocks) {
        this.allPossibleLineBlocks = allPossibleLineBlocks;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Block)) {
            return false;
        }

        Block block = (Block) obj;
        if (block.lineIndexes.size() != lineIndexes.size()) {
            return false;
        }

        for (int i = 0; i < lineIndexes.size(); i++) {
            if (block.lineIndexes.get(i).intValue() != lineIndexes.get(i).intValue()) {
                return false;
            }
        }

        return true;
    }

    public String getStringKey() {
        if (keyCache != null) {
            return keyCache;
        }

        StringBuilder stringBuffer = new StringBuilder();

        lineIndexes.forEach(id -> stringBuffer.append(id).append(";"));

        keyCache = stringBuffer.toString();

        return keyCache;
    }
}
