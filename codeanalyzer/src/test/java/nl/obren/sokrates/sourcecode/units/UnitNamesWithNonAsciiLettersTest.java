/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * A unit whose name contains a letter outside ASCII must still be extracted.
 *
 * <p>Identifier patterns were written as {@code [a-zA-Z0-9_...]}, so a method called {@code prüfen} or
 * {@code beregnØre} did not match a unit signature at all. The unit was not reported: not merely named
 * oddly, but absent from unit counts, size and complexity, with nothing to indicate a file had been
 * skipped. German, Nordic, French and Spanish code bases were under-reported without any signal.
 *
 * <p>Each case is paired with an ASCII equivalent, so the assertion is that the two behave the same
 * rather than that some particular number comes out.
 */
public class UnitNamesWithNonAsciiLettersTest {

    @Test
    public void cStyleLanguages() {
        assertSameAsAscii("A.java",
                "class A {\n  void check() {\n    int x = 1;\n  }\n}\n",
                "class A {\n  void prüfen() {\n    int x = 1;\n  }\n}\n");
        assertSameAsAscii("A.cs",
                "class A {\n  public void Check() {\n    int x = 1;\n  }\n}\n",
                "class A {\n  public void Prüfen() {\n    int x = 1;\n  }\n}\n");
        assertSameAsAscii("a.go", "func check() {\n x := 1\n}\n", "func prüfen() {\n x := 1\n}\n");
        assertSameAsAscii("a.rs", "fn check() {\n let x = 1;\n}\n", "fn prüfen() {\n let x = 1;\n}\n");
        assertSameAsAscii("a.scala", "def check() {\n val x = 1\n}\n", "def prüfen() {\n val x = 1\n}\n");
        assertSameAsAscii("a.groovy", "def check() {\n def x = 1\n}\n", "def prüfen() {\n def x = 1\n}\n");
        assertSameAsAscii("a.php", "function check() {\n $x = 1;\n}\n", "function prüfen() {\n $x = 1;\n}\n");
        assertSameAsAscii("a.cpp", "void check() {\n int x = 1;\n}\n", "void prüfen() {\n int x = 1;\n}\n");
    }

    @Test
    public void languagesWithTheirOwnIdentifierPattern() {
        // These declare an identifier pattern of their own rather than using the C-style one, which is
        // why the same defect had to be corrected in each of them.
        assertSameAsAscii("a.js", "function check() {\n var x = 1;\n}\n", "function prüfen() {\n var x = 1;\n}\n");
        assertSameAsAscii("a.ts", "function check() {\n var x = 1;\n}\n", "function prüfen() {\n var x = 1;\n}\n");
        assertSameAsAscii("a.kt", "fun check() {\n val x = 1\n}\n", "fun prüfen() {\n val x = 1\n}\n");
        assertSameAsAscii("a.swift", "func check() {\n let x = 1\n}\n", "func prüfen() {\n let x = 1\n}\n");
        assertSameAsAscii("a.hh", "function check() {\n $x = 1;\n}\n", "function prüfen() {\n $x = 1;\n}\n");
        assertSameAsAscii("a.r", "check <- function() {\n x <- 1\n}\n", "prüfen <- function() {\n x <- 1\n}\n");
        assertSameAsAscii("a.pas",
                "procedure A.check();\nbegin\n x := 1;\nend;\n",
                "procedure A.prüfen();\nbegin\n x := 1;\nend;\n");
    }

    @Test
    public void aNonAsciiNameIsReportedInFull() {
        List<UnitInfo> units = unitsOf("A.java", "class A {\n  void beregnØre() {\n    int x = 1;\n  }\n}\n");

        assertEquals(1, units.size());
        // Not truncated at the non-ASCII letter, which is how a too-narrow pattern would fail if it
        // matched the prefix rather than failing outright.
        assertEquals("void beregnØre()", units.get(0).getShortName().trim());
    }

    @Test
    public void aDecomposedAccentIsTreatedLikeAPrecomposedOne() {
        // The same name written two ways: é as one code point, and as "e" followed by a combining acute.
        // Editors and pipelines differ on which they produce, and a class of letters alone does not admit
        // the second form - the combining mark is a mark, not a letter, so the name broke off at it.
        String precomposed = "class A {\n  void café() {\n    int x = 1;\n  }\n}\n";
        String decomposed = "class A {\n  void cafe\u0301() {\n    int x = 1;\n  }\n}\n";

        assertEquals(1, unitsOf("A.java", precomposed).size());
        assertEquals("the two spellings are the same name and must extract alike",
                1, unitsOf("A.java", decomposed).size());
    }

    @Test
    public void aStrayCombiningMarkDoesNotInventAUnit() {
        // Admitting combining marks has to stop short of letting one BEGIN a token: a mark with no base
        // letter is garbled input, not a name, and a line of it before a declaration was briefly enough
        // to match a signature. Real accented text is unaffected, since a mark there always follows a
        // letter and so falls in the tail.
        assertEquals(0, unitsOf("A.java", "class A {\n  \u0301 void foo() {\n    int x = 1;\n  }\n}\n").size());
        assertEquals(0, unitsOf("a.hh", "\u0301 function foo() {\n  $x = 1;\n}\n").size());
        assertEquals(0, unitsOf("a.cpp", "\u0301 void foo() {\n  int x = 1;\n}\n").size());
        assertEquals(0, unitsOf("a.r", "\u0301 <- function() {\n x <- 1\n}\n").size());
        assertEquals(0, unitsOf("a.pas", "procedure A.\u0301();\nbegin\n x := 1;\nend;\n").size());

        // The same lines without the stray mark are ordinary declarations and must still extract.
        assertEquals(1, unitsOf("A.java", "class A {\n  void foo() {\n    int x = 1;\n  }\n}\n").size());
        assertEquals(1, unitsOf("a.hh", "function foo() {\n  $x = 1;\n}\n").size());
        assertEquals(1, unitsOf("a.r", "foo <- function() {\n x <- 1\n}\n").size());
        assertEquals(1, unitsOf("a.pas", "procedure A.foo();\nbegin\n x := 1;\nend;\n").size());
    }

    private void assertSameAsAscii(String fileName, String asciiContent, String nonAsciiContent) {
        int ascii = unitsOf(fileName, asciiContent).size();
        int nonAscii = unitsOf(fileName, nonAsciiContent).size();

        assertEquals(fileName + ": the ASCII fixture must extract a unit for the comparison to mean "
                + "anything", 1, ascii);
        assertEquals(fileName + ": a non-ASCII name must not change how many units are found",
                ascii, nonAscii);
    }

    private List<UnitInfo> unitsOf(String fileName, String content) {
        SourceFile sourceFile = new SourceFile(new File(fileName), content);
        return LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile).extractUnits(sourceFile);
    }
}
