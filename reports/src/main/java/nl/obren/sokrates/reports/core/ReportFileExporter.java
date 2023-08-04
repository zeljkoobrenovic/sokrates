/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.utils.AnimalIcons;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

public class ReportFileExporter {
    private static String htmlReportsSubFolder = "html";

    public static void exportHtml(File folder, String subFolder, RichTextReport report, String customHeaderFragment) {
        htmlReportsSubFolder = subFolder;
        File htmlReportsFolder = getHtmlReportsFolder(folder);
        String reportFileName = getReportFileName(report);
        export(htmlReportsFolder, report, reportFileName, customHeaderFragment);
    }

    private static void export(File folder, RichTextReport report, String reportFileName, String customHeaderFragment) {
        File reportFile = new File(folder, reportFileName);
        try {
            PrintWriter out = new PrintWriter(reportFile);
            String reportsHtmlHeader = ReportConstants.REPORTS_HTML_HEADER.replace("<title></title>", "<title>" +
                    report.getDisplayName().replaceAll("<.*?>", "").replaceAll("\\[.*?\\]", "") + "</title>");
            reportsHtmlHeader = reportsHtmlHeader.replace("<!-- CUSTOM HEADER FRAGMENT -->", customHeaderFragment);
            if (report.isEmbedded()) {
                reportsHtmlHeader = reportsHtmlHeader.replace(" ${margin-left}", "0");
                reportsHtmlHeader = reportsHtmlHeader.replace(" ${margin-right}", "0");
            } else {
                reportsHtmlHeader = reportsHtmlHeader.replace(" ${margin-left}", "5%");
                reportsHtmlHeader = reportsHtmlHeader.replace(" ${margin-right}", "5%");
            }
            reportsHtmlHeader = minimize(reportsHtmlHeader);
            out.println(reportsHtmlHeader + "\n<body><div id=\"report\">\n" + "\n");
            new ReportRenderer().render(report, getReportRenderingClient(out, folder));
            out.println("</div>\n</body>\n</html>");
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static String minimize(String html) {
        html = StringUtils.replace(html, "  ", " ");
        html = StringUtils.replace(html, "\n\n", "\n");
        return html;
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
            indexReport.addContentInDiv(metadata.getDescription(), "white-space: nowrap; overflow: hidden; margin-left: 2px; margin-top: -8px; margin-bottom: 6px; color: grey; font-size: 90%");
        }

        appendLinks(indexReport, analysisResults);

        boolean hasLinks = metadata.getLinks().size() > 0;
        indexReport.addContentInDiv("", "height; 10px; border-top: 1px solid #ccc; margin-top: " + (hasLinks ? 6 : 0) + "px; margin-bottom: -4px;");

        int mainLoc = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        int mainFilesCount = analysisResults.getMainAspectAnalysisResults().getFilesCount();
        int testLoc = analysisResults.getTestAspectAnalysisResults().getLinesOfCode();
        int secondaryLoc = analysisResults.getBuildAndDeployAspectAnalysisResults().getLinesOfCode()
                + analysisResults.getGeneratedAspectAnalysisResults().getLinesOfCode()
                + analysisResults.getOtherAspectAnalysisResults().getLinesOfCode();
        int testFilesCount = analysisResults.getTestAspectAnalysisResults().getFilesCount();
        int secondaryFilesCount = analysisResults.getBuildAndDeployAspectAnalysisResults().getFilesCount()
                + analysisResults.getGeneratedAspectAnalysisResults().getFilesCount()
                + analysisResults.getOtherAspectAnalysisResults().getFilesCount();
        indexReport.startDiv("white-space: nowrap; overflow: hidden");

        addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(mainLoc), "lines of main code", FormattingUtils.getSmallTextForNumber(mainFilesCount) + " files", MAIN_LOC_COLOR, "main lines of code", "main", "SourceCodeOverview.html");
        addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(testLoc), "lines of test code", FormattingUtils.getSmallTextForNumber(testFilesCount) + " files", TEST_LOC_COLOR, "test code in scope", "test", "SourceCodeOverview.html");
        addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(secondaryLoc), "lines of other code", FormattingUtils.getSmallTextForNumber(secondaryFilesCount) + " files", TEST_LOC_COLOR, "build & deployment, generated, all other code in scope", "build", "SourceCodeOverview.html");
        ContributorsAnalysisResults contributorsAnalysisResults = analysisResults.getContributorsAnalysisResults();
        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            SourceFileAgeDistribution lastModified = analysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution();
            SourceFileAgeDistribution firstChange = analysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution();
            int notChanged = lastModified.getVeryHighRiskValue();
            double notChangedPerc = lastModified.getVeryHighRiskPercentage();
            int old = firstChange.getVeryHighRiskValue();
            double oldPerc = firstChange.getVeryHighRiskPercentage();
            addInfoBlockWithColor(indexReport, FormattingUtils.getFormattedPercentage(100 - notChangedPerc) + "%", "main code touched", "1 year (" + FormattingUtils.getSmallTextForNumber(mainLoc - notChanged) + " LOC)", MAIN_LOC_FRESH_COLOR, "", "touch", "FileAge.html");
            addInfoBlockWithColor(indexReport, FormattingUtils.getFormattedPercentage(100 - oldPerc) + "%", "new main code", "1 year (" + FormattingUtils.getSmallTextForNumber(mainLoc - old) + " LOC)", MAIN_LOC_FRESH_COLOR, "", "new", "FileAge.html");
            int recentContributors = (int) analysisResults.getContributorsAnalysisResults().getContributors().stream().filter(c -> c.getCommitsCount30Days() > 0).count();
            addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumber(recentContributors), "recent contributors", "past 30 days", PEOPLE_COLOR, "", "contributors", "Contributors.html");
        }
        indexReport.endDiv();
        StringBuilder icons = new StringBuilder("");
        AnimalIcons animalIcons = new AnimalIcons(70);
        String icon = "<div style='cursor: help' title='" + animalIcons.getInfo() + "'>" + animalIcons.getAnimalIconsForMainLoc(analysisResults.getMainAspectAnalysisResults().getLinesOfCode()) + "</div>";
        addIconsMainCode(analysisResults, icons);
        indexReport.startDiv("margin-left: 0px; border-left: 8px solid " + MAIN_LOC_COLOR + "; margin-top: -50px; padding-top: 32px; margin-bottom: 10px; padding-left: 9px; padding-bottom: 10px");
        indexReport.startTable("margin-bottom: -30px");
        indexReport.startTableRow();
        indexReport.addTableCell(icon, "border: none; vertical-align: middle; width: 80px; text-align: center; padding-right: 30px; padding-bottom: 34px");
        indexReport.addTableCell(icons.toString(), "border: none;");
        indexReport.endTableRow();
        indexReport.endTable();
        indexReport.endDiv();

        indexReport.startTabGroup();
        indexReport.addTab("overview", "Overview", true);
        indexReport.addTab("commits", "Activity", false);
        indexReport.addTab("reports", "All Reports", false);
        indexReport.addTab("visuals", "Visuals", false);
        indexReport.endDiv();

        indexReport.startTabContentSection("overview", true);
        indexReport.addLineBreak();
        indexReport.startDiv("margin: 10px");
        summarize(indexReport, analysisResults);
        indexReport.addLineBreak();
        indexReport.endDiv();

        indexReport.endTabContentSection();
        indexReport.startTabContentSection("commits", false);

        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            indexReport.startDiv("margin: 32px; font-size: 110%");
            indexReport.addLevel2Header("Overall Activity Per Year", "");
            List<ContributionTimeSlot> contributorsPerYear = contributorsAnalysisResults.getContributorsPerYear();

            indexReport.addParagraph("Latest commit date: " + contributorsAnalysisResults.getLatestCommitDate() + "",
                    "color: grey; font-size: 80%; margin-bottom: 2px;");
            indexReport.addParagraph("Reference analysis date: " + DateUtils.getAnalysisDate() + "",
                    "color: grey; font-size: 80%;");

            indexReport.startTable();
            indexReport.startTableRow();

            indexReport.startTableCell("border: none; vertical-align: top;");

            long contributorsCount = contributorsAnalysisResults.getContributors().stream().filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count();
            indexReport.startDiv("margin-top: 8px; width: 70px; height: 81px; background-color: white; border-radius: 5px; vertical-align: middle; text-align: center");
            indexReport.addContentInDiv(FormattingUtils.getSmallTextForNumber(contributorsAnalysisResults.getCommitsCount30Days()),
                    "padding-top: 12px; font-size: 36px;");
            indexReport.addContentInDiv("commits<br>(30 days)", "color: black; font-size: 80%");


            indexReport.startDiv("margin-top: 32px; width: 70px; height: 81px; background-color: white; border-radius: 5px; vertical-align: middle; text-align: center");
            indexReport.addContentInDiv(FormattingUtils.getSmallTextForNumber((int) contributorsCount),
                    "padding-top: 12px; font-size: 36px;");
            indexReport.addContentInDiv("contributors<br>(30 days)", "color: black; font-size: 80%");

            indexReport.endDiv();

            indexReport.endTableCell();
            indexReport.startTableCell("border: none");
            ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, contributorsPerYear, 20, true, 8);
            indexReport.endTableCell();
            indexReport.endTableRow();
            indexReport.endTable();

            indexReport.startDiv("font-size: 110%");
            indexReport.addLineBreak();
            indexReport.addLevel3Header("Activity Per File Extension");
            indexReport.startTable();
            indexReport.startTableRow();
            indexReport.addTableCell(getIconSvg("commits") + "<div style='font-size: 80%'>commits</div>", "border: none; text-align: center");
            indexReport.startTableCell("border: none");
            List<HistoryPerExtension> historyPerExtensionPerYear = analysisResults.getFilesHistoryAnalysisResults().getHistoryPerExtensionPerYear();
            List<String> extensions = analysisResults.getMainAspectAnalysisResults().getExtensions();
            HistoryPerLanguageGenerator.getInstanceCommits(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(indexReport);
            indexReport.endTableCell();
            indexReport.endTableRow();
            indexReport.startTableRow();
            indexReport.addTableCell("&nbsp;", "border: none");
            indexReport.addTableCell("&nbsp;", "border: none");
            indexReport.endTableRow();
            indexReport.startTableRow();
            indexReport.addTableCell(getIconSvg("contributors") + "<div style='font-size: 80%'>contributors</div>", "border: none; text-align: center");
            indexReport.startTableCell("border: none");
            HistoryPerLanguageGenerator.getInstanceContributors(historyPerExtensionPerYear, extensions).addHistoryPerLanguage(indexReport);
            indexReport.endTableCell();
            indexReport.endTableRow();
            indexReport.endTable();
            indexReport.endTabContentSection();
            indexReport.endDiv();
            indexReport.endDiv();
        }
        indexReport.endTabContentSection();

        indexReport.startTabContentSection("reports", false);
        indexReport.startDiv("margin: 24px");
        for (String[] report : reportList) {
            addReportFragment(htmlExportFolder, indexReport, report);
        }
        indexReport.endDiv();
        indexReport.endTabContentSection();

        indexReport.startTabContentSection("visuals", false);
        indexReport.startDiv("margin: 24px");
        addVisuals(indexReport, analysisResults);
        indexReport.endDiv();
        indexReport.endTabContentSection();

        String dateOfUpdate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String referenceDate = new SimpleDateFormat("yyyy-MM-dd").format(DateUtils.getCalendar().getTime());
        indexReport.addParagraph("generated by <a target='_blank' href='https://sokrates.dev/'>sokrates.dev</a> " +
                        " (<a href='../data/config.json' target='_blank'>configuration</a>)" +
                        " on " + dateOfUpdate + (!referenceDate.equals(dateOfUpdate) ? "; reference date: " + referenceDate : ""),
                "color: grey; font-size: 80%; margin-left: 10px; margin-bottom: 30px");
        export(htmlExportFolder, indexReport, "index.html", analysisResults.getCodeConfiguration().getAnalysis().getCustomHtmlReportHeaderFragment());
    }

    private static void addVisuals(RichTextReport report, CodeAnalysisResults analysisResults) {
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        report.addLevel2Header("Code Explorers");

        report.startTable();
        addScopeVisuals(report, "main", main.getFilesCount());
        addScopeVisuals(report, "test", test.getFilesCount());
        addScopeVisuals(report, "build and deployment", build.getFilesCount());
        addScopeVisuals(report, "generated", generated.getFilesCount());
        addScopeVisuals(report, "other", other.getFilesCount());
        report.endTable();

        report.addLevel2Header("Component Explorers");

        report.startTable();
        int index[] = {0};
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            index[0] += 1;
            report.startTableRow();
            report.addTableCell(logicalDecomposition.getKey());
            report.startTableCell();
            report.addNewTabLink("Bubble Chart", "visuals/bubble_chart_components_" + index[0] + ".html");
            report.endTableCell();
            report.startTableCell();
            report.addNewTabLink("Tree Map", "visuals/tree_map_components_" + index[0] + ".html");
            report.endTableCell();
            report.startTableCell();
            report.addNewTabLink("Racing Charts<br>(Commits All Time)", "visuals/racing_charts_component_commits_" + index[0] + ".html");
            report.endTableCell();
            report.startTableCell();
            report.addNewTabLink("Racing Charts<br>(Commits 12 Months Windows)", "visuals/racing_charts_component_commits_12_months_window_" + index[0] + ".html");
            report.endTableCell();
            report.endTableRow();
        });
        report.endTable();
    }

    private static void addScopeVisuals(RichTextReport report, String scopeName, int filesCount) {
        String technicalName = scopeName.toLowerCase().replace(" ", "_");
        boolean exists = filesCount > 0;
        report.startTableRow();
        report.addTableCell(scopeName.toUpperCase(), "");
        report.addTableCell(filesCount + (filesCount == 1 ? " file" : " files"), "");
        report.startTableCell();
        if (exists) {
            report.addNewTabLink("Circles", "visuals/zoomable_circles_" + technicalName.replace("_and_deployment", "") + ".html");
        } else {
            report.addContentInDiv("Circles", "color: #c0c0c0");
        }
        report.endTableCell();
        report.startTableCell();
        if (exists) {
            report.addNewTabLink("Sunburst", "visuals/zoomable_sunburst_" + technicalName.replace("_and_deployment", "") + ".html");
        } else {
            report.addContentInDiv("Sunburst", "color: #c0c0c0");
        }
        report.endTableCell();
        report.startTableCell();
        if (exists) {
            report.addNewTabLink("Text List", "../data/text/aspect_" + technicalName + ".txt");
        } else {
            report.addContentInDiv("Text List", "color: #c0c0c0");
        }
        report.endTableCell();
        report.endTableRow();
    }

    private static void addInfoBlockWithColor(RichTextReport report, String mainValue, String subtitle, String extra, String color, String tooltip, String icon, String link) {
        boolean isZero = mainValue.replaceAll("<.*?>", "").replaceAll("\\%", "").equals("0");

        String style = "border-radius: 12px;cursor: pointer;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 130px; height: 102px; z-index: 2;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";

        String specialColor = isZero ? " color: grey;" : "color: black;";
        report.startNewTabLink(link, specialColor + "");
        report.startDiv("display: inline-block; text-align: center; margin-top: 12px; cursor: pointer;");
        report.addHtmlContent("<div style='vertical-alignment: bottom; margin: 0px; margin-bottom: -10px; z-index: 3;" + (isZero ? "opacity: 0.4;" : "") + "'>" + getIconSvg(icon, 60) + "</div>");
        report.startDiv(style, tooltip);
        report.addHtmlContent("<div style='font-size: 40px; margin-top: 12px;" + specialColor + "'>" + mainValue + "</div>");
        report.addHtmlContent("<div style='color: #434343; font-size: 12px;" + specialColor + "'>" + subtitle + "</div>");
        report.addHtmlContent("<div style='color: #434343; font-size: 11px; color: grey'>" + extra + "</div>");
        report.endDiv();
        report.endDiv();
        report.endNewTabLink();
    }

    private static void addInfoBlockWithColorWithIcon(RichTextReport report, String mainValue, String subtitle, String extra, String color, String tooltip, String icon) {
        String style = "border-radius: 12px;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 130px; height: 93px;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";

        report.startDiv(style, tooltip);
        String specialColor = mainValue.equals("<b>0</b>") ? " color: grey;" : "";
        report.addHtmlContent("<div style='font-size: 40px; margin-top: 10px;" + specialColor + "'>" + mainValue + "</div>");
        report.addHtmlContent("<div style='color: #434343; font-size: 12px;" + specialColor + "'>" + subtitle + "</div>");
        report.addHtmlContent("<div style='color: #434343; font-size: 11px;color: grey'>" + extra + "</div>");
        report.endDiv();
    }

    private static void addIconsMainCode(CodeAnalysisResults analysisResults, StringBuilder summary) {
        List<NumericMetric> extensions = analysisResults.getMainAspectAnalysisResults().getLinesOfCodePerExtension();
        summary.append("<div style='margin-bottom: 20px; white-space: nowrap; overflow: hidden;'>");
        Set<String> alreadyAddedImage = new HashSet<>();
        extensions.forEach(ext -> {
            String lang = ext.getName().toUpperCase().replace("*.", "").trim();
            String image = DataImageUtils.getLangDataImage(lang);
            if (image == null || !alreadyAddedImage.contains(image)) {
                int loc = ext.getValue().intValue();
                int fontSize = loc >= 1000 ? 20 : 20;
                int width = loc >= 1000 ? 42 : 43;
                summary.append("<div style='width: " + width + "px; text-align: center; display: inline-block; border-radius: 5px; background-color: white; padding: 8px; margin-right: 4px;'>"
                        + DataImageUtils.getLangDataImageDiv42(lang)
                        + "<div style='margin-top: 3px; font-size: " + fontSize + "px'>" + FormattingUtils.getSmallTextForNumber(loc) + "</div>"
                        + "<div style='font-size: 10px; white-space: no-wrap; overflow: hidden; color: grey;'>" + lang.toLowerCase() + "</div>"
                        + "</div>");
                if (image != null) {
                    alreadyAddedImage.add(image);
                }
            }
        });
        summary.append("</div>");
    }

    private static String getDetailsIcon() {
        return getIconSvg("details", 22);
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
        if (links.size() > 0) {
            report.startDiv("font-size: 80%; margin-top: 8px; margin-left: 2px;");
            links.forEach(link -> {
                if (links.indexOf(link) > 0) {
                    report.addHtmlContent(" | ");
                }
                report.addNewTabLink(link.getLabel(), link.getHref());
            });
            report.endDiv();
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
            indexReport.addHtmlContent("<a style='text-decoration: none' href=\"" + reportFileName + "\">");
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
            indexReport.addHtmlContent("<b>" + reportTitle + "</b>");
            indexReport.endDiv();
            indexReport.addHtmlContent("</a>");
        } else {
            indexReport.startDiv("");
            indexReport.addHtmlContent("<b>" + reportTitle + "</b>");
            indexReport.endDiv();
        }
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
            list.add(new String[]{"FileAge.html", "File Age & Freshness", "file_history"});
            list.add(new String[]{"FileChangeFrequency.html", "File Change Frequency", "change"});
            list.add(new String[]{"FileTemporalDependencies.html", "Temporal Dependencies", "temporal_dependency"});
            list.add(new String[]{"Commits.html", "Commits", "commits"});
            list.add(new String[]{"Contributors.html", "Contributors", "contributors"});
        }
        if (showUnits) {
            list.add(new String[]{"UnitSize.html", "Unit Size", "unit_size"});
            list.add(new String[]{"ConditionalComplexity.html", "Conditional Complexity", "conditional"});
        }
        if (showConcerns) {
            list.add(new String[]{"FeaturesOfInterest.html", "Features of Interest", "cross_cutting_concerns"});
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
            list.add(new String[]{"", "File Age & Freshness", "file_history"});
            list.add(new String[]{"", "File Change Frequency", "change"});
            list.add(new String[]{"", "Temporal Dependencies", "temporal_dependency"});
            list.add(new String[]{"", "Contributors", "contributors"});
        }
        if (!showUnits) {
            list.add(new String[]{"", "Unit Size", "unit_size"});
            list.add(new String[]{"", "Conditional Complexity", "conditional"});
        }
        if (!showConcerns) {
            list.add(new String[]{"", "Features of Interest", "cross_cutting_concerns"});
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