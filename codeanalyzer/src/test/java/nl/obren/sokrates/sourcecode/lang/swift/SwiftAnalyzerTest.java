/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.swift;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SwiftAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = SwiftExampleFragments.FRAGMENT_1;

        SwiftAnalyzer analyzer = new SwiftAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(SwiftExampleFragments.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    @Ignore
    public void cleanForLinesOfCodeCalculationsNestedComments() {
        String code = SwiftExampleFragments.COMMENT_NESTED;

        SwiftAnalyzer analyzer = new SwiftAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(SwiftExampleFragments.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = SwiftExampleFragments.FRAGMENT_1;

        SwiftAnalyzer analyzer = new SwiftAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(SwiftExampleFragments.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        String code = SwiftExampleFragments.UNIT_1;

        SwiftAnalyzer analyzer = new SwiftAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(3, units.size());

        assertEquals("func minMax()", units.get(0).getShortName());
        assertEquals(12, units.get(0).getLinesOfCode());
        assertEquals(4, units.get(0).getMcCabeIndex());
        assertEquals(1, units.get(0).getNumberOfParameters());
        assertEquals(1, units.get(0).getStartLine());
        assertEquals(12, units.get(0).getEndLine());

        assertEquals("func printAndCount()", units.get(1).getShortName());
        assertEquals(4, units.get(1).getLinesOfCode());
        assertEquals(1, units.get(1).getMcCabeIndex());
        assertEquals(1, units.get(1).getNumberOfParameters());
        assertEquals(14, units.get(1).getStartLine());
        assertEquals(17, units.get(1).getEndLine());

        assertEquals("func printWithoutCounting()", units.get(2).getShortName());
        assertEquals(3, units.get(2).getLinesOfCode());
        assertEquals(1, units.get(2).getMcCabeIndex());
        assertEquals(1, units.get(2).getNumberOfParameters());
        assertEquals(18, units.get(2).getStartLine());
        assertEquals(20, units.get(2).getEndLine());
    }

    @Test
    public void extractInitUnits() {
        String code = SwiftExampleFragments.INIT_UNITS;

        SwiftAnalyzer analyzer = new SwiftAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(2, units.size());

        assertEquals("init()", units.get(0).getShortName());
        assertEquals(5, units.get(0).getLinesOfCode());
        assertEquals(1, units.get(0).getMcCabeIndex());
        assertEquals(3, units.get(0).getNumberOfParameters());
        assertEquals(3, units.get(0).getStartLine());
        assertEquals(7, units.get(0).getEndLine());

        assertEquals("init()", units.get(1).getShortName());
        assertEquals(5, units.get(1).getLinesOfCode());
        assertEquals(1, units.get(1).getMcCabeIndex());
        assertEquals(1, units.get(1).getNumberOfParameters());
        assertEquals(8, units.get(1).getStartLine());
        assertEquals(12, units.get(1).getEndLine());
    }
}
