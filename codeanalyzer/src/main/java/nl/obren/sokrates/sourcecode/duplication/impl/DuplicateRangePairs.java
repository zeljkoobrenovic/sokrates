/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

import nl.obren.sokrates.sourcecode.duplication.impl.DuplicateRangePair;

import java.util.List;
import java.util.ArrayList;

public class DuplicateRangePairs {
    private List<DuplicateRangePair> ranges = new ArrayList<>();

    public boolean includes(DuplicateRangePair duplicateRangePair) {
        for (DuplicateRangePair pair : ranges) {
            if (pair.includes(duplicateRangePair.getRange1(), duplicateRangePair.getRange2())) {
                return true;
            }
        }
        return false;
    }

    public List<DuplicateRangePair> getRanges() {
        return ranges;
    }

    public void setRanges(List<DuplicateRangePair> ranges) {
        this.ranges = ranges;
    }
}
