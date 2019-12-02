package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReportFileExporter {
    public static void exportHtml(File folder, RichTextReport report) {
        File htmlReportsFolder = getHtmlReportsFolder(folder);
        String reportFileName = getReportFileName(report);
        export(htmlReportsFolder, report, reportFileName);
    }

    private static void export(File folder, RichTextReport report, String reportFileName) {
        File reportFile = new File(folder, reportFileName);
        try {
            PrintWriter out = new PrintWriter(reportFile);
            out.println(ReportConstants.REPORTS_HTML_HEADER + "\n<body><div id=\"report\">\n" + "\n");
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
        return report.getFileName();
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
        new SummaryUtils().summarize(analysisResults, indexReport);
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
            indexReport.startDiv("padding: 20px;");
            indexReport.addHtmlContent(ReportConstants.REPORT_SVG_ICON);
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

        indexReport.startDiv("padding: 20px;");
        indexReport.addHtmlContent(ReportConstants.REPORT_SVG_ICON);
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
                {"Components.html", "Components and Dependencies"},
                {"Duplication.html", "Duplication "},
                {"FileSize.html", "File Size"},
                {"UnitSize.html", "Unit Size"},
                {"ConditionalComplexity.html", "Conditional Complexity"},
                {"CrossCuttingConcerns.html", "Cross - Cutting Concerns"},
                {"Metrics.html", "All Metrics"},
                {"Trend.html", "Trend"},
                {"Controls.html", "Goals & Controls"},
                {"Findings.html", "Notes & Findings"},

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
