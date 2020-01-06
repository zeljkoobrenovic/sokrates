/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.rust;

import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

import java.util.Arrays;

public class RustHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    public RustHeuristicUnitsExtractor() {
        super.setMcCabeIndexLiterals(Arrays.asList(
                " if ",
                " loop ",
                " while ",
                " for ",
                "case ",
                "&&",
                "||",
                " catch "));
    }

    @Override
    public boolean isUnitSignature(String line) {
        return line.replace("\t", " ").trim().startsWith("fn ");
    }
}
