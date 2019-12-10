/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication_alt;

import nl.obren.sokrates.sourcecode.duplication.impl.DuplicateRange;
import nl.obren.sokrates.sourcecode.duplication.impl.DuplicateRangePair;
import nl.obren.sokrates.sourcecode.duplication.impl.DuplicateRangePairs;
import org.junit.Test;

import static org.junit.Assert.*;

public class DuplicateRangePairsTest {
    @Test
    public void includes() throws Exception {
        DuplicateRangePairs pairs = new DuplicateRangePairs();

        assertFalse(pairs.includes(new DuplicateRangePair(new DuplicateRange(110, 190), new DuplicateRange(410, 490))));
        assertFalse(pairs.includes(new DuplicateRangePair(new DuplicateRange(1100, 200), new DuplicateRange(1410, 1490))));
        assertFalse(pairs.includes(new DuplicateRangePair(new DuplicateRange(2000, 2100), new DuplicateRange(3000, 3100))));

        DuplicateRange range1 = new DuplicateRange(100, 200);
        DuplicateRange range2 = new DuplicateRange(400, 500);
        DuplicateRange range3 = new DuplicateRange(1100, 1200);
        DuplicateRange range4 = new DuplicateRange(1400, 1500);

        DuplicateRangePair pair1 = new DuplicateRangePair(range1, range2);
        DuplicateRangePair pair2 = new DuplicateRangePair(range3, range4);

        pairs.getRanges().add(pair1);

        assertTrue(pairs.includes(new DuplicateRangePair(new DuplicateRange(110, 190), new DuplicateRange(410, 490))));
        assertFalse(pairs.includes(new DuplicateRangePair(new DuplicateRange(1100, 200), new DuplicateRange(1410, 1490))));
        assertFalse(pairs.includes(new DuplicateRangePair(new DuplicateRange(2000, 2100), new DuplicateRange(3000, 3100))));

        pairs.getRanges().add(pair2);

        assertTrue(pairs.includes(new DuplicateRangePair(new DuplicateRange(110, 190), new DuplicateRange(410, 490))));
        assertTrue(pairs.includes(new DuplicateRangePair(new DuplicateRange(1100, 200), new DuplicateRange(1410, 1490))));
        assertFalse(pairs.includes(new DuplicateRangePair(new DuplicateRange(2000, 2100), new DuplicateRange(3000, 3100))));
    }

}
