/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.yaml;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.lang.shell.ShellAnalyzer;
import nl.obren.sokrates.sourcecode.lang.shell.ShellCodeSamples;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class YamlAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        YamlAnalyzer analyzer = new YamlAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.rb"), YamlCodeSamples.SAMPLE_1));

        assertEquals(YamlCodeSamples.SAMPLE_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        YamlAnalyzer analyzer = new YamlAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.rb"), YamlCodeSamples.SAMPLE_1));

        assertEquals(YamlCodeSamples.SAMPLE_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }
}
