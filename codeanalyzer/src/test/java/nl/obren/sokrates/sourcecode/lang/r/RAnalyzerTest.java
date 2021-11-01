/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.r;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.lang.yaml.YamlAnalyzer;
import nl.obren.sokrates.sourcecode.lang.yaml.YamlCodeSamples;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static nl.obren.sokrates.sourcecode.lang.r.RCodeSamples.*;
import static org.junit.Assert.*;

public class RAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
       RAnalyzer analyzer = new RAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.r"), EXAMPLE_1));

        assertEquals(EXAMPLE_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        RAnalyzer analyzer = new RAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.r"), EXAMPLE_1));

        assertEquals(EXAMPLE_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        RAnalyzer analyzer = new RAnalyzer();
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.r"), EXAMPLE_1));

        assertEquals(units.size(), 1);
        assertEquals(EXAMPLE_1_UNIT_CLEANED_BODY, units.get(0).getCleanedBody());
        assertEquals(units.get(0).getShortName(), "mysummary <- function()");
        assertEquals(units.get(0).getLinesOfCode(), 14);
        assertEquals(units.get(0).getMcCabeIndex(), 4);
        assertEquals(units.get(0).getNumberOfParameters(), 3);
    }
}
