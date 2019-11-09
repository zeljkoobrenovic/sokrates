package nl.obren.sokrates.sourcecode.aspects;

import nl.obren.sokrates.sourcecode.SourceFile;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class SourceCodeAspectTest {
    @Test
    public void getSourceFile() throws Exception {
        SourceCodeAspect aspect = new SourceCodeAspect();

        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/A.java")));

        assertNotNull(aspect.getSourceFile(new File("/testdir/A.java")));
        assertNull(aspect.getSourceFile(new File("/testdir/B.java")));
    }

    @Test
    public void getLinesOfCode() throws Exception {
        SourceCodeAspect aspect = new SourceCodeAspect();

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
        SourceCodeAspect aspect1 = new SourceCodeAspect();

        aspect1.getSourceFiles().add(new SourceFile(new File("/testdir/A.java")));
        aspect1.getSourceFiles().add(new SourceFile(new File("/testdir/B.java")));
        aspect1.getSourceFiles().add(new SourceFile(new File("/testdir/C.java")));

        assertEquals(aspect1.getSourceFiles().size(), 3);

        aspect1.remove(new SourceCodeAspect());

        assertEquals(aspect1.getSourceFiles().size(), 3);

        SourceCodeAspect aspect2 = new SourceCodeAspect();

        aspect2.getSourceFiles().add(new SourceFile(new File("/testdir/A.java")));
        aspect2.getSourceFiles().add(new SourceFile(new File("/testdir/D.java")));
        aspect2.getSourceFiles().add(new SourceFile(new File("/testdir/F.java")));

        aspect1.remove(aspect2);

        assertEquals(aspect1.getSourceFiles().size(), 2);
        assertEquals(aspect2.getSourceFiles().size(), 3);
    }

    @Test
    public void getAspectsPerExtensions() throws Exception {
        SourceCodeAspect aspect = new SourceCodeAspect();

        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/A.java")));

        assertEquals(aspect.getAspectsPerExtensions().size(), 1);
        assertEquals(aspect.getAspectsPerExtensions().get(0).getName(), "  *.java");
        assertEquals(aspect.getAspectsPerExtensions().get(0).getSourceFiles().size(), 1);
        assertEquals(aspect.getAspectsPerExtensions().get(0).getSourceFiles().get(0).getFile().getName(), "A.java");

        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/a.js")));

        assertEquals(aspect.getAspectsPerExtensions().size(), 2);
        assertEquals(aspect.getAspectsPerExtensions().get(0).getName(), "  *.java");
        assertEquals(aspect.getAspectsPerExtensions().get(0).getSourceFiles().size(), 1);
        assertEquals(aspect.getAspectsPerExtensions().get(0).getSourceFiles().get(0).getFile().getName(), "A.java");
        assertEquals(aspect.getAspectsPerExtensions().get(1).getName(), "  *.js");
        assertEquals(aspect.getAspectsPerExtensions().get(1).getSourceFiles().size(), 1);
        assertEquals(aspect.getAspectsPerExtensions().get(1).getSourceFiles().get(0).getFile().getName(), "a.js");

        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/B.java")));
        aspect.getSourceFiles().add(new SourceFile(new File("/testdir/b.js")));

        assertEquals(aspect.getAspectsPerExtensions().size(), 2);
        assertEquals(aspect.getAspectsPerExtensions().get(0).getName(), "  *.java");
        assertEquals(aspect.getAspectsPerExtensions().get(0).getSourceFiles().size(), 2);
        assertEquals(aspect.getAspectsPerExtensions().get(0).getSourceFiles().get(0).getFile().getName(), "A.java");
        assertEquals(aspect.getAspectsPerExtensions().get(0).getSourceFiles().get(1).getFile().getName(), "B.java");
        assertEquals(aspect.getAspectsPerExtensions().get(1).getName(), "  *.js");
        assertEquals(aspect.getAspectsPerExtensions().get(1).getSourceFiles().size(), 2);
        assertEquals(aspect.getAspectsPerExtensions().get(1).getSourceFiles().get(0).getFile().getName(), "a.js");
        assertEquals(aspect.getAspectsPerExtensions().get(1).getSourceFiles().get(1).getFile().getName(), "b.js");
    }

    @Test
    public void getAspectsPerExtensionsSorted() throws Exception {
        SourceCodeAspect aspect = new SourceCodeAspect();

        SourceFile sourceFile1 = new SourceFile(new File("/testdir/A.java"));
        sourceFile1.setLinesOfCode(20);
        aspect.getSourceFiles().add(sourceFile1);

        SourceFile sourceFile2 = new SourceFile(new File("/testdir/B.java"));
        sourceFile2.setLinesOfCode(2000);
        aspect.getSourceFiles().add(sourceFile2);

        SourceFile sourceFile3 = new SourceFile(new File("/testdir/C.java"));
        sourceFile3.setLinesOfCode(2);
        aspect.getSourceFiles().add(sourceFile3);

        assertEquals(aspect.getAspectsPerExtensions().size(), 1);
        assertEquals(aspect.getAspectsPerExtensions().get(0).getName(), "  *.java");
        assertEquals(aspect.getAspectsPerExtensions().get(0).getSourceFiles().size(), 3);
    }

}