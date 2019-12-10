/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication_alt;

import nl.obren.sokrates.sourcecode.duplication.impl.DuplicateRange;
import org.junit.Test;

import static org.junit.Assert.*;

public class DuplicateRangeTest {
    @Test
    public void includes() throws Exception {
        DuplicateRange range = new DuplicateRange(1, 10);

        assertTrue(range.includes(new DuplicateRange(1, 10)));
        assertTrue(range.includes(new DuplicateRange(1, 5)));
        assertTrue(range.includes(new DuplicateRange(5, 10)));
        assertTrue(range.includes(new DuplicateRange(3, 7)));

        assertFalse(range.includes(new DuplicateRange(0, 10)));
        assertFalse(range.includes(new DuplicateRange(1, 11)));
        assertFalse(range.includes(new DuplicateRange(0, 13)));
        assertFalse(range.includes(new DuplicateRange(32, 77)));
    }

}
