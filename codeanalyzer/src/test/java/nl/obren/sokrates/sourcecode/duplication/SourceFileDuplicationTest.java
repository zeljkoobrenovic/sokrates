/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class SourceFileDuplicationTest {
    @Test
    public void addBlock() throws Exception {
        SourceFileDuplication sourceFileDuplication = new SourceFileDuplication();

        sourceFileDuplication.addLines(1, 1);
        sourceFileDuplication.addLines(2, 2);
        sourceFileDuplication.addLines(3, 10);

        assertEquals(sourceFileDuplication.getDuplicatedLinesOfCode(), 10);

        sourceFileDuplication.addLines(1, 10);
        sourceFileDuplication.addLines(2, 9);
        sourceFileDuplication.addLines(3, 8);

        assertEquals(sourceFileDuplication.getDuplicatedLinesOfCode(), 10);
    }

}
