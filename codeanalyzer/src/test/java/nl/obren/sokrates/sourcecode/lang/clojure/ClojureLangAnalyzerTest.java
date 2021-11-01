/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.clojure;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClojureLangAnalyzerTest {
    private final String code = "; Stripped entirely\n" +
            ";; Appears in text section of marginalia\n" +
            "(defn foobar []\n" +
            "   ; Appears in code section of marginalia output\n" +
            "   ;; Again, appears in code section of marginalia output\n" +
            "   6)";

    @Test
    public void cleanForLinesOfCodeCalculations() {
        ClojureLangAnalyzer analyzer = new ClojureLangAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("dummy.clj"), code);
        assertEquals("(defn foobar []\n   6)", analyzer.cleanForLinesOfCodeCalculations(sourceFile).getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        ClojureLangAnalyzer analyzer = new ClojureLangAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("dummy.clj"), code);
        assertEquals("(defn foobar []\n" +
                "6)", analyzer.cleanForDuplicationCalculations(sourceFile).getCleanedContent());
    }

    @Test
    public void extractUnits() {
        ClojureLangAnalyzer analyzer = new ClojureLangAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("dummy.clj"), code);
        List<UnitInfo> units = analyzer.extractUnits(sourceFile);
        assertEquals(0, units.size());
    }
}
