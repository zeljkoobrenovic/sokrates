package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.io.File;
import java.util.List;

public class FilesReportUtils {

    public static String getFilesTable(List<SourceFile> sourceFiles) {
        StringBuilder table = new StringBuilder();

        table.append("<table style='width: 80%'>\n");
        table.append("<th>File</th><th># lines</th>\n");

        sourceFiles.forEach(sourceFile -> {
            table.append("<tr>\n");

            String href = "../src/main/" + sourceFile.getRelativePath();

            File file = new File(sourceFile.getRelativePath());
            table.append("<td><b>"
                    + "<a target='blank' href='"
                    + href + "'>"
                    + file.getName() + "</a></b><br/>in " + file.getParent() + "<br/>" +
                    "</td>\n");
            table.append("<td>" + sourceFile.getLinesOfCode() + "</td>\n");

            table.append("</tr>\n");
        });

        table.append("</table>\n");

        return table.toString();
    }
}
