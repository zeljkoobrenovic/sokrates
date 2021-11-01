/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.search;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class FoundLineTest {
    @Test
    public void testToString() throws Exception {
        assertEquals(new FoundLine(42, "line abcd", "abcd").toString(), "Line 42: line abcd");
    }

}
