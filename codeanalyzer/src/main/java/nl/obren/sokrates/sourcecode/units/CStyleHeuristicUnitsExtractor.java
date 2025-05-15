/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CStyleHeuristicUnitsExtractor {
    private boolean extractRecursively = false;

    private SourceFile sourceFile;
    private CleanedContent cleanedContent;

    private List<UnitInfo> units;
    private List<String> normalLines;
    private List<String> cleanedLines;
    private List<UnitInfo> previousUnits;

    private List<String> mcCabeIndexLiterals = Arrays.asList(
            " if ",
            " while ",
            " for ",
            " foreach ",
            "case ",
            "&&",
            "||",
            " ? ",
            " catch ");

    // for languages with embedded units (e.g. JavaScript functions)
    public void setExtractRecursively(boolean extractRecursively) {
        this.extractRecursively = extractRecursively;
    }

    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        initDataStructures(sourceFile);

        for (int lineIndex = 0; lineIndex < cleanedLines.size(); lineIndex++) {
            String line = cleanedLines.get(lineIndex).trim();
            if (isUnitSignature(line)) {
                lineIndex = extractUnit(lineIndex, line);
            }
        }

        removeOverlaps(units);
        return units;
    }
    public int getMcCabeIndex(SourceFile sourceFile) {
        String cleanedBody = getCleanedBody(cleanedLines, 0, cleanedLines.size());
        return getMcCabeIndex(cleanedBody);
    }

    private int extractUnit(int lineIndex, String line) {
        int endOfUnitBodyIndex = getEndOfUnitBodyIndex(cleanedLines, lineIndex);
        if (isValidLineRange(lineIndex, endOfUnitBodyIndex)) {
            UnitInfo unit = createUnitInfo(lineIndex, endOfUnitBodyIndex);

            String cleanedBody = getCleanedBody(cleanedLines, lineIndex, endOfUnitBodyIndex);
            unit.setCleanedBody(cleanedBody);
            unit.setBody(getNormalBody(cleanedContent, normalLines, lineIndex, endOfUnitBodyIndex));
            unit.setMcCabeIndex(getMcCabeIndex(cleanedBody));

            setNameAndParameters(line, unit, cleanedBody);

            units.add(unit);

            addChildren(previousUnits, unit);
            if (!extractRecursively) {
                lineIndex = endOfUnitBodyIndex;
            }
        }
        return lineIndex;
    }

    private UnitInfo createUnitInfo(int lineIndex, int endOfUnitBodyIndex) {
        UnitInfo unit = new UnitInfo();
        unit.setStartLine(cleanedContent.getFileLineIndexes().get(lineIndex) + 1);
        unit.setEndLine(cleanedContent.getFileLineIndexes().get(endOfUnitBodyIndex) + 1);
        unit.setSourceFile(sourceFile);
        unit.setLinesOfCode(endOfUnitBodyIndex - lineIndex + 1);
        return unit;
    }

    private void initDataStructures(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
        this.cleanedContent = getCleanContent(sourceFile);
        units = new ArrayList<>();
        normalLines = sourceFile.getLines();
        cleanedLines = SourceCodeCleanerUtils.splitInLines(cleanedContent.getCleanedContent());
        previousUnits = new ArrayList<>();
    }

    private void addChildren(List<UnitInfo> previousUnits, UnitInfo unit) {
        while (previousUnits.size() > 0) {
            UnitInfo previousUnit = previousUnits.get(previousUnits.size() - 1);
            if (isInsideUnit(previousUnit, unit)) {
                previousUnit.getChildren().add(unit);
                break;
            } else {
                previousUnits.remove(previousUnit);
            }
        }
        previousUnits.add(unit);
    }

    private void removeOverlaps(List<UnitInfo> units) {
        if (extractRecursively) {
            units.forEach(unit -> {
                removeOverlaps(unit);
            });
        }
    }

    private void removeOverlaps(UnitInfo unit) {
        unit.getChildren().forEach(childUnit -> {
            if (unit.getLinesOfCode() > childUnit.getLinesOfCode()) {
                unit.setLinesOfCode(unit.getLinesOfCode() - childUnit.getLinesOfCode());
            }
            if (unit.getMcCabeIndex() >= childUnit.getMcCabeIndex()) {
                unit.setMcCabeIndex(unit.getMcCabeIndex() - childUnit.getMcCabeIndex() + 1);
            }
            removeOverlaps(childUnit);
        });
    }

    private boolean isInsideUnit(UnitInfo parent, UnitInfo child) {
        return parent.getStartLine() < child.getStartLine()
                && parent.getEndLine() > child.getEndLine();
    }


    private String getNormalBody(CleanedContent cleanedContent, List<String> normalLines,
                                 int lineIndex, int endOfUnitBodyIndex) {
        StringBuilder body = new StringBuilder();
        if (isValidLineRange(lineIndex, endOfUnitBodyIndex) && isValidPhysicalFileLineRange(cleanedContent,
                lineIndex, endOfUnitBodyIndex)) {
            List<Integer> lineIndexes = cleanedContent.getFileLineIndexes();
            for (int bodyIndex = lineIndexes.get(lineIndex); bodyIndex < normalLines.size() && bodyIndex <= lineIndexes.get(endOfUnitBodyIndex); bodyIndex++) {
                body.append(normalLines.get(bodyIndex) + "\n");
            }
        }
        return body.toString();
    }

    private boolean isValidPhysicalFileLineRange(CleanedContent cleanedContent, int lineIndex, int endOfUnitBodyIndex) {
        return lineIndex < cleanedContent.getFileLineIndexes().size() && endOfUnitBodyIndex < cleanedContent
                .getFileLineIndexes().size();
    }

    private String getCleanedBody(List<String> cleanedLines, int lineIndex, int endOfUnitBodyIndex) {
        StringBuilder cleandedBody = new StringBuilder();
        if (isValidLineRange(lineIndex, endOfUnitBodyIndex)) {
            for (int bodyIndex = lineIndex; bodyIndex <= endOfUnitBodyIndex; bodyIndex++) {
                cleandedBody.append(StringUtils.appendIfMissing(cleanedLines.get(bodyIndex), "\n"));
            }
        }
        return cleandedBody.toString();
    }

    private boolean isValidLineRange(int lineIndex, int endOfUnitBodyIndex) {
        return endOfUnitBodyIndex >= lineIndex;
    }

    protected void setNameAndParameters(String line, UnitInfo unit, String cleanedBody) {
        int indexOfFirstBracket = line.indexOf("(");
        if (indexOfFirstBracket > 0) {
            unit.setShortName(line.substring(0, indexOfFirstBracket).trim() + "()");
        } else {
            unit.setShortName(line.replace("{", "").trim() + "()");
        }
        unit.setNumberOfParameters(getNumberOfParameters(cleanedBody));
    }

    private CleanedContent getCleanContent(SourceFile sourceFile) {
        LanguageAnalyzer languageAnalyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile);

        CleanedContent normallyCleanedContent = languageAnalyzer.cleanForLinesOfCodeCalculations(sourceFile);
        normallyCleanedContent.setCleanedContent(extraCleanContent(normallyCleanedContent.getCleanedContent()));

        return normallyCleanedContent;
    }

    private int getNumberOfParameters(String body) {
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
        bodyForSearch = bodyForSearch.replace("{", " {");

        int mcCabeIndex = 1;
        for (String mcCabeIndexLiteral : getMcCabeIndexLiterals()) {
            mcCabeIndex += StringUtils.countMatches(bodyForSearch, mcCabeIndexLiteral);
        }

        return mcCabeIndex;
    }

    protected String extraCleanContent(String content) {
        String cleanedContent = content.replace("\\\"", "");
        cleanedContent = cleanedContent.replace("://", ":/ /");
        cleanedContent = cleanedContent.replaceAll("@\".*?\"", "\"\"");
        cleanedContent = cleanedContent.replaceAll("\".*?\"", "\"\"");
        cleanedContent = cleanedContent.replaceAll("'.*?'", "''");
        cleanedContent = cleanedContent.replaceAll("/.+?/", "\"\"");
        cleanedContent = cleanedContent.replace("://", ":/ /");
        cleanedContent = cleanedContent.replaceAll("[<].*?[>]", "");
        cleanedContent = cleanedContent.replace("\t", " ");
        cleanedContent = SourceCodeCleanerUtils.normalizeLineEnds(cleanedContent);

        return cleanedContent;
    }

    protected int getEndOfUnitBodyIndex(List<String> lines, int startIndex) {
        StringBuilder unitBody = new StringBuilder();
        int startCount = 0;
        int endCount = 0;
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            unitBody.append(line + "\n");
            startCount += StringUtils.countMatches(line, "{");
            endCount += StringUtils.countMatches(line, "}");

            boolean hasValidBody = startCount > 0 && startCount == endCount;

            if(hasValidBody) {
                return i;
            }
        }

        return -1;
    }

    protected boolean isUnitSignature(String line) {
        line = extraCleanContent(line);
        if (line.contains("(") && !line.contains(";") && !line.contains("new ") && !line.trim().startsWith("else ")
                && !line.contains("return ") && !line.trim().startsWith("?") && !line.trim().startsWith(":")) {
            line = line.substring(0, line.indexOf("(") + 1);
            String identifierPattern = "[a-zA-Z0-9_$?:~]+";
            String startUnitRegex = "(" + identifierPattern + "[ ]+)+" + identifierPattern + "[ ]*[(]";
            Pattern pattern = Pattern.compile(startUnitRegex);
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }

    public List<String> getMcCabeIndexLiterals() {
        return mcCabeIndexLiterals;
    }

    public void setMcCabeIndexLiterals(List<String> mcCabeIndexLiterals) {
        this.mcCabeIndexLiterals = mcCabeIndexLiterals;
    }
}
