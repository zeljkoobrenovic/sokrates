/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.groovy;

import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

public class GroovyHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    @Override
    public boolean isUnitSignature(String line) {
        return super.isUnitSignature(line) || isStaticUnit(line);
    }

    @Override
    public void setNameAndParameters(String line, UnitInfo unit, String cleanedBody) {
        if (isStaticUnit(line)) {
            unit.setShortName("static");
            unit.setNumberOfParameters(0);
        } else {
            super.setNameAndParameters(line, unit, cleanedBody);
        }
    }

    private boolean isStaticUnit(String line) {
        line = line.replace("\t", "");
        line = line.replace(" ", "");
        line = line.trim();
        return line.equalsIgnoreCase("static{");
    }
}
