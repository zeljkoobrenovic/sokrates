package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitParser;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public class PerlAnalyzer extends LanguageAnalyzer {
    public PerlAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        String content = emptyComments(sourceFile);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = emptyComments(sourceFile);

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("use .*;", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("package .*", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    private String emptyComments(SourceFile sourceFile) {
        String content = sourceFile.getContent();

        content = SourceCodeCleanerUtils.emptyComments(content, "#", "\n=", "\n=cut").getCleanedContent();

        return content;
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        CStyleHeuristicUnitParser heuristicUnitParser = new CStyleHeuristicUnitParser() {
            @Override
            public boolean isUnitSignature(String line) {
                return RegexUtils.matchesEntirely(".*( |\t|)sub( |\t).*", line);
            }

            @Override
            public void setNameAndParameters(String line, UnitInfo unit, String cleandedBody) {
                super.setNameAndParameters(line, unit, cleandedBody);
                unit.setNumberOfParameters(0);
            }
        };
        return heuristicUnitParser.extractUnits(sourceFile);

    }


    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new PerlHeuristicDependenciesExtractor().extractDependencies(sourceFiles, progressFeedback);
    }

    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_ADVANCED_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_ADVANCED_CYCLOMATIC_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_BASIC_DEPENDENCIES_ANALYSIS);

        return features;
    }
}
