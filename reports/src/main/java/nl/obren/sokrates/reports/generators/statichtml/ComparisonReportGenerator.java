package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.ReferenceAnalysisResult;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ComparisonReportGenerator {
    private RichTextReport report;
    private File codeConfigurationFile;

    public ComparisonReportGenerator(File codeConfigurationFile) {
        this.codeConfigurationFile = codeConfigurationFile;
    }

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport report) {
        this.report = report;

        List<ReferenceAnalysisResult> referenceResults = codeAnalysisResults.getCodeConfiguration().getCompareResultsWith();

        if (referenceResults.size() == 0) {
            report.addParagraph("No reference analysis results have been defined.");
            return report;
        }

        referenceResults.forEach(result -> {
            processReferenceResults(codeAnalysisResults, report, result);
        });

        return report;
    }

    private void processReferenceResults(CodeAnalysisResults codeAnalysisResults, RichTextReport report, ReferenceAnalysisResult result) {
        String json = null;
        try {
            String analysisResultsPath = result.getAnalysisResultsPath();
            File file = new File(codeConfigurationFile, analysisResultsPath);
            if (!file.exists()) {
                file = new File(codeConfigurationFile.getParentFile(), analysisResultsPath);
            }
            if (file.exists()) {
                report.startSection(result.getLabel(), result.getAnalysisResultsPath());
                json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                CodeAnalysisResults refData = (CodeAnalysisResults) new JsonMapper().getObject(json, CodeAnalysisResults.class);

                sumarize(report, codeAnalysisResults, refData);

                report.startTable();
                report.addTableHeader("Metric", "Current Value", "Difference", "Reference Value");
                codeAnalysisResults.getMetricsList().getMetrics().forEach(metric -> {
                    addRow(metric, refData);
                });
                report.endTable();
            } else {
                report.addParagraph("ERROR: could not find the reference result file '" + analysisResultsPath + "'.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sumarize(RichTextReport report, CodeAnalysisResults codeAnalysisResults, CodeAnalysisResults refData) {
        StringBuilder summary = new StringBuilder("");
        summary.append("<p>Main Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode()) + " LOC");
        summarizeListOfLocAspects(summary, codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCodePerExtension());
        summary.append("</p>");

        summary.append("<p style='color: lightgrey'>Main Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(refData.getMainAspectAnalysisResults().getLinesOfCode()) + " LOC");
        summarizeListOfLocAspects(summary, refData.getMainAspectAnalysisResults().getLinesOfCodePerExtension());
        summary.append("</p>");

        summary.append("<p>Test Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(codeAnalysisResults.getTestAspectAnalysisResults().getLinesOfCode()) + " LOC");
        summarizeListOfLocAspects(summary, codeAnalysisResults.getTestAspectAnalysisResults().getLinesOfCodePerExtension());

        summary.append("</p>");

        summary.append("<p style='color: lightgrey'>Test Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(refData.getTestAspectAnalysisResults().getLinesOfCode()) + " LOC");
        summarizeListOfLocAspects(summary, refData.getTestAspectAnalysisResults().getLinesOfCodePerExtension());

        summary.append("</p>");

        report.addParagraph(summary.toString());
    }

    private void summarizeListOfLocAspects(StringBuilder summary, List<NumericMetric> linesOfCodePerAspect) {
        if (linesOfCodePerAspect.size() > 0) {
            summary.append(" = ");
        }
        final boolean[] first = {true};
        linesOfCodePerAspect.forEach(ext -> {
            if (!first[0]) {
                summary.append(" + ");
            } else {
                first[0] = false;
            }
            summary.append(
                    ext.getName().toUpperCase().replace("*.", "") + "</b> ("
                            + RichTextRenderingUtils.renderNumber(ext.getValue().intValue())
                            + " LOC)");
        });
    }


    private void addRow(Metric metric, CodeAnalysisResults refData) {
        report.startTableRow();
        report.startTableCell();
        if (metric.getScopeQualifier() != null) {
            report.addHtmlContent("<i>" + metric.getScopeQualifier() + "</i><br/>");
        }
        report.addHtmlContent("<b>" + metric.getId() + "</b><br/>");
        report.addHtmlContent("<i>" + metric.getDescription() + "</i><br/>");
        report.endTableCell();
        report.startTableCell("text-align: right");
        report.addHtmlContent("<b>" + metric.getValue() + "</b>");
        report.endTableCell();

        Metric refMetric = refData.getMetricsList().getMetricById(metric.getId());
        if (refMetric != null) {
            int diff = metric.getValue().intValue() - refMetric.getValue().intValue();
            report.startTableCell("text-align: center;" + (diff == 0 ? "color: lightgrey" : ""));
            report.addHtmlContent("" + (diff > 0 ? "+" : "") + diff);
            report.endTableCell();
            report.startTableCell("text-align: left; color: lightgrey");
            report.addHtmlContent("" + refMetric.getValue());
            report.endTableCell();
        } else {
            report.startTableCell();
            report.endTableCell();
            report.startTableCell();
            report.endTableCell();
        }
        report.endTableRow();
    }

}
