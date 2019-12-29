/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.js;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.RegexUtils;
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

public class JavaScriptAnalyzer extends LanguageAnalyzer {
    public JavaScriptAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        return getCleaner().clean(sourceFile.getContent());
    }

    private CommentsAndEmptyLinesCleaner getCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("/*", "*/");
        cleaner.addCommentBlockHelper("//", "\n");
        cleaner.addStringBlockHelper("\"", "\\");
        cleaner.addStringBlockHelper("'", "\\");
        cleaner.addStringBlockHelper("`", "\\");
        cleaner.addStringBlockHelper("/", "\\");

        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = getCleaner().cleanKeepEmptyLines(sourceFile.getContent());

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[{]", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[}]", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("import.*", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        CStyleHeuristicUnitParser heuristicUnitParser = new CStyleHeuristicUnitParser() {
            @Override
            public boolean isUnitSignature(String line) {
                return super.isUnitSignature(line) || isFunction(line);
            }
        };
        heuristicUnitParser.setExtractRecursively(true);
        List<UnitInfo> units = heuristicUnitParser.extractUnits(sourceFile);
        return units;
    }

    private boolean isFunction(String line) {
        String idRegex = "[a-zA-Z_$][a-zA-Z_$0-9]*";
        return !line.contains(";")
                && doesNotStartWithKeyword(line)
                && (RegexUtils.matchesEntirely("[ ]*function[ ]*[(].*", line)
                || RegexUtils.matchesEntirely(".*(=|:)[ ]*function[ ]*[(].*", line)
                || RegexUtils.matchesEntirely("[ ]*[(][ ]*function[ ]*[(].*", line)
                || RegexUtils.matchesEntirely("[ ]*define[(][ ]*function[ ]*[(].*", line)
                || RegexUtils.matchesEntirely("[ ]*" + idRegex + "[(].*?[)][ ]*[{][ ]*", line));
    }

    public boolean doesNotStartWithKeyword(String line) {
        String controlFlowKeywords[] = new String[]{"if", "while", "for", "switch", "catch"};

        for (String keyword : controlFlowKeywords) {
            if (line.trim().startsWith(keyword + " ") || line.trim().startsWith(keyword + "(")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new DependenciesAnalysis();
    }


    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_NO_DEPENDENCIES_ANALYSIS);

        return features;
    }

}
