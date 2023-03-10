package nl.obren.sokrates.sourcecode.lang.fsharp;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class FSharpHeuristicUnitsExtractor {

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
                        body.append(normalLines.get(bodyIndex)).append("\n");
                    }
                    UnitInfo unit = new UnitInfo();
                    unit.setStartLine(lineIndex);
                    unit.setEndLine(endOfUnitBodyIndex);
                    unit.setSourceFile(sourceFile);
                    unit.setLinesOfCode(endOfUnitBodyIndex - lineIndex + 1);
                    unit.setCleanedBody(body.toString());
                    unit.setBody(body.toString());
                    unit.setMcCabeIndex(getMcCabeIndex(body.toString()));

                    int noOfParams = 0;
                    String shortName = "";
                    boolean seenLet = false;
                    boolean seenInlineOrRec = false;

                    Queue<String> params = getParameters(body.toString());
                    while (!params.isEmpty()){
                        String token = params.poll();

                        if (token.equals(":")) { break; }
                        if (token.equals("let")) { seenLet = true; continue; }
                        if (token.equals("inline") || token.equals("rec")) { seenInlineOrRec = true; continue; }

                        if (seenLet || seenInlineOrRec) { shortName = token; seenLet = false; seenInlineOrRec = false; }

                        noOfParams++;
                    }

                    unit.setShortName(shortName);
                    unit.setNumberOfParameters(noOfParams);
                    lineIndex = endOfUnitBodyIndex;
                    units.add(unit);
                }
            }
        }
        return units;
    }

    private Queue<String> getParameters(String body) {
        Queue<String> result = new LinkedList<>();
        String bodyForSearch = " " + body.replace("\n", " ");
        int endIndex = bodyForSearch.indexOf(" = ");
        String paramString = bodyForSearch.substring(0, endIndex).trim();
        paramString = StringUtils.normalizeSpace(paramString.replaceAll("\\(.+?\\)", "p"));

        if (StringUtils.isNotBlank(paramString)) {
             result = new LinkedList<>(Arrays.asList(paramString.split(" ")));
        }
        
        return result;
    }

    protected boolean isUnitSignature(String line) {
        String l = replaceTabs(line).trim();
        return l.startsWith("let ");
    }

    private CleanedContent getCleanContent(SourceFile sourceFile) {
        CleanedContent normallyCleanedContent = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile)
                .cleanForLinesOfCodeCalculations(sourceFile);
        normallyCleanedContent.setCleanedContent(extraCleanContent(normallyCleanedContent.getCleanedContent()));
        return normallyCleanedContent;
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

    protected String extraCleanContent(String content) {
        String cleanedContent = emptyStrings(content);
        cleanedContent = SourceCodeCleanerUtils.normalizeLineEnds(cleanedContent);

        return cleanedContent;
    }

    private String emptyStrings(String cleanedContent) {
        cleanedContent = cleanedContent.replaceAll("\".*?\"", "\"\"");
        cleanedContent = cleanedContent.replaceAll("'.*?'", "''");
        return cleanedContent;
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
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " else ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " && ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " || ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " | ");

        return mcCabeIndex;
    }
}
