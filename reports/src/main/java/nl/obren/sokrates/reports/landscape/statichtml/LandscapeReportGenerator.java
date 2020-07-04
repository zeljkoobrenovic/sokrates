/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.core.SummaryUtils;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LandscapeReportGenerator {
    private static final Log LOG = LogFactory.getLog(LandscapeReportGenerator.class);

    private RichTextReport landscapeReport = new RichTextReport("Landscape Report", "index.html");
    private LandscapeAnalysisResults landscapeAnalysisResults;

    public LandscapeReportGenerator(LandscapeAnalysisResults landscapeAnalysisResults) {
        String landscapeName = landscapeAnalysisResults.getConfiguration().getMetadata().getName();
        if (StringUtils.isNotBlank(landscapeName)) {
            landscapeReport.setDisplayName(landscapeName);
        }
        this.landscapeAnalysisResults = landscapeAnalysisResults;

        addBigSummary(landscapeAnalysisResults);

        addProjectsSection(landscapeAnalysisResults.getProjectAnalysisResults());

        addExtensions();
    }

    private void addBigSummary(LandscapeAnalysisResults landscapeAnalysisResults) {
        List<NumericMetric> linesOfCodePerExtension = landscapeAnalysisResults.getLinesOfCodePerExtension();

        landscapeReport.startDiv("margin-top: 32px;");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getProjectsCount()), "projects");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(linesOfCodePerExtension.size()), linesOfCodePerExtension.size() == 1 ? " extension" : " extensions");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainLoc()), "Lines of code (main)");
        landscapeReport.addLineBreak();
        addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getTestLoc()), "(test)");
        addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getGeneratedLoc()), "(generated)");
        addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getBuildAndDeploymentLoc()), "(build)");
        addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getOtherLoc()), "(other)");
        //addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainFileCount()), "files");
        landscapeReport.endDiv();
        landscapeReport.addLineBreak();

    }

    private void addExtensions() {
        List<NumericMetric> linesOfCodePerExtension = landscapeAnalysisResults.getLinesOfCodePerExtension();
        landscapeReport.startSubSection("Extensions (" + linesOfCodePerExtension.size() + ")", "");
        landscapeReport.startDiv("");
        landscapeReport.addHtmlContent("( ");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_extensions.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_extensions.html");
        landscapeReport.addHtmlContent(" )");
        landscapeReport.addLineBreak();
        landscapeReport.addLineBreak();
        landscapeReport.endDiv();
        landscapeReport.startDiv("");
        linesOfCodePerExtension.forEach(extension -> {
            addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(extension.getValue().intValue()), extension.getName().replace("*.", ""));
        });
        landscapeReport.endDiv();
        landscapeReport.endSection();
    }

    private void addProjectsSection(List<ProjectAnalysisResults> projectsAnalysisResults) {
        Collections.sort(projectsAnalysisResults, (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
        landscapeReport.startSubSection("Projects (" + projectsAnalysisResults.size() + ")", "");

        if (projectsAnalysisResults.size() > 0) {
            List<NumericMetric> projectSizes = new ArrayList<>();
            projectsAnalysisResults.forEach(projectAnalysisResults -> {
                CodeAnalysisResults analysisResults = projectAnalysisResults.getAnalysisResults();
                projectSizes.add(new NumericMetric(analysisResults.getMetadata().getName(), analysisResults.getMainAspectAnalysisResults().getLinesOfCode()));
            });
            landscapeReport.addHtmlContent("( ");
            landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_projects.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("tree map", "visuals/tree_map_projects.html");
            landscapeReport.addHtmlContent(" )");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.startTable("width: 100%");
            landscapeReport.addTableHeader("", "Project", "Main<br/>Language", "LOC<br/>(main)", "Duplication", "LOC<br/>(test)", "LOC<br/>(generated)", "LOC<br/>(build)", "LOC<br/>(other)", "Report");
            projectsAnalysisResults.forEach(projectAnalysis -> {
                addProjectRow(projectAnalysis);
            });
            landscapeReport.endTable();
        }

        landscapeReport.endSection();
    }

    private void addProjectRow(ProjectAnalysisResults projectAnalysis) {
        CodeAnalysisResults analysisResults = projectAnalysis.getAnalysisResults();
        Metadata metadata = analysisResults.getMetadata();
        String logoLink = metadata.getLogoLink();

        landscapeReport.startTableRow();
        landscapeReport.addTableCell(StringUtils.isNotBlank(logoLink) ? "<img src='" + logoLink + "' style='width: 20px'>" : "", "text-align: center");
        landscapeReport.addTableCell(metadata.getName());
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysisResults.getTestAspectAnalysisResults();
        AspectAnalysisResults generated = analysisResults.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults build = analysisResults.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults other = analysisResults.getOtherAspectAnalysisResults();

        DuplicationAnalysisResults duplication = analysisResults.getDuplicationAnalysisResults();

        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        StringBuilder locSummary = new StringBuilder();
        if (linesOfCodePerExtension.size() > 0) {
            locSummary.append(linesOfCodePerExtension.get(0).getName().replace("*.", "").trim().toUpperCase());
        }
        landscapeReport.addTableCell(locSummary.toString().replace("> = ", ">"), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(main.getLinesOfCode()), "text-align: center");

        landscapeReport.addTableCell(FormattingUtils.getFormattedPercentage(duplication.getOverallDuplication().getDuplicationPercentage().doubleValue()) + "%", "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(test.getLinesOfCode()), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(generated.getLinesOfCode()), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(build.getLinesOfCode()), "text-align: center");
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(other.getLinesOfCode()), "text-align: center");
        String projectReportUrl = landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/index.html";
        landscapeReport.addTableCell("<a href='" + projectReportUrl + "' target='_blank'>"
                + "<div style='height: 40px'>" + ReportFileExporter.getIconSvg("report", 40) + "</div></a>", "text-align: center");
        landscapeReport.endTableRow();
    }

    private void addInfoBlock(String mainValue, String subtitle) {
        String style = "border-radius: 12px;";

        style += "margin: 12px 12px 12px 0px;";
        style += "display: inline-block; width: 160px; height: 120px;";
        style += "background-color: skyblue; text-align: center; vertical-align: middle; margin-bottom: 36px;";

        landscapeReport.startDiv(style);
        landscapeReport.addHtmlContent("<div style='font-size: 50px; margin-top: 20px'>" + mainValue + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 16px'>" + subtitle + "</div>");
        landscapeReport.endDiv();
    }

    private void addSmallInfoBlock(String mainValue, String subtitle) {
        String style = "border-radius: 8px;";

        style += "margin: 4px 4px 4px 0px;";
        style += "display: inline-block; width: 80px; height: 64px;";
        style += "background-color: lightgrey; text-align: center; vertical-align: middle; margin-bottom: 16px;";

        landscapeReport.startDiv(style);
        landscapeReport.addHtmlContent("<div style='font-size: 26px; margin-top: 8px'>" + mainValue + "</div>");
        landscapeReport.addHtmlContent("<div style='color: #434343; font-size: 14px'>" + subtitle + "</div>");
        landscapeReport.endDiv();
    }

    public List<RichTextReport> report() {
        List<RichTextReport> reports = new ArrayList<>();

        reports.add(this.landscapeReport);

        return reports;
    }
}
