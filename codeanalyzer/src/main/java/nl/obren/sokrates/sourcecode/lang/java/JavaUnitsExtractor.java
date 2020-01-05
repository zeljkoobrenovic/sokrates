/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.java;

import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitParser;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.Arrays;
import java.util.List;

public class JavaUnitsExtractor extends CStyleHeuristicUnitParser {
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

    @Override
    public List<String> getMcCabeIndexLiterals() {
        return Arrays.asList(" if ", " while ", " for ", " case ", "&&", "||", " ? ", " catch ");
    }
}
