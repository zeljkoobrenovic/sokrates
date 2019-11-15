package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReportFileExporter {
    public final static String REPORTS_HTML_HEADER = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <style type=\"text/css\" media=\"all\">\n" +
            "        body {\n" +
            "            font-family: Vollkorn, Ubuntu, Optima, Segoe, Segoe UI, Candara, Calibri, Arial, sans-serif;\n" +
            "            margin-left: 5%;\n" +
            "            margin-right: 5%;\n" +
            "        }\n" +
            "\n" +
            "        table {\n" +
            "            color: #333;\n" +
            "            border-collapse: collapse;\n" +
            "            border-spacing: 0;\n" +
            "        }\n" +
            "\n" +
            "        td, th {\n" +
            "            border: 1px solid #CCC;\n" +
            "            height: 30px;\n" +
            "            padding-left: 10px;\n" +
            "            padding-right: 10px;\n" +
            "        }\n" +
            "\n" +
            "        th {\n" +
            "            background: #F3F3F3;\n" +
            "            font-weight: bold;\n" +
            "        }\n" +
            "\n" +
            "        td {\n" +
            "            background: #FFFFFF;\n" +
            "            text-align: left;\n" +
            "        }\n" +
            "\n" +
            "        h3 {\n" +
            "            margin-top: 0\n" +
            "        }\n" +
            "\n" +
            "        h1 {\n" +
            "            margin-bottom: 0;\n" +
            "        }\n" +
            "\n" +
            "        p {\n" +
            "            margin-top: 0;\n" +
            "        }\n" +
            "\n" +
            "        .reportSubtitle {\n" +
            "            color: grey;\n" +
            "        }\n" +
            "\n" +
            "        .group {\n" +
            "            margin-bottom: 10px;\n" +
            "            border: solid lightgrey 1px;\n" +
            "            box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.2), 0 3px 10px 0 rgba(0, 0, 0, 0.19);\n" +
            "            border-radius: 2px;\n" +
            "        }\n" +
            "\n" +
            "        .section {\n" +
            "            margin-bottom: 30px;\n" +
            "            border: solid lightgrey 1px;\n" +
            "            box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.2), 0 3px 10px 0 rgba(0, 0, 0, 0.19);\n" +
            "            border-radius: 2px;\n" +
            "            padding: 0;\n" +
            "        }\n" +
            "\n" +
            "        .sectionHeader {\n" +
            "            margin-bottom: 30px;\n" +
            "            padding: 20px;\n" +
            "            box-shadow: 0 2px 4px 0 rgba(0, 0, 0, 0.2), 0 3px 10px 0 rgba(0, 0, 0, 0.19);\n" +
            "            background-color: lightskyblue;\n" +
            "        }\n" +
            "\n" +
            "        .sectionTitle {\n" +
            "            font-size: 140%;\n" +
            "        }\n" +
            "\n" +
            "        .sectionSubtitle {\n" +
            "            font-size: 90%; color: grey;\n" +
            "        }\n" +
            "\n" +
            "        .subSectionTitle {\n" +
            "            font-size: 140%;\n" +
            "        }\n" +
            "\n" +
            "        .subSectionSubtitle {\n" +
            "            font-size: 90%; color: grey;\n" +
            "        }\n" +
            "\n" +
            "        .subSection {\n" +
            "            margin-bottom: 30px;\n" +
            "            border: solid lightgrey 1px;\n" +
            "            padding-top: 0;\n" +
            "            padding-bottom: 0;\n" +
            "            box-shadow: 0 0px 0px 0 rgba(0, 0, 0, 0.2), 0 1px 9px 0 rgba(0, 0, 0, 0.19);\n" +
            "        }\n" +
            "\n" +
            "        .subSectionHeader {\n" +
            "            border: solid lightgrey 1px;\n" +
            "            padding: 6px;\n" +
            "            background-color: lightgrey;\n" +
            "            box-shadow: 0 0px 0px 0 rgba(0, 0, 0, 0.2), 0 2px 9px 0 rgba(0, 0, 0, 0.19);\n" +
            "        }\n" +
            "\n" +
            "        .sectionBody {\n" +
            "            margin: 20px;\n" +
            "        }" +
            "    </style>\n" +
            "    <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=Ubuntu\">\n" +
            "    <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css?family=Lato\">\n" +
            "    <script type=\"text/javascript\">\n" +
            "        function showHide(id) {\n" +
            "            var e = document.getElementById(id);\n" +
            "            e.style.display = (e.style.display == 'block') ? 'none' : 'block';\n" +
            "        }\n" +
            "    </script>\n" +
            "</head>\n";

    public static void exportHtml(File folder, RichTextReport report) {
        File htmlReportsFolder = getHtmlReportsFolder(folder);
        String reportFileName = getReportFileName(report);
        export(htmlReportsFolder, report, reportFileName);
    }

    private static void export(File folder, RichTextReport report, String reportFileName) {
        File reportFile = new File(folder, reportFileName);
        try {
            PrintWriter out = new PrintWriter(reportFile);
            out.println(REPORTS_HTML_HEADER + "\n<body><div id=\"report\">\n" + "\n");
            new ReportRenderer().render(report, text -> {
                out.println(text);
            });
            out.println("</div>\n</body>\n</html>");
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static String getReportFileName(RichTextReport report) {
        return report.getId().replace("-", "").replace(" ", "") + ".html";
    }

    public static void exportReportsIndexFile(File reportsFolder, CodeAnalysisResults analysisResults) {
        String[][] reportList = getReportsList();

        File htmlExportFolder = getHtmlReportsFolder(reportsFolder);

        Metadata metadata = analysisResults.getCodeConfiguration().getMetadata();
        String title = metadata.getName();
        RichTextReport indexReport = new RichTextReport(title, "", metadata.getLogoLink());
        if (StringUtils.isNotBlank(metadata.getDescription())) {
            indexReport.addParagraph(metadata.getDescription());
        }
        indexReport.startSection("Summary", "");
        indexReport.startUnorderedList();
        summarize(indexReport, analysisResults);
        indexReport.endUnorderedList();
        indexReport.endSection();
        indexReport.startSection("Reports", "");
        for (String[] report : reportList) {
            addReportFragment(htmlExportFolder, indexReport, report);
        }
        indexReport.endSection();
        indexReport.startSection("Explorers", "");
        for (String[] explorer : getExplorersList()) {
            addExplorerFragment(indexReport, explorer);
        }
        indexReport.endSection();
        appendLinks(indexReport, analysisResults);
        export(htmlExportFolder, indexReport, "index.html");
    }

    private static void summarize(RichTextReport indexReport, CodeAnalysisResults analysisResults) {
        StringBuilder summary = new StringBuilder("");
        summary.append("<p>Main Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(analysisResults.getMainAspectAnalysisResults().getLinesOfCode()) + " LOC");
        summary.append(" = ");
        final boolean[] first = {true};
        analysisResults.getMainAspectAnalysisResults().getLinesOfCodePerExtension().forEach(ext -> {
            if (!first[0]) {
                summary.append(" + ");
            } else {
                first[0] = false;
            }
            summary.append(
                    ext.getName().toUpperCase().replace("*.", "") + "</b> ("
                            + RichTextRenderingUtils.renderNumber(ext.getValue().intValue())
                            + " LOC)");
        });
        summary.append("</p>");
        summary.append("<p>Test Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(analysisResults.getTestAspectAnalysisResults().getLinesOfCode()) + " LOC");
        List<NumericMetric> linesOfCodePerExtension = analysisResults.getTestAspectAnalysisResults().getLinesOfCodePerExtension();
        if (linesOfCodePerExtension.size() > 0) {
            summary.append(" = ");
            first[0] = true;
            linesOfCodePerExtension.forEach(ext -> {
                if (!first[0]) {
                    summary.append(" + ");
                } else {
                    first[0] = false;
                }
                summary.append(
                        "<b>" + ext.getName().toUpperCase().replace("*.", "") + "</b> ("
                                + RichTextRenderingUtils.renderNumber(ext.getValue().intValue())
                                + " LOC)");
            });
        }
        summary.append("</p>");

        indexReport.addParagraph(summary.toString());

        Number duplicationPercentage = analysisResults.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage();
        double duplication = duplicationPercentage.doubleValue();
        if (!analysisResults.getCodeConfiguration().getAnalysis().isSkipDuplication()) {
            indexReport.addParagraph("Duplication: <b style='" + (duplication > 5.0 ? "color: crimson" : "") + "'>" + FormattingUtils.getFormattedPercentage(duplication) + "%</b>");
        }
        int mainLOC = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        summarizeFileSize(indexReport, analysisResults);

        int linesOfCodeInUnits = analysisResults.getUnitsAnalysisResults().getLinesOfCodeInUnits();
        int veryLongUnitsLOC = analysisResults.getUnitsAnalysisResults().getUnitSizeRiskDistribution().getVeryHighRiskValue();
        int lowUnitsLOC = analysisResults.getUnitsAnalysisResults().getUnitSizeRiskDistribution().getLowRiskValue();
        int veryComplexUnitsLOC = analysisResults.getUnitsAnalysisResults().getCyclomaticComplexityRiskDistribution().getVeryHighRiskValue();
        int lowComplexUnitsLOC = analysisResults.getUnitsAnalysisResults().getCyclomaticComplexityRiskDistribution().getLowRiskValue();

        indexReport.addParagraph("Unit Size: <b style='" + (veryLongUnitsLOC > 1 ? "color: crimson" : "") + "'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, veryLongUnitsLOC))
                + "%</b> very long (>100 LOC), <b style='color: green'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, lowUnitsLOC))
                + "%</b> short (<= 20 LOC)");
        indexReport.addParagraph("Cyclomatic Complexity: <b style='" + (veryComplexUnitsLOC > 1 ? "color: crimson" : "") + "'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, veryComplexUnitsLOC))
                + "%</b> very complex (McCabe index > 25), <b style='color: green'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, lowComplexUnitsLOC))
                + "%</b> simple (McCabe index <= 5)");

        indexReport.addHtmlContent("<p>Logical Component Decomposition:");
        first[0] = true;
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(decomposition -> {
            if (!first[0]) {
                indexReport.addHtmlContent(", ");
            } else {
                first[0] = false;
            }
            indexReport.addHtmlContent(decomposition.getKey() + " (" + decomposition.getComponents().size() + " components)");
        });
        indexReport.addHtmlContent("</p>");
        List<String> summaryFindings = analysisResults.getCodeConfiguration().getSummaryFindings();
        if (summaryFindings != null && summaryFindings.size() > 0) {
            indexReport.addParagraph("Other findings:");
            indexReport.startUnorderedList();
            summaryFindings.forEach(summaryFinding -> {
                indexReport.addListItem(summaryFinding);
            });
            indexReport.endUnorderedList();
        }
    }

    private static void summarizeFileSize(RichTextReport indexReport, CodeAnalysisResults analysisResults) {
        FilesAnalysisResults filesAnalysisResults = analysisResults.getFilesAnalysisResults();
        if (filesAnalysisResults != null && filesAnalysisResults.getOveralFileSizeDistribution() != null) {
            int mainLOC = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
            int veryLongFilesLOC = filesAnalysisResults.getOveralFileSizeDistribution().getVeryHighRiskValue();
            int shortFilesLOC = filesAnalysisResults.getOveralFileSizeDistribution().getLowRiskValue();

            indexReport.addParagraph("File Size: <b style='" + (veryLongFilesLOC > 1 ? "color: crimson" : "") + "'>"
                    + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(mainLOC, veryLongFilesLOC))
                    + "%</b> very long (>1000 LOC), <b style='color: green'>"
                    + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(mainLOC, shortFilesLOC))
                    + "%</b> short (<= 200 LOC)");
        }
    }

    private static void appendLinks(RichTextReport report, CodeAnalysisResults analysisResults) {
        List<Link> links = analysisResults.getCodeConfiguration().getMetadata().getLinks();
        links.add(0, new Link("Configuration file (JSON)", "../../config.json"));
        if (links.size() > 0) {
            report.startSection("Links", "");
            report.startUnorderedList();
            links.forEach(link -> {
                report.addListItem("<a href='" + link.getHref() + "' target='_blank'>" + link.getLabel() + "</a>");
            });
            report.endUnorderedList();
            report.endSection();
        }
    }


    private static File getHtmlReportsFolder(File reportsFolder) {
        File htmlExportFolder = new File(reportsFolder, "html");
        htmlExportFolder.mkdirs();
        return htmlExportFolder;
    }

    private static void addReportFragment(File reportsFolder, RichTextReport indexReport, String[] report) {
        String reportFileName = report[0];
        String reportTitle = report[1];
        indexReport.addHtmlContent("<div class='group' style='padding: 10px; margin: 10px; width: 180px; height: 200px; text-align: center; display: inline-block; vertical-align: top'>");
        File reportFile = new File(reportsFolder, reportFileName);
        if (reportFile.exists()) {
            indexReport.startDiv("font-size:90%; color:deepskyblue");
            indexReport.addHtmlContent("Analysis Report");
            indexReport.endDiv();
            indexReport.startDiv("");
            indexReport.addHtmlContent("<img style='margin-top:15px; margin-bottom: 20px;width:80px' src='https://www.zeljkoobrenovic.com/images/report.png'>");
            indexReport.endDiv();
            indexReport.startDiv("font-size:100%; color:blue; ");
            indexReport.addHtmlContent("<b><a style='text-decoration: none' href=\"" + reportFileName + "\">" + reportTitle + "</a></b>");
            indexReport.endDiv();
            indexReport.startDiv("margin-top: 10px; font-size: 90%; color: lightgrey");
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
            indexReport.addHtmlContent(format.format(new Date()));
            indexReport.endDiv();
        } else {
            indexReport.addHtmlContent("<span style=\"color:grey\">" + reportTitle + "</span>");
        }
        indexReport.addHtmlContent("</div>");
    }

    private static void addExplorerFragment(RichTextReport indexReport, String explorer[]) {
        indexReport.addHtmlContent("<div class='group' style='padding: 10px; margin: 10px; width: 180px; height: 200px; text-align: center; display: inline-block'>");

        indexReport.startDiv("font-size:90%; color:deepskyblue");
        indexReport.addHtmlContent("Interactive Explorer");
        indexReport.endDiv();

        indexReport.startDiv("");
        indexReport.addHtmlContent("<img style='margin-top:15px; margin-bottom: 20px;width:80px' src='https://www.zeljkoobrenovic.com/images/report.png'>");
        indexReport.endDiv();

        indexReport.startDiv("font-size:100%; color:blue; ");
        indexReport.addHtmlContent("<b><a style='text-decoration: none' href=\"../explorers/" + explorer[0] + "\">" + explorer[1] + "</a></b>");
        indexReport.endDiv();

        indexReport.startDiv("margin-top: 10px; font-size: 90%; color: lightgrey");
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
        indexReport.addHtmlContent(format.format(new Date()));
        indexReport.endDiv();

        indexReport.addHtmlContent("</div>");
    }

    private static String[][] getReportsList() {
        return new String[][]{
                {"SourceCodeOverview.html", "Source Code Overview"},
                {"Components.html", "Components"},
                {"Duplication.html", "Duplication "},
                {"FileSize.html", "File Size"},
                {"UnitSize.html", "Unit Size"},
                {"CyclomaticComplexity.html", "Cyclomatic Complexity"},
                {"CrossCuttingConcerns.html", "Cross - Cutting Concerns"},
                {"Findings.html", "Findings"},
                {"Metrics.html", "Metrics"},
                {"Comparison.html", "Comparison"},
                {"Controls.html", "Controls"}
        };
    }

    private static String[][] getExplorersList() {
        return new String[][]{
                {"MainFiles.html", "Files"},
                {"Units.html", "Units"},
                {"Duplicates.html", "Duplicates"},
                {"Dependencies.html", "Dependencies"}
        };
    }

}
