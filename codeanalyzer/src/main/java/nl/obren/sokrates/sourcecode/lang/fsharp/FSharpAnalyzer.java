package nl.obren.sokrates.sourcecode.lang.fsharp;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FSharpAnalyzer extends LanguageAnalyzer {

    public FSharpAnalyzer() {

    }

    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        return getCleaner().clean(sourceFile.getContent());
    }

    private CommentsAndEmptyLinesCleaner getCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("(*", "*)");
        cleaner.addCommentBlockHelper("//", "\n");
        cleaner.addStringBlockHelper("\"", "\\");
        cleaner.addStringBlockHelper("@\"", "\"", "\"");
        cleaner.addStringBlockHelper("'", "\\");

        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = getCleaner().cleanKeepEmptyLines(sourceFile.getContent());

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("namespace .*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("open .*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("begin", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("end", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("with", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("done", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("in", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        return new FSharpHeuristicUnitsExtractor().extractUnits(sourceFile);
    }

    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new FSharpHeuristicDependenciesExtractor().extractDependencies(sourceFiles, progressFeedback);
    }

    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_ADVANCED_DEPENDENCIES_ANALYSIS + " (based on namespace heuristics)");

        return features;
    }
}
