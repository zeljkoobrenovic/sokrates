/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.objectpascal;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObjectPascalHeuristicUnitsExtractor {
    private UnitInfo unit = null;
    private StringBuilder unitBody = null;
    private StringBuilder cleanedUnitBody = null;
    private String prefix = "";
    private int loc = 0;
    private SourceFile sourceFile;
    private String cleanedFileContent;
    private ObjectPascalAnalyzer analyzer;

    public ObjectPascalHeuristicUnitsExtractor(SourceFile sourceFile, String cleanedFileContent, ObjectPascalAnalyzer analyzer) {
        this.sourceFile = sourceFile;
        this.cleanedFileContent = cleanedFileContent;
        this.analyzer = analyzer;
    }

    public List<UnitInfo> extractUnits() {
        List<UnitInfo> units = new ArrayList<>();

        List<String> lines = Arrays.asList(cleanedFileContent.replace("\t", "    ").split("\n"));

        int lineIndex = 1;
        for (String line : lines) {
            if (isFunctionStartLine(line)) {
                initUnit(units, line, lineIndex);
            } else if (unit != null) {
                updateUnit(line);

                if (line.equals(prefix + "end;")) {
                    endUnit(lineIndex);
                }
            }
            lineIndex++;
        }

        return units;
    }

    private boolean isFunctionStartLine(String line) {
        String trimmedLine = line.trim().replace("\t", " ");
        return RegexUtils.matchesEntirely("(class[ ]+)?(procedure|function|method)[ ]+([a-zA-Z0-9_]+[.])+[a-zA-Z0-9_]+[ ]*(\\(|;).*", trimmedLine);
    }

    private void updateUnit(String line) {
        unitBody.append(line).append("\n");
        if (!line.trim().isEmpty()) {
            cleanedUnitBody.append(line).append("\n");
            loc += 1;
        }
    }

    private void endUnit(int lineIndex) {
        unit.setBody(unitBody.toString());
        unit.setCleanedBody(cleanedUnitBody.toString());
        unit.setLinesOfCode(loc);
        unit.setEndLine(lineIndex);

        updateParamsAndMcCabeIndex(unitBody.toString());
        unit = null;
        unitBody = null;
        cleanedUnitBody = null;
    }

    private void initUnit(List<UnitInfo> units, String line, int lineIndex) {
        String trimmedLine = line.trim();
        unit = new UnitInfo();
        units.add(unit);
        unit.setStartLine(lineIndex);

        unit.setSourceFile(sourceFile);

        prefix = "";
        int i = 0;
        while (line.charAt(i++) == ' ') {
            prefix += ' ';
        }

        unitBody = new StringBuilder();
        unitBody.append(line).append("\n");

        cleanedUnitBody = new StringBuilder();
        cleanedUnitBody.append(line).append("\n");

        unit.setShortName(trimmedLine.replaceAll("[(].*", "()"));
        loc = 1;
    }

    private CommentsAndEmptyLinesCleaner getCommentsAndEmptyLinesCleanerExtraString() {
        CommentsAndEmptyLinesCleaner cleaner = analyzer.getCommentsAndEmptyLinesCleaner();
        cleaner.getCodeBlockParsers().forEach(codeBlockParser -> codeBlockParser.setRemoveWhenCleaning(true));
        return cleaner;
    }

    private void updateParamsAndMcCabeIndex(String body) {
        CommentsAndEmptyLinesCleaner cleaner = getCommentsAndEmptyLinesCleanerExtraString();
        String content = cleaner.cleanKeepEmptyLines(body);

        String bodyForSearch = " " + content.replace("\n", " ");
        bodyForSearch = bodyForSearch.replace("(", " (");

        if (unit.getShortName().contains("(")) {
            unit.setShortName(unit.getShortName().substring(0, unit.getShortName().indexOf("(")).trim() + "()");
            int startOfParamsSearchIndex = body.indexOf("(");
            int endOfParamsSearchIndex = body.indexOf(")", startOfParamsSearchIndex + 1);
            if (startOfParamsSearchIndex > 0 && endOfParamsSearchIndex > 0 && endOfParamsSearchIndex > startOfParamsSearchIndex) {
                String trimmedParams = body.substring(startOfParamsSearchIndex + 1, endOfParamsSearchIndex).trim();
                if (!trimmedParams.isEmpty()) {
                    String[] params = trimmedParams.split("\\;");
                    unit.setNumberOfParameters(params.length);
                }
            }
        }

        bodyForSearch = bodyForSearch.toLowerCase();

        int mcCabeIndex = 1;
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " if ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " repeat ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " while ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " for ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " case ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " AND");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " OR ");

        unit.setMcCabeIndex(mcCabeIndex);
    }
}
