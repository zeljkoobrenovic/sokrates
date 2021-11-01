/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

import nl.obren.sokrates.sourcecode.duplication.impl.DuplicateRange;

public class DuplicateRangePair {
    private DuplicateRange range1;
    private DuplicateRange range2;

    public DuplicateRangePair(DuplicateRange range1, DuplicateRange range2) {
        this.range1 = range1;
        this.range2 = range2;
    }

    public boolean includes(DuplicateRange otherRange1, DuplicateRange otherRange2) {
        return range1.includes(otherRange1) && range2.includes(otherRange2);
    }

    public DuplicateRange getRange1() {
        return range1;
    }

    public void setRange1(DuplicateRange range1) {
        this.range1 = range1;
    }

    public DuplicateRange getRange2() {
        return range2;
    }

    public void setRange2(DuplicateRange range2) {
        this.range2 = range2;
    }
}
