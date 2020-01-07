/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.scala;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ScalaHeuristicUnitsExtractorTest {
    @Test
    public void isUnitSignature() throws Exception {
        ScalaHeuristicUnitsExtractor parser = new ScalaHeuristicUnitsExtractor();

        assertTrue(parser.isUnitSignature("def method()"));

        assertFalse(parser.isUnitSignature(""));
        assertFalse(parser.isUnitSignature("return test();"));
        assertFalse(parser.isUnitSignature("new A().run();"));
    }

}
