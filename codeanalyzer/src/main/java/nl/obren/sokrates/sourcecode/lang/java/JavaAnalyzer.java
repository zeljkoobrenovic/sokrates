package nl.obren.sokrates.sourcecode.lang.java;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitParser;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public class JavaAnalyzer extends LanguageAnalyzer {
    public JavaAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        CommentsAndEmptyLinesCleaner cleaner = getCommentsAndEmptyLinesCleaner();

        return cleaner.clean(sourceFile.getContent());
    }

    private CommentsAndEmptyLinesCleaner getCommentsAndEmptyLinesCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("/*", "*/");
        cleaner.addCommentBlockHelper("//", "\n");
        cleaner.addStringBlockHelper("\"", "\\");
        cleaner.addStringBlockHelper("'", "\\");
        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        CommentsAndEmptyLinesCleaner cleaner = getCommentsAndEmptyLinesCleaner();

        String content = cleaner.cleanRaw(sourceFile.getContent());

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("import .*;", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("package .*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[{]", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[}]", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        CStyleHeuristicUnitParser heuristicUnitParser = new CStyleHeuristicUnitParser() {
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

        };
        return heuristicUnitParser.extractUnits(sourceFile);
    }

    private boolean isStaticUnit(String line) {
        line = line.replace("\t", "");
        line = line.replace(" ", "");
        line = line.trim();
        return line.equalsIgnoreCase("static{");
    }


    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new JavaHeuristicDependenciesExtractor().extractDependencies(sourceFiles, progressFeedback);
    }

    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_ADVANCED_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_ADVANCED_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_ADVANCED_DEPENDENCIES_ANALYSIS + " (based on package names)");

        return features;
    }
}
