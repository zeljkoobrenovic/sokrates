package nl.obren.sokrates.sourcecode.lang.plsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlSqlHeuristicUnitsExtractor {

    public List<UnitInfo> extractUnits(SourceFile sourceFile) {

        List<UnitInfo> units = new ArrayList<>();

        CleanedContent cleanedContent = getCleanContent(sourceFile);

        List<String> normalLines = sourceFile.getLines();
        List<String> lines = SourceCodeCleanerUtils.splitInLines(cleanedContent.getCleanedContent());

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex).trim();
            if (isUnitSignature(line) && !(line.contains("BODY") || line.contains("body"))) {
                String blockName = "";
                if(line.trim().startsWith("CREATE ") || line.trim().startsWith("create ")
                || line.trim().startsWith("PROCEDURE ") || line.trim().startsWith("procedure ")
                || line.trim().startsWith("FUNCTION ") || line.trim().startsWith("function ")) {
                    blockName = getName(line);
                }
                int endOfUnitBodyIndex = getEndOfUnitBodyIndex(lines, lineIndex, blockName);
                if (endOfUnitBodyIndex >= lineIndex) {
                    StringBuilder body = new StringBuilder();
                    for (int bodyIndex = cleanedContent.getFileLineIndexes().get(lineIndex);
                         bodyIndex <= cleanedContent.getFileLineIndexes().get(endOfUnitBodyIndex);
                         bodyIndex++) {
                        body.append(normalLines.get(bodyIndex)).append("\n");
                    }
                    UnitInfo unit = new UnitInfo();
                    unit.setSourceFile(sourceFile);
                    unit.setLinesOfCode(endOfUnitBodyIndex - lineIndex + 1);
                    unit.setCleanedBody(body.toString());
                    unit.setBody(body.toString());
                    unit.setStartLine(cleanedContent.getFileLineIndexes().get(lineIndex) + 1);
                    unit.setEndLine(cleanedContent.getFileLineIndexes().get(endOfUnitBodyIndex) + 1);
                    unit.setMcCabeIndex(getMcCabeIndex(body.toString()));
                    if(!blockName.isEmpty()) {
                        unit.setShortName(blockName);
                    } else {
                        unit.setShortName("AnonymousBlock");
                    }
                    unit.setNumberOfParameters(getNumberOfParameters(body.toString()));
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

    protected String extraCleanContent(String content) {
        String cleanedContent = emptyStrings(content);

        cleanedContent = SourceCodeCleanerUtils.normalizeLineEnds(cleanedContent);

        return cleanedContent;
    }

    private String emptyStrings(String cleanedContent) {
        cleanedContent = cleanedContent.replaceAll("'.*?'", "''");
        return cleanedContent;
    }

    private String getName(String line) {
        String name = "";
        String strippedLine = line.trim();
        List<String> literals = Arrays.asList("CREATE ", "OR REPLACE ", " TYPE ", "FUNCTION ", "PROCEDURE ",
                " PACKAGE ", " TRIGGER ", " BODY ", " IS", " AS", " AUTHID ", " DEFINER ");

        for (String literal : literals) {
            strippedLine = strippedLine.replace(literal, " ");
            strippedLine = strippedLine.replace(literal.toLowerCase(), " ");
        }
        strippedLine = strippedLine.trim();
        strippedLine = strippedLine.replace("\"", "");
        strippedLine = strippedLine.replace("'", "");

        if(strippedLine.contains("(")) {
            strippedLine = strippedLine.substring(0, strippedLine.indexOf("(")).trim();
        }

        if(strippedLine.contains("RETURN")) {
            strippedLine = strippedLine.substring(0, strippedLine.indexOf("RETURN")).trim();
        }

        if(strippedLine.contains("return")) {
            strippedLine = strippedLine.substring(0, strippedLine.indexOf("return")).trim();
        }

        if(strippedLine.contains(".")) {
            strippedLine = strippedLine.substring(strippedLine.indexOf(".")+1).trim();
        }

        name = strippedLine.trim();
        return name;
    }

    private int getNumberOfParameters(String body) {

        String firstLine = body.substring(0, body.indexOf("\n"));
        String bodyForSearch = body.replace("\n", " ");

        int startIndex = -1;
        if(firstLine.contains("(")) startIndex = bodyForSearch.indexOf("(");
        if (startIndex >= 0) {
            int endIndex = bodyForSearch.indexOf(")", startIndex + 1);
            if (endIndex > startIndex) {
                String paramString = bodyForSearch.substring(startIndex + 1, endIndex).trim();
                if (StringUtils.isNotBlank(paramString)) {
                    String[] params = paramString.split(",");
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

        List<String> mcCabeIndexLiterals = Arrays.asList(
                " IF ", " ELSE ", " ELSEIF ", " CASE ", " WHEN ", " LOOP ", " AND ", " OR "
        );
        int mcCabeIndex = 1;

        for (String literal : mcCabeIndexLiterals) {
            mcCabeIndex += StringUtils.countMatches(bodyForSearch, literal);
            mcCabeIndex += StringUtils.countMatches(bodyForSearch, literal.toLowerCase());
        }
//        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " OR ");
        mcCabeIndex -= StringUtils.countMatches(bodyForSearch, " REPLACE ");
        mcCabeIndex -= StringUtils.countMatches(bodyForSearch, "REPLACE".toLowerCase());

        return mcCabeIndex;
    }

    public int getEndOfUnitBodyIndex(List<String> lines, int startLineIndex, String blockName) {

        int index = startLineIndex + 1;
        for (String line : lines.subList(startLineIndex + 1, lines.size())) {
            if (!line.trim().isEmpty()) {
                if ((line.trim().startsWith("END;") || line.trim().startsWith("end;"))
                        || ((line.trim().startsWith("END ") || line.trim().startsWith("end "))
                        && line.contains(blockName))) {
                    return index - 1;
                }
            }
            index++;
        }

        return lines.size() - 1;
    }

    protected boolean isUnitSignature(String line) {
        return line.trim().startsWith("CREATE ")
                || line.trim().startsWith("create ")
                || line.trim().startsWith("DECLARE ")
                || line.trim().startsWith("declare ")
                || line.trim().startsWith("BEGIN ")
                || line.trim().startsWith("begin ")
                || line.trim().startsWith("PROCEDURE ")
                || line.trim().startsWith("procedure ")
                || line.trim().startsWith("FUNCTION ")
                || line.trim().startsWith("function ");
    }
}
