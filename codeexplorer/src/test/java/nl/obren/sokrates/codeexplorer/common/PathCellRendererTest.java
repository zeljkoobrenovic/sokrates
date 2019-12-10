/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.common;

import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;

public class PathCellRendererTest {
    @Test
    public void getPathPrefix() throws Exception {
        PathCellRenderer renderer = new PathCellRenderer();

        assertEquals(renderer.getPathPrefix(new File("/root/a/b/c/A.java")), "/root/a/b/c/");
        assertEquals(renderer.getPathPrefix(new File("a/A.java")), "a/");
        assertEquals(renderer.getPathPrefix(new File("A.java")), "");
    }

}
