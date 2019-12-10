/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;

public class DuplicationInstanceTest {
    @Test
    public void testToString() throws Exception {
        DuplicationInstance instance = new DuplicationInstance();
        instance.setDisplayContent("a\nb\nc");
        DuplicatedFileBlock block = new DuplicatedFileBlock();
        block.setSourceFile(new SourceFile(new File("/root/folder/file.ext")).relativize(new File("/root")));
        block.setStartLine(10);
        block.setEndLine(15);
        instance.getDuplicatedFileBlocks().add(block);
        instance.setBlockSize(3);
        assertEquals(instance.toString(), "3 lines: 'folder/file.ext[10:16]a\n" +
                "b\n" +
                "c");
    }

}
