/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.rust;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RustAnalyzerTest {


    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = RustCodeSamples.FRAGMENT_1;

        RustAnalyzer analyzer = new RustAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(RustCodeSamples.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = RustCodeSamples.FRAGMENT_1;

        RustAnalyzer analyzer = new RustAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(RustCodeSamples.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        String code = RustCodeSamples.FRAGMENT_1;

        RustAnalyzer analyzer = new RustAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(3, units.size());

        assertEquals("fn main()", units.get(0).getShortName());
        assertEquals(4, units.get(0).getLinesOfCode());
        assertEquals(1, units.get(0).getMcCabeIndex());
        assertEquals(0, units.get(0).getNumberOfParameters());
        assertEquals(1, units.get(0).getStartLine());
        assertEquals(4, units.get(0).getEndLine());

        assertEquals("fn a()", units.get(1).getShortName());
        assertEquals(8, units.get(1).getLinesOfCode());
        assertEquals(2, units.get(1).getMcCabeIndex());
        assertEquals(2, units.get(1).getNumberOfParameters());
        assertEquals(5, units.get(1).getStartLine());
        assertEquals(13, units.get(1).getEndLine());

        assertEquals("fn b()", units.get(2).getShortName());
        assertEquals(5, units.get(2).getLinesOfCode());
        assertEquals(2, units.get(2).getMcCabeIndex());
        assertEquals(1, units.get(2).getNumberOfParameters());
        assertEquals(14, units.get(2).getStartLine());
        assertEquals(18, units.get(2).getEndLine());
    }

    private static String clean(String code) {
        return new RustAnalyzer().cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code)).getCleanedContent();
    }

    private static List<UnitInfo> units(String code) {
        return new RustAnalyzer().extractUnits(new SourceFile(new File("a.rs"), code));
    }

    @Test
    public void lifetimesDoNotStartAString() {
        String code = "fn lookup(name: &str) -> Option<&'static str> {\n" +
                "    None\n" +
                "}\n" +
                "\n" +
                "fn other() {\n" +
                "    if true { }\n" +
                "}\n";

        CleanedContent cleanedContent = new RustAnalyzer().cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));
        assertEquals(6, cleanedContent.getCleanedLinesCount());

        List<UnitInfo> units = units(code);
        assertEquals(2, units.size());
        assertEquals("fn other()", units.get(1).getShortName());
    }

    @Test
    public void genericLifetimesAreKept() {
        String code = "fn f<'a, 'b>(x: &'a str, y: &'b str) -> &'a str {\n" +
                "    x // \"comment\"\n" +
                "}\n";

        assertEquals("fn f<'a, 'b>(x: &'a str, y: &'b str) -> &'a str {\n" +
                "    x \n" +
                "}", clean(code));
        assertEquals(2, units(code).get(0).getNumberOfParameters());
    }

    @Test
    public void charLiteralsAreStrings() {
        String code = "fn f() {\n" +
                "    let a = '\"'; // quote\n" +
                "    let b = '{';\n" +
                "    let c = '\\'';\n" +
                "    let d = b'x';\n" +
                "    let e = '\\u{1F600}';\n" +
                "}\n" +
                "fn g() {\n" +
                "}\n";

        assertEquals("fn f() {\n" +
                "    let a = '\"'; \n" +
                "    let b = '{';\n" +
                "    let c = '\\'';\n" +
                "    let d = b'x';\n" +
                "    let e = '\\u{1F600}';\n" +
                "}\n" +
                "fn g() {\n" +
                "}", clean(code));

        List<UnitInfo> units = units(code);
        assertEquals(2, units.size());
        assertEquals(7, units.get(0).getLinesOfCode());
    }

    @Test
    public void blockCommentsAreRemoved() {
        String code = "/*\n" +
                " * SPDX-License-Identifier: AGPL-3.0-only\n" +
                " */\n" +
                "\n" +
                "fn main() { /* inline */ }\n";

        assertEquals("fn main() {  }", clean(code));
    }

    @Test
    public void rawStringsMayContainQuotes() {
        String code = "fn f() {\n" +
                "    let a = r#\"he said \"hi\" // not a comment\"#;\n" +
                "    let b = r##\"a \"# b\"##;\n" +
                "    let c = br#\"\\\"#;\n" +
                "}\n" +
                "fn g() {\n" +
                "}\n";

        assertEquals("fn f() {\n" +
                "    let a = r#\"he said \"hi\" // not a comment\"#;\n" +
                "    let b = r##\"a \"# b\"##;\n" +
                "    let c = br#\"\\\"#;\n" +
                "}\n" +
                "fn g() {\n" +
                "}", clean(code));
        assertEquals(2, units(code).size());
    }

    @Test
    public void functionsWithModifiersAreUnits() {
        String code = "fn a() {}\n" +
                "pub fn b(x: u8) {}\n" +
                "pub(crate) async fn c(x: u8, y: u8) {}\n" +
                "const fn d() -> u32 { 1 }\n" +
                "pub unsafe extern \"C\" fn e<F: Fn(u8, u8) -> u8>(f: F, v: Vec<(u8, u8)>,) {}\n" +
                "fnord();\n";

        List<UnitInfo> units = units(code);
        assertEquals(5, units.size());
        assertEquals("fn a()", units.get(0).getShortName());
        assertEquals(0, units.get(0).getNumberOfParameters());
        assertEquals("fn b()", units.get(1).getShortName());
        assertEquals(1, units.get(1).getNumberOfParameters());
        assertEquals("fn c()", units.get(2).getShortName());
        assertEquals(2, units.get(2).getNumberOfParameters());
        assertEquals("fn d()", units.get(3).getShortName());
        assertEquals(0, units.get(3).getNumberOfParameters());
        assertEquals("fn e()", units.get(4).getShortName());
        assertEquals(2, units.get(4).getNumberOfParameters());
    }

    @Test
    public void traitDeclarationsWithoutBodyAreNotUnits() {
        String code = "trait T {\n" +
                "    fn len(&self) -> usize;\n" +
                "    fn classify(\n" +
                "        &self,\n" +
                "    ) -> impl Future<Output = ()> + Send;\n" +
                "    fn is_empty(&self) -> bool {\n" +
                "        self.len() == 0\n" +
                "    }\n" +
                "}\n";

        List<UnitInfo> units = units(code);
        assertEquals(1, units.size());
        assertEquals("fn is_empty()", units.get(0).getShortName());
        assertEquals(6, units.get(0).getStartLine());
        assertEquals(8, units.get(0).getEndLine());
    }

    @Test
    public void lifetimesAroundBracesKeepUnitBoundaries() {
        String code = "fn f<'a>(x: &'a u8) { let y: &'a u8 = x; }\n" +
                "fn g(c: char) -> bool {\n" +
                "    c == '}'\n" +
                "}\n" +
                "fn h() {\n" +
                "}\n";

        List<UnitInfo> units = units(code);
        assertEquals(3, units.size());
        assertEquals(1, units.get(0).getLinesOfCode());
        assertEquals(1, units.get(0).getNumberOfParameters());
        assertEquals(3, units.get(1).getLinesOfCode());
        assertEquals("fn h()", units.get(2).getShortName());
    }
}
