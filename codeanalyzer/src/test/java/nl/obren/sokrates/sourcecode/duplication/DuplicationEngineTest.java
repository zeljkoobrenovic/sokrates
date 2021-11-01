/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class DuplicationEngineTest {
    @Test
    public void findDuplicates() {
        DuplicationEngine engine = new DuplicationEngine();

        SourceFile sourceFile1 = new SourceFile(new File("file1.unknown"), "a\n" +
                "b\n" +
                "c\n" +
                "d\n" +
                "e\n" +
                "f\n" +
                "g\n" +
                "h\n" +
                "i\n" +
                "j\n" +
                "k\n" +
                "l\n" +
                "m\n" +
                "n\n" +
                "o\n" +
                "j\n" +
                "j\n" +
                "j\n" +
                "k\n");
        SourceFile sourceFile2 = new SourceFile(new File("file2.unknown"), "a\n" +
                "b\n" +
                "c\n" +
                "d\n" +
                "e\n" +
                "f\n" +
                "g\n" +
                "break here\n" +
                "i\n" +
                "j\n" +
                "k\n" +
                "l\n" +
                "m\n" +
                "n\n" +
                "o\n" +
                "j\n" +
                "j\n" +
                "j\n" +
                "k\n");

        List<SourceFile> sourceFiles = Arrays.asList(sourceFile1, sourceFile2);
        List<DuplicationInstance> duplicates = engine.findDuplicates(sourceFiles, 6, new ProgressFeedback());

        assertEquals(duplicates.size(), 8);

        Collections.sort(duplicates, Comparator.comparingInt(d -> d.getDuplicatedFileBlocks().get(0).getStartLine()));

        List<DuplicatedFileBlock> duplicatedFileBlocks1 = duplicates.get(0).getDuplicatedFileBlocks();
        List<DuplicatedFileBlock> duplicatedFileBlocks2 = duplicates.get(1).getDuplicatedFileBlocks();
        assertEquals(duplicatedFileBlocks1.size(), 2);
        assertEquals(duplicatedFileBlocks1.get(0).getSourceFile().getFile().getName(), "file1.unknown");
        assertEquals(duplicatedFileBlocks1.get(0).getStartLine(), 1);
        assertEquals(duplicatedFileBlocks1.get(0).getEndLine(), 6);
        assertEquals(duplicatedFileBlocks1.get(1).getSourceFile().getFile().getName(), "file2.unknown");
        assertEquals(duplicatedFileBlocks1.get(1).getStartLine(), 1);
        assertEquals(duplicatedFileBlocks1.get(1).getEndLine(), 6);

        assertEquals(duplicatedFileBlocks2.size(), 2);
        assertEquals(duplicatedFileBlocks2.get(0).getSourceFile().getFile().getName(), "file1.unknown");
        assertEquals(duplicatedFileBlocks2.get(0).getStartLine(), 2);
        assertEquals(duplicatedFileBlocks2.get(0).getEndLine(), 7);
        assertEquals(duplicatedFileBlocks2.get(1).getSourceFile().getFile().getName(), "file2.unknown");
        assertEquals(duplicatedFileBlocks2.get(1).getStartLine(), 2);
        assertEquals(duplicatedFileBlocks2.get(1).getEndLine(), 7);

    }

}
