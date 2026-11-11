/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.duplication;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry in a duplicate {@code src/fragments/<type>.json} bundle, consumed by
 * {@code src/viewer.html}'s {@code renderDuplicate}. The JSON field names ({@code ext} and
 * {@code blocks[]} with {@code file}/{@code from}/{@code to}/{@code code}) are a contract with
 * the viewer's render logic.
 */
public class DuplicateFragmentExport {
    private String ext = "";
    private List<Block> blocks = new ArrayList<>();

    public DuplicateFragmentExport() {
    }

    public DuplicateFragmentExport(String ext) {
        this.ext = ext;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public void addBlock(String file, int from, int to, String code) {
        this.blocks.add(new Block(file, from, to, code));
    }

    public static class Block {
        private String file = "";
        private int from = 0;
        private int to = 0;
        private String code = "";

        public Block() {
        }

        public Block(String file, int from, int to, String code) {
            this.file = file;
            this.from = from;
            this.to = to;
            this.code = code;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public int getFrom() {
            return from;
        }

        public void setFrom(int from) {
            this.from = from;
        }

        public int getTo() {
            return to;
        }

        public void setTo(int to) {
            this.to = to;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
