/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.charts.SimpleOneBarChart;
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
import java.util.Optional;
import java.util.stream.Collectors;

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

        addLanguages();
    }

    private void addBigSummary(LandscapeAnalysisResults landscapeAnalysisResults) {
        List<NumericMetric> linesOfCodePerExtension = landscapeAnalysisResults.getLinesOfCodePerExtension();

        landscapeReport.startDiv("margin-top: 32px;");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getProjectsCount()), "projects");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainLoc()), "lines of code");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(linesOfCodePerExtension.size()), linesOfCodePerExtension.size() == 1 ? " language" : " languages");
        //addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainFileCount()), "files");
        landscapeReport.endDiv();

    }

    private void addLanguages() {
        List<NumericMetric> linesOfCodePerExtension = landscapeAnalysisResults.getLinesOfCodePerExtension();
        landscapeReport.startSubSection("Languages", "");
        landscapeReport.startDiv("");
        landscapeReport.addContentInDiv(getVolumeVisual(linesOfCodePerExtension));
        landscapeReport.addHtmlContent("( ");
        landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_languages.html");
        landscapeReport.addHtmlContent(" | ");
        landscapeReport.addNewTabLink("tree map", "visuals/tree_map_languages.html");
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
        landscapeReport.startSubSection("Projects", "");

        if (projectsAnalysisResults.size() > 0) {
            List<NumericMetric> projectSizes = new ArrayList<>();
            projectsAnalysisResults.forEach(projectAnalysisResults -> {
                CodeAnalysisResults analysisResults = projectAnalysisResults.getAnalysisResults();
                projectSizes.add(new NumericMetric(analysisResults.getCodeConfiguration().getMetadata().getName(), analysisResults.getMainAspectAnalysisResults().getLinesOfCode()));
            });
            landscapeReport.addContentInDiv(getVolumeVisual(projectSizes));
            landscapeReport.addHtmlContent("( ");
            landscapeReport.addNewTabLink("bubble chart", "visuals/bubble_chart_projects.html");
            landscapeReport.addHtmlContent(" | ");
            landscapeReport.addNewTabLink("tree map", "visuals/tree_map_projects.html");
            landscapeReport.addHtmlContent(" )");
            landscapeReport.addLineBreak();
            landscapeReport.addLineBreak();
            landscapeReport.startTable("width: 100%");
            landscapeReport.addTableHeader("", "Project", "Lines of code", "Languages", "Duplication", "Report");
            projectsAnalysisResults.forEach(projectAnalysis -> {
                addProjectRow(projectAnalysis);
            });
            landscapeReport.endTable();
        }

        landscapeReport.endSection();
    }

    private void addProjectRow(ProjectAnalysisResults projectAnalysis) {
        CodeAnalysisResults analysisResults = projectAnalysis.getAnalysisResults();
        Metadata metadata = analysisResults.getCodeConfiguration().getMetadata();
        String logoLink = metadata.getLogoLink();

        landscapeReport.startTableRow();
        landscapeReport.addTableCell(StringUtils.isNotBlank(logoLink) ? "<img src='" + logoLink + "' style='width: 20px'>" : "");
        landscapeReport.addTableCell(metadata.getName());
        AspectAnalysisResults main = analysisResults.getMainAspectAnalysisResults();

        DuplicationAnalysisResults duplication = analysisResults.getDuplicationAnalysisResults();
        landscapeReport.addTableCell(FormattingUtils.getFormattedCount(main.getLinesOfCode()), "text-align: right");

        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        StringBuilder locSummary = new StringBuilder();
        new SummaryUtils().summarizeListOfLocAspects(locSummary, main.getLinesOfCode(), linesOfCodePerExtension);
        landscapeReport.addTableCell(locSummary.toString().replace("> = ", ">"));

        landscapeReport.addTableCell(FormattingUtils.getFormattedPercentage(duplication.getOverallDuplication().getDuplicationPercentage().doubleValue()) + "%", "text-align: right");
        String projectReportUrl = landscapeAnalysisResults.getConfiguration().getProjectReportsUrlPrefix() + projectAnalysis.getSokratesProjectLink().getHtmlReportsRoot() + "/index.html";
        landscapeReport.addTableCell("<a href='" + projectReportUrl + "' target='_blank'>...</a>", "text-align: center");
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

    private String getVolumeVisual(List<NumericMetric> linesOfCodePerExtension) {
        int total[] = {0};
        linesOfCodePerExtension.forEach(extension -> total[0] += extension.getValue().intValue());

        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(400);
        chart.setBarHeight(12);
        chart.setMaxBarWidth(400);
        chart.setBarStartXOffset(0);
        chart.setFontSize("small");


        List<Integer> values = linesOfCodePerExtension.stream().map(metric -> metric.getValue().intValue()).collect(Collectors.toList());
        Collections.sort(values);
        Collections.reverse(values);
        return chart.getStackedBarSvg(values, Palette.getDefaultPalette(), "", "");
    }


}
