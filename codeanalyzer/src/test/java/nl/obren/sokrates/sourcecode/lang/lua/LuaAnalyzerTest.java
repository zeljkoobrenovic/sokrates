/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.lua;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LuaAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = LuaCodeSamples.FRAGMENT_1;

        LuaAnalyzer analyzer = new LuaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(LuaCodeSamples.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = LuaCodeSamples.FRAGMENT_1;

        LuaAnalyzer analyzer = new LuaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(LuaCodeSamples.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        String code = LuaCodeSamples.FRAGMENT_1;

        LuaAnalyzer analyzer = new LuaAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(2, units.size());

        assertEquals("function add()", units.get(0).getShortName());
        assertEquals(7, units.get(0).getLinesOfCode());
        assertEquals(2, units.get(0).getMcCabeIndex());
        assertEquals(1, units.get(0).getNumberOfParameters());
        assertEquals(2, units.get(0).getStartLine());
        assertEquals(8, units.get(0).getEndLine());

        assertEquals("function b()", units.get(1).getShortName());
        assertEquals(8, units.get(1).getLinesOfCode());
        assertEquals(4, units.get(1).getMcCabeIndex());
        assertEquals(0, units.get(1).getNumberOfParameters());
        assertEquals(9, units.get(1).getStartLine());
        assertEquals(24, units.get(1).getEndLine());
    }
}
