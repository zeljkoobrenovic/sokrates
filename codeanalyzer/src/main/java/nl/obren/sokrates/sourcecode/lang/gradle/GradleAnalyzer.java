/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.gradle;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.lang.groovy.GroovyAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.List;

public class GradleAnalyzer extends GroovyAnalyzer {
    public GradleAnalyzer() {
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        return new GradleHeuristicUnitsExtractor().extractUnits(sourceFile);
    }
}
