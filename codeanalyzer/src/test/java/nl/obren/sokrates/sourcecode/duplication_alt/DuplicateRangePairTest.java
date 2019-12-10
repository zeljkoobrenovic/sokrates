/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication_alt;

import nl.obren.sokrates.sourcecode.duplication.impl.DuplicateRange;
import nl.obren.sokrates.sourcecode.duplication.impl.DuplicateRangePair;
import org.junit.Test;
import static org.junit.Assert.*;

public class DuplicateRangePairTest {
    @Test
    public void includes() throws Exception {
        DuplicateRange range1 = new DuplicateRange(100, 200);
        DuplicateRange range2 = new DuplicateRange(400, 500);

        DuplicateRangePair pair = new DuplicateRangePair(range1, range2);

        assertTrue(pair.includes(new DuplicateRange(110, 190), new DuplicateRange(420, 480)));
        assertTrue(pair.includes(new DuplicateRange(100, 200), new DuplicateRange(400, 500)));
        assertTrue(pair.includes(new DuplicateRange(150, 200), new DuplicateRange(450, 500)));
        assertTrue(pair.includes(new DuplicateRange(100, 150), new DuplicateRange(400, 450)));

        // both out of range
        assertFalse(pair.includes(new DuplicateRange(80, 210), new DuplicateRange(380, 520)));

        // one out of range
        assertFalse(pair.includes(new DuplicateRange(110, 190), new DuplicateRange(380, 520)));
        assertFalse(pair.includes(new DuplicateRange(80, 210), new DuplicateRange(420, 480)));
    }

}
