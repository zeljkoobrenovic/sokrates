/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class FilesReportUtils {

    public static String getFilesTable(List<SourceFile> sourceFiles, boolean linkToFiles, boolean showAge, boolean showLineLength) {
        return getFilesTable(sourceFiles, linkToFiles, showAge, showLineLength, 300);
    }

    public static String getFilesTable(List<SourceFile> sourceFiles, boolean linkToFiles, boolean showAge, boolean showLineLength, int maxHeight) {
        return getFilesTable(sourceFiles, linkToFiles, showAge, showLineLength, maxHeight, false);
    }

    // showChurn adds a "line churn" column (+added / -deleted from the file's modification history),
    // shown alongside the age columns. Only the file-churn report passes true.
    public static String getFilesTable(List<SourceFile> sourceFiles, boolean linkToFiles, boolean showAge, boolean showLineLength, int maxHeight, boolean showChurn) {
        StringBuilder table = new StringBuilder();

        table.append("<div style='width: 100%; overflow-x: scroll; overflow-y: scroll; max-height: " + maxHeight + "px;'>\n");
        table.append("<table style='width: 80%'>\n");
        table.append("<tr>");
        String header = "<th>File</th><th># lines</th><th># units</th>";
        if (showLineLength) {
            header += "<th># long lines</th>";
        }
        if (showAge) {
            header += "<th>created</th>";
            header += "<th>last modified</th>";
            header += "<th># changes<br>(days)</th>";
            header += "<th># contributors</th>";
            if (showChurn) {
                header += "<th>line<br>churn</th>";
            }
            header += "<th>first<br>contributor</th>";
            header += "<th>latest<br>contributor</th>";
        }
        table.append(header + "\n");
        table.append("</tr>\n");

        sourceFiles.stream().forEach(sourceFile -> {
            table.append("<tr>\n");

            File file = new File(sourceFile.getRelativePath());

            String fileNameFragment;

            if (linkToFiles) {
                String href = "../src/viewer.html#aspect=main&file=" + sourceFile.getRelativePath();
                fileNameFragment = "<a target='blank' href='" + href + "'>" + file.getName() + "</a>";
            } else {
                fileNameFragment = file.getName();
            }

            String parent = StringUtils.abbreviate(file.getParent(), 150);
            table.append("<td>" +
                    "<div style='white-space: nowrap; '><div style='display: inline-block; vertical-align: top; margin-top: 3px; margin-right: 4px;'>" +
                    DataImageUtils.getLangDataImageDiv30(ExtensionGroupExtractor.getExtension(file.getName())) +
                    "</div><div style='display: inline-block;'><b>"
                    + fileNameFragment + "</b><div style='white-space: nowrap; overflow: hidden'>in " + (parent != null ? parent : "root") + "</div>" +
                    "</div></div>" +
                    "</td>\n");
            table.append("<td style='text-align: center'>" + sourceFile.getLinesOfCode() + "</td>\n");
            if (sourceFile.getUnitsCount() > 0) {
                table.append("<td style='text-align: center'>" + sourceFile.getUnitsCount() + "</td>\n");
            } else {
                table.append("<td style='text-align: center; color: lightgrey'>-</td>\n");
            }
            if (showLineLength) {
                table.append("<td style='text-align: center'>" + sourceFile.getLongLinesCount(120) + "</td>\n");
            }

            if (showAge) {
                FileModificationHistory history = sourceFile.getFileModificationHistory();
                if (history != null) {
                    table.append("<td style='text-align: center; white-space: nowrap; font-size: 80%'>" + history.getOldestDate() + "</td>\n");
                    table.append("<td style='text-align: center; white-space: nowrap; font-size: 80%;'>" + history.getLatestDate() + "</td>\n");
                    table.append("<td style='text-align: center'>" + history.getDates().size() + "</td>\n");
                    table.append("<td style='text-align: center'>" + history.countContributors() + "</td>\n");

                    if (showChurn) {
                        if (history != null && (history.getLinesAdded() > 0 || history.getLinesDeleted() > 0)) {
                            table.append("<td style='text-align: center; white-space: nowrap;'>"
                                    + "<span style='color: #2e7d32;'>+" + history.getLinesAdded() + "</span> / "
                                    + "<span style='color: #c62828;'>-" + history.getLinesDeleted() + "</span></td>\n");
                        } else {
                            table.append("<td style='text-align: center; color: lightgrey'>-</td>\n");
                        }
                    }
                    table.append("<td style='text-align: center; font-size: 80%; color: grey'>" + StringUtils.abbreviate(history.getOldestContributor(), 30) + "</td>\n");
                    table.append("<td style='text-align: center; font-size: 80%; color: grey'>" + StringUtils.abbreviate(history.getLatestContributor(), 30) + "</td>\n");
                } else {
                    // Age columns in the header (created, last modified, # changes, # contributors,
                    // first contributor, latest contributor) plus the optional churn column: emit one
                    // empty cell per header column so history-less files stay aligned.
                    int emptyCells = showChurn ? 7 : 6;
                    for (int i = 0; i < emptyCells; i++) {
                        table.append("<td style='text-align: center'></td>\n");
                    }
                }
            }

            table.append("</tr>\n");
        });

        table.append("</table>\n");
        table.append("</div>\n");

        return table.toString();
    }
}
