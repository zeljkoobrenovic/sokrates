/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.SourceFile;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class FilesReportUtils {

    public static String getFilesTable(List<SourceFile> sourceFiles, boolean linkToFiles) {
        StringBuilder table = new StringBuilder();

        table.append("<div style='witdh: 100%; overflow-x: scroll'>\n");
        table.append("<table style='width: 80%'>\n");
        table.append("<tr>");
        table.append("<th>File</th><th># lines</th><th># units</th>\n");
        // table.append("<th>File</th><th># lines</th><th># units</th><th># unit lines</th><th>McCabe index</th>\n");
        table.append("<tr>");

        sourceFiles.forEach(sourceFile -> {
            table.append("<tr>\n");

            File file = new File(sourceFile.getRelativePath());

            String fileNameFragment;

            if (linkToFiles) {
                String href = "../src/main/" + sourceFile.getRelativePath();
                fileNameFragment = "<a target='blank' href='" + href + ".html'>" + file.getName() + "</a>";
            } else {
                fileNameFragment = file.getName();
            }

            String parent = StringUtils.abbreviate(file.getParent(), 150);
            table.append("<td><b>"
                    + fileNameFragment + "</b><br/>in " + parent + "<br/>" +
                    "</td>\n");
            table.append("<td style='text-align: center'>" + sourceFile.getLinesOfCode() + "</td>\n");
            if (sourceFile.getUnitsCount() > 0) {
                table.append("<td style='text-align: center'>" + sourceFile.getUnitsCount() + "</td>\n");
            } else {
                table.append("<td style='text-align: center; color: lightgrey'>-</td>\n");
            }
            // table.append("<td style='text-align: center'>" + sourceFile.getLinesOfCodeInUnits() + "</td>\n");
            // table.append("<td style='text-align: center'>" + sourceFile.getUnitsMcCabeIndexSum() + "</td>\n");

            table.append("</tr>\n");
        });

        table.append("</table>\n");
        table.append("</div>\n");

        return table.toString();
    }
}
