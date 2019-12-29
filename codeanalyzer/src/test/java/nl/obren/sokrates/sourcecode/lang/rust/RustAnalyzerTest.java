/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
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
}
