package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.Metric;

import java.util.List;

public class MetricsListReportGenerator {
    private RichTextReport report;

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport report) {
        this.report = report;

        List<Metric> metrics = codeAnalysisResults.getMetricsList().getMetrics();

        report.startTable();
        report.addTableHeader("Metric", "Value");

        metrics.forEach(metric -> {
            addRow(metric);
        });

        report.endTable();

        return report;
    }

    private void addRow(Metric metric) {
        report.startTableRow();
        report.startTableCell();
        report.addHtmlContent("<b>" + metric.getId() + "</b><br/>");
        report.addHtmlContent("<i>" + metric.getDescription() + "</i><br/>");
        report.endTableCell();
        report.startTableCell("text-align: left");
        report.addHtmlContent("" + metric.getValue());
        report.endTableCell();
        report.endTableRow();
    }
}
