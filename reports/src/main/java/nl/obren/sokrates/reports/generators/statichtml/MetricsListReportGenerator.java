package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.Metric;

public class MetricsListReportGenerator {
    private RichTextReport metricsReport;

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport metricsReport) {
        this.metricsReport = metricsReport;
        metricsReport.startTable();
        metricsReport.addTableHeader("Metric", "Value");
        codeAnalysisResults.getMetricsList().getMetrics().forEach(metric -> {
            addRow(metric);
        });
        metricsReport.endTable();

        return metricsReport;
    }

    private void addRow(Metric metric) {
        metricsReport.startTableRow();
        metricsReport.startTableCell();
        metricsReport.addHtmlContent("<b>" + metric.getId() + "</b><br/>");
        metricsReport.addHtmlContent("<i>" + metric.getDescription() + "</i><br/>");
        metricsReport.endTableCell();
        metricsReport.startTableCell("text-align: left");
        metricsReport.addHtmlContent("" + metric.getValue());
        metricsReport.endTableCell();
        metricsReport.endTableRow();
    }
}
