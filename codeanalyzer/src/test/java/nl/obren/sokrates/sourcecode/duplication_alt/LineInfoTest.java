/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication_alt;

import nl.obren.sokrates.sourcecode.duplication.impl.LineInfo;
import org.junit.Test;

import static org.junit.Assert.*;

public class LineInfoTest {
    @Test
    public void getId() throws Exception {
        assertEquals(new LineInfo(0).getId(), 0);
        assertEquals(new LineInfo(1).getId(), 1);
        assertEquals(new LineInfo(2).getId(), 2);
        assertEquals(new LineInfo(3).getId(), 3);
    }

}
