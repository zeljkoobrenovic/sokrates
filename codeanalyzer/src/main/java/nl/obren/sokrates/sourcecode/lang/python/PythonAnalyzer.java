package nl.obren.sokrates.sourcecode.lang.python;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsCleanerUtils;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public class PythonAnalyzer extends LanguageAnalyzer {
    public PythonAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        String content = CommentsCleanerUtils.cleanLineComments(sourceFile.getContent(), "#");
        content = CommentsCleanerUtils.cleanBlockComments(content, "\"\"\"", "\"\"\"");
        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
}

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = CommentsCleanerUtils.cleanLineComments(sourceFile.getContent(), "#");
        content = CommentsCleanerUtils.cleanBlockComments(content, "\"\"\"", "\"\"\"");
        content = CommentsCleanerUtils.cleanBlockComments(content, "'''", "'''");

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("from .*import.*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("import .*", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        return new PythonHeuristicUnitParser().extractUnits(sourceFile);
    }

    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new PythonDependenciesExtractor().extractDependencies(sourceFiles, progressFeedback);
    }


    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_ADVANCED_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_ADVANCED_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_BASIC_DEPENDENCIES_ANALYSIS);

        return features;
    }
}
