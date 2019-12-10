/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.cleaners;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SourceCodeCleanerUtilsTest {
    @Test
    public void cleanComments() throws Exception {
        String code = "A {\n"
                + "  a;\n"
                + "  // comment 1\n"
                + "  b;\n"
                + "  c;\n"
                + "  /*d;\n"
                + "  e;\n"
                + "  f;*/\n"
                + "  g;\n"
                + "}";

        CleanedContent cleanedContent = SourceCodeCleanerUtils.emptyComments(code, "//", "/*", "*/");
        assertEquals(cleanedContent.getCleanedContent(), "A {\n" +
                "  a;\n" +
                "  \n" +
                "  b;\n" +
                "  c;\n" +
                "  \n" +
                "\n" +
                "\n" +
                "  g;\n" +
                "}");
        assertEquals(cleanedContent.getCleanedLinesCount(), 10);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 10);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 0);
        assertEquals(cleanedContent.getFileLineIndexes().get(9).intValue(), 9);
    }

    @Test
    public void cleanCommentsAndEmptyLines() throws Exception {
        String code = "A {\n"
                + "  a;\n"
                + "  // comment 1\n"
                + "  b;\n"
                + "  c;\n"
                + "  /*d;\n"
                + "  e;\n"
                + "  f;*/\n"
                + "  g;\n"
                + "}";

        CleanedContent cleanedContent = SourceCodeCleanerUtils.cleanCommentsAndEmptyLines(code, "//", "/*", "*/");
        assertEquals(cleanedContent.getCleanedContent(), "A {\n" +
                "  a;\n" +
                "  b;\n" +
                "  c;\n" +
                "  g;\n" +
                "}");
        assertEquals(cleanedContent.getCleanedLinesCount(), 6);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 6);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 3);
        assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(4).intValue(), 8);
        assertEquals(cleanedContent.getFileLineIndexes().get(5).intValue(), 9);
    }

    @Test
    public void cleanEmptyLinesWithLineIndexes() throws Exception {
        CleanedContent cleanedContent = SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes("class A {}");
        assertEquals(cleanedContent.getCleanedContent(), "class A {}");
        assertEquals(cleanedContent.getFileLineIndexes().size(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 0);

        cleanedContent = SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes("class A {\n\n\n}");
        assertEquals(cleanedContent.getCleanedContent(), "class A {\n}");
        assertEquals(cleanedContent.getFileLineIndexes().size(), 2);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 0);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 3);
    }

    @Test
    public void cleanEmptyLinesWithLineIndexes2() throws Exception {
        CleanedContent cleanedContent = SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes("a\n\rb\nc\nd\n\n\ne\n\nf\ng\nh");
        assertEquals(cleanedContent.getCleanedContent(), "a\n" +
                "b\n" +
                "c\n" +
                "d\n" +
                "e\n" +
                "f\n" +
                "g\n" +
                "h");
        assertEquals(cleanedContent.getFileLineIndexes().size(), 8);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 0);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 2);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 3);
        assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(4).intValue(), 7);
        assertEquals(cleanedContent.getFileLineIndexes().get(5).intValue(), 9);
        assertEquals(cleanedContent.getFileLineIndexes().get(6).intValue(), 10);
        assertEquals(cleanedContent.getFileLineIndexes().get(7).intValue(), 11);
    }

    @Test
    public void removeLinesMatchingPattern() throws Exception {
        assertEquals(SourceCodeCleanerUtils.emptyLinesMatchingPattern("[}]", "class A {\n  \n \n}\n"), "class A {\n" +
                "  \n" +
                " \n" +
                "\n");
        assertEquals(SourceCodeCleanerUtils.emptyLinesMatchingPattern(".*[{]", "class A {\n  \n \n}\n"), "\n" +
                "  \n" +
                " \n" +
                "}\n");
    }

    @Test
    public void cleanEmptyLines() throws Exception {
        assertEquals(SourceCodeCleanerUtils.cleanEmptyLines("class A {}"), "class A {}");
        assertEquals(SourceCodeCleanerUtils.cleanEmptyLines("class A {\n}"), "class A {\n}");
        assertEquals(SourceCodeCleanerUtils.cleanEmptyLines("class A {\n\n}"), "class A {\n}");
        assertEquals(SourceCodeCleanerUtils.cleanEmptyLines("class A {\n\n\n}"), "class A {\n}");
        assertEquals(SourceCodeCleanerUtils.cleanEmptyLines("class A {\n  \n \n}\n"), "class A {\n}");
    }

    @Test
    public void trimLines() throws Exception {
        assertEquals(SourceCodeCleanerUtils.trimLines("class A {}"), "class A {}");
        assertEquals(SourceCodeCleanerUtils.trimLines("class A {\n}"), "class A {\n}");
        assertEquals(SourceCodeCleanerUtils.trimLines("class A {\n a;\n}"), "class A {\na;\n}");
        assertEquals(SourceCodeCleanerUtils.trimLines("class A {\n\ta;\n   b;\n}"), "class A {\na;\nb;\n}");
        assertEquals(SourceCodeCleanerUtils.trimLines("class A {\n  \n \n}\n"), "class A {\n\n\n}");
    }

    @Test
    public void getUnifiedEndOfLines() throws Exception {
        assertEquals(SourceCodeCleanerUtils.normalizeLineEnds("a\nb\nc"), "a\nb\nc");
        assertEquals(SourceCodeCleanerUtils.normalizeLineEnds("a\r\nb\r\nc"), "a\nb\nc");
        assertEquals(SourceCodeCleanerUtils.normalizeLineEnds("a\rb\rc"), "a\nb\nc");
        assertEquals(SourceCodeCleanerUtils.normalizeLineEnds("\t"), "    ");
        assertEquals(SourceCodeCleanerUtils.normalizeLineEnds("\ta\r\tb\r\tc"), "    a\n    b\n    c");
    }


}
