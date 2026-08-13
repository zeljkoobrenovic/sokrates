/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;

public class SourceFileFilterTest {
    @Test
    public void testPathMatches() throws Exception {
        assertTrue(new SourceFileFilter("/root/a/b.*", "").pathMatches("/root/a/b/c"));
        assertFalse(new SourceFileFilter("/root/a/b.*", "").pathMatches("/otherroot/a/b/c"));

        assertTrue(new SourceFileFilter("/root/a/b.*", "").pathMatches("\\root\\a\\b\\c"));
        assertTrue(new SourceFileFilter("/root/a/b.*", "").pathMatches("\\root/a/b\\c"));

        assertTrue(new SourceFileFilter("\\\\root\\\\a\\\\b.*", "").pathMatches("/root/a/b/c"));
        assertTrue(new SourceFileFilter("\\\\root\\\\a\\\\b.*", "").pathMatches("\\root\\a\\b\\c"));
        assertTrue(new SourceFileFilter("\\\\root\\\\a\\\\b.*", "").pathMatches("\\root/a/b\\c"));
    }

    @Test
    public void testPathMatchesPatternWithForwardSlashes() {
        // The pattern is also tried with "\" replaced by "/". Here the pattern is the regex a\.b
        // (a, any character, b): only the pattern rewritten to a/.b matches the path a/1b.
        assertTrue(new SourceFileFilter("a\\.b", "").pathMatches("a/1b"));
        assertFalse(new SourceFileFilter("a\\.b", "").pathMatches("a/1c"));
    }

    @Test
    public void testPathMatchesPathWithBackSlashes() {
        // The path is also tried with "/" replaced by "\", so a pattern that excludes forward
        // slashes still matches a path that contains them.
        assertTrue(new SourceFileFilter("[^/]*[.]java", "").pathMatches("comp1/src/Test.java"));
        assertFalse(new SourceFileFilter("[^/]*[.]java", "").pathMatches("comp1/src/Test.html"));
    }

    @Test
    public void testPathMatchesPatternWithForwardSlashesAndPathWithBackSlashes() {
        // The pattern rewritten to forward slashes is also tried against the path rewritten to
        // backslashes. Here the pattern is the regex [^\/]ab (one character that is neither a backslash
        // nor a slash, then ab): only the pattern rewritten to [^//]ab, against the path rewritten to
        // \ab, matches - the path alone or the pattern alone still has a separator the class rejects.
        assertTrue(new SourceFileFilter("[^\\\\/]ab", "").pathMatches("/ab"));
        assertFalse(new SourceFileFilter("[^\\\\/]ab", "").pathMatches("/ac"));
    }

    @Test
    public void testToString() {
        assertEquals(new SourceFileFilter("a", "").toString(), "path like \"a\"");
        assertEquals(new SourceFileFilter("", "b").toString(), "content like \"b\"");
        assertEquals(new SourceFileFilter("a", "b").toString(), "path like \"a\" AND content like \"b\"");
    }

    @Test
    public void testGetMatchingLinesCount() throws Exception {
        List<String> lines = Arrays.asList(("package nl.obren.codeexplorer.model.elements;\n" +
                "\n" +
                "public class ModelElement {\n" +
                "    private String note = \"\";\n" +
                "\n" +
                "    public String getNote() {\n" +
                "        return note;\n" +
                "    }\n" +
                "\n" +
                "    public void setNote(String note) {\n" +
                "        this.note = note;\n" +
                "    }\n" +
                "}\n").split("\n"));

        assertEquals(lines.size(), 13);

        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*"), 13);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, "package.*"), 1);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, "public.*"), 1);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*public.*"), 3);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*return.*"), 1);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*String.*"), 3);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*[}]"), 3);
    }

    @Test
    public void testMatches() throws Exception {
        assertTrue(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), "Z.*"));
        assertTrue(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), ".*Z"));
        assertTrue(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), "A.*"));
        assertTrue(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), ".*"));

        assertFalse(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), "E.*"));
        assertFalse(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), ".*T"));
    }

    @Test
    public void testGetPathMatch() throws Exception {
        SourceFile sourceFile = new SourceFile();
        sourceFile.setRelativePath("comp1/src/main/test/Test.java");

        assertTrue(new SourceFileFilter("", "").pathMatches(sourceFile.getRelativePath()));
        assertTrue(new SourceFileFilter(".*/Test[.]java", "").pathMatches(sourceFile.getRelativePath()));
        assertTrue(new SourceFileFilter(".*[.]java", "").pathMatches(sourceFile.getRelativePath()));
        assertTrue(new SourceFileFilter(".*/main/.*", "").pathMatches(sourceFile.getRelativePath()));

        assertFalse(new SourceFileFilter(".*/comp2/.*", "").pathMatches(sourceFile.getRelativePath()));
        assertFalse(new SourceFileFilter(".*[.]html", "").pathMatches(sourceFile.getRelativePath()));
    }

    @Test
    public void testGetContentMatch() throws Exception {
        SourceFile sourceFile = new SourceFile();
        sourceFile.setContent("package nl.obren.codeexplorer.model.elements;\n" +
                "\n" +
                "public class ModelElement {\n" +
                "    private String note = \"\";\n" +
                "\n" +
                "    public String getNote() {\n" +
                "        return note;\n" +
                "    }\n" +
                "\n" +
                "    public void setNote(String note) {\n" +
                "        this.note = note;\n" +
                "    }\n" +
                "}\n");

        assertTrue(new SourceFileFilter("", "package.*").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*public.*").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*private.*").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*;").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*}").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*[{].*").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", " ").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", "").contentMatches(sourceFile.getLines()));

        assertFalse(new SourceFileFilter("", "packasge.*").contentMatches(sourceFile.getLines()));
        assertFalse(new SourceFileFilter("", ".*[}] .*").contentMatches(sourceFile.getLines()));
        assertFalse(new SourceFileFilter("", ".*test.*").contentMatches(sourceFile.getLines()));
    }
}
