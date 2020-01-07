/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

public class PerlHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    @Override
    public boolean isUnitSignature(String line) {
        return RegexUtils.matchesEntirely(".*( |\t|)sub( |\t).*", line);
    }

    @Override
    public void setNameAndParameters(String line, UnitInfo unit, String cleandedBody) {
        super.setNameAndParameters(line, unit, cleandedBody);
        unit.setNumberOfParameters(0);
    }
}
