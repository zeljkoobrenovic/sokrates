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
        // loop has no condition; its exits (if ... break) are counted where they are
        assertEquals(1, units.get(2).getMcCabeIndex());
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

    private static int mcCabe(String body) {
        List<UnitInfo> units = units("fn f(x: Option<u8>, s: &str) -> u8 {\n" + body + "}\n");
        assertEquals(1, units.size());
        return units.get(0).getMcCabeIndex();
    }

    @Test
    public void matchArmsAreDecisions() {
        assertEquals(3, mcCabe("    match x {\n" +
                "        Some(0) => 0,\n" +
                "        Some(n) => n,\n" +
                "        None => 1,\n" +
                "    }\n"));
        assertEquals(1, mcCabe("    match x { _ => 0 }\n"));
        // a guard is a decision of its own
        assertEquals(3, mcCabe("    match x {\n" +
                "        Some(n) if n > 1 => n,\n" +
                "        _ => 0,\n" +
                "    }\n"));
    }

    @Test
    public void conditionsAndLoopsAreDecisions() {
        assertEquals(7, mcCabe("    if let Some(n) = x { return n; } else if s.is_empty() { return 1; }\n" +
                "    while let Some(n) = x { break; }\n" +
                "    for c in s.chars() { }\n" +
                "    if s.len() > 1 && s.len() < 9 { }\n"));
        assertEquals(3, mcCabe("    let ok = s.is_empty()\n" +
                "        || s.len() > 9\n" +
                "        || x.is_none();\n" +
                "    0\n"));
    }

    @Test
    public void lookalikesAreNotDecisions() {
        assertEquals(1, mcCabe("    let a = s.to_lowercase();\n" +
                "    let b = s.to_uppercase();\n" +
                "    let c: &&str = &&s;\n" +
                "    let d = [1, 2].iter().filter(|&&v| v > 1).count();\n" +
                "    let e = x.unwrap_or_else(|| 0);\n" +
                "    let g = || 1;\n" +
                "    std::thread::spawn(move || 0);\n" +
                "    struct S;\n" +
                "    impl<'a> From<&'a str> for S { fn from(_: &'a str) -> S { S } }\n" +
                "    let h: Box<dyn for<'b> Fn(&'b str)> = Box::new(|_| ());\n" +
                "    loop { break; }\n" +
                "    0\n"));
    }

    @Test
    public void macroArgumentsAreNotDecisions() {
        // a lookup table written with =>, not a match
        assertEquals(1, mcCabe("    hashify::map!(\n" +
                "        s.as_bytes(),\n" +
                "        \"a\" => 1,\n" +
                "        \"b\" => 2,\n" +
                "        \"c\" => 3,\n" +
                "    );\n" +
                "    let v = vec![x.is_some() && s.is_empty()];\n" +
                "    println!(\"{}\", if v.len() > 1 { 1 } else { 2 });\n" +
                "    0\n"));
        // the code around a macro still counts, as does a negation !( ... )
        assertEquals(5, mcCabe("    if !(x.is_some() && s.is_empty()) { }\n" +
                "    assert!(s.len() > 1 || x.is_none());\n" +
                "    match x {\n" +
                "        Some(_) => format!(\"{}\", match s { _ => 1 }),\n" +
                "        None => String::new(),\n" +
                "    };\n" +
                "    if s != \"\" { }\n" +
                "    0\n"));
    }

    @Test
    public void withoutMacroArguments() {
        assertEquals("a!( ;b![ ;c!{ ;!(x) != y", RustHeuristicUnitsExtractor.withoutMacroArguments(
                "a!(x => (y), [z]);b![1 => 2];c!{ {d} };!(x) != y"));
        assertEquals("open!( ", RustHeuristicUnitsExtractor.withoutMacroArguments("open!(never closed => 1"));
    }
}
