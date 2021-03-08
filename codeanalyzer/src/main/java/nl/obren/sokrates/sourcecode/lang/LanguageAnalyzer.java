/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.MetaDependencyRule;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public abstract class LanguageAnalyzer {
    public static final String FEATURE_ALL_STANDARD_ANALYSES = "All basic standard analyses supported (source code overview, duplication, file size, concerns, findings, metrics, controls)";
    public static final String FEATURE_BASIC_CODE_CLEANING = "Basic code cleaning (empty lines removed for LOC calculations and duplication calculations)";
    public static final String FEATURE_ADVANCED_CODE_CLEANING = "Advanced code cleaning (empty lines and comments removed for LOC calculations, additional cleaning for duplication calculations)";
    public static final String FEATURE_UNIT_SIZE_ANALYSIS = "Unit size analysis";
    public static final String FEATURE_CONDITIONAL_COMPLEXITY_ANALYSIS = "Conditional complexity analysis";
    public static final String FEATURE_ADVANCED_DEPENDENCIES_ANALYSIS = "Advanced heuristic dependency analysis";

    public static final String FEATURE_NO_UNIT_SIZE_ANALYSIS = "No unit size analysis";
    public static final String FEATURE_NO_CONDITIONAL_COMPLEXITY_ANALYSIS = "No conditional complexity analysis";
    public static final String FEATURE_NO_DEPENDENCIES_ANALYSIS = "No dependency analysis";

    public static final String FEATURE_BASIC_DEPENDENCIES_ANALYSIS = "Basic heuristic dependency analysis";

    public abstract CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile);

    public abstract CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile);

    public abstract List<UnitInfo> extractUnits(SourceFile sourceFile);

    public abstract DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback);

    public abstract List<String> getFeaturesDescription();

    public List<MetaDependencyRule> getMetaDependencyRules() {
        return new ArrayList<>();
    }
}
