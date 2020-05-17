/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landspace.statichtml;

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
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeGroupAnalysisResults;
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

        landscapeAnalysisResults.getGroupsAnalysisResults().forEach(groupAnalysisResults -> {
            addGroup(groupAnalysisResults);
        });

        addDetailedNumbers(landscapeAnalysisResults);
    }

    private void addDetailedNumbers(LandscapeAnalysisResults landscapeAnalysisResults) {
        landscapeReport.startSection("Details", "All the details");
        landscapeReport.startUnorderedList();
        landscapeReport.addListItem(landscapeAnalysisResults.getProjectsCount() + " projects");
        landscapeReport.addListItem(landscapeAnalysisResults.getMainLoc() + " lines of code");
        landscapeReport.addListItem(landscapeAnalysisResults.getMainFileCount() + " main files");
        landscapeReport.endUnorderedList();
        landscapeReport.endSection();
    }

    private void addBigSummary(LandscapeAnalysisResults landscapeAnalysisResults) {
        landscapeReport.startDiv("margin-top: 32px;");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getProjectsCount()), "projects");
        addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainLoc()), "lines of code");
        //addInfoBlock(FormattingUtils.getSmallTextForNumber(landscapeAnalysisResults.getMainFileCount()), "files");
        landscapeReport.endDiv();

        List<NumericMetric> linesOfCodePerExtension = getLinesOfCodePerExtension(landscapeAnalysisResults.getAllProjects());
        landscapeReport.addLevel2Header(linesOfCodePerExtension.size() + (linesOfCodePerExtension.size() == 1 ? " Language" : " Languages"));
        System.out.println(linesOfCodePerExtension.size());
        landscapeReport.startDiv("");
        landscapeReport.addHtmlContent(getVolumeVisual(linesOfCodePerExtension));
        landscapeReport.endDiv();
        landscapeReport.startDiv("margin-bottom: 36px");
        linesOfCodePerExtension.forEach(extension -> {
            addSmallInfoBlock(FormattingUtils.getSmallTextForNumber(extension.getValue().intValue()), extension.getName());
        });
        landscapeReport.endDiv();
    }

    private void addGroup(LandscapeGroupAnalysisResults groupAnalysisResults) {
        List<ProjectAnalysisResults> projectsAnalysisResults = groupAnalysisResults.getProjectsAnalysisResults();
        Collections.sort(projectsAnalysisResults, (a, b) -> b.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode() - a.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode());
        int projectsCount = projectsAnalysisResults.size();
        String countText = projectsCount + (projectsCount == 1 ? " project" : " projects");
        int subGroupsCount = groupAnalysisResults.getSubGroupsAnalysisResults().size();
        if (subGroupsCount > 0) {
            countText += ", " + subGroupsCount + (subGroupsCount == 1 ? " sub-group" : " sub-groups");
        }
        landscapeReport.startSubSection(groupAnalysisResults.getGroup().getMetadata().getName(),
                "Group details (" + countText + ")");

        if (projectsAnalysisResults.size() > 0) {
            landscapeReport.addLevel2Header("Projects");
            List<NumericMetric> projectSizes = new ArrayList<>();
            projectsAnalysisResults.forEach(projectAnalysisResults -> {
                CodeAnalysisResults analysisResults = projectAnalysisResults.getAnalysisResults();
                projectSizes.add(new NumericMetric(analysisResults.getCodeConfiguration().getMetadata().getName(), analysisResults.getMainAspectAnalysisResults().getLinesOfCode()));
            });
            landscapeReport.addContentInDiv(getVolumeVisual(projectSizes));
            landscapeReport.startTable();
            landscapeReport.addTableHeader("", "Project", "Lines of code", "Languages", "Duplication", "Report");
            projectsAnalysisResults.forEach(projectAnalysis -> {
                addProjectRow(projectAnalysis);
            });
            landscapeReport.endTable();
        }

        groupAnalysisResults.getSubGroupsAnalysisResults().forEach(subGroupAnalysisResults -> {
            landscapeReport.addLineBreak();
            addGroup(subGroupAnalysisResults);
        });

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

    public List<NumericMetric> getLinesOfCodePerExtension(List<ProjectAnalysisResults> projects) {
        List<NumericMetric> linesOfCodePerExtension = new ArrayList<>();
        projects.forEach(projectAnalysisResults -> {
            AspectAnalysisResults main = projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults();
            List<NumericMetric> projectLinesOfCodePerExtension = main.getLinesOfCodePerExtension();
            projectLinesOfCodePerExtension.forEach(metric -> {
                String id = metric.getName();
                Optional<NumericMetric> existingMetric = linesOfCodePerExtension.stream().filter(c -> c.getName().equalsIgnoreCase(id)).findAny();
                if (existingMetric.isPresent()) {
                    existingMetric.get().setValue(existingMetric.get().getValue().intValue() + metric.getValue().intValue());
                } else {
                    linesOfCodePerExtension.add(metric);
                }
            });
        });

        Collections.sort(linesOfCodePerExtension, (a, b) -> b.getValue().intValue() - a.getValue().intValue());
        return linesOfCodePerExtension;
    }

    private String getVolumeVisual(List<NumericMetric> linesOfCodePerExtension) {
        int total[] = {0};
        linesOfCodePerExtension.forEach(extension -> total[0] += extension.getValue().intValue());

        SimpleOneBarChart chart = new SimpleOneBarChart();
        chart.setWidth(400);
        chart.setBarHeight(40);
        chart.setMaxBarWidth(400);
        chart.setBarStartXOffset(0);
        chart.setFontSize("small");


        List<Integer> values = linesOfCodePerExtension.stream().map(metric -> metric.getValue().intValue()).collect(Collectors.toList());
        Collections.sort(values);
        Collections.reverse(values);
        return chart.getStackedBarSvg(values, Palette.getDefaultPalette(), "", "");
    }


}
