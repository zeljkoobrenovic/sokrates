/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.d;

import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

public class DHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    @Override
    public boolean isUnitSignature(String line) {
        return !line.replace("\t", " ").trim().startsWith("static if") && super.isUnitSignature(line);
    }
}
