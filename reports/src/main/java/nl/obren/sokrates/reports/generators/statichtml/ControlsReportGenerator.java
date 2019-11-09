package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.ReportUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ControlStatus;

public class ControlsReportGenerator {
    private RichTextReport metricsReport;

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport metricsReport) {
        this.metricsReport = metricsReport;
        metricsReport.startTable();
        metricsReport.addTableHeader("", "Control", "Status", "Value");
        codeAnalysisResults.getControlResults().getControlStatuses().forEach(controlResult -> {
            addRow(controlResult);
        });
        metricsReport.endTable();

        return metricsReport;
    }

    private void addRow(ControlStatus controlStatus) {
        metricsReport.startTableRow();
        metricsReport.startTableCell();
        String status = controlStatus.getStatus();
        metricsReport.addHtmlContent(ReportUtils.getSvgCircle(getColor(status)));
        metricsReport.endTableCell();
        metricsReport.startTableCell();
        metricsReport.addHtmlContent("" + controlStatus.getControl().getDescription() + "");
        metricsReport.endTableCell();
        metricsReport.startTableCell();
        metricsReport.addHtmlContent(status);
        metricsReport.endTableCell();
        metricsReport.startTableCell();
        metricsReport.addHtmlContent("" + controlStatus.getMetric().getId() + "=<b>" + controlStatus.getMetric().getValue() + "</b>");
        metricsReport.endTableCell();
        metricsReport.endTableRow();
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
