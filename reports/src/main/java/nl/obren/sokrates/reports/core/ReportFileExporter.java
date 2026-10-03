/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.generators.statichtml.CommitsReportGenerator;
import nl.obren.sokrates.reports.generators.statichtml.ContributorsReportUtils;
import nl.obren.sokrates.reports.generators.statichtml.HistoryPerLanguageGenerator;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.HtmlEscapeUtils;
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
import nl.obren.sokrates.sourcecode.core.CustomTab;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Predicate;
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
        RichTextReport indexReport = new RichTextReport(metadata.getName(), "", metadata.getLogoLink());
        if (StringUtils.isNotBlank(metadata.getDescription())) {
            indexReport.setDescription(metadata.getDescription());
        }
        appendLinks(indexReport, analysisResults);
        boolean hasLinks = metadata.getLinks().size() > 0;
        indexReport.addContentInDiv("", "height; 10px; margin-top: " + (hasLinks ? 6 : 0) + "px; margin-bottom: 6px;");

        List<CustomTab> customTabs = getCustomTabs(analysisResults);
        addTabStrip(indexReport, customTabs);
        addOverviewTab(indexReport, analysisResults);
        addAnalysesTab(indexReport, analysisResults, reportList, htmlExportFolder);
        addExplorerTabs(indexReport, customTabs);
        ReportActivityTab.addActivityTab(indexReport, analysisResults);
        addVisualsAndDataTabs(indexReport, analysisResults, htmlExportFolder);
        addFooter(indexReport);
        export(htmlExportFolder, indexReport, "index.html", analysisResults.getCodeConfiguration().getAnalysis().getCustomHtmlReportHeaderFragment());

        exportRootRedirect(reportsFolder);
    }

    private static void addTabStrip(RichTextReport indexReport, List<CustomTab> customTabs) {
        indexReport.startTabGroup();
        indexReport.addTab("overview", "Overview", true);
        indexReport.addTab("quality", "Analyses", false);
        indexReport.addTab("commits", "Activity", false);
        indexReport.addTab("files", "Files", false);
        indexReport.addTab("units", "Units*", false);
        // The plain "commits" id is taken by the Activity tab above.
        indexReport.addTab("commits-explorer", "Commits", false);
        indexReport.addTab("visuals", "Visuals", false);
        indexReport.addTab("data", "Data", false);
        for (int i = 0; i < customTabs.size(); i++) {
            indexReport.addTabText(customTabId(i), customTabs.get(i).getLabel(), false);
        }
        indexReport.endDiv();
    }

    private static void addOverviewTab(RichTextReport indexReport, CodeAnalysisResults analysisResults) {
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
        // The per-language icons used to sit here (always "main", above the scope toggle); they now live
        // inside each scope panel of the activity table, showing that scope's languages.
        indexReport.startDiv("margin-left: 0px; margin-top: -33px; margin-bottom: 0px; padding-left: 0px; padding-bottom: 10px");
        indexReport.startDiv("");

        if (contributorsAnalysisResults.getCommitsCount() > 0) {
            ReportActivityTab.addSummaryActivityTable(analysisResults, indexReport);
        } else {
            // No git history: no activity table (and thus no per-scope panels) — still show the main
            // language icons so the Overview isn't missing them.
            addScopeLanguageIcons(indexReport, analysisResults, "main");
        }

        indexReport.endDiv();
        indexReport.endDiv();

        indexReport.addHtmlContent("<iframe src='Structure.html' style='border: none; width: 1000px; height: 1090px; overflow: hidden'></iframe>");
        indexReport.endTabContentSection();
    }

    private static void addAnalysesTab(RichTextReport indexReport, CodeAnalysisResults analysisResults, List<String[]> reportList, File htmlExportFolder) {
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
    }

    private static void addExplorerTabs(RichTextReport indexReport, List<CustomTab> customTabs) {
        indexReport.startTabContentSection("files", false);
        indexReport.addLineBreak();
        indexReport.addHtmlContent("<iframe src='../explorers/files-explorer.html' style='width: 100%; border: none; height: calc(100vh - 220px); overflow: hidden; margin-top: -12px'></iframe>");

        indexReport.endTabContentSection();

        indexReport.startTabContentSection("units", false);
        indexReport.addLineBreak();
        indexReport.addHtmlContent("<iframe src='../explorers/units-explorer.html' style='width: 100%; border: none; height: calc(100vh - 220px); overflow: hidden; margin-top: -12px'></iframe>");

        indexReport.endTabContentSection();

        indexReport.startTabContentSection("commits-explorer", false);
        indexReport.addLineBreak();
        indexReport.addHtmlContent("<iframe src='../explorers/commits-explorer.html' style='width: 100%; border: none; height: calc(100vh - 220px); overflow: hidden; margin-top: -12px'></iframe>");

        indexReport.endTabContentSection();

        for (int i = 0; i < customTabs.size(); i++) {
            indexReport.startTabContentSection(customTabId(i), false);
            indexReport.addLineBreak();
            indexReport.addHtmlContent(customTabIframe(customTabs.get(i)));
            indexReport.endTabContentSection();
        }
    }

    private static void addVisualsAndDataTabs(RichTextReport indexReport, CodeAnalysisResults analysisResults, File htmlExportFolder) {
        indexReport.startTabContentSection("visuals", false);
        indexReport.startDiv("margin: 24px");
        ReportVisualsTab.addVisuals(indexReport, analysisResults, htmlExportFolder);
        indexReport.endDiv();
        indexReport.endTabContentSection();

        indexReport.startTabContentSection("data", false);
        indexReport.startDiv("margin: 24px");
        ReportDataTab.addData(indexReport, analysisResults);
        indexReport.endDiv();
        addPrompts(indexReport, analysisResults);
        indexReport.endTabContentSection();
    }

    private static void addFooter(RichTextReport indexReport) {
        String dateOfUpdate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String referenceDate = new SimpleDateFormat("yyyy-MM-dd").format(DateUtils.getCalendar().getTime());
        indexReport.addParagraph("generated by <a target='_blank' href='https://sokrates.dev/'>sokrates.dev</a> " +
                        " (<a href='#' onclick=\"return downloadDataFile('config.json')\" target='_blank'>configuration</a>)" +
                        " on " + dateOfUpdate + (!referenceDate.equals(dateOfUpdate) ? "; reference date: " + referenceDate : ""),
                "color: grey; font-size: 80%; margin-left: 10px; margin-bottom: 30px");
    }

    // Writes a minimal index.html at the reports root that redirects to html/index.html,
    // so opening the reports folder lands on the main report.
    private static void exportRootRedirect(File reportsFolder) {
        String target = htmlReportsSubFolder + "/index.html";
        File redirectFile = new File(reportsFolder, "index.html");
        try {
            PrintWriter out = new PrintWriter(redirectFile);
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("<meta charset=\"UTF-8\">");
            out.println("<meta http-equiv=\"refresh\" content=\"0; url=" + target + "\">");
            out.println("<title>Sokrates report</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("Redirecting to <a href=\"" + target + "\">" + target + "</a>...");
            out.println("</body>");
            out.println("</html>");
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void addPrompts(RichTextReport report, CodeAnalysisResults analysisResults) {
        report.addLineBreak();
        report.startDiv("margin: 20px");
        report.addLevel2Header("AI Prompts", "");
        report.addParagraph("Generative AI tools, like ChatGPT or Gemini, can help you explore and discuss various aspects of source code repositories using simple prompts and file uploads. Sokrates provides you with curated data that you can use to analyze your source code further.", "color: grey; font-size: 90%; margin-top: 0");

        report.startDiv("margin: 6px");
        PromptsUtils.addRepositoryPromptSection("git-history-analyzer", report, analysisResults, "Example Prompt 1: Repository Evolution Analyzer (based on git history)", "", Arrays.asList(new Link[]{new Link("git-history.zip", "../data/zips/git-history.zip")}));

        PromptsUtils.addRepositoryPromptSection("path-name-conventions-analyzer", report, analysisResults, "Example Prompt 2: File name conventions", "", Arrays.asList(new Link("files.json", "../data/files.json")));

        PromptsUtils.addRepositoryPromptSection("technology-analyzer", report, analysisResults, "Example Prompt 3: Technology analyzer (based of file paths)", "", Arrays.asList(new Link("files.json", "../data/files.json")));

        report.endDiv();

        report.endDiv();
    }

    // Renders the language icons for a scope inside its activity panel (replacing the old single
    // always-main icon strip above the toggle). No-op when the scope has no extensions. The unscoped tab
    // shows "-" for the number (its files aren't analyzed, so there is no lines-of-code).
    static void addScopeLanguageIcons(RichTextReport indexReport, CodeAnalysisResults analysisResults, String scope) {
        List<NumericMetric> extensions = extensionsForScope(analysisResults, scope);
        if (extensions == null || extensions.isEmpty()) {
            return;
        }
        boolean showDash = "unscoped".equals(scope);
        StringBuilder icons = new StringBuilder("");
        addIconsForExtensions(extensions, icons, showDash);
        indexReport.addHtmlContent(icons.toString());
    }

    static void addInfoBlockWithColor(RichTextReport report, String mainValue, String subtitle, String extra, String color, String tooltip, String icon, String link) {
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

    // Renders the per-extension language icons for a given extension list. The number line shows the
    // extension's lines of code, unless showDash is true (the unscoped tab, whose files aren't analyzed
    // and have no LOC) — then it shows "-". First/biggest gets a larger icon. No-op (empty div) when
    // there are no extensions.
    private static void addIconsForExtensions(List<NumericMetric> extensions, StringBuilder summary, boolean showDash) {
        summary.append("<div style='margin-bottom: 20px; white-space: nowrap; overflow: hidden;'>");
        boolean first[] = {true};
        extensions.stream().limit(16).forEach(ext -> {
            String lang = ext.getName().toUpperCase().replace("*.", "").trim();
            int value = ext.getValue().intValue();
            int fontSize = 20;
            int width = (first[0] ? value >= 1000 ? 64 : 65 : value >= 1000 ? 42 : 43);
            String numberLine = showDash ? "-" : FormattingUtils.getSmallTextForNumberMinK(value);
            summary.append("<div style='width: " + width + "px; text-align: center; display: inline-block; border-radius: 5px; background-color: white; padding: 8px; margin-right: 4px;'>"
                    + (first[0] ? DataImageUtils.getLangDataImageDiv64(lang) : DataImageUtils.getLangDataImageDiv42(lang))
                    + "<div style='margin-top: 3px; font-size: " + fontSize + "px'>" + numberLine + "</div>"
                    + "<div style='font-size: 10px; white-space: no-wrap; overflow: hidden; color: grey;'>" + HtmlEscapeUtils.escape(lang.toLowerCase()) + "</div>"
                    + "</div>");
            first[0] = false;
        });
        summary.append("</div>");
    }

    // The per-extension list backing a scope's language icons:
    //  - main/test/build/generated/other: that aspect's lines-of-code per extension.
    //  - "All": the union of ALL analyzed scopes' LOC per extension (unscoped excluded — those files
    //    aren't analyzed, so they carry no LOC and would distort the union).
    //  - "unscoped": the residual git-history extensions with distinct file counts (rendered with "-"
    //    for the number, since these files are never analyzed).
    private static List<NumericMetric> extensionsForScope(CodeAnalysisResults analysisResults, String scope) {
        switch (scope) {
            case "main":
                return analysisResults.getMainAspectAnalysisResults().getLinesOfCodePerExtension();
            case "test":
                return analysisResults.getTestAspectAnalysisResults().getLinesOfCodePerExtension();
            case "build":
                return analysisResults.getBuildAndDeployAspectAnalysisResults().getLinesOfCodePerExtension();
            case "generated":
                return analysisResults.getGeneratedAspectAnalysisResults().getLinesOfCodePerExtension();
            case "other":
                return analysisResults.getOtherAspectAnalysisResults().getLinesOfCodePerExtension();
            case "unscoped":
                return analysisResults.getContributorsAnalysisResults().getUnscopedExtensionFileCounts();
            default: // "All": union of analyzed scopes, by LOC desc
                return unionAnalyzedExtensions(analysisResults);
        }
    }

    // Merges the per-extension LOC across all analyzed scopes (main/test/build/generated/other) into a
    // single extension->total-LOC list, ordered by LOC descending. Backs the "All" tab's language icons.
    private static List<NumericMetric> unionAnalyzedExtensions(CodeAnalysisResults analysisResults) {
        Map<String, Integer> locByExtension = new LinkedHashMap<>();
        for (String scope : new String[]{"main", "test", "build", "generated", "other"}) {
            for (NumericMetric ext : extensionsForScope(analysisResults, scope)) {
                locByExtension.merge(ext.getName(), ext.getValue().intValue(), Integer::sum);
            }
        }
        List<NumericMetric> result = new ArrayList<>();
        locByExtension.forEach((name, loc) -> result.add(new NumericMetric(name, loc)));
        result.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
        return result;
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
            report.startDiv("font-size: 70%; margin-top: 0px; margin-bottom: 14px; margin-top: -2px; margin-left: 0;");
            links.forEach(link -> {
                if (links.indexOf(link) > 0) {
                    report.addHtmlContent(" | ");
                }
                report.startDiv("display: inline-block; padding: 4px 6px; border-radius: 999px; background-color: #f4f4f4;");
                report.addNewTabLink(link.getLabel() + "&nbsp;" + OPEN_IN_NEW_TAB_SVG_ICON_EXTRA_SMALL, link.getHref());
                report.endDiv();
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

    static List<CustomTab> getCustomTabs(CodeAnalysisResults analysisResults) {
        List<CustomTab> tabs = new ArrayList<>();
        CodeConfiguration configuration = analysisResults.getCodeConfiguration();
        if (configuration != null && configuration.getCustomTabs() != null) {
            configuration.getCustomTabs().stream().filter(tab -> tab != null && tab.isValid()).forEach(tabs::add);
        }
        return tabs;
    }

    static String customTabId(int index) {
        // Ids must not clash with the built-in tab ids (overview, quality, commits, files, ...)
        return "custom-tab-" + (index + 1);
    }

    static String customTabIframe(CustomTab tab) {
        return "<iframe src='" + HtmlEscapeUtils.escape(tab.getIframeLink().trim()) + "' style='width: 100%; border: none; height: calc(100vh - 220px); overflow: hidden; margin-top: -12px'></iframe>";
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

    /** What the overview can link to: the flags the report list depends on. */
    private static final class ReportAvailability {
        final boolean mainExists, history, duplication, dependencies, concerns, controls, units, findings;

        ReportAvailability(CodeAnalysisResults analysisResults, File sokratesConfigFolder) {
            CodeConfiguration config = analysisResults.getCodeConfiguration();
            mainExists = analysisResults.getMainAspectAnalysisResults().getFilesCount() > 0;
            history = mainExists && config.getFileHistoryAnalysis().filesHistoryImportPathExists(sokratesConfigFolder);
            duplication = mainExists && !analysisResults.skipDuplicationAnalysis();
            dependencies = mainExists && !config.getAnalysis().isSkipDependencies();
            concerns = mainExists && config.countAllConcernsDefinitions() > 1;
            controls = mainExists && config.getGoalsAndControls().size() > 0;
            units = mainExists && analysisResults.getUnitsAnalysisResults().getTotalNumberOfUnits() > 0;
            File findingsFile = CodeConfigurationUtils.getDefaultSokratesFindingsFile(sokratesConfigFolder);
            findings = findingsFile.exists() && FileUtils.sizeOf(findingsFile) > 10;
        }
    }

    /** A report link: file (blank = not available, shown greyed), label, icon, and when it is listed. */
    private static final class ReportEntry {
        final String file, label, icon;
        final Predicate<ReportAvailability> when;

        ReportEntry(String file, String label, String icon, Predicate<ReportAvailability> when) {
            this.file = file;
            this.label = label;
            this.icon = icon;
            this.when = when;
        }
    }

    // The available reports, in display order, followed by the greyed placeholders of what is not available.
    private static final List<ReportEntry> REPORT_ENTRIES = Arrays.asList(
            new ReportEntry("SourceCodeOverview.html", "Source Code Overview", "codebase", a -> true),
            new ReportEntry("Components.html", "Components", "code_organization", a -> a.mainExists),
            new ReportEntry("ComponentsAndDependencies.html", "Component Dependencies*", "dependencies", a -> a.mainExists && a.dependencies),
            new ReportEntry("FileTemporalDependencies.html", "Temporal Dependencies", "temporal_dependency", a -> a.mainExists && a.history),
            new ReportEntry("Duplication.html", "Duplication", "duplication", a -> a.duplication),
            new ReportEntry("FileSize.html", "File Size", "file_size", a -> a.mainExists),
            new ReportEntry("FileAge.html", "File Age & Freshness", "file_history", a -> a.history),
            new ReportEntry("FileChurn.html", "File Churn", "change", a -> a.history),
            new ReportEntry("Commits.html", "Commits", "commits", a -> a.history),
            new ReportEntry("Contributors.html", "Contributors", "contributors", a -> a.history),
            new ReportEntry("UnitSize.html", "Unit Size*", "unit_size", a -> a.units),
            new ReportEntry("ConditionalComplexity.html", "Conditional Complexity*", "conditional", a -> a.units),
            new ReportEntry("FeaturesOfInterest.html", "Features of Interest", "cross_cutting_concerns", a -> a.concerns),
            new ReportEntry("Metrics.html", "All Metrics", "metrics", a -> true),
            new ReportEntry("Controls.html", "Goals & Controls", "goal", a -> a.controls),
            new ReportEntry("Notes.html", "Notes & Findings", "notes", a -> a.findings),
            new ReportEntry("", "Components and Dependencies", "dependencies", a -> !a.mainExists && a.dependencies),
            new ReportEntry("", "Components", "dependencies", a -> !a.mainExists && !a.dependencies),
            new ReportEntry("", "Duplication", "duplication", a -> !a.duplication),
            new ReportEntry("", "File Size", "file_size", a -> !a.mainExists),
            new ReportEntry("", "File Age & Freshness", "file_history", a -> !a.history),
            new ReportEntry("", "File Churn", "change", a -> !a.history),
            new ReportEntry("", "Temporal Dependencies", "temporal_dependency", a -> !a.history),
            new ReportEntry("", "Contributors", "contributors", a -> !a.history),
            new ReportEntry("", "Unit Size", "unit_size", a -> !a.units),
            new ReportEntry("", "Conditional Complexity", "conditional", a -> !a.units),
            new ReportEntry("ComponentsAndDependencies.html", "Component Dependencies*", "dependencies", a -> !a.dependencies),
            new ReportEntry("", "Features of Interest", "cross_cutting_concerns", a -> !a.concerns),
            new ReportEntry("", "Goals & Controls", "goal", a -> !a.controls),
            new ReportEntry("", "Notes & Findings", "notes", a -> !a.findings));

    private static List<String[]> getReportsList(CodeAnalysisResults analysisResults, File sokratesConfigFolder) {
        ReportAvailability availability = new ReportAvailability(analysisResults, sokratesConfigFolder);
        List<String[]> list = new ArrayList<>();
        for (ReportEntry entry : REPORT_ENTRIES) {
            if (entry.when.test(availability)) {
                list.add(new String[]{entry.file, entry.label, entry.icon});
            }
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
