/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.python.PythonHeuristicUnitParser;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class PythonHeuristicUnitParserTest {
    @Test
    public void replaceTabs() throws Exception {
        assertEquals(PythonHeuristicUnitParser.replaceTabs("\t"), "    ");
        assertEquals(PythonHeuristicUnitParser.replaceTabs(" \t"), "     ");
        assertEquals(PythonHeuristicUnitParser.replaceTabs(" \t "), "      ");
        assertEquals(PythonHeuristicUnitParser.replaceTabs("\t "), "     ");
    }

    @Test
    public void getLineIndentLevel() throws Exception {
        assertEquals(PythonHeuristicUnitParser.getLineIndentLevel("def a():"), 0);
        assertEquals(PythonHeuristicUnitParser.getLineIndentLevel(""), 0);
        assertEquals(PythonHeuristicUnitParser.getLineIndentLevel(" def a():"), 1);
        assertEquals(PythonHeuristicUnitParser.getLineIndentLevel("\tdef a():"), 4);
        assertEquals(PythonHeuristicUnitParser.getLineIndentLevel("if a"), 0);
    }

    @Test
    public void getEndOfUnitBodyIndex() throws Exception {
        assertEquals(new PythonHeuristicUnitParser().getEndOfUnitBodyIndex(SourceCodeCleanerUtils.splitInLines("def a():\n  a\n  b"), 0), 2);
    }

}
