/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PerlAnalyzer extends LanguageAnalyzer {
    public PerlAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        return getCleaner().clean(removePerlBlockComments(sourceFile));
    }

    private String removePerlBlockComments(SourceFile sourceFile) {
        List<String> lines = Arrays.asList(sourceFile.getContent().split("\n"));

        StringBuilder stringBuilder = new StringBuilder();

        boolean inBlock = false;
        for (String line : lines) {
            if (inBlock) {
                if (line.trim().equalsIgnoreCase("=cut")) {
                    inBlock = false;
                }
                stringBuilder.append("\n");
            } else {
                if (line.startsWith("=")) {
                    inBlock = true;
                } else {
                    stringBuilder.append(line);
                }
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }

    private CommentsAndEmptyLinesCleaner getCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("#", "\n");
        cleaner.addStringBlockHelper("\"", "\\");
        cleaner.addStringBlockHelper("'", "\\");
        cleaner.addStringBlockHelper("qq//", "/", "\\");
        cleaner.addStringBlockHelper("q//", "/", "\\");

        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = getCleaner().cleanKeepEmptyLines(removePerlBlockComments(sourceFile));

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("use .*;", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("package .*", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        return new PerlHeuristicUnitsExtractor().extractUnits(sourceFile);
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
        features.add(FEATURE_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_BASIC_DEPENDENCIES_ANALYSIS);

        return features;
    }
}
