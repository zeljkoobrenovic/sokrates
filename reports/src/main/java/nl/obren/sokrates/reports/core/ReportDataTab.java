package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import java.util.*;
import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

/** The Data tab of the repository report index: the links into data/data.zip and the per-scope file lists. Moved out of {@link ReportFileExporter}. */
class ReportDataTab {
    private ReportDataTab() {
    }

    static void addData(RichTextReport report, CodeAnalysisResults analysisResults) {
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        report.addLevel2Header("Lists of Files Per Scope");

        report.startUnorderedList();
        addListsOfFilesInScope(report, "main", main.getFilesCount());
        addListsOfFilesInScope(report, "test", test.getFilesCount());
        addListsOfFilesInScope(report, "build and deployment", build.getFilesCount());
        addListsOfFilesInScope(report, "generated", generated.getFilesCount());
        addListsOfFilesInScope(report, "other", other.getFilesCount());
        report.startListItem();
        report.addHtmlContent("FILES: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/mainFilesWithHistory.txt')\">" + "History Data" + "</a>");
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("IGNORED FILES: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/excluded_files_ignored_extensions.txt')\">" + "By Extension" + "</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/excluded_files_ignored_rules.txt')\">" + "By Rule" + "</a>");
        report.endListItem();
        report.endUnorderedList();

        report.addLineBreak();
        report.addLevel2Header("Analysis Results");
        report.startUnorderedList();

        report.startListItem();
        report.addHtmlContent("CONFIGURATION: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('config.json')\">" + "JSON" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("ALL ANALYSIS RESULTS: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('analysisResults.json')\">" + "JSON" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("DUPLICATES: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/duplicates.txt')\">" + "TXT" + "</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('duplicates.json')\">" + "JSON" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("UNITS: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/units.txt')\">" + "TXT" + "</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('units.json')\">" + "JSON" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("CONTRIBUTORS: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/contributors.txt')\">" + "TXT" + "</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('contributors.json')\">" + "JSON" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("LOGICAL DECOMPOSITIONS: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('logical_decompositions.json')\">" + "JSON" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("CONCERNS: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('concerns.json')\">" + "JSON" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("CONTROLS: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/controls.txt')\">" + "TXT" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("ALL METRICS: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/metrics.txt')\">" + "TXT" + "</a>");
        report.endListItem();

        report.endUnorderedList();

        //

        report.addLineBreak();
        report.addLevel2Header("Zipped Files");
        report.startUnorderedList();

        report.startListItem();
        report.addHtmlContent("GIT HISTORY: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('zips/git-history.zip')\">" + "ZIP" + "</a>");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("ALL FILES IN ALL ANALYSIS SCOPES: ");
        report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('zips/all_files.zip')\">" + "ZIP" + "</a>");
        report.endListItem();

        report.endUnorderedList();
    }

    private static void addListsOfFilesInScope(RichTextReport report, String scopeName, int filesCount) {
        String technicalName = scopeName.toLowerCase().replace(" ", "_");
        boolean exists = filesCount > 0;
        String infoText = filesCount + (filesCount == 1 ? " file" : " files");
        String displayName = scopeName.toUpperCase();
        report.startListItem();
        if (exists) {
            report.addHtmlContent("<a href=\"#\" onclick=\"return downloadDataFile('text/aspect_" + technicalName + ".txt')\">" + displayName + " (" + infoText + ")</a>");
        } else {
            report.addContentInDiv(displayName, "color: #c0c0c0");
        }
        report.endListItem();
    }
}
