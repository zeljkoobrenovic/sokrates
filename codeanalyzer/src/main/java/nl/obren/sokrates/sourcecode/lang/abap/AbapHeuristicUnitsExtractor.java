/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.abap;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AbapHeuristicUnitsExtractor {

   
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
                        body.append(normalLines.get(bodyIndex) + "\n");
                    }
                    UnitInfo unit = new UnitInfo();
                    unit.setSourceFile(sourceFile);
                    unit.setLinesOfCode(endOfUnitBodyIndex - lineIndex + 1);
                    unit.setCleanedBody(body.toString());
                    unit.setBody(body.toString());
                    unit.setMcCabeIndex(getMcCabeIndex(body.toString()));
                    unit.setShortName(line.trim().replace("METHOD ", "").replace(".", "").trim());
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
        return 0;
    }

    private int getMcCabeIndex(String body) {
        String bodyForSearch = " " + body.replace("\n", " ");
        bodyForSearch = bodyForSearch.replace("(", " (");
        
        int mcCabeIndex = 1;
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " IF ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " ELSE ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " ELSEIF ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " WHILE ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " LOOP ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " CASE ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " WHEN ");

        return mcCabeIndex;
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

    
    public int getEndOfUnitBodyIndex(List<String> lines, int startLineIndex) {
        
        int index = startLineIndex + 1;
        for (String line : lines.subList(startLineIndex + 1, lines.size())) {
            if (!line.trim().isEmpty()) {
                if (line.trim().startsWith("ENDMETHOD.")) {
                    return index - 1;
                }
            }
            index++;
        }

        return lines.size() - 1;
    }

    protected boolean isUnitSignature(String line) {
        return line.trim().startsWith("METHOD ");
    }
}
