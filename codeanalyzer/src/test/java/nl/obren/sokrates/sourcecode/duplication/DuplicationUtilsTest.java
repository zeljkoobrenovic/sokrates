/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DuplicationUtilsTest {
    @Test
    public void getNumberOfDuplicatedLines() throws Exception {
        List<DuplicationInstance> duplicationInstances = new ArrayList<>();

        assertEquals(DuplicationUtils.getNumberOfDuplicatedLines(duplicationInstances), 0);

        duplicationInstances.add(getDuplicationInstance("folder1", 100, 200, 50));
        assertEquals(DuplicationUtils.getNumberOfDuplicatedLines(duplicationInstances), 200);

        // check that lines in multiple blocks are not counted twice
        duplicationInstances.add(getDuplicationInstance("folder1", 100, 200, 50));
        assertEquals(DuplicationUtils.getNumberOfDuplicatedLines(duplicationInstances), 200);

        duplicationInstances.add(getDuplicationInstance("folder2", 100, 200, 50));
        assertEquals(DuplicationUtils.getNumberOfDuplicatedLines(duplicationInstances), 200);

        // check that lines in multiple blocks are not counted twice
        duplicationInstances.add(getDuplicationInstance("folder1", 100, 200, 500));
        assertEquals(DuplicationUtils.getNumberOfDuplicatedLines(duplicationInstances), 300);
    }

    private DuplicationInstance getDuplicationInstance(String pathPrefix, int blockSize, int startLine1, int startLine2) {
        DuplicationInstance duplicationInstance = new DuplicationInstance();

        DuplicatedFileBlock duplicatedFileBlock1 = new DuplicatedFileBlock();
        duplicatedFileBlock1.setSourceFile(new SourceFile(new File(pathPrefix + "1", " ")));
        duplicatedFileBlock1.setCleanedStartLine(startLine1);
        duplicatedFileBlock1.setCleanedEndLine(startLine1 + blockSize - 1);
        duplicationInstance.getDuplicatedFileBlocks().add(duplicatedFileBlock1);

        DuplicatedFileBlock duplicatedFileBlock2 = new DuplicatedFileBlock();
        duplicatedFileBlock2.setSourceFile(new SourceFile(new File(pathPrefix + "2", " ")));
        duplicatedFileBlock2.setCleanedStartLine(startLine2);
        duplicatedFileBlock2.setCleanedEndLine(startLine2 + blockSize - 1);
        duplicationInstance.getDuplicatedFileBlocks().add(duplicatedFileBlock2);

        return duplicationInstance;
    }

    @Test
    public void indexOf() throws Exception {
        List<String> lines = Arrays.asList("A", "B", "C", "D", "E", "F");

        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("A")), 0);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("B")), 1);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("C")), 2);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("F")), 5);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("X")), -1);

        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("A", "B")), 0);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("B", "C", "D")), 1);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("B", "C", "D", "A")), -1);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("C", "D")), 2);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("E", "F")), 4);
        assertEquals(DuplicationUtils.indexOf(lines, Arrays.asList("X", "Y")), -1);

    }

    @Test
    public void getLinesAsString() throws Exception {
        List<String> lines = Arrays.asList("A", "B", "C", "D", "E", "F");

        assertEquals(DuplicationUtils.getLinesAsString(lines, 0, lines.size()), "A\nB\nC\nD\nE\nF");
        assertEquals(DuplicationUtils.getLinesAsString(lines, 0, 3), "A\nB\nC");
        assertEquals(DuplicationUtils.getLinesAsString(lines, 1, 3), "B\nC\nD");
        assertEquals(DuplicationUtils.getLinesAsString(lines, 4, 1), "E");
        assertEquals(DuplicationUtils.getLinesAsString(lines, 5, 1), "F");
        assertEquals(DuplicationUtils.getLinesAsString(lines, 0, 0), "");
        assertEquals(DuplicationUtils.getLinesAsString(lines, 1, 0), "");
    }

}
