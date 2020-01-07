/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.r;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

public class RHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    @Override
    public boolean isUnitSignature(String line) {
        return RegexUtils.matchesEntirely("[ ]*[a-zA-Z_0-9]+[ ]* [<][-][ ]*function[ ]*[(].*", line);
    }
}
