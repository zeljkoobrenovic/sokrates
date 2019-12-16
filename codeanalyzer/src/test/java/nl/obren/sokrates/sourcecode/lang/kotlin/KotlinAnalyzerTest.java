/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
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
}
