/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.cleaners;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommentsAndEmptyLinesCleanerTest {

    @Test
    public void clean() {

        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner("//", "/*", "*/", "\"", "\\");
        String code = "#include \"add.h\"\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}";

        assertEquals("#include \"add.h\"\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    return add(x, add(x,x));\n" +
                "}", cleaner.clean(code).getCleanedContent());
    }

    @Test
    public void anUnclosedStringIsAnOrdinaryCharacterNotTheRestOfTheFile() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner("//", "/*", "*/", "\"", "\\");
        String code = "a = x\"; // stray quote\n" +
                "// comment\n" +
                "b = 1; // comment";

        assertEquals("a = x\";\n" +
                "b = 1;", cleaner.clean(code).getCleanedContent().replaceAll(" +\n", "\n").trim());
    }

    @Test
    public void anUnclosedBlockCommentStillRunsToTheEnd() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner("//", "/*", "*/", "\"", "\\");

        assertEquals("a = 1;", cleaner.clean("a = 1;\n/* never closed\nb = 2;").getCleanedContent());
    }
}
