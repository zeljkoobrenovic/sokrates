/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.shell;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class ShellAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        ShellAnalyzer analyzer = new ShellAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.sh"), ShellCodeSamples.SAMPLE_1));

        assertEquals(ShellCodeSamples.SAMPLE_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        ShellAnalyzer analyzer = new ShellAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.sh"), ShellCodeSamples.SAMPLE_1));

        assertEquals(ShellCodeSamples.SAMPLE_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
    }
}
