package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.core.SummaryUtils;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.ReferenceAnalysisResult;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TrendReportGenerator {
    private RichTextReport report;
    private File codeConfigurationFile;

    public TrendReportGenerator(File codeConfigurationFile) {
        this.codeConfigurationFile = codeConfigurationFile;
    }

    public RichTextReport generateReport(CodeAnalysisResults codeAnalysisResults, RichTextReport report) {
        this.report = report;
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("Trend report shows difference in metric between the latest measurements and previous reference measurements.");
        report.addListItem("For more insights in the value of trend analysis, Sokrates recommends reading the section \"Favor tracking trends over absolute numbers\" in the article <a href='https://martinfowler.com/articles/useOfMetrics.html#FavorTrackingTrendsOverAbsoluteNumbers' target='_blank'>An Appropriate Use of Metrics</a>, (MartinFowler.com).");
        report.endUnorderedList();
        report.endSection();

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
                report.startSection("Current vs. " + result.getLabel(), result.getAnalysisResultsPath());
                json = getJson(file);
                CodeAnalysisResults refData = (CodeAnalysisResults) new JsonMapper().getObject(json, CodeAnalysisResults.class);

                new SummaryUtils().summarizeAndCompare(codeAnalysisResults, refData, report);

                report.startShowMoreBlock("", "Detailed comparison of all metrics...");
                report.startTable();
                report.addTableHeader("Metric", "Reference Value", "Current Value", "Difference");
                codeAnalysisResults.getMetricsList().getMetrics().forEach(metric -> {
                    addRow(metric, refData);
                });
                report.endTable();
                report.endShowMoreBlock();
            } else {
                report.addParagraph("ERROR: could not find the reference result file '" + analysisResultsPath + "'.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getJson(File file) throws IOException {
        if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("zip")) {
            String json = ZipUtils.unzipFirstEntryAsString(file);
            return StringUtils.isNotBlank(json) ? json : "";
        } else {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        }
    }

    private void addRow(Metric metric, CodeAnalysisResults refData) {
        Metric refMetric = refData.getMetricsList().getMetricById(metric.getId());
        double currentValue = metric.getValue().doubleValue();
        double refValue = refMetric != null ? refMetric.getValue().doubleValue() : 0;

        report.startTableRow();
        report.startTableCell();
        report.addHtmlContent("<b>" + metric.getId() + "</b><br/>");
        report.endTableCell();


        report.startTableCell("text-align: center; color: lightgrey");
        report.addHtmlContent("" + refValue);
        report.endTableCell();
        report.startTableCell("text-align: center");
        report.addHtmlContent("<b>" + currentValue + "</b>");
        report.endTableCell();

        addDiffCell(currentValue, refValue);

        report.endTableRow();
    }

    private void addDiffCell(double currentValue, double refValue) {
        double diff = currentValue - refValue;
        String diffText = getDiffText(diff, refValue);
        report.startTableCell("text-align: right; color: " +
                (diff == 0 ? "lightgrey" : (diff < 0 ? "#b9936c" : "#6b5b95")));
        if (diff > 0) {
            report.addHtmlContent("+" + diffText + " ⬆");
        } else if (diff < 0) {
            report.addHtmlContent("" + diffText + " ⬇ ");
        } else {
            report.addHtmlContent("" + diffText + "");
        }
        report.endTableCell();
    }

    private String getDiffText(double diff, double refValue) {
        double roundingError = 0.0000000000000000000001;
        String diffText;
        diff = ((int) diff * 100.0) / 100;
        if (Math.abs(diff) < roundingError) {
            diffText = "0 (0%)";
        } else if (Math.abs(refValue) < roundingError) {
            diffText = diff + " (NEW)";
        } else {
            double percentage = 100.0 * diff / refValue;
            diffText = diff + " (" + (percentage > 0 ? "+" : (percentage < 0 ? "-" : ""))
                    + FormattingUtils.getFormattedPercentage(Math.abs(percentage)) + "%)";
        }
        return diffText;
    }
}
