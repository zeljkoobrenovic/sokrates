/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.ts;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.lang.rust.RustAnalyzer;
import nl.obren.sokrates.sourcecode.lang.rust.RustCodeSamples;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TypeScriptAnalyzerTest {
    @Test
    public void extractUnits() {
        TypeScriptAnalyzer analyzer = new TypeScriptAnalyzer();
        String code = TypeScriptCodeFragments.UNITS_CODE;

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(3, units.size());

        assertEquals("public static clampedInt()", units.get(0).getShortName());
        assertEquals(14, units.get(0).getLinesOfCode());
        assertEquals(3, units.get(0).getMcCabeIndex());
        assertEquals(4, units.get(0).getNumberOfParameters());
        assertEquals(3, units.get(0).getStartLine());
        assertEquals(16, units.get(0).getEndLine());

        assertEquals("constructor()", units.get(1).getShortName());
        assertEquals(11, units.get(1).getLinesOfCode());
        assertEquals(2, units.get(1).getMcCabeIndex());
        assertEquals(6, units.get(1).getNumberOfParameters());
        assertEquals(21, units.get(1).getStartLine());
        assertEquals(31, units.get(1).getEndLine());

        assertEquals("public validate()", units.get(2).getShortName());
        assertEquals(3, units.get(2).getLinesOfCode());
        assertEquals(1, units.get(2).getMcCabeIndex());
        assertEquals(1, units.get(2).getNumberOfParameters());
        assertEquals(33, units.get(2).getStartLine());
        assertEquals(35, units.get(2).getEndLine());    }

}
