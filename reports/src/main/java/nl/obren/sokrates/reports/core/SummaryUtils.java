package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.List;

public class SummaryUtils {
    private static void summarizeFileSize(RichTextReport report, CodeAnalysisResults analysisResults) {
        FilesAnalysisResults filesAnalysisResults = analysisResults.getFilesAnalysisResults();
        if (filesAnalysisResults != null && filesAnalysisResults.getOveralFileSizeDistribution() != null) {
            int mainLOC = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();
            int veryLongFilesLOC = filesAnalysisResults.getOveralFileSizeDistribution().getVeryHighRiskValue();
            int shortFilesLOC = filesAnalysisResults.getOveralFileSizeDistribution().getLowRiskValue();

            report.addParagraph("File Size: <b style='" + (veryLongFilesLOC > 1 ? "color: crimson" : "") + "'>"
                    + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(mainLOC, veryLongFilesLOC))
                    + "%</b> very long (>1000 LOC), <b style='color: green'>"
                    + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(mainLOC, shortFilesLOC))
                    + "%</b> short (<= 200 LOC)");
        }
    }

    private static void summarizeListOfLocAspects(StringBuilder summary, List<NumericMetric> linesOfCodePerAspect) {
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

    public void summarize(CodeAnalysisResults analysisResults, RichTextReport report) {
        summarizeVolume(analysisResults, report);
        sumariseDuplication(analysisResults, report);
        summarizeFileSize(report, analysisResults);
        summarizeUnitSize(analysisResults, report);
        summarizeUnitComplexity(analysisResults, report);
        summarizeComponents(analysisResults, report);
        addSummaryFindings(analysisResults, report);
    }

    public void summarizeAndCompare(CodeAnalysisResults analysisResults, CodeAnalysisResults refData, RichTextReport report) {
        StringBuilder summary = new StringBuilder("");
        summarizeMainCode(analysisResults, summary);

        summary.append(addDiffDiv(analysisResults.getMainAspectAnalysisResults().getLinesOfCode(),
                refData.getMainAspectAnalysisResults().getLinesOfCode()));
        summary.append("<div style='opacity:0.4;margin-left: 24px;font-size:90%;margin-bottom:46px'>");
        summarizeMainCode(refData, summary);
        summary.append("</div>");

        summarizeTestCode(analysisResults, summary);

        summary.append(addDiffDiv(analysisResults.getTestAspectAnalysisResults().getLinesOfCode(),
                refData.getTestAspectAnalysisResults().getLinesOfCode()));
        summary.append("<div style='opacity:0.4;margin-left: 24px;font-size:90%;margin-bottom:46px'>");
        summarizeTestCode(refData, summary);
        summary.append("</div>");

        report.addParagraph(summary.toString());

        report.startDiv("color:black");
        sumariseDuplication(analysisResults, report);
        report.endDiv();

        report.addParagraph(addDiffDiv(analysisResults.getDuplicationAnalysisResults().getOverallDuplication().getDuplicatedLinesOfCode(),
                refData.getDuplicationAnalysisResults().getOverallDuplication().getDuplicatedLinesOfCode()));
        report.startDiv("opacity:0.4;margin-left: 24px;font-size:90%;margin-bottom:46px");
        sumariseDuplication(refData, report);
        report.endDiv();

        report.startDiv("color:black");
        summarizeFileSize(report, analysisResults);
        report.endDiv();

        report.startDiv("opacity:0.4;margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeFileSize(report, refData);
        report.endDiv();

        report.startDiv("color:black");
        summarizeUnitSize(analysisResults, report);
        report.endDiv();

        report.startDiv("opacity:0.4;margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeUnitSize(refData, report);
        report.endDiv();

        report.startDiv("color:black");
        summarizeUnitComplexity(analysisResults, report);
        report.endDiv();

        report.startDiv("opacity:0.4;margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeUnitComplexity(refData, report);
        report.endDiv();

        report.startDiv("color:black");
        summarizeComponents(analysisResults, report);
        report.endDiv();

        report.startDiv("opacity:0.4;margin-left: 24px;font-size:90%;margin-bottom:46px");
        summarizeComponents(refData, report);
        report.endDiv();

        report.startDiv("color:black");
        addSummaryFindings(analysisResults, report);
        report.endDiv();

        report.startDiv("opacity:0.4;margin-left: 24px;font-size:90%;margin-bottom:46px");
        addSummaryFindings(refData, report);
        report.endDiv();
    }

    private void summarizeVolume(CodeAnalysisResults analysisResults, RichTextReport report) {
        StringBuilder summary = new StringBuilder("");
        summarizeMainCode(analysisResults, summary);
        summarizeTestCode(analysisResults, summary);
        report.addParagraph(summary.toString());
    }

    private void summarizeTestCode(CodeAnalysisResults analysisResults, StringBuilder summary) {
        summary.append("<p>Test Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(analysisResults.getTestAspectAnalysisResults().getLinesOfCode()) + " LOC");
        List<NumericMetric> linesOfCodePerExtension = analysisResults.getTestAspectAnalysisResults().getLinesOfCodePerExtension();
        summarizeListOfLocAspects(summary, linesOfCodePerExtension);
        summary.append("</p>");
    }

    private void summarizeMainCode(CodeAnalysisResults analysisResults, StringBuilder summary) {
        summary.append("<p>Main Code: ");
        summary.append(RichTextRenderingUtils.renderNumberStrong(analysisResults.getMainAspectAnalysisResults().getLinesOfCode()) + " LOC");
        summarizeListOfLocAspects(summary, analysisResults.getMainAspectAnalysisResults().getLinesOfCodePerExtension());

        summary.append("</p>");
    }

    private void addSummaryFindings(CodeAnalysisResults analysisResults, RichTextReport report) {
        List<String> summaryFindings = analysisResults.getCodeConfiguration().getSummaryFindings();
        if (summaryFindings != null && summaryFindings.size() > 0) {
            report.addParagraph("Other findings:");
            report.startUnorderedList();
            summaryFindings.forEach(summaryFinding -> {
                report.addListItem(summaryFinding);
            });
            report.endUnorderedList();
        }
    }

    private void summarizeComponents(CodeAnalysisResults analysisResults, RichTextReport report) {
        report.addHtmlContent("<p>Logical Component Decomposition:");
        boolean first[] = {true};
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(decomposition -> {
            if (!first[0]) {
                report.addHtmlContent(", ");
            } else {
                first[0] = false;
            }
            report.addHtmlContent(decomposition.getKey() + " (" + decomposition.getComponents().size() + " components)");
        });
        report.addHtmlContent("</p>");
    }

    private void summarizeUnitComplexity(CodeAnalysisResults analysisResults, RichTextReport report) {
        int linesOfCodeInUnits = analysisResults.getUnitsAnalysisResults().getLinesOfCodeInUnits();
        int veryComplexUnitsLOC = analysisResults.getUnitsAnalysisResults().getCyclomaticComplexityRiskDistribution().getVeryHighRiskValue();
        int lowComplexUnitsLOC = analysisResults.getUnitsAnalysisResults().getCyclomaticComplexityRiskDistribution().getLowRiskValue();

        report.addParagraph("Cyclomatic Complexity: <b style='" + (veryComplexUnitsLOC > 1 ? "color: crimson" : "") + "'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, veryComplexUnitsLOC))
                + "%</b> very complex (McCabe index > 25), <b style='color: green'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, lowComplexUnitsLOC))
                + "%</b> simple (McCabe index <= 5)");
    }

    private void summarizeUnitSize(CodeAnalysisResults analysisResults, RichTextReport report) {
        int linesOfCodeInUnits = analysisResults.getUnitsAnalysisResults().getLinesOfCodeInUnits();
        int veryLongUnitsLOC = analysisResults.getUnitsAnalysisResults().getUnitSizeRiskDistribution().getVeryHighRiskValue();
        int lowUnitsLOC = analysisResults.getUnitsAnalysisResults().getUnitSizeRiskDistribution().getLowRiskValue();

        report.addParagraph("Unit Size: <b style='" + (veryLongUnitsLOC > 1 ? "color: crimson" : "") + "'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, veryLongUnitsLOC))
                + "%</b> very long (>100 LOC), <b style='color: green'>"
                + FormattingUtils.getFormattedPercentage(RichTextRenderingUtils.getPercentage(linesOfCodeInUnits, lowUnitsLOC))
                + "%</b> short (<= 20 LOC)");
    }

    private void sumariseDuplication(CodeAnalysisResults analysisResults, RichTextReport report) {
        Number duplicationPercentage = analysisResults.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage();
        double duplication = duplicationPercentage.doubleValue();
        if (!analysisResults.getCodeConfiguration().getAnalysis().isSkipDuplication()) {
            report.addParagraph("Duplication: <b style='" + (duplication > 5.0 ? "color: crimson" : "") + "'>" + FormattingUtils.getFormattedPercentage(duplication) + "%</b>");
        }
    }

    private String addDiffDiv(double value, double refValue) {
        double diff = value - refValue;
        String diffText = getDiffText(diff, refValue);
        StringBuilder html = new StringBuilder("<div style='margin-left: 24px; margin-top: 0; text-align: left; color: " +
                (diff == 0 ? "lightgrey" : (diff < 0 ? "#b9936c" : "#6b5b95")) + "'>");
        if (diff > 0) {
            html.append("+" + diffText + " ⬆");
        } else if (diff < 0) {
            html.append("" + diffText + " ⬇ ");
        } else {
            html.append("" + diffText + "");
        }

        html.append("</div>");

        return html.toString();
    }

    private String getDiffText(double diff, double refValue) {
        String diffText;
        if (Math.abs(refValue) < 0.0000000000000000000001) {
            diffText = "";
        } else {
            double percentage = 100.0 * diff / refValue;
            diffText = diff + " (" + (percentage > 0 ? "+" : (percentage < 0 ? "-" : ""))
                    + FormattingUtils.getFormattedPercentage(Math.abs(percentage)) + "%)";
        }
        return diffText;
    }

}