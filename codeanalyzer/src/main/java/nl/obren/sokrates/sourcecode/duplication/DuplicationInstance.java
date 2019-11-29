package nl.obren.sokrates.sourcecode.duplication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.FormattingUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DuplicationInstance {
    private static final int MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY = 10;
    private String displayContent = "";
    private List<DuplicatedFileBlock> duplicatedFileBlocks = new ArrayList<>();
    private int blockSize;

    public DuplicationInstance() {
    }

    public String getDisplayContent() {
        return displayContent;
    }

    public void setDisplayContent(String displayContent) {
        this.displayContent = displayContent;
    }

    public List<DuplicatedFileBlock> getDuplicatedFileBlocks() {
        return duplicatedFileBlocks;
    }

    public void setDuplicatedFileBlocks(List<DuplicatedFileBlock> duplicatedFileBlocks) {
        this.duplicatedFileBlocks = duplicatedFileBlocks;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(blockSize + " lines: '");

        duplicatedFileBlocks.forEach(block -> {
            stringBuilder.append(block.getSourceFile().getRelativePath() + "[" + block.getStartLine() + ":" + (block.getEndLine() + 1) + "]");
        });
        stringBuilder.append(displayContent);

        return stringBuilder.toString();
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    @JsonIgnore
    public String getLinesDisplayString() {
        StringBuilder stringBuilder = new StringBuilder();
        int i[] = {0};
        duplicatedFileBlocks.forEach(block -> {
            if (i[0] == MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY) {
                stringBuilder.append("\n");
                stringBuilder.append("...");
            } else if (i[0] < MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY) {
                if (!stringBuilder.toString().isEmpty()) {
                    stringBuilder.append("\n");
                }
                stringBuilder.append(block.getStartLine() + ":" + block.getEndLine() + " ");
                stringBuilder.append("(" + FormattingUtils.getFormattedPercentage(block.getPercentage()) + "%)");
            }
            i[0]++;
        });

        return stringBuilder.toString();
    }

    @JsonIgnore
    public String getSampleDisplayString() {
        String[] lines = displayContent.split("\n");
        String sample = "";

        for (int i = 0; i < Math.min(MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY, lines.length); i++) {
            String line = lines[i];
            sample += line + "\n";
        }
        if (lines.length > MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY) {
            sample += "...";
        }
        return sample;
    }

    @JsonIgnore
    public String getFoldersDisplayString() {
        StringBuilder stringBuilder = new StringBuilder();
        int i[] = {0};
        duplicatedFileBlocks.forEach(block -> {
            if (i[0] == MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY) {
                stringBuilder.append("\n");
                stringBuilder.append("...");
            } else if (i[0] < MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY) {
                if (!stringBuilder.toString().isEmpty()) {
                    stringBuilder.append("\n");
                }
                String relativeParent = new File(block.getSourceFile().getRelativePath()).getParent();
                String parent = StringUtils.defaultIfBlank(relativeParent, "ROOT");
                stringBuilder.append(parent);
            }
            i[0]++;
        });

        return stringBuilder.toString();
    }

    @JsonIgnore
    public String getFilesDisplayString(boolean linkToFiles) {
        StringBuilder stringBuilder = new StringBuilder();
        int i[] = {0};
        duplicatedFileBlocks.forEach(block -> {
            if (i[0] == MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY) {
                stringBuilder.append("\n");
                stringBuilder.append("...");
            } else if (i[0] < MAX_NUMBER_OF_SAMPLES_FOR_DISPLAY) {
                if (!stringBuilder.toString().isEmpty()) {
                    stringBuilder.append("\n");
                }
                if (linkToFiles) {
                    stringBuilder.append("<a href='");
                    stringBuilder.append("../src/main/" + block.getSourceFile().getRelativePath());
                    stringBuilder.append("' target='_blank'>");
                    stringBuilder.append(block.getSourceFile().getFile().getName());
                    stringBuilder.append("</a>");
                } else {
                    stringBuilder.append(block.getSourceFile().getFile().getName());
                }
            }
            i[0]++;
        });

        return stringBuilder.toString();
    }
}
