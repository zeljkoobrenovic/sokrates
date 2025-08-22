/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.kotlin;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class KotlinAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = KotlinExampleFragments.FRAGMENT_1;

        KotlinAnalyzer analyzer = new KotlinAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(KotlinExampleFragments.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = KotlinExampleFragments.FRAGMENT_1;

        KotlinAnalyzer analyzer = new KotlinAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(KotlinExampleFragments.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        String code = KotlinExampleFragments.FRAGMENT_1;

        KotlinAnalyzer analyzer = new KotlinAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(2, units.size());

        assertEquals("fun main()", units.get(0).getShortName());
        assertEquals(4, units.get(0).getLinesOfCode());
        assertEquals(1, units.get(0).getMcCabeIndex());
        assertEquals(0, units.get(0).getNumberOfParameters());
        assertEquals(2, units.get(0).getStartLine());
        assertEquals(5, units.get(0).getEndLine());

        assertEquals("fun main()", units.get(1).getShortName());
        assertEquals(5, units.get(1).getLinesOfCode());
        assertEquals(2, units.get(1).getMcCabeIndex());
        assertEquals(1, units.get(1).getNumberOfParameters());
        assertEquals(7, units.get(1).getStartLine());
        assertEquals(11, units.get(1).getEndLine());
    }

    @Test
    public void extractUnitsFromInterfaceMethods() {
        String code = KotlinExampleFragments.INTERFACE_METHODS_FRAGMENT;

        KotlinAnalyzer analyzer = new KotlinAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        // Should extract 4 units: 3 interface methods (0 LOC each) + 1 implemented method
        assertEquals(4, units.size());

        // Interface methods should have 0 lines of code
        assertEquals("fun findAllByPropertyId()", units.get(0).getShortName());
        assertEquals(0, units.get(0).getLinesOfCode());

        assertEquals("fun findAllByPropertyIdAndUnitId()", units.get(1).getShortName());
        assertEquals(0, units.get(1).getLinesOfCode());

        assertEquals("fun simpleMethod()", units.get(2).getShortName());
        assertEquals(0, units.get(2).getLinesOfCode());

        // Implemented method should have > 0 lines of code
        assertEquals("fun implementedMethod()", units.get(3).getShortName());
        assertEquals(3, units.get(3).getLinesOfCode());
    }

    @Test
    public void extractUnitsWithBraceOnNewLine() {
        String code = KotlinExampleFragments.BRACE_ON_NEW_LINE_FRAGMENT;

        KotlinAnalyzer analyzer = new KotlinAnalyzer();
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        // Should extract 2 units: 1 interface method (0 LOC) + 1 implemented method (4 LOC)
        assertEquals(2, units.size());

        // Interface method should have 0 lines of code (brace is far away, but still no implementation)
        assertEquals("fun interfaceMethodWithBraceOnNewLine()", units.get(0).getShortName());
        assertEquals(0, units.get(0).getLinesOfCode());

        // Implemented method should have > 0 lines of code (brace on separate line)
        assertEquals("fun implementedMethodWithBraceOnNewLine()", units.get(1).getShortName());
        assertEquals(7, units.get(1).getLinesOfCode()); // 4 lines signature + 1 line brace + 1 line return + 1 line closing brace
    }

}
