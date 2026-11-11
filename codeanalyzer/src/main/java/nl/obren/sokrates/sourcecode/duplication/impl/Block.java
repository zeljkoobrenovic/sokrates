/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class Block {
    // Polynomial rolling-hash base. A prime keeps the distribution well spread; overflow on a 64-bit
    // long is intentional and acts as the modulus.
    private static final long HASH_BASE = 1000003L;

    private List<Integer> lineIndexes = new ArrayList<>();
    private List<SourceFile> files = new ArrayList<>();
    private String keyCache = null;
    private long hashKey;
    private boolean hashKeyComputed = false;

    public Block() {
    }

    public Block(List<Integer> lineIndexes) {
        this.lineIndexes = lineIndexes;
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
        hashKeyComputed = false;
    }

    public List<Block> extractAllPossibleSubBlocks(int subBlockSize) {
        if (subBlockSize < 1) {
            throw new IllegalArgumentException("The sub-block size has to be bigger than 0.");
        }

        List<Block> subBlocks = new ArrayList<>();
        int count = lineIndexes.size() - subBlockSize + 1;
        if (count <= 0) {
            return subBlocks;
        }

        // The highest power of HASH_BASE in a window, used to drop the leading element as the window slides.
        long topPower = 1;
        for (int i = 1; i < subBlockSize; i++) {
            topPower *= HASH_BASE;
        }

        // Seed the rolling hash for the first window, then slide it one element at a time so each
        // subsequent window costs O(1) instead of rebuilding the hash (or a string key) from scratch.
        long rollingHash = 0;
        for (int i = 0; i < subBlockSize; i++) {
            rollingHash = rollingHash * HASH_BASE + lineIndexes.get(i);
        }

        for (int i = 0; i < count; i++) {
            Block block = new Block();
            block.lineIndexes = new ArrayList<>(lineIndexes.subList(i, i + subBlockSize));
            block.hashKey = rollingHash;
            block.hashKeyComputed = true;
            subBlocks.add(block);

            if (i + subBlockSize < lineIndexes.size()) {
                rollingHash = (rollingHash - lineIndexes.get(i) * topPower) * HASH_BASE
                        + lineIndexes.get(i + subBlockSize);
            }
        }

        return subBlocks;
    }

    public long getHashKey() {
        if (!hashKeyComputed) {
            long hash = 0;
            for (Integer lineIndex : lineIndexes) {
                hash = hash * HASH_BASE + lineIndex;
            }
            hashKey = hash;
            hashKeyComputed = true;
        }
        return hashKey;
    }

    @Override
    public int hashCode() {
        long hash = getHashKey();
        return (int) (hash ^ (hash >>> 32));
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
