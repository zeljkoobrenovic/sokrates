/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import org.junit.Test;

import java.io.File;

import static java.nio.file.FileSystems.getDefault;
import static org.junit.Assert.assertEquals;

public class SourceFileTest {
    @Test
    public void setLinesOfCodeFromLines() throws Exception {
        SourceFile sourceFile = new SourceFile(new File("test"), "");

        sourceFile.setContent("class A {\n"
                + "    private a;\n"
                + "    private b;\n"
                + "}");
        sourceFile.setLinesOfCodeFromContent();

        assertEquals(sourceFile.getLinesOfCode(), 4);

        sourceFile.setContent("class B {\n"
                + "    private c;\n"
                + "\n"
                + "    private d;\n"
                + "   \n"
                + "\t\t\n"
                + "\t    \t\n"
                + "}");
        sourceFile.setLinesOfCodeFromContent();

        assertEquals(sourceFile.getLinesOfCode(), 4);
    }

    @Test
    public void getContent() throws Exception {
        File file = new File("/testproject/src/main/java/A.java");
        SourceFile sourceFile = new SourceFile(file);

        assertEquals(sourceFile.getContent(), "");

        sourceFile.setContent("class A {}");

        assertEquals(sourceFile.getContent(), "class A {}");
    }

    @Test
    public void getLines() throws Exception {
        File file = new File("/testproject/src/main/java/A.java");
        SourceFile sourceFile = new SourceFile(file);

        assertEquals(sourceFile.getLines().size(), 0);

        sourceFile.setContent("class A {\n"
                + "    private a;\n"
                + "    private b;\n"
                + "}");

        assertEquals(sourceFile.getLines().size(), 4);
        assertEquals(sourceFile.getLines().get(0), "class A {");
        assertEquals(sourceFile.getLines().get(3), "}");
    }

    @Test
    public void relativize() throws Exception {
        File file = new File("/testproject/src/main/java/A.java");

        SourceFile sourceFile = new SourceFile(file);

        assertEquals(sourceFile.getFile(), file);

        sourceFile.relativize(new File("/testproject"));

        assertEquals(sourceFile.getRelativePath(), "src" + getDefault().getSeparator() + "main" + getDefault().getSeparator() + "java" + getDefault().getSeparator() + "A.java");

        sourceFile.relativize(new File("/testproject/src/main/java"));
        assertEquals(sourceFile.getRelativePath(), "A.java");
    }

    @Test
    public void setFile() throws Exception {
        File file = new File("/testproject/src/main/java/A.java");

        SourceFile sourceFile = new SourceFile(file);

        assertEquals(sourceFile.getFile(), file);
        assertEquals(sourceFile.getExtension(), "java");
    }

}
