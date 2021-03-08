/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.adabasnatural;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.MetaDependencyRule;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdabasNaturalAnalyzer extends LanguageAnalyzer {
    public AdabasNaturalAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        return getCleaner().clean(getLinesWithoutComments(sourceFile));
    }

    private String getLinesWithoutComments(SourceFile sourceFile) {
        List<String> lines = sourceFile.getLines();
        List<String> linesWithoutComments = new ArrayList<>();
        lines.forEach(line -> {
            if (line.trim().startsWith("*")) {
                linesWithoutComments.add("");
            } else {
                linesWithoutComments.add(line);
            }
        });
        return linesWithoutComments.stream().collect(Collectors.joining("\n"));
    }

    private CommentsAndEmptyLinesCleaner getCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("/*", "\n");

        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = getCleaner().cleanKeepEmptyLines(getLinesWithoutComments(sourceFile));

        content = SourceCodeCleanerUtils.trimLines(content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        UnitInfo unitInfo = new UnitInfo();
        unitInfo.setShortName(sourceFile.getFile().getName());
        unitInfo.setSourceFile(sourceFile);
        unitInfo.setLinesOfCode(sourceFile.getLinesOfCode());
        String cleanedContent = this.cleanForLinesOfCodeCalculations(sourceFile).getCleanedContent();
        updateUnitInfo(cleanedContent, unitInfo);
        unitInfo.setBody(sourceFile.getContent());
        unitInfo.setCleanedBody(cleanedContent);
        unitInfo.setStartLine(0);
        unitInfo.setEndLine(sourceFile.getLinesOfCode());
        unitInfo.setLongName(sourceFile.getFile().getName());
        return Arrays.asList(unitInfo);
    }

    private void updateUnitInfo(String cleanedContent, UnitInfo unitInfo) {
        int index = 1;
        int params = 0;
        boolean inDecideBlock = false;
        boolean inDataParamsBlock = false;

        for (String line : cleanedContent.split("\n")) {
            String trimmedLine = line.trim() + " ";
            if (inDecideBlock) {
                if (trimmedLine.startsWith("END-DECIDE")) {
                    inDecideBlock = false;
                } else {
                        if (trimmedLine.startsWith("VALUE ") || trimmedLine.startsWith("NONE ")) {
                        index += 1;
                    }
                }
            } else if (inDataParamsBlock) {
                if (trimmedLine.startsWith("END-DEFINE")) {
                    inDataParamsBlock = false;
                } else {
                    if (trimmedLine.startsWith("USING ") || trimmedLine.contains("#")) {
                        params += 1;
                    }
                }
            } else {
                if (trimmedLine.startsWith("IF ")) {
                    index += 1;
                }
                if (trimmedLine.startsWith("DECIDE ")) {
                    inDecideBlock = true;
                }
                if (trimmedLine.startsWith("DEFINE DATA PARAMETER")) {
                    inDataParamsBlock = true;
                }
                if (trimmedLine.startsWith("FIND ")) {
                    index += 1;
                }
            }
        }

        unitInfo.setMcCabeIndex(index);
        unitInfo.setNumberOfParameters(params);
    }


    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new DependenciesAnalysis();
    }

    @Override
    public List<MetaDependencyRule> getMetaDependencyRules() {
        MetaDependencyRule using = new MetaDependencyRule("", "[ ]*USING[ ]+.*", "content");
        using.getNameOperations().add(new OperationStatement("replace", Arrays.asList("[ ]*USING[ ]+", "")));
        return Arrays.asList(using);
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
