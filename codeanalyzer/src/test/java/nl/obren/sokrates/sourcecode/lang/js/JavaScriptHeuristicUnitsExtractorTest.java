/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.js;

import org.junit.Test;

import static org.junit.Assert.*;

public class JavaScriptHeuristicUnitsExtractorTest {

    @Test
    public void isUnitSignature() {
    }

    @Test
    public void doesNotStartWithKeyword() {
        JavaScriptHeuristicUnitsExtractor unitsExtractor = new JavaScriptHeuristicUnitsExtractor();

        assertTrue(unitsExtractor.doesNotStartWithKeyword("myFunction() {"));
        assertTrue(unitsExtractor.doesNotStartWithKeyword("myFunction (param1, param2) {"));

        assertFalse(unitsExtractor.doesNotStartWithKeyword("while (true) {"));
        assertFalse(unitsExtractor.doesNotStartWithKeyword("while(true) {"));
        assertFalse(unitsExtractor.doesNotStartWithKeyword("if (a) {"));
        assertFalse(unitsExtractor.doesNotStartWithKeyword("if(a) {"));
        assertFalse(unitsExtractor.doesNotStartWithKeyword("switch(b) {"));
        assertFalse(unitsExtractor.doesNotStartWithKeyword("switch (b) {"));
        assertFalse(unitsExtractor.doesNotStartWithKeyword("catch (e) {"));
        assertFalse(unitsExtractor.doesNotStartWithKeyword("catch(e) {"));
    }
}
