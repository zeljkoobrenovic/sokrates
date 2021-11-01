/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.objectpascal;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ObjectPascalAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = ObjectPascalCodeSamples.FRAGMENT_1;

        ObjectPascalAnalyzer analyzer = new ObjectPascalAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(ObjectPascalCodeSamples.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = ObjectPascalCodeSamples.FRAGMENT_1;

        ObjectPascalAnalyzer analyzer = new ObjectPascalAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(ObjectPascalCodeSamples.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        String code = ObjectPascalCodeSamples.UNIT1;

        ObjectPascalAnalyzer analyzer = new ObjectPascalAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(3, units.size());

        assertEquals("procedure THelloWorld.Put;", units.get(0).getShortName());
        assertEquals(4, units.get(0).getLinesOfCode());
        assertEquals(1, units.get(0).getMcCabeIndex());
        assertEquals(0, units.get(0).getNumberOfParameters());
        assertEquals(8, units.get(0).getStartLine());
        assertEquals(11, units.get(0).getEndLine());

        assertEquals("procedure THelloWorld.Free;", units.get(1).getShortName());
        assertEquals(3, units.get(1).getLinesOfCode());
        assertEquals(1, units.get(1).getMcCabeIndex());
        assertEquals(0, units.get(1).getNumberOfParameters());
        assertEquals(13, units.get(1).getStartLine());
        assertEquals(16, units.get(1).getEndLine());

        assertEquals("procedure THelloWorld.Init()", units.get(2).getShortName());
        assertEquals(7, units.get(2).getLinesOfCode());
        assertEquals(1, units.get(2).getMcCabeIndex());
        assertEquals(2, units.get(2).getNumberOfParameters());
        assertEquals(18, units.get(2).getStartLine());
        assertEquals(26, units.get(2).getEndLine());
    }
}
