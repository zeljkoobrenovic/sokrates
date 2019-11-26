package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.ReportUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ControlStatus;

public class ControlsReportGenerator {
    private RichTextReport report;

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport metricsReport) {
        this.report = metricsReport;

        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("Controls enable you to set alarms for any of the Sokrates metrics. An alarm is defines with a desired range and tolerance.");
        report.addListItem("For more insights in the value of trend analysis, Sokrates recommends reading the section \"Explicitly link metrics to goals\" in the article <a href='https://martinfowler.com/articles/useOfMetrics.html#ExplicitlyLinkMetricsToGoals' target='_blank'>An Appropriate Use of Metrics</a>, (MartinFowler.com), e.g.:");
        report.startUnorderedList();
        report.addListItem("<i>\"We would like our code to be less complex and easier to change. Therefore we should aim to write short methods (less than 15 lines) with a low cyclomatic complexity (less than 20 is good). We should also aim to have a small handful of parameters (up to four) so that methods remain as focused as possible.\"</i>");
        report.endUnorderedList();
        report.endUnorderedList();
        report.endSection();

        codeAnalysisResults.getControlResults().getGoalsAnalysisResults().forEach(goalsAnalysisResults -> {
            report.startSection(goalsAnalysisResults.getMetricsWithGoal().getGoal(), goalsAnalysisResults.getMetricsWithGoal().getDescription());
            report.startTable();
            report.addTableHeader("", "Control", "Status", "Metric", "Desired Range<br/>[from - to] ±tolerance", "Current Value");
            goalsAnalysisResults.getControlStatuses().forEach(controlResult -> {
                addRow(controlResult);
            });
            report.endTable();
            report.endSection();
        });

        return metricsReport;
    }

    private void addRow(ControlStatus controlStatus) {
        report.startTableRow();
        report.startTableCell();
        String status = controlStatus.getStatus();
        report.addHtmlContent(ReportUtils.getSvgCircle(getColor(status)));
        report.endTableCell();
        report.startTableCell();
        report.addHtmlContent("" + controlStatus.getControl().getDescription() + "");
        report.endTableCell();
        report.startTableCell();
        report.addHtmlContent(status);
        report.endTableCell();
        report.addTableCell(controlStatus.getMetric().getId(), "text-align: center");
        report.addTableCell("" + controlStatus.getControl().getDesiredRange().getTextDescription(), "text-align: center");
        report.addTableCell("<b>" + controlStatus.getMetric().getValue() + "</b>", "text-align: center");
        report.endTableRow();
    }

    private String getColor(String status) {
        String upperCaseStatus = status.toUpperCase();
        return upperCaseStatus.equals("OK")
                ? "darkgreen"
                : upperCaseStatus.equals("FAILED")
                ? "crimson"
                : "orange";
    }
}
