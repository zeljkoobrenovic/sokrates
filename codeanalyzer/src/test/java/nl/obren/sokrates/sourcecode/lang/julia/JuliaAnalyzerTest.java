/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.julia;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JuliaAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = JuliaCodeSamples.FRAGMENT_1;

        JuliaAnalyzer analyzer = new JuliaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(JuliaCodeSamples.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }
    @Test
    public void cleanForLinesOfCodeCalculationsBig() {
        String code = JuliaCodeSamples.BIG_FRAGMENT;

        JuliaAnalyzer analyzer = new JuliaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(JuliaCodeSamples.BIG_FRAGMENT_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = JuliaCodeSamples.FRAGMENT_1;

        JuliaAnalyzer analyzer = new JuliaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(JuliaCodeSamples.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        String code = JuliaCodeSamples.FRAGMENT_1;

        JuliaAnalyzer analyzer = new JuliaAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(3, units.size());

        assertEquals("function()", units.get(0).getShortName());
        assertEquals(5, units.get(0).getLinesOfCode());
        assertEquals(1, units.get(0).getMcCabeIndex());
        assertEquals(1, units.get(0).getNumberOfParameters());
        assertEquals(1, units.get(0).getStartLine());
        assertEquals(5, units.get(0).getEndLine());

        assertEquals("@adjoint function pairwise()", units.get(1).getShortName());
        assertEquals(7, units.get(1).getLinesOfCode());
        assertEquals(2, units.get(1).getMcCabeIndex());
        assertEquals(2, units.get(1).getNumberOfParameters());
        assertEquals(7, units.get(1).getStartLine());
        assertEquals(13, units.get(1).getEndLine());

        assertEquals("function f()", units.get(2).getShortName());
        assertEquals(9, units.get(2).getLinesOfCode());
        assertEquals(3, units.get(2).getMcCabeIndex());
        assertEquals(0, units.get(2).getNumberOfParameters());
        assertEquals(20, units.get(2).getStartLine());
        assertEquals(28, units.get(2).getEndLine());
    }
}
