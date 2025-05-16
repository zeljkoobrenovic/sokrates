/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.reports.utils.PromptsUtils;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            String titleText = extractTitle(report.getDisplayName());
            String reportsHtmlHeader = ReportConstants.REPORTS_HTML_HEADER.replace(
                    "<title></title>",
                    "<title>" + titleText + "</title>"
            );
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

    protected static String extractTitle(String displayName) {
        return displayName.replaceAll("<.*?>", " ").replaceAll("  ", " ").trim();
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
            indexReport.addContentInDiv(metadata.getDescription(), "white-space: nowrap; overflow: hidden; margin-left: 2px; margin-top: 0px; margin-bottom: 14px; color: grey; font-size: 90%");
        }

        appendLinks(indexReport, analysisResults);

        boolean hasLinks = metadata.getLinks().size() > 0;
        indexReport.addContentInDiv("", "height; 10px; margin-top: " + (hasLinks ? 6 : 0) + "px; margin-bottom: 6px;");

        int linesOfCodeMain = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        int mainLoc = linesOfCodeMain;
        int mainFilesCount = analysisResults.getMainAspectAnalysisResults().getFilesCount();
        int testLoc = analysisResults.getTestAspectAnalysisResults().getLinesOfCode();
        int secondaryLoc = analysisResults.getBuildAndDeployAspectAnalysisResults().getLinesOfCode()
                + analysisResults.getGeneratedAspectAnalysisResults().getLinesOfCode()
                + analysisResults.getOtherAspectAnalysisResults().getLinesOfCode();
        int testFilesCount = analysisResults.getTestAspectAnalysisResults().getFilesCount();
        int secondaryFilesCount = analysisResults.getBuildAndDeployAspectAnalysisResults().getFilesCount()
                + analysisResults.getGeneratedAspectAnalysisResults().getFilesCount()
                + analysisResults.getOtherAspectAnalysisResults().getFilesCount();


        indexReport.startTabGroup();
        indexReport.addTab("overview", "Overview", true);
        indexReport.addTab("quality", "Analyses", false);
        indexReport.addTab("commits", "Activity", false);
        indexReport.addTab("files", "Files", false);
        indexReport.addTab("visuals", "Visuals", false);
        indexReport.addTab("data", "Data", false);
        indexReport.addTab("prompts", "AI Prompts", false);
        indexReport.endDiv();

        indexReport.startTabContentSection("overview", true);

        indexReport.startDiv("white-space: nowrap; overflow: hidden");

        addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumberMinK(mainLoc), "lines of main code", FormattingUtils.getSmallTextForNumber(mainFilesCount) + " files", MAIN_LOC_COLOR, "main lines of code", "main", "SourceCodeOverview.html");
        addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumberMinK(testLoc), "lines of test code", FormattingUtils.getSmallTextForNumber(testFilesCount) + " files", TEST_LOC_COLOR, "test code in scope", "test", "SourceCodeOverview.html");
        addInfoBlockWithColor(indexReport, FormattingUtils.getSmallTextForNumberMinK(secondaryLoc), "lines of other code", FormattingUtils.getSmallTextForNumber(secondaryFilesCount) + " files", TEST_LOC_COLOR, "build & deployment, generated, all other code in scope", "build", "SourceCodeOverview.html");
        ContributorsAnalysisResults contributorsAnalysisResults = analysisResults.getContributorsAnalysisResults();
        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            SourceFileAgeDistribution lastModified = analysisResults.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution();
            SourceFileAgeDistribution firstChange = analysisResults.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution();
            int notChanged = lastModified.getVeryHighRiskValue();
            double notChangedPerc = lastModified.getVeryHighRiskPercentage();
            int old = firstChange.getVeryHighRiskValue();
            double oldPerc = firstChange.getVeryHighRiskPercentage();
            int ageInDays = analysisResults.getFilesHistoryAnalysisResults().getAgeInDays();
            String age = ageInDays < 365 ? "<1y" : (int) Math.round(ageInDays / 365.0) + "y";
            addInfoBlockWithColor(indexReport, age, "age", FormattingUtils.formatCount(ageInDays) + " days", MAIN_LOC_FRESH_COLOR, "", "file_history", "FileAge.html");
            addInfoBlockWithColor(indexReport, FormattingUtils.getFormattedPercentage(100 - notChangedPerc) + "%", "main code touched", "1 year (" + FormattingUtils.getSmallTextForNumber(mainLoc - notChanged) + " LOC)", MAIN_LOC_FRESH_COLOR, "", "touch", "FileAge.html");
            addInfoBlockWithColor(indexReport, FormattingUtils.getFormattedPercentage(100 - oldPerc) + "%", "new main code", "1 year (" + FormattingUtils.getSmallTextForNumber(mainLoc - old) + " LOC)", MAIN_LOC_FRESH_COLOR, "", "new", "FileAge.html");
        }
        indexReport.endDiv();
        StringBuilder icons = new StringBuilder("");
        addIconsMainCode(analysisResults, icons);
        indexReport.startDiv("margin-left: 0px; margin-top: -65px; padding-top: 32px; margin-bottom: 0px; padding-left: 0px; padding-bottom: 10px");
        indexReport.startTable("margin-bottom: -20px");
        indexReport.startTableRow();
        indexReport.addTableCell(icons.toString(), "border: none;");
        indexReport.endTableRow();
        indexReport.endTable();
        indexReport.startDiv("");

        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            addSummaryActivityTable(contributorsAnalysisResults, indexReport);
        }

        indexReport.endDiv();
        indexReport.endDiv();

        indexReport.addHtmlContent("<iframe src='Structure.html' style='border: none; width: 1000px; height: 1090px; overflow: hidden'></iframe>");
        indexReport.endTabContentSection();

        indexReport.startTabContentSection("quality", false);
        indexReport.addLineBreak();
        indexReport.startDiv("margin: 10px");
        summarize(indexReport, analysisResults);
        indexReport.addLineBreak();
        indexReport.endDiv();
        indexReport.startDiv("margin: 24px");
        indexReport.addLevel2Header("All Analysis Reports");
        for (String[] report : reportList) {
            addReportFragment(htmlExportFolder, indexReport, report);
        }
        indexReport.endDiv();

        indexReport.endTabContentSection();

        indexReport.startTabContentSection("files", false);
        indexReport.addLineBreak();
        indexReport.addHtmlContent("<iframe src='../explorers/files-explorer.html' style='width: 100%; border: none; height: calc(100vh - 220px); overflow: hidden; margin-top: -12px'></iframe>");

        indexReport.endTabContentSection();

        indexReport.startTabContentSection("commits", false);

        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            indexReport.startDiv("margin: 32px; font-size: 110%");
            indexReport.addLevel2Header("Overall Activity Per Year", "");

            indexReport.addParagraph("Latest commit date: " + contributorsAnalysisResults.getLatestCommitDate() + "",
                    "color: grey; font-size: 80%; margin-bottom: 2px;");
            indexReport.addParagraph("Reference analysis date: " + DateUtils.getAnalysisDate() + "",
                    "color: grey; font-size: 80%;");

            long contributorsCount = contributorsAnalysisResults.getContributors().stream().filter(c -> !c.isBot() && c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count();
            int commitsCount30Days = contributorsAnalysisResults.getCommitsCount30Days();

            indexReport.startTable();
            indexReport.startTableRow();

            indexReport.startTableCell("border: none; vertical-align: top;");

            indexReport.startDiv("margin-top: 8px; width: 80px; height: 81px; background-color: white; border-radius: 5px; vertical-align: middle; text-align: center");
            indexReport.startNewTabLink("Commits.html", "");
            indexReport.addContentInDiv(FormattingUtils.getSmallTextForNumber(commitsCount30Days),
                    "padding-top: 12px; font-size: 36px;");
            indexReport.addContentInDiv((commitsCount30Days == 1 ? "commit" : "commits") + "<br>(30 days)", "color: black; font-size: 80%");
            indexReport.endNewTabLink();
            indexReport.endDiv();

            indexReport.startDiv("margin-top: 32px; width: 80px; height: 81px; background-color: white; border-radius: 5px; vertical-align: middle; text-align: center");
            indexReport.startNewTabLink("Contributors.html", "");
            indexReport.addContentInDiv(FormattingUtils.getSmallTextForNumber((int) contributorsCount),
                    "padding-top: 12px; font-size: 36px;");
            indexReport.addContentInDiv((contributorsCount == 1 ? "contributor" : "contributors") + "<br>(30 days)", "color: black; font-size: 80%");
            indexReport.endNewTabLink();
            indexReport.endDiv();

            indexReport.endTableCell();

            indexReport.startTableCell("border: none");
            List<ContributionTimeSlot> contributorsPerYear = contributorsAnalysisResults.getContributorsPerYear();
            ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, contributorsPerYear, 20, true, true, 8, commitsCount30Days == 0);
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
        } else {
            indexReport.addParagraph("No commit history found.", "color: grey; margin-left: 10px; margin: 15px");
        }
        indexReport.endTabContentSection();

        indexReport.startTabContentSection("visuals", false);
        indexReport.startDiv("margin: 24px");
        addVisuals(indexReport, analysisResults, htmlExportFolder);
        indexReport.endDiv();
        indexReport.endTabContentSection();

        indexReport.startTabContentSection("data", false);
        indexReport.startDiv("margin: 24px");
        addData(indexReport, analysisResults);
        indexReport.endDiv();
        indexReport.endTabContentSection();

        indexReport.startTabContentSection("prompts", false);
        indexReport.startDiv("margin: 24px");
        addPrompts(indexReport, analysisResults);
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

    private static void addPrompts(RichTextReport report, CodeAnalysisResults analysisResults) {
        report.addParagraph("Generative AI tools, like ChatGPT or Gemini, can help you explore and discuss various aspects of source code repositories using simple prompts and file uploads. Sokrates provides you with curated data that you can use to analyze your source code further.", "");

        PromptsUtils.addRepositoryPromptSection("git-history-analyzer", report, analysisResults, "Example Prompt 1: Repository Evolution Analyzer (based on git history)", "", Arrays.asList(new Link[]{new Link("git-history.zip", "../data/zips/git-history.zip")}));

        PromptsUtils.addRepositoryPromptSection("path-name-conventions-analyzer", report, analysisResults, "Example Prompt 2: File name conventions", "", Arrays.asList(new Link("files.json", "../data/files.json")));

        PromptsUtils.addRepositoryPromptSection("technology-analyzer", report, analysisResults, "Example Prompt 3: Technology analyzer (based of file paths)", "", Arrays.asList(new Link("files.json", "../data/files.json")));
    }

    private static void addSummaryActivityTable(ContributorsAnalysisResults contributorsAnalysisResults, RichTextReport indexReport) {
        List<ContributionTimeSlot> contributorsPerYear = contributorsAnalysisResults.getContributorsPerYear();
        Map<String, ContributionTimeSlot> map = new HashMap<>();
        contributorsPerYear.forEach(c -> map.put(c.getTimeSlot(), c));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        String year = currentYear + "";

        while (!map.containsKey(year)) {
            contributorsPerYear.add(new ContributionTimeSlot(year));
            currentYear -= 1;
            year = currentYear + "";
        }

        long contributorsCount = contributorsAnalysisResults.getContributors().stream().filter(c -> !c.isBot() && c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS)).count();
        int commitsCount30Days = contributorsAnalysisResults.getCommitsCount30Days();
        indexReport.startTable("margin-bottom: -20px; border-top: 1px dashed grey; border-bottom: 1px dashed grey; padding-top: 10px; margin-top: 10px; margin-bottom: 10px;");
        indexReport.startTableRow();

        indexReport.startTableCell("border: none; vertical-align: top;");

        indexReport.startDiv("margin-top: 8px; width: 80px; height: 81px; background-color: white; border-radius: 5px; vertical-align: middle; text-align: center");
        indexReport.startNewTabLink("Commits.html", commitsCount30Days == 0 ? "opacity: 0.4" : "");
        indexReport.addContentInDiv(FormattingUtils.getSmallTextForNumber(commitsCount30Days),
                "padding-top: 12px; font-size: 36px;");
        indexReport.addContentInDiv((commitsCount30Days == 1 ? "commit" : "commits") + "<br>(30 days)", "color: black; font-size: 80%");
        indexReport.endNewTabLink();
        indexReport.endDiv();

        indexReport.startDiv("margin-top: 32px; width: 80px; height: 81px; background-color: white; border-radius: 5px; vertical-align: middle; text-align: center");
        indexReport.startNewTabLink("Contributors.html", contributorsCount == 0 ? "opacity: 0.4" : "");
        indexReport.addContentInDiv(FormattingUtils.getSmallTextForNumber((int) contributorsCount),
                "padding-top: 12px; font-size: 36px;");
        indexReport.addContentInDiv((contributorsCount == 1 ? "contributor" : "contributors") + "<br>(30 days)", "color: black; font-size: 80%");
        indexReport.endNewTabLink();
        indexReport.endDiv();
        indexReport.endTableCell();
        indexReport.startTableCell("border: none");
        ContributorsReportUtils.addContributorsPerTimeSlot(indexReport, contributorsPerYear, 20, true, true, 8, contributorsCount == 0);
        indexReport.endTableCell();
        indexReport.endTableRow();
        indexReport.endTable();
    }

    private static void addVisuals(RichTextReport report, CodeAnalysisResults analysisResults, File htmlExportFolder) {
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        report.addLevel2Header("Visual Code Explorers");

        report.startTable();
        addScopeVisuals(report, "main", main.getFilesCount());
        addScopeVisuals(report, "test", test.getFilesCount());
        addScopeVisuals(report, "build and deployment", build.getFilesCount());
        addScopeVisuals(report, "generated", generated.getFilesCount());
        addScopeVisuals(report, "other", other.getFilesCount());
        report.endTable();

        report.addLineBreak();
        report.addLineBreak();
        report.addLevel2Header("File Visualizations");

        report.addParagraph("<a target='_blank' href='FileSize.html'>File size</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(getIconSvg("file_size", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("3D view of file size", "visuals/files_3d.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by size category", "visuals/zoomable_circles_main_loc_coloring_categories.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles_main_loc_coloring.html");
        report.endListItem();
        report.endUnorderedList();
        report.endTable();

        boolean showDuplication = !analysisResults.skipDuplicationAnalysis() && analysisResults.getDuplicationAnalysisResults().getAllDuplicates().size() > 0;
        if (showDuplication) {
            report.addParagraph("<a target='_blank' href='Duplication.html'>Duplication</a> views:", "margin-bottom: 0;");
            report.startTable("");
            report.startTableRow();
            report.startTableCell("border: none");
            report.addHtmlContent(getIconSvg("duplication", 50));
            report.endTableCell();
            report.startTableCell("border: none");
            report.startUnorderedList();
            report.startListItem();
            report.addNewTabLink("graphviz graph of duplication among files", "visuals/duplication_among_files.svg");
            report.endListItem();
            report.startListItem();
            report.addNewTabLink("2D force graph of duplication among files", "visuals/duplication_among_files_force_2d.html");
            report.endListItem();
            report.startListItem();
            report.addNewTabLink("3D force graph of duplication among files", "visuals/duplication_among_files_force_3d.html");
            report.endListItem();
            report.startListItem();
            report.addNewTabLink("2D view of duplication among files (with duplicates)", "visuals/duplication_among_files_with_duplicates_force_2d.html");
            report.endListItem();
            report.startListItem();
            report.addNewTabLink("3D view of duplication among files (with duplicates)", "visuals/duplication_among_files_with_duplicates_force_3d.html");
            report.endListItem();
            report.endUnorderedList();
            report.endTable();
        }

        report.addParagraph("<a target='_blank' href='FileAge.html'>File age</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(getIconSvg("file_history", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("files grouped by age category", "visuals/zoomable_circles_main_age_coloring_categories.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles_main_age_coloring.html");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(getIconSvg("file_history", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("files grouped by freshness category", "visuals/zoomable_circles_main_freshness_coloring_categories.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles_main_freshness_coloring.html");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addParagraph("<a target='_blank' href='FileChangeFrequency.html'>File change frequency</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(getIconSvg("change", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("files grouped by change frequency category", "visuals/zoomable_circles_main_update_frequency_coloring_categories.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles_main_update_frequency_coloring.html");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addParagraph("<a target='_blank' href='FileChangeFrequency.html'>Contributors per file</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(getIconSvg("change", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("files grouped by number of contributors category", "visuals/zoomable_circles_main_contributors_count_coloring_categories.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("files grouped by folder", "visuals/zoomable_circles_main_contributors_count_coloring.html");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();
        report.addLevel2Header("Contributor Visualizations");
        report.addParagraph("<a target='_blank' href='Contributors.html'>Contributor dependency</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(getIconSvg("contributors", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addHtmlContent("past 30 days: ");
        report.addNewTabLink("graphviz", "visuals/people_dependencies_30_1.svg");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph", "visuals/people_dependencies_30_1_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with files)", "visuals/people_dependencies_via_files_30_2_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with shared files only)", "visuals/people_dependencies_via_files_30_2_force_2d_only_shared_file.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/people_dependencies_30_1_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with files)", "visuals/people_dependencies_via_files_30_2_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with shared files only)", "visuals/people_dependencies_via_files_30_2_force_3d_only_shared_file.html");
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past 3 months: ");
        report.addNewTabLink("graphviz", "visuals/people_dependencies_90_3.svg");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph", "visuals/people_dependencies_90_3_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with files)", "visuals/people_dependencies_via_files_90_4_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with shared files only)", "visuals/people_dependencies_via_files_90_4_force_2d_only_shared_file.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/people_dependencies_90_3_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with files)", "visuals/people_dependencies_via_files_90_4_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with shared files only)", "visuals/people_dependencies_via_files_90_4_force_3d_only_shared_file.html");
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past 6 months: ");
        report.addNewTabLink("graphviz", "visuals/people_dependencies_180_5.svg");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph", "visuals/people_dependencies_180_5_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with files)", "visuals/people_dependencies_via_files_180_6_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with shared files only)", "visuals/people_dependencies_via_files_180_6_force_2d_only_shared_file.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/people_dependencies_180_5_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with files)", "visuals/people_dependencies_via_files_180_6_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with shared files only)", "visuals/people_dependencies_via_files_180_6_force_3d_only_shared_file.html");
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past year: ");
        report.addNewTabLink("graphviz", "visuals/people_dependencies_365_7.svg");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph", "visuals/people_dependencies_365_7_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with files)", "visuals/people_dependencies_via_files_365_8_force_2d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("2D graph (with shared files only)", "visuals/people_dependencies_via_files_365_8_force_2d_only_shared_file.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph", "visuals/people_dependencies_365_7_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with files)", "visuals/people_dependencies_via_files_365_8_force_3d.html");
        report.addHtmlContent(" | ");
        report.addNewTabLink("3D graph (with shared files only)", "visuals/people_dependencies_via_files_365_8_force_3d_only_shared_file.html");
        report.endListItem();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();
        report.addLevel2Header("Components and Dependencies Visualizations");

        report.startTable("text-align: center");
        report.addHtmlContent("<tr>");
        report.addHtmlContent("<td rowspan='3' style='border: none'></td>");
        report.addHtmlContent("<td colspan='2' style='text-align: center; border: none'>" + getIconSvg("code_organization", 50) + "</td>");
        report.addHtmlContent("<td colspan='6' style='text-align: center; border: none'>" + getIconSvg("temporal_dependency", 50) + "</td>");
        report.addHtmlContent("<td colspan='1' style='text-align: center; border: none'>" + getIconSvg("duplication", 50) + "</td>");
        report.addHtmlContent("<td colspan='2' style='text-align: center; border: none'>" + getIconSvg("commits", 50) + "</td>");
        report.addHtmlContent("</tr>");
        report.addHtmlContent("<tr>");
        report.addHtmlContent("<td colspan='2' rowspan='2' style='text-align: center'><a target='_blank' href='Components.html'>Components</a></td>");
        report.addHtmlContent("<td colspan='9' style='text-align: center'><a target='_blank' href='FileTemporalDependencies.html'>Temporal Dependencies</a></td>");
        report.addHtmlContent("<td colspan='1' rowspan='2' style='text-align: center'><a target='_blank' href='Duplication.html'>Duplication</a></td>");
        report.addHtmlContent("<td colspan='2' rowspan='2' style='text-align: center'><a target='_blank' href='Commits.html'>Commits Racing Charts</a></td>");
        report.addHtmlContent("</tr>");
        report.addHtmlContent("<tr>");
        report.addHtmlContent("<td colspan='3' style='text-align: center'>30 days</td>");
        report.addHtmlContent("<td colspan='3' style='text-align: center'>3 months</td>");
        report.addHtmlContent("<td colspan='3' style='text-align: center'>6 months</td>");
        report.addHtmlContent("</tr>");

        int index[] = {0};
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            index[0] += 1;
            report.startTableRow();
            report.addTableCell(logicalDecomposition.getKey().toUpperCase() + " (" + logicalDecomposition.getComponents().size() + ")");
            report.startTableCell("text-align: center");
            report.addNewTabLink("Bubble Chart", "visuals/bubble_chart_components_" + index[0] + ".html");
            report.endTableCell();
            report.startTableCell("text-align: center");
            report.addNewTabLink("Tree Map", "visuals/tree_map_components_" + index[0] + ".html");
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days().size() > 0) {
                report.addNewTabLink("graphviz", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_30_days.svg");
            } else {
                report.addContentInDiv("graphviz", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days().size() > 0) {
                report.addNewTabLink("2D", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_30_days_force_2d.html");
            } else {
                report.addContentInDiv("2D", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days().size() > 0) {
                report.addNewTabLink("3D", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_30_days_force_3d.html");
            } else {
                report.addContentInDiv("3D", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether90Days().size() > 0) {
                report.addNewTabLink("graphviz", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_90_days.svg");
            } else {
                report.addContentInDiv("graphviz", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether90Days().size() > 0) {
                report.addNewTabLink("2D", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_90_days_force_2d.html");
            } else {
                report.addContentInDiv("2D", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether90Days().size() > 0) {
                report.addNewTabLink("3D", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_90_days_force_3d.html");
            } else {
                report.addContentInDiv("3D", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days().size() > 0) {
                report.addNewTabLink("graphviz", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_180_days.svg");
            } else {
                report.addContentInDiv("graphviz", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days().size() > 0) {
                report.addNewTabLink("2D", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_180_days_force_2d.html");
            } else {
                report.addContentInDiv("2D", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days().size() > 0) {
                report.addNewTabLink("3D", "visuals/file_changed_together_dependencies_logical_decomposition_" + index[0] + "_180_days_force_3d.html");
            } else {
                report.addContentInDiv("3D", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            String duplicationGraphPath = "visuals/duplication_dependencies_" + index[0] + ".svg";
            if (new File(htmlExportFolder, duplicationGraphPath).exists()) {
                report.addNewTabLink("Duplication Graph", duplicationGraphPath);
            } else {
                report.addContentInDiv("Duplication Graph", "color: #c0c0c0");
            }
            report.endTableCell();
            report.startTableCell("text-align: center");
            report.addNewTabLink("All Time", "visuals/racing_charts_component_commits_" + index[0] + ".html?tickDuration=600");
            report.endTableCell();
            report.startTableCell("text-align: center");
            report.addNewTabLink("12 Months", "visuals/racing_charts_component_commits_12_months_window_" + index[0] + ".html?tickDuration=600");
            report.endTableCell();
            report.endTableRow();
        });
        report.endTable();


        report.addLineBreak();
        report.addLineBreak();
        report.addLevel2Header("File Dependencies Visualizations");

        report.addParagraph("<a target='_blank' href='FileTemporalDependencies.html'>Temporal dependencies</a> among files:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(getIconSvg("temporal_dependency", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addHtmlContent("past 30 days: ");
        if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days().size() > 0) {
            report.addNewTabLink("graphviz", "visuals/file_changed_together_dependencies_files_30_days.svg");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph", "visuals/file_changed_together_dependencies_files_30_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_30_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph", "visuals/file_changed_together_dependencies_files_30_days_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_30_days_force_3d.html");
        } else {
            report.addHtmlContent("no dependencies");
        }
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past 3 months: ");
        if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether90Days().size() > 0) {
            report.addNewTabLink("graphviz", "visuals/file_changed_together_dependencies_files_90_days.svg");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph", "visuals/file_changed_together_dependencies_files_90_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_90_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph", "visuals/file_changed_together_dependencies_files_90_days_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_90_days_force_3d.html");
        } else {
            report.addHtmlContent("no dependencies");
        }
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("past 6 months: ");
        if (analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether180Days().size() > 0) {
            report.addNewTabLink("graphviz", "visuals/file_changed_together_dependencies_files_180_days.svg");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph", "visuals/file_changed_together_dependencies_files_180_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("2D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_180_days_force_2d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph", "visuals/file_changed_together_dependencies_files_180_days_force_3d.html");
            report.addHtmlContent(" | ");
            report.addNewTabLink("3D graph (with commits)", "visuals/file_changed_together_dependencies_with_commits_components_180_days_force_3d.html");
        } else {
            report.addHtmlContent("no dependencies");
        }
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();
        report.addLineBreak();
        report.addLevel2Header("Units Visualizations");

        report.addParagraph("Unit <a target='_blank' href='UnitSize.html'>size</a> and <a target='_blank' href='ConditionalComplexity.html'>conditional complexity</a> views:", "margin-bottom: 0;");
        report.startTable("");
        report.startTableRow();
        report.startTableCell("border: none");
        report.addHtmlContent(getIconSvg("unit_size", 50));
        report.endTableCell();
        report.startTableCell("border: none");
        report.startUnorderedList();
        report.startListItem();
        report.addNewTabLink("3D view of unit size", "visuals/units_3d_size.html");
        report.endListItem();
        report.startListItem();
        report.addNewTabLink("3D view of unit complexity", "visuals/units_3d_complexity.html");
        report.endListItem();
        report.endUnorderedList();
        report.endTableCell();
        report.endTableRow();
        report.endTable();

        report.addLineBreak();

    }

    private static void addData(RichTextReport report, CodeAnalysisResults analysisResults) {
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
        report.addNewTabLink("History Data", "../data/text/mainFilesWithHistory.txt");
        report.endListItem();
        report.startListItem();
        report.addHtmlContent("IGNORED FILES: ");
        report.addNewTabLink("By Extension", "../data/text/excluded_files_ignored_extensions.txt");
        report.addHtmlContent(" | ");
        report.addNewTabLink("By Rule", "../data/text/excluded_files_ignored_rules.txt");
        report.endListItem();
        report.endUnorderedList();

        report.addLineBreak();
        report.addLevel2Header("Analysis Results");
        report.startUnorderedList();

        report.startListItem();
        report.addHtmlContent("CONFIGURATION: ");
        report.addNewTabLink("JSON", "../data/config.json");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("ALL ANALYSIS RESULTS: ");
        report.addNewTabLink("JSON", "../data/analysisResults.json");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("DUPLICATES: ");
        report.addNewTabLink("TXT", "../data/text/duplicates.txt");
        report.addHtmlContent(" | ");
        report.addNewTabLink("JSON", "../data/duplicates.json");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("UNITS: ");
        report.addNewTabLink("TXT", "../data/text/units.txt");
        report.addHtmlContent(" | ");
        report.addNewTabLink("JSON", "../data/units.json");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("CONTRIBUTORS: ");
        report.addNewTabLink("TXT", "../data/text/contributors.txt");
        report.addHtmlContent(" | ");
        report.addNewTabLink("JSON", "../data/contributors.json");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("LOGICAL DECOMPOSITIONS: ");
        report.addNewTabLink("JSON", "../data/logical_decompositions.json");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("CONCERNS: ");
        report.addNewTabLink("JSON", "../data/concerns.json");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("CONTROLS: ");
        report.addNewTabLink("TXT", "../data/text/controls.txt");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("ALL METRICS: ");
        report.addNewTabLink("TXT", "../data/text/metrics.txt");
        report.endListItem();


        report.endUnorderedList();

        //

        report.addLineBreak();
        report.addLevel2Header("Zipped Files");
        report.startUnorderedList();

        report.startListItem();
        report.addHtmlContent("GIT HISTORY: ");
        report.addNewTabLink("ZIP", "../data/zips/git-history.zip");
        report.endListItem();

        report.startListItem();
        report.addHtmlContent("ALL FILES IN ALL ANALYSIS SCOPES: ");
        report.addNewTabLink("ZIP", "../data/zips/all_files.zip");
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
            report.addNewTabLink(displayName + " (" + infoText + ")", "../data/text/aspect_" + technicalName + ".txt");
        } else {
            report.addContentInDiv(displayName, "color: #c0c0c0");
        }
        report.endListItem();
    }

    private static void addScopeVisuals(RichTextReport report, String scopeName, int filesCount) {
        String technicalName = scopeName.toLowerCase().replace(" ", "_");
        boolean exists = filesCount > 0;
        report.startTableRow(exists ? "" : "color: #c0c0c0");
        report.startTableCell();
        report.addHtmlContent(getIconSvg(technicalName, 42));
        report.endTableCell();
        report.addTableCell(scopeName.toUpperCase() + " (" + filesCount + ")", "");
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
        report.endTableCell();
        report.endTableRow();
    }

    private static void addInfoBlockWithColor(RichTextReport report, String mainValue, String subtitle, String extra, String color, String tooltip, String icon, String link) {
        boolean isZero = mainValue.replaceAll("<.*?>", "").replaceAll("\\%", "").equals("0");

        String style = "border-radius: 12px;cursor: pointer;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 130px; height: 102px; z-index: 2;";
        style += "background-color: " + color + "; text-align: center; vertical-align: middle; margin-bottom: 36px;";
        style += "box-shadow: rgba(0, 0, 0, 0.15) 2.4px 2.4px 3.2px;";

        String specialColor = isZero ? " color: grey;" : "color: black;";
        report.startNewTabLink(link, specialColor + "");
        report.startDiv("display: inline-block; text-align: center; margin-top: 12px; cursor: pointer;");
        report.addHtmlContent("<div style='vertical-alignment: bottom; margin: 0px; margin-bottom: -10px; z-index: 3;" + (isZero ? "opacity: 0.4;" : "") + "'>" + getIconSvg(icon, 40) + "</div>");
        report.startDiv(style, tooltip);
        report.addHtmlContent("<div style='font-size: 40px; margin-top: 12px;" + specialColor + "'>" + mainValue + "</div>");
        report.addHtmlContent("<div style='color: #434343; font-size: 12px;" + specialColor + "'>" + subtitle + "</div>");
        report.addHtmlContent("<div style='margin-top: 4px; color: #434343; font-size: 11px;'>" + extra + "</div>");
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
        boolean first[] = {true};
        extensions.stream().limit(16).forEach(ext -> {
            String lang = ext.getName().toUpperCase().replace("*.", "").trim();
            int loc = ext.getValue().intValue();
            int fontSize = loc >= 1000 ? 20 : 20;
            int width = (first[0] ? loc >= 1000 ? 64 : 65 : loc >= 1000 ? 42 : 43);
            summary.append("<div style='width: " + width + "px; text-align: center; display: inline-block; border-radius: 5px; background-color: white; padding: 8px; margin-right: 4px;'>"
                    + (first[0] ? DataImageUtils.getLangDataImageDiv64(lang) : DataImageUtils.getLangDataImageDiv42(lang))
                    + "<div style='margin-top: 3px; font-size: " + fontSize + "px'>" + FormattingUtils.getSmallTextForNumberMinK(loc) + "</div>"
                    + "<div style='font-size: 10px; white-space: no-wrap; overflow: hidden; color: grey;'>" + lang.toLowerCase() + "</div>"
                    + "</div>");
            first[0] = false;
        });
        summary.append("</div>");
    }

    public static String getDetailsIcon() {
        return getIconSvg("details", 22);
    }


    private static void summarize(RichTextReport indexReport, CodeAnalysisResults analysisResults) {
        new SummaryUtils().summarize(analysisResults, indexReport);
    }

    private static void appendLinks(RichTextReport report, CodeAnalysisResults analysisResults) {
        List<Link> links = analysisResults.getCodeConfiguration().getMetadata().getLinks();
        if (links.size() > 0) {
            report.startDiv("font-size: 80%; margin-top: 0px; margin-bottom: 12px; margin-left: 2px;");
            links.forEach(link -> {
                if (links.indexOf(link) > 0) {
                    report.addHtmlContent(" | ");
                }
                report.addNewTabLink(link.getLabel() + "&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON_SMALL, link.getHref());
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
            indexReport.addHtmlContent("<a target='_blank' style='text-decoration: none' href=\"" + reportFileName + "\">");
            indexReport.addHtmlContent("<div class='group' style='border-radius: 8px; padding: 0px 20px 5px 20px; margin: 10px; width: 130px; height: 175px; text-align: center; display: inline-block; vertical-align: top'>");
        } else {
            indexReport.addHtmlContent("<div class='group' style='border-radius: 8px; padding: 0px 20px 5px 20px; margin: 10px; width: 130px; height: 175px; text-align: center; display: inline-block; vertical-align: top; opacity: 0.4'>");
        }

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
        boolean showDuplication = mainExists && !analysisResults.skipDuplicationAnalysis();
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
            list.add(new String[]{"Components.html", "Components", "code_organization"});
            if (showDependencies) {
                list.add(new String[]{"ComponentsAndDependencies.html", "Component Dependencies*", "dependencies"});
            }
            if (showHistoryReport) {
                list.add(new String[]{"FileTemporalDependencies.html", "Temporal Dependencies", "temporal_dependency"});
            }
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
            list.add(new String[]{"Commits.html", "Commits", "commits"});
            list.add(new String[]{"Contributors.html", "Contributors", "contributors"});
        }
        if (showUnits) {
            list.add(new String[]{"UnitSize.html", "Unit Size*", "unit_size"});
            list.add(new String[]{"ConditionalComplexity.html", "Conditional Complexity*", "conditional"});
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
        if (!showDependencies) {
            list.add(new String[]{"ComponentsAndDependencies.html", "Component Dependencies*", "dependencies"});
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
