/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.ReportUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.Metric;

public class MetricsListReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private RichTextReport report;

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport report) {
        this.codeAnalysisResults = codeAnalysisResults;
        this.report = report;

        report.setDescription("All numeric values measured by Sokrates.");
        addMetricsTable();
        addAboutSection();

        return report;
    }

    private void addMetricsTable() {
        report.startSection("Metrics", "");
        report.startDiv("width: 100%; overflow-x: auto");
        report.startDataTable();
        report.addTableHeader("Metric", "Value");

        codeAnalysisResults.getMetricsList().getMetrics().forEach(metric -> {
            addRow(metric);
        });

        report.endTable();
        report.endDiv();
        report.endSection();
    }

    private void addAboutSection() {
        report.startSection("About This Analysis", "");
        report.startUnorderedList();
        report.addListItem("Metrics are all numeric values measured by Sokrates.");
        report.addListItem("You can use these metrics to define <a href='Controls.html'>goals and controls</a>.");
        report.endUnorderedList();
        report.endSection();
    }

    private void addRow(Metric metric) {
        report.startTableRow();

        report.startTableCell();
        report.addHtmlContent("<b>" + metric.getId() + "</b>");
        report.endTableCell();

        report.startTableCell("text-align: right");
        report.addHtmlContent("" + ReportUtils.formatNumber(metric.getValue()));
        report.endTableCell();

        report.endTableRow();
    }
}
