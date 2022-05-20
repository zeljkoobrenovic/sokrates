/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdabasNaturalAnalyzer extends LanguageAnalyzer {
    private CleanedContent cleanedContent;

    private List<String> cleanedLines;
    private List<UnitInfo> units;

    public AdabasNaturalAnalyzer() {
    }

    private List<String> unitLiterals = Arrays.asList(
            " function",
            " subroutine");
    private List<String> conditionalLiterals = Arrays.asList(
                " ACCEPT"," REJECT"," AT BREAK"," BEFORE BREAK PROCESSING", " DECIDE FOR"," DECIDE ON"," IF"," FOR", " REPEAT");

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        cleanedContent = getCleaner().clean(getLinesWithoutComments(sourceFile));
        return cleanedContent;
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

        units = new ArrayList<>();
        cleanedLines = SourceCodeCleanerUtils
                .splitInLines(cleanForLinesOfCodeCalculations(sourceFile).getCleanedContent());

        for (int lineIndex = 0; lineIndex < cleanedLines.size(); lineIndex++) {
            String line = cleanedLines.get(lineIndex).trim();
            if (isUnitSignature(line)) {
                lineIndex = extractUnit(sourceFile, lineIndex, line);
            }
        }

        return units;
    }

    private int extractUnit(SourceFile sourceFile, int lineIndex, String line) {
        int endOfUnitBodyIndex = getEndOfUnitBodyIndex(cleanedLines, lineIndex);
        if (endOfUnitBodyIndex >= lineIndex) {
            UnitInfo unitInfo = new UnitInfo();
            unitInfo.setShortName(getUnitName(line, sourceFile));
            unitInfo.setSourceFile(sourceFile);
            unitInfo.setLinesOfCode(endOfUnitBodyIndex - lineIndex);
            String cleanedUnitContent = getCleanedBody(lineIndex, endOfUnitBodyIndex);
            unitInfo.setCleanedBody(getCleanedBody(lineIndex, endOfUnitBodyIndex));
            unitInfo.setStartLine(cleanedContent.getFileLineIndexes().get(lineIndex) + 1);
            unitInfo.setBody(getBody(lineIndex, endOfUnitBodyIndex, sourceFile));
            unitInfo = updateUnitInfo(cleanedUnitContent, unitInfo);
            units.add(unitInfo);
        }
        return lineIndex;
    }

    private int getEndOfUnitBodyIndex(List<String> lines, int startIndex) {
        int startCount = 0;
        int endCount = 0;
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            for (String unitLiteral : unitLiterals) {
                startCount += StringUtils.countMatches(line.toLowerCase(), "define" + unitLiteral);
                endCount += StringUtils.countMatches(line.toLowerCase(), "end-" + unitLiteral.trim());
            }
            boolean hasValidBody = startCount > 0 && startCount == endCount;
            if (hasValidBody) {
                return i;
            }
        }

        return -1;
    }

    private String getCleanedBody(int lineIndex, int endOfUnitBodyIndex) {
        StringBuilder body = new StringBuilder();

        for (int bodyIndex = lineIndex; bodyIndex <= endOfUnitBodyIndex; bodyIndex++) {
            body.append(cleanedLines.get(bodyIndex) + "\n");
        }
        return body.toString();
    }

    private String getBody(int lineIndex, int endOfUnitBodyIndex, SourceFile sourceFile) {
        StringBuilder body = new StringBuilder();
        List<Integer> lineIndexes = cleanedContent.getFileLineIndexes();
        for (int bodyIndex = lineIndexes.get(lineIndex); bodyIndex <= lineIndexes
                .get(endOfUnitBodyIndex); bodyIndex++) {

            body.append(sourceFile.getLines().get(bodyIndex) + "\n");
        }

        return body.toString();
    }

    private boolean isUnitSignature(String line) {
        for (String unitLiteral : unitLiterals) {
            if (line.toLowerCase().contains("define" + unitLiteral)) {
                return true;
            }
        }
        return false;
    }

    private String getUnitName(String line, SourceFile sourceFile) {
        String unitName = sourceFile.getFile().getName();

        line = line.replace("DEFINE", "");
        for (String unitLiteral : unitLiterals) {
            line = line.replace(unitLiteral.toUpperCase(), "");
        }
        unitName = line.trim().split(" ")[0];
        return unitName;
    }

    private UnitInfo updateUnitInfo(String cleanedContent, UnitInfo unitInfo) {
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
                    if (trimmedLine.startsWith("VALUE ") || trimmedLine.startsWith("WHEN ") || trimmedLine.startsWith("NONE ")) {
                        index += 1;
                    }
                }
            } else if (inDataParamsBlock) {
                if (trimmedLine.startsWith("END-DEFINE")) {
                    inDataParamsBlock = false;
                } else {
                    if (trimmedLine.startsWith("USING ") ) {
                        params += 1;
                    }
                }
            } else {
                for (String mcCabeIndexLiteral : conditionalLiterals) {
                    index += StringUtils.countMatches(trimmedLine, mcCabeIndexLiteral);
                }
                
                if (trimmedLine.startsWith("DECIDE ")) {
                    inDecideBlock = true;
                }
                if (trimmedLine.startsWith("DEFINE DATA PARAMETER")) {
                    inDataParamsBlock = true;
                }
                
            }
        }

        unitInfo.setMcCabeIndex(index);
        unitInfo.setNumberOfParameters(params);
        return unitInfo;
    }

    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new AdabasDependenciesExtractor().extractDependencies(sourceFiles, progressFeedback);
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
        features.add(FEATURE_BASIC_DEPENDENCIES_ANALYSIS);

        return features;
    }
}
