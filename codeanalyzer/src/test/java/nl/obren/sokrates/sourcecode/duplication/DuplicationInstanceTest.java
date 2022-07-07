/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.SourceFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static org.junit.jupiter.api.condition.OS.*;

public class DuplicationInstanceTest {
    @Test
    @EnabledOnOs({LINUX, MAC})
    public void testToStringOnLinuxOrMac() {
        DuplicationInstance instance = prepTestData();
        assertEquals(instance.toString(), "3 lines: 'folder/file.ext[10:16]a\n" +
                "b\n" +
                "c");
    }

    @Test
    @EnabledOnOs(WINDOWS)
    public void testToStringOnWindows() {
        DuplicationInstance instance = prepTestData();
        //After change in SourceFile: 
        /*assertEquals(instance.toString(), "3 lines: 'folder\\file.ext[10:16]a\n" +
                "b\n" +
                "c");
        */
        assertEquals(instance.toString(), "3 lines: 'folder/file.ext[10:16]a\n" +
                "b\n" +
                "c");
        
            }

    private DuplicationInstance prepTestData() {
        DuplicationInstance instance = new DuplicationInstance();
        instance.setDisplayContent("a\nb\nc");
        DuplicatedFileBlock block = new DuplicatedFileBlock();
        block.setSourceFile(new SourceFile(new File("/root/folder/file.ext")).relativize(new File("/root")));
        block.setStartLine(10);
        block.setEndLine(15);
        instance.getDuplicatedFileBlocks().add(block);
        instance.setBlockSize(3);
        return instance;
    }

}
