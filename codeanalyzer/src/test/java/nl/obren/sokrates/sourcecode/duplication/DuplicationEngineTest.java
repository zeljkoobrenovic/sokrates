/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class DuplicationEngineTest {
    @Test
    public void findDuplicates() throws Exception {
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
        List<DuplicationInstance> duplicates = engine.findDuplicates(sourceFiles, new ProgressFeedback());

        assertEquals(duplicates.size(), 2);

        assertEquals(duplicates.get(0).getDuplicatedFileBlocks().size(), 2);
        assertEquals(duplicates.get(0).getDuplicatedFileBlocks().get(0).getSourceFile().getFile().getName(), "file1.unknown");
        assertEquals(duplicates.get(0).getDuplicatedFileBlocks().get(0).getStartLine(), 1);
        assertEquals(duplicates.get(0).getDuplicatedFileBlocks().get(0).getEndLine(), 7);
        assertEquals(duplicates.get(0).getDuplicatedFileBlocks().get(1).getSourceFile().getFile().getName(), "file2.unknown");
        assertEquals(duplicates.get(0).getDuplicatedFileBlocks().get(1).getStartLine(), 1);
        assertEquals(duplicates.get(0).getDuplicatedFileBlocks().get(1).getEndLine(), 7);

        assertEquals(duplicates.get(1).getDuplicatedFileBlocks().size(), 2);
        assertEquals(duplicates.get(1).getDuplicatedFileBlocks().get(0).getSourceFile().getFile().getName(), "file1.unknown");
        assertEquals(duplicates.get(1).getDuplicatedFileBlocks().get(0).getStartLine(), 9);
        assertEquals(duplicates.get(1).getDuplicatedFileBlocks().get(0).getEndLine(), 19);
        assertEquals(duplicates.get(1).getDuplicatedFileBlocks().get(1).getSourceFile().getFile().getName(), "file2.unknown");
        assertEquals(duplicates.get(1).getDuplicatedFileBlocks().get(1).getStartLine(), 9);
        assertEquals(duplicates.get(1).getDuplicatedFileBlocks().get(1).getEndLine(), 19);

    }

}
