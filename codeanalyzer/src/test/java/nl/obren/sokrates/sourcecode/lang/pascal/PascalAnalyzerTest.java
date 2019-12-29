/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.pascal;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class PascalAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = PascalCodeSamples.FRAGMENT_1;

        PascalAnalyzer analyzer = new PascalAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(PascalCodeSamples.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = PascalCodeSamples.FRAGMENT_1;

        PascalAnalyzer analyzer = new PascalAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(PascalCodeSamples.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
    }
}
