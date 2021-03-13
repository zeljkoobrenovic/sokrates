/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.ReportUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ControlStatus;
import nl.obren.sokrates.sourcecode.analysis.results.GoalsAnalysisResults;

import java.util.List;

public class ControlsReportGenerator {
    private RichTextReport report;

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport metricsReport) {
        this.report = metricsReport;

        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("Controls enable you to set alarms for any of the <a href='Metrics.html'>Sokrates metrics</a>. An alarm is defined with a desired range and tolerance.");
        report.endUnorderedList();

        report.startShowMoreBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("For more insights in the value of trend analysis, Sokrates recommends reading the section \"Explicitly link metrics to goals\" in the article <a href='https://martinfowler.com/articles/useOfMetrics.html#ExplicitlyLinkMetricsToGoals' target='_blank'>An Appropriate Use of Metrics</a>, (MartinFowler.com), e.g.:");
        report.startUnorderedList();
        report.addListItem("<i>\"We would like our code to be less complex and easier to change. Therefore we should aim to write short methods (less than 15 lines) with a low conditional complexity (less than 20 is good). We should also aim to have a small handful of parameters (up to four) so that methods remain as focused as possible.\"</i>");
        report.endUnorderedList();
        report.endUnorderedList();
        report.endShowMoreBlock();
        report.endSection();

        List<GoalsAnalysisResults> goals = codeAnalysisResults.getControlResults().getGoalsAnalysisResults();
        if (goals.size() == 0) {
            report.addParagraph("No goals have been defined.");
            return report;
        }

        goals.forEach(goalsAnalysisResults -> {
            report.startSection(goalsAnalysisResults.getMetricsWithGoal().getGoal(),
                    goalsAnalysisResults.getMetricsWithGoal().getDescription());
            report.startDiv("width: 100%; overflow-x: auto");
            report.startTable();
            report.addTableHeader("", "Status", "Metric", "Desired Range<br/>[from - to] ±tolerance", "Current Value", "Description");
            goalsAnalysisResults.getControlStatuses().forEach(controlResult -> {
                addRow(controlResult);
            });
            report.endTable();
            report.endDiv();
            report.endSection();
        });

        return metricsReport;
    }

    private void addRow(ControlStatus controlStatus) {
        String status = controlStatus.getStatus();

        report.startTableRow();
        report.addTableCell(ReportUtils.getSvgCircle(getColor(status)));
        report.addTableCell(status);
        report.addTableCell(controlStatus.getMetric().getId(), "text-align: center");
        report.addTableCell("" + controlStatus.getControl().getDesiredRange().getTextDescription(), "text-align: center");
        Number value = controlStatus.getMetric().getValue();
        report.addTableCell("<b>" + ReportUtils.formatNumber(value) + "</b>", "text-align: center");
        report.addTableCell("" + controlStatus.getControl().getDescription() + "");
        report.endTableRow();
    }

    private String getColor(String status) {
        String upperCaseStatus = status.toUpperCase();
        return upperCaseStatus.equals("OK") ? "darkgreen"
                : upperCaseStatus.equals("FAILED") ? "crimson"
                : (upperCaseStatus.startsWith("IGNORE") ? "grey"
                : "orange");
    }
}
