/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.gradle;

import nl.obren.sokrates.sourcecode.lang.groovy.GroovyHeuristicUnitsExtractor;

public class GradleHeuristicUnitsExtractor extends GroovyHeuristicUnitsExtractor {
    @Override
    public boolean isUnitSignature(String line) {
        return line.trim().startsWith("task") && super.isUnitSignature(line);
    }
}
