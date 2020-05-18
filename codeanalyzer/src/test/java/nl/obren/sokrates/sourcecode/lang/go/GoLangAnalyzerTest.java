/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.go;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GoLangAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        GoLangAnalyzer analyzer = new GoLangAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("dummy.go"), GoExampleFragments.FRAGMENT_1);
        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertEquals(GoExampleFragments.FRAGMENT_1_CLEANED_FOR_LOC, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        GoLangAnalyzer analyzer = new GoLangAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("dummy.go"), GoExampleFragments.FRAGMENT_1);
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(GoExampleFragments.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        GoLangAnalyzer analyzer = new GoLangAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("dummy.go"), GoExampleFragments.FRAGMENT_1);
        List<UnitInfo> units = analyzer.extractUnits(sourceFile);

        assertEquals(1, units.size());
        assertEquals("func main()", units.get(0).getShortName());
        assertEquals("func main() {\n" +
                " fmt.Println(\"\")\n" +
                "}\n", units.get(0).getCleanedBody());
        assertEquals(3, units.get(0).getLinesOfCode());
        assertEquals(1, units.get(0).getMcCabeIndex());
    }
}
