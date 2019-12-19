/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.List;

public class UtilsReportUtils {

    public static String getUnitsTable(List<UnitInfo> units, String fragmentType, boolean cacheFiles) {
        StringBuilder table = new StringBuilder();

        table.append("<table style='width: 80%'>\n");
        table.append("<th>Unit</th><th># lines</th><th>McCabe index</th><th># params</th>\n");
        int index[] = {0};
        units.forEach(unit -> {
            table.append("<tr>\n");
            index[0]++;
            String divId = "unitCode_" + index[0];
            String fileLink = cacheFiles
                    ? "<a style='color: grey' target='_blank' href='../src/main/"
                    + unit.getSourceFile().getRelativePath() + ".html'>"
                    + unit.getSourceFile().getRelativePath()
                    + "</a>"
                    : unit.getSourceFile().getRelativePath();
            table.append("<td><b><a target='_blank'" +
                    "href='../src/fragments/" + fragmentType + "/" + fragmentType + "_"
                    + index[0] + "."
                    + unit.getSourceFile().getExtension()
                    + ".html'>"
                    + unit.getShortName() + "</a></b><br/>in "
                    + fileLink
                    + "</td>\n");
            table.append("<td>" + unit.getLinesOfCode() + "</td>\n");
            table.append("<td>" + unit.getMcCabeIndex() + "</td>\n");
            table.append("<td>" + unit.getNumberOfParameters() + "</td>\n");

            table.append("</tr>\n");
        });
        table.append("</table>\n");

        return table.toString();
    }
}
