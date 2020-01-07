/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.vb;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VisualBasicHeuristicUnitsExtractor {
    private VisualBasicAnalyzer analyzer;

    public VisualBasicHeuristicUnitsExtractor(VisualBasicAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        List<UnitInfo> units = new ArrayList<>();

        CommentsAndEmptyLinesCleaner cleaner = analyzer.getCommentsAndEmptyLinesCleaner();
        List<String> lines = Arrays.asList(cleaner.cleanKeepEmptyLines(sourceFile.getContent()).split("\n"));

        List<String> unitEnds = Arrays.asList("End Sub", "End Function");
        int loc = 0;
        int endLine = 0;
        for (int i = lines.size() - 1; i > 0; i--) {
            String line = lines.get(i);
            for (String unitEnd : unitEnds) {
                if (line.trim().equalsIgnoreCase(unitEnd)) {
                    endLine = i;
                    loc = 0;
                    String prefix = line.substring(0, line.indexOf(unitEnd));
                    String body = line;
                    String cleanedBody = line;
                    i--;
                    while (i >= 0) {
                        line = lines.get(i);
                        body = line + "\n" + body;
                        if (line.length() > prefix.length() && line.startsWith(prefix) && line.charAt(prefix.length()) != ' ') {
                            units.add(0, getUnitInfo(sourceFile, loc, endLine, i, line, body, cleanedBody));
                            break;
                        } else {
                            if (!line.trim().isEmpty()) {
                                cleanedBody = line + "\n" + cleanedBody;
                                loc += 1;
                            }
                        }
                        i--;
                    }
                }
            }
        }

        return units;
    }

    private UnitInfo getUnitInfo(SourceFile sourceFile, int loc, int endLine, int startLine, String line, String body, String cleanedBody) {
        UnitInfo unit = new UnitInfo();
        unit.setSourceFile(sourceFile);
        unit.setShortName(line.trim());
        unit.setLinesOfCode(loc);
        unit.setStartLine(startLine);
        unit.setEndLine(endLine);
        unit.setBody(body);
        unit.setCleanedBody(cleanedBody);

        updateParamsAndMcCabeIndex(unit, body);

        return unit;
    }

    private void updateParamsAndMcCabeIndex(UnitInfo unit, String body) {
        CommentsAndEmptyLinesCleaner cleaner = analyzer.getCommentsAndEmptyLinesCleanerExtraString();
        String content = cleaner.cleanKeepEmptyLines(body);

        String bodyForSearch = " " + content.replace("\n", " ");
        bodyForSearch = bodyForSearch.replace("(", " (");
        bodyForSearch = bodyForSearch.replace("{", " {");

        int startOfParamsSearchIndex = body.indexOf("(");
        int endOfParamsSearchIndex = body.indexOf(")", startOfParamsSearchIndex + 1);
        if (unit.getShortName().contains("(")) {
            unit.setShortName(unit.getShortName().substring(0, unit.getShortName().indexOf("(")).trim() + "()");
        }
        if (startOfParamsSearchIndex > 0 && endOfParamsSearchIndex > 0 && endOfParamsSearchIndex > startOfParamsSearchIndex) {
            String trimedParams = body.substring(startOfParamsSearchIndex + 1, endOfParamsSearchIndex).trim();
            if (!trimedParams.isEmpty()) {
                String[] params = trimedParams.split("\\,");
                unit.setNumberOfParameters(params.length);
            }
        }

        int mcCabeIndex = 1;
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " End If ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " ElseIf ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " Catch ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " Finally ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " While ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " Loop ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " For ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " And ");
        mcCabeIndex += StringUtils.countMatches(bodyForSearch, " Or ");

        unit.setMcCabeIndex(mcCabeIndex);
    }
}
