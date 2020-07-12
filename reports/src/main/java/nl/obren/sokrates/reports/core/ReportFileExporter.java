/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionYear;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.contributors.ContributorsImport;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ReportFileExporter {

    private static String htmlReportsSubFolder = "html";

    public static void exportHtml(File folder, String subFolder, RichTextReport report) {
        htmlReportsSubFolder = subFolder;
        File htmlReportsFolder = getHtmlReportsFolder(folder);
        String reportFileName = getReportFileName(report);
        export(htmlReportsFolder, report, reportFileName);
    }

    private static void export(File folder, RichTextReport report, String reportFileName) {
        File reportFile = new File(folder, reportFileName);
        try {
            PrintWriter out = new PrintWriter(reportFile);
            out.println(ReportConstants.REPORTS_HTML_HEADER + "\n<body><div id=\"report\">\n" + "\n");
            new ReportRenderer().render(report, getReportRenderingClient(out, folder));
            out.println("</div>\n</body>\n</html>");
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static ReportRenderingClient getReportRenderingClient(PrintWriter out, File reportsFolder) {
        return new ReportRenderingClient() {
            @Override
            public void append(String text) {
                out.println(text);
            }

            @Override
            public File getVisualsExportFolder() {
                File visualsFolder = new File(reportsFolder, "visuals");
                visualsFolder.mkdirs();
                return visualsFolder;
            }
        };
    }

    private static String getReportFileName(RichTextReport report) {
        return report.getFileName();
    }

    public static void exportReportsIndexFile(File reportsFolder, CodeAnalysisResults analysisResults, File sokratesConfigFolder) {
        List<String[]> reportList = getReportsList(analysisResults, sokratesConfigFolder);

        File htmlExportFolder = getHtmlReportsFolder(reportsFolder);

        Metadata metadata = analysisResults.getCodeConfiguration().getMetadata();
        String title = metadata.getName();
        RichTextReport indexReport = new RichTextReport(title, "", metadata.getLogoLink());
        if (StringUtils.isNotBlank(metadata.getDescription())) {
            indexReport.addParagraph(metadata.getDescription());
        }
        indexReport.startSection("Summary", "");
        indexReport.startDiv("");
        summarize(indexReport, analysisResults);
        indexReport.endDiv();
        indexReport.endSection();
        addContributorsSection(analysisResults, sokratesConfigFolder, indexReport);
        indexReport.startSection("Reports", "");
        for (String[] report : reportList) {
            addReportFragment(htmlExportFolder, indexReport, report);
        }
        indexReport.endSection();
        appendLinks(indexReport, analysisResults);
        export(htmlExportFolder, indexReport, "index.html");
    }

    public static void addContributorsSection(CodeAnalysisResults analysisResults, File sokratesConfigFolder, RichTextReport indexReport) {
        CodeConfiguration codeConfiguration = analysisResults.getCodeConfiguration();
        ContributorsImport contributorsImport = codeConfiguration.getContributorsAnalysis().getContributors(sokratesConfigFolder);
        List<Contributor> contributors = contributorsImport.getContributors();
        List<ContributionYear> contributorsPerYear = contributorsImport.getContributorsPerYear();
        if (contributors.size() > 0) {
            indexReport.startSection("Contributors (" + contributors.size() + ")", "");
            addContributors(indexReport, contributors);
            addContributorsPerYear(indexReport, contributorsPerYear);
            indexReport.addNewTabLink("Contributor details...", "../data/text/contributors.txt");
            indexReport.endSection();
        }
    }

    private static void addContributorsPerYear(RichTextReport indexReport, List<ContributionYear> contributorsPerYear) {
        if (contributorsPerYear.size() > 0) {
            int limit = 20;
            if (contributorsPerYear.size() > limit) {
                contributorsPerYear = contributorsPerYear.subList(contributorsPerYear.size() - limit, contributorsPerYear.size());
            }

            indexReport.startSubSection("Trend", "");
            int maxContributors = contributorsPerYear.stream().mapToInt(c -> c.getContributorsCount()).max().orElse(1);
            int maxCommits = contributorsPerYear.stream().mapToInt(c -> c.getCommitsCount()).max().orElse(1);

            indexReport.startTable();

            indexReport.startTableRow();
            indexReport.addTableCell("Commits", "border: none;");
            String style = "border: none; text-align: center; vertical-align: bottom; font-size: 80%";
            contributorsPerYear.forEach(year -> {
                indexReport.startTableCell(style);
                int count = year.getCommitsCount();
                indexReport.addParagraph(count + "", "margin: 2px");
                int height = 1 + (int) (64.0 * count / maxCommits);
                indexReport.addHtmlContent("<div style='width: 100%; background-color: darkgrey; height:" + height + "px'></div>");
                indexReport.endTableCell();
            });
            indexReport.endTableRow();

            indexReport.startTableRow();
            indexReport.addTableCell("Contributors", "border: none;");
            contributorsPerYear.forEach(year -> {
                indexReport.startTableCell(style);
                int count = year.getContributorsCount();
                indexReport.addParagraph(count + "", "margin: 2px");
                int height = 1 + (int) (64.0 * count / maxContributors);
                indexReport.addHtmlContent("<div style='width: 100%; background-color: skyblue; height:" + height + "px'></div>");
                indexReport.endTableCell();
            });
            indexReport.endTableRow();

            indexReport.startTableRow();
            indexReport.addTableCell("", "border: none; ");
            contributorsPerYear.forEach(year -> {
                indexReport.addTableCell(year.getYear(), "border: none; text-align: center; font-size: 90%");
            });
            indexReport.endTableRow();

            indexReport.endTable();

            indexReport.endSection();
        }
    }

    public static void addContributors(RichTextReport indexReport, List<Contributor> contributors) {
        Collections.sort(contributors, (a, b) -> b.getCommitsCount() - a.getCommitsCount());
        int max = contributors.get(0).getCommitsCount();
        int total = contributors.stream().mapToInt(c -> c.getCommitsCount()).sum();
        long activeCount = contributors.stream().filter(c -> c.isActive()).count();
        long rookiesCount = contributors.stream().filter(c -> c.isRookie()).count();
        long veteransCount = activeCount - rookiesCount;
        long historicalCount = contributors.size() - activeCount;
        indexReport.startSubSection("Recent Contributors (" + activeCount
                        + " = " + veteransCount + " " + (veteransCount == 1 ? "veteran" : "veterans")
                        + " + " + rookiesCount + " " + (rookiesCount == 1 ? "rookie" : "rookies") + ")",
                "Contributed in past 6 months (a rookie = the first contribution in past year)");
        contributors.stream().filter(c -> c.isActive()).forEach(contributor -> {
            addContributor(indexReport, max, total, contributor);
        });
        indexReport.endSection();
        indexReport.startSubSection("Historical Contributors (" + historicalCount + ")", "Last contributed more than 6 months ago");
        contributors.stream().filter(c -> !c.isActive()).forEach(contributor -> {
            addContributor(indexReport, max, total, contributor);
        });
        indexReport.endSection();
    }

    public static void addContributor(RichTextReport indexReport, int max, int total, Contributor contributor) {
        int commitsCount = contributor.getCommitsCount();
        double opacity = 0.2 + 0.8 * commitsCount / max;
        double percentage = 100.0 * commitsCount / total;
        String info = StringEscapeUtils.escapeHtml4(contributor.getName()
                + " " + commitsCount
                + " commits (" + FormattingUtils.getFormattedPercentage(percentage) + "%),"
                + " between " + contributor.getFirstCommitDate() + " and " + contributor.getLatestCommitDate());

        if (contributor.isRookie()) {
            indexReport.addHtmlContent("<div style='border:2px solid green; border-radius: 5px; display: inline-block;opacity:" + opacity + "' title='" + info + "'>");
        } else {
            indexReport.addHtmlContent("<div style='display: inline-block;opacity:" + opacity + "' title='" + info + "'>");
        }
        indexReport.addHtmlContent(contributor.isActive() ? getIconSvg("contributor") : getIconSvg("contributor_historical"));
        indexReport.addHtmlContent("</div>");
    }

    private static void addExplorersSection(RichTextReport indexReport) {
        indexReport.startSection("Explorers", "");
        for (String[] explorer : getExplorersList()) {
            addExplorerFragment(indexReport, explorer);
        }
        indexReport.endSection();
    }

    private static void summarize(RichTextReport indexReport, CodeAnalysisResults analysisResults) {
        new SummaryUtils().summarize(analysisResults, indexReport);
    }

    private static void appendLinks(RichTextReport report, CodeAnalysisResults analysisResults) {
        List<Link> links = analysisResults.getCodeConfiguration().getMetadata().getLinks();
        links.add(0, new Link("Configuration file (JSON)", "../data/config.json"));
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
        File htmlExportFolder = new File(reportsFolder, htmlReportsSubFolder);
        htmlExportFolder.mkdirs();
        return htmlExportFolder;
    }

    public static String getIconSvg(String icon) {
        return getIconSvg(icon, 80);
    }

    public static String getIconSvg(String icon, int size) {
        String svg = HtmlTemplateUtils.getResource("/icons/" + icon + ".svg");
        svg = svg.replaceAll("height='.*?'", "height='" + size + "px'");
        svg = svg.replaceAll("width='.*?'", "width='" + size + "px'");
        return svg;
    }

    private static void addReportFragment(File reportsFolder, RichTextReport indexReport, String[] report) {
        String reportFileName = report[0];
        String reportTitle = report[1];
        File reportFile = new File(reportsFolder, reportFileName);
        boolean showReport = reportFile.exists() && StringUtils.isNotBlank(reportFileName);

        if (showReport) {
            indexReport.addHtmlContent("<div class='group' style='padding: 10px; margin: 10px; width: 180px; height: 200px; text-align: center; display: inline-block; vertical-align: top'>");
            indexReport.startDiv("font-size:90%; color:deepskyblue");
            indexReport.addHtmlContent("Analysis Report");
        } else {
            indexReport.addHtmlContent("<div class='group' style='padding: 10px; margin: 10px; width: 180px; height: 200px; text-align: center; display: inline-block; vertical-align: top; opacity: 0.4'>");
            indexReport.startDiv("font-size:90%;");
            indexReport.addHtmlContent("Analysis Report");
        }

        indexReport.endDiv();
        indexReport.startDiv("padding: 20px;");
        if (StringUtils.isNotBlank(report[2])) {
            indexReport.addHtmlContent(getIconSvg(report[2]));
        } else {
            indexReport.addHtmlContent(ReportConstants.REPORT_SVG_ICON);
        }
        indexReport.endDiv();
        if (showReport) {
            indexReport.startDiv("color:blue; ");
            indexReport.addHtmlContent("<b><a style='text-decoration: none' href=\"" + reportFileName + "\">" + reportTitle + "</a></b>");
            indexReport.endDiv();
        } else {
            indexReport.startDiv("");
            indexReport.addHtmlContent("<b>" + reportTitle + "</b>");
            indexReport.endDiv();
        }
        indexReport.startDiv("margin-top: 10px; font-size: 90%; color: lightgrey");
        if (showReport) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
            indexReport.addHtmlContent(format.format(new Date()));
        }
        indexReport.endDiv();
        indexReport.endDiv();
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

    private static List<String[]> getReportsList(CodeAnalysisResults analysisResults, File sokratesConfigFolder) {
        List<String[]> list = new ArrayList<>();

        CodeConfiguration config = analysisResults.getCodeConfiguration();
        boolean mainExists = analysisResults.getMainAspectAnalysisResults().getFilesCount() > 0;
        boolean showHistoryReport = mainExists && config.getFileHistoryAnalysis().filesHistoryImportPathExists(sokratesConfigFolder);
        boolean showDuplication = mainExists && !config.getAnalysis().isSkipDuplication();
        boolean showDependencies = mainExists && !config.getAnalysis().isSkipDependencies();
        boolean showTrends = mainExists && config.getTrendAnalysis().getReferenceAnalyses(sokratesConfigFolder).size() > 0;
        boolean showConcerns = mainExists && config.countAllConcernsDefinitions() > 1;
        boolean showControls = mainExists && config.getGoalsAndControls().size() > 0;
        boolean showUnits = mainExists && analysisResults.getUnitsAnalysisResults().getTotalNumberOfUnits() > 0;

        File findingsFile = CodeConfigurationUtils.getDefaultSokratesFindingsFile(sokratesConfigFolder);

        boolean showFindings = false;

        if (findingsFile.exists()) {
            showFindings = FileUtils.sizeOf(findingsFile) > 10;
        }

        list.add(new String[]{"SourceCodeOverview.html", "Source Code Overview", "codebase"});
        if (mainExists) {
            list.add(new String[]{"Components.html", showDependencies ? "Components and Dependencies" : "Components", "dependencies"});
        }

        if (showDuplication) {
            list.add(new String[]{"Duplication.html", "Duplication", "duplication"});
        }

        if (mainExists) {
            list.add(new String[]{"FileSize.html", "File Size", "file_size"});
        }
        if (showHistoryReport) {
            list.add(new String[]{"FileHistory.html", "File Change History", "file_history"});
        }
        if (showUnits) {
            list.add(new String[]{"UnitSize.html", "Unit Size", "unit_size"});
            list.add(new String[]{"ConditionalComplexity.html", "Conditional Complexity", "conditional"});
        }
        if (showConcerns) {
            list.add(new String[]{"Concerns.html", "Concerns", "cross_cutting_concerns"});
        }
        list.add(new String[]{"Metrics.html", "All Metrics", "metrics"});
        if (showTrends) {
            list.add(new String[]{"Trend.html", "Trend", "trend"});
        }
        if (showControls) {
            list.add(new String[]{"Controls.html", "Goals & Controls", "goal"});
        }

        if (showFindings) {
            list.add(new String[]{"Notes.html", "Notes & Findings", "notes"});
        }

        if (!mainExists) {
            list.add(new String[]{"", showDependencies ? "Components and Dependencies" : "Components", "dependencies"});
        }
        if (!showDuplication) {
            list.add(new String[]{"", "Duplication", "duplication"});
        }
        if (!mainExists) {
            list.add(new String[]{"", "File Size", "file_size"});
        }
        if (!showHistoryReport) {
            list.add(new String[]{"", "File Change History", "file_history"});
        }
        if (!showUnits) {
            list.add(new String[]{"", "Unit Size", "unit_size"});
            list.add(new String[]{"", "Conditional Complexity", "conditional"});
        }
        if (!showConcerns) {
            list.add(new String[]{"", "Concerns", "cross_cutting_concerns"});
        }
        if (!showTrends) {
            list.add(new String[]{"", "Trend", "trend"});
        }
        if (!showControls) {
            list.add(new String[]{"", "Goals & Controls", "goal"});
        }

        if (!showFindings) {
            list.add(new String[]{"", "Notes & Findings", "notes"});
        }

        return list;
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
