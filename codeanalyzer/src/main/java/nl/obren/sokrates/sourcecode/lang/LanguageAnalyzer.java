package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.List;

public abstract class LanguageAnalyzer {
    public abstract CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile);

    public abstract CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile);

    public abstract List<UnitInfo> extractUnits(SourceFile sourceFile);

    public abstract DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback);
}
