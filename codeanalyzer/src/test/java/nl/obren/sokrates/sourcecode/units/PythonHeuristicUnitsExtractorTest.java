/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.python.PythonHeuristicUnitsExtractor;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class PythonHeuristicUnitsExtractorTest {
    @Test
    public void replaceTabs() throws Exception {
        assertEquals(PythonHeuristicUnitsExtractor.replaceTabs("\t"), "    ");
        assertEquals(PythonHeuristicUnitsExtractor.replaceTabs(" \t"), "     ");
        assertEquals(PythonHeuristicUnitsExtractor.replaceTabs(" \t "), "      ");
        assertEquals(PythonHeuristicUnitsExtractor.replaceTabs("\t "), "     ");
    }

    @Test
    public void getLineIndentLevel() throws Exception {
        assertEquals(PythonHeuristicUnitsExtractor.getLineIndentLevel("def a():"), 0);
        assertEquals(PythonHeuristicUnitsExtractor.getLineIndentLevel(""), 0);
        assertEquals(PythonHeuristicUnitsExtractor.getLineIndentLevel(" def a():"), 1);
        assertEquals(PythonHeuristicUnitsExtractor.getLineIndentLevel("\tdef a():"), 4);
        assertEquals(PythonHeuristicUnitsExtractor.getLineIndentLevel("if a"), 0);
    }

    @Test
    public void getEndOfUnitBodyIndex() throws Exception {
        assertEquals(new PythonHeuristicUnitsExtractor().getEndOfUnitBodyIndex(SourceCodeCleanerUtils.splitInLines("def a():\n  a\n  b"), 0), 2);
    }

}
