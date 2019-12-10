/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class NamedSourceCodeAspectTest {
    @Test
    public void getSourceFile() throws Exception {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect();

        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/A.java")));

        assertNotNull(aspect.getSourceFile(new File("/testdir/A.java")));
        assertNull(aspect.getSourceFile(new File("/testdir/B.java")));
    }

    @Test
    public void getLinesOfCode() throws Exception {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect();

        SourceFile sourceFile1 = new SourceFile(new File("/testdir/A.java"));
        sourceFile1.setLinesOfCode(100);
        aspect.getSourceFiles().add(sourceFile1);

        SourceFile sourceFile2 = new SourceFile(new File("/testdir/B.java"));
        sourceFile2.setLinesOfCode(150);
        aspect.getSourceFiles().add(sourceFile2);

        assertEquals(aspect.getLinesOfCode(), 250);
    }

    @Test
    public void remove() throws Exception {
        NamedSourceCodeAspect aspect1 = new NamedSourceCodeAspect();

        aspect1.getSourceFiles().add(new SourceFile(new File("/testdir/A.java")));
        aspect1.getSourceFiles().add(new SourceFile(new File("/testdir/B.java")));
        aspect1.getSourceFiles().add(new SourceFile(new File("/testdir/C.java")));

        assertEquals(aspect1.getSourceFiles().size(), 3);

        aspect1.remove(new NamedSourceCodeAspect());

        assertEquals(aspect1.getSourceFiles().size(), 3);

        NamedSourceCodeAspect aspect2 = new NamedSourceCodeAspect();

        aspect2.getSourceFiles().add(new SourceFile(new File("/testdir/A.java")));
        aspect2.getSourceFiles().add(new SourceFile(new File("/testdir/D.java")));
        aspect2.getSourceFiles().add(new SourceFile(new File("/testdir/F.java")));

        aspect1.remove(aspect2);

        assertEquals(aspect1.getSourceFiles().size(), 2);
        assertEquals(aspect2.getSourceFiles().size(), 3);
    }

    @Test
    public void getAspectsPerExtensions() throws Exception {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect();

        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/A.java")));

        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).size(), 1);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getName(), "  *.java");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getSourceFiles().size(), 1);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getSourceFiles().get(0).getFile().getName(), "A.java");

        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/a.js")));

        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).size(), 2);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getName(), "  *.java");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getSourceFiles().size(), 1);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getSourceFiles().get(0).getFile().getName(), "A.java");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(1).getName(), "  *.js");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(1).getSourceFiles().size(), 1);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(1).getSourceFiles().get(0).getFile().getName(), "a.js");

        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/B.java")));
        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/b.js")));

        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).size(), 2);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getName(), "  *.java");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getSourceFiles().size(), 2);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getSourceFiles().get(0).getFile().getName(), "A.java");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getSourceFiles().get(1).getFile().getName(), "B.java");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(1).getName(), "  *.js");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(1).getSourceFiles().size(), 2);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(1).getSourceFiles().get(0).getFile().getName(), "a.js");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(1).getSourceFiles().get(1).getFile().getName(), "b.js");
    }

    @Test
    public void getAspectsPerExtensionsSorted() throws Exception {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect();

        SourceFile sourceFile1 = new SourceFile(new File("/testdir/A.java"));
        sourceFile1.setLinesOfCode(20);
        aspect.getSourceFiles().add(sourceFile1);

        SourceFile sourceFile2 = new SourceFile(new File("/testdir/B.java"));
        sourceFile2.setLinesOfCode(2000);
        aspect.getSourceFiles().add(sourceFile2);

        SourceFile sourceFile3 = new SourceFile(new File("/testdir/C.java"));
        sourceFile3.setLinesOfCode(2);
        aspect.getSourceFiles().add(sourceFile3);

        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).size(), 1);
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getName(), "  *.java");
        assertEquals(SourceCodeAspectUtils.getAspectsPerExtensions(aspect).get(0).getSourceFiles().size(), 3);
    }

    @Test
    public void getFileSystemFriendlyName() {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect();

        aspect.setName("main");
        assertEquals("main", aspect.getFileSystemFriendlyName(""));

        aspect.setName("main");
        assertEquals("prefix1_main", aspect.getFileSystemFriendlyName("prefix1 "));

        aspect.setName("test");
        assertEquals("test", aspect.getFileSystemFriendlyName(""));

        aspect.setName("build and deploy");
        assertEquals("build_and_deploy", aspect.getFileSystemFriendlyName(""));

        aspect.setName("This is my aspect - good (between 3 and 6)");
        assertEquals("This_is_my_aspect___good__between_3_and_6_", aspect.getFileSystemFriendlyName(""));

        aspect.setName("328457qw9et&^*%$&#%^@*");
        assertEquals("328457qw9et___________", aspect.getFileSystemFriendlyName(""));
    }
}
