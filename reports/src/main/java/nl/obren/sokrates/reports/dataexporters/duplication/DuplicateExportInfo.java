package nl.obren.sokrates.reports.dataexporters.duplication;

import java.util.ArrayList;
import java.util.List;

public class DuplicateExportInfo {
    private int blockSize;
    private List<DuplicateFileBlockExportInfo> duplicatedFileBlocks = new ArrayList<>();

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public List<DuplicateFileBlockExportInfo> getDuplicatedFileBlocks() {
        return duplicatedFileBlocks;
    }

    public void setDuplicatedFileBlocks(List<DuplicateFileBlockExportInfo> duplicatedFileBlocks) {
        this.duplicatedFileBlocks = duplicatedFileBlocks;
    }
}
