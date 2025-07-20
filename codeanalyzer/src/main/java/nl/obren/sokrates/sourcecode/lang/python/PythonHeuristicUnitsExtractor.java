/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.python;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PythonHeuristicUnitsExtractor {

    public static String replaceTabs(String line) {
        return line.replace("\t", "    ");
    }

    public static int getLineIndentLevel(String startLine) {
        String start = replaceTabs(startLine);
        int startLevel = 0;
        while (start.length() > startLevel && start.charAt(startLevel) == ' ') {
            startLevel++;
        }
        return startLevel;
    }

    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        List<UnitInfo> units = new ArrayList<>();

        CleanedContent cleanedContent = getCleanContent(sourceFile);

        List<String> normalLines = sourceFile.getLines();
        List<String> lines = SourceCodeCleanerUtils.splitInLines(cleanedContent.getCleanedContent());
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex).trim();
            if (isUnitSignature(line)) {
                int endOfUnitBodyIndex = getEndOfUnitBodyIndex(lines, lineIndex);
                if (endOfUnitBodyIndex >= lineIndex) {
                    StringBuilder body = new StringBuilder();
                    for (int bodyIndex = cleanedContent.getFileLineIndexes().get(lineIndex);
                         bodyIndex <= cleanedContent.getFileLineIndexes().get(endOfUnitBodyIndex);
                         bodyIndex++) {
                        if (bodyIndex < normalLines.size()) {
                            body.append(normalLines.get(bodyIndex) + "\n");
                        }
                    }
                    UnitInfo unit = new UnitInfo();
                    unit.setSourceFile(sourceFile);
                    unit.setLinesOfCode(endOfUnitBodyIndex - lineIndex + 1);
                    unit.setCleanedBody(body.toString());
                    unit.setBody(body.toString());
                    unit.setMcCabeIndex(getMcCabeIndex(body.toString()));
                    unit.setShortName(line.substring(0, line.indexOf("(")).trim() + "()");
                    unit.setNumberOfParameters(getNumerOfParameters(body.toString()));
                    lineIndex = endOfUnitBodyIndex;
                    units.add(unit);
                }
            }

        }

        return units;
    }

    private CleanedContent getCleanContent(SourceFile sourceFile) {
        CleanedContent normallyCleanedContent = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile)
                .cleanForLinesOfCodeCalculations(sourceFile);
        normallyCleanedContent.setCleanedContent(extraCleanContent(normallyCleanedContent.getCleanedContent()));
        return normallyCleanedContent;
    }

    private int getNumerOfParameters(String body) {
        String bodyForSearch = " " + body.replace("\n", " ");

        int startIndex = bodyForSearch.indexOf("(");
        if (startIndex >= 0) {
            int endIndex = bodyForSearch.indexOf(")", startIndex + 1);
            if (endIndex > startIndex) {
                String paramString = bodyForSearch.substring(startIndex + 1, endIndex).trim();
                if (StringUtils.isNotBlank(paramString)) {
                    String params[] = paramString.split(",");
                    return params.length;
                } else {
                    return 0;
                }
            }
        }

        return 0;
    }

    private int getMcCabeIndex(String body) {
        String bodyForSearch = " " + body.replace("\n", " ");
        bodyForSearch = bodyForSearch.replace("(", " (");
        bodyForSearch = bodyForSearch.replaceAll("\".*?\"", "\"\"");
        bodyForSearch = bodyForSearch.replaceAll("'.*?'", "''");

        int mcCabeIndex = 1;
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " if ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " elif ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " for ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " while ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " except ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " and ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " or ");

        return mcCabeIndex;
    }

    protected String extraCleanContent(String content) {
        String cleanedContent = espaceCommentLikeUrlStrings(content);
        cleanedContent = emptyStrings(cleanedContent);
        cleanedContent = replaceTabs(cleanedContent);
        cleanedContent = SourceCodeCleanerUtils.normalizeLineEnds(cleanedContent);

        return cleanedContent;
    }

    private String espaceCommentLikeUrlStrings(String content) {
        return content.replace("://", ":/ /");
    }

    private String emptyStrings(String cleanedContent) {
        cleanedContent = cleanedContent.replaceAll("\".*?\"", "\"\"");
        cleanedContent = cleanedContent.replaceAll("'.*?'", "''");
        return cleanedContent;
    }

    public int getEndOfUnitBodyIndex(List<String> lines, int startLineIndex) {
        int startLineIndentLevel = getLineIndentLevel(lines.get(startLineIndex));

        int index = startLineIndex + 1;
        for (String line : lines.subList(startLineIndex + 1, lines.size())) {
            if (!replaceTabs(line).trim().isEmpty()) {
                if (getLineIndentLevel(line) <= startLineIndentLevel) {
                    return index - 1;
                }
            }
            index++;
        }

        return lines.size() - 1;
    }

    protected boolean isUnitSignature(String line) {
        return replaceTabs(line).trim().startsWith("def ");
    }
}
