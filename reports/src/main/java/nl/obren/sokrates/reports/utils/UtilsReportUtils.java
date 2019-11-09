package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.List;

public class UtilsReportUtils {

    public static String getUnitsTable(List<UnitInfo> units, String fragmentType) {
        StringBuilder table = new StringBuilder();

        table.append("<table style='width: 80%'>\n");
        table.append("<th>Unit</th><th># lines</th><th>McCabe index</th><th># params</th>\n");
        int index[] = {0};
        units.forEach(unit -> {
            table.append("<tr>\n");
            index[0]++;
            String divId = "unitCode_" + index[0];
            table.append("<td><b><a target='_blank'" +
                    "href='../src/fragments/" + fragmentType + "/" + fragmentType + "_" +
                    +index[0] + "."
                    + unit.getSourceFile().getExtension()
                    + "'>"
                    + unit.getShortName() + "</a></b><br/>in " + unit.getSourceFile().getRelativePath()
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
