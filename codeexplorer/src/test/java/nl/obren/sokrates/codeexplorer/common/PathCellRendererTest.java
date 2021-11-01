/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.common;

import org.junit.Test;

import java.io.File;

import static java.nio.file.FileSystems.getDefault;
import static junit.framework.TestCase.assertEquals;

public class PathCellRendererTest {
    @Test
    public void getPathPrefix() throws Exception {
        PathCellRenderer renderer = new PathCellRenderer();

        assertEquals(renderer.getPathPrefix(new File("/root/a/b/c/A.java")), getDefault().getSeparator() + "root" + getDefault().getSeparator() + "a" + getDefault().getSeparator() + "b" + getDefault().getSeparator() + "c" + getDefault().getSeparator());
        assertEquals(renderer.getPathPrefix(new File("a/A.java")), "a" + getDefault().getSeparator());
        assertEquals(renderer.getPathPrefix(new File("A.java")), "");
    }

}
