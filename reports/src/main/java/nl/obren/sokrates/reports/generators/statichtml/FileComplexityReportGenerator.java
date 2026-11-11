/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.FilesReportUtils;
import nl.obren.sokrates.reports.utils.PieChartUtils;
import nl.obren.sokrates.reports.utils.RiskDistributionStatsReportUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileComplexityDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File complexity: main files classified by the sum of the McCabe indexes of their units (files with
 * units only), with the bands of {@code analysis.fileComplexityThresholds}.
 */
public class FileComplexityReportGenerator {
    private final CodeAnalysisResults codeAnalysisResults;
    private final Thresholds thresholds;
    private final List<String> labels;

    public FileComplexityReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
        this.thresholds = codeAnalysisResults.getCodeConfiguration().getAnalysis().getFileComplexityThresholds();
        this.labels = thresholds.getLabels();
    }

    public void addFileComplexityToReport(RichTextReport report) {
        report.setDescription("The distribution of files by complexity: the sum of the McCabe indexes of their units.");

        FilesAnalysisResults files = codeAnalysisResults.getFilesAnalysisResults();
        RiskDistributionStats overall = files.getOverallFileComplexityDistribution();
        if (overall == null || overall.getTotalCount() == 0) {
            report.startSection("File Complexity", "");
            report.addParagraph("No main code file has units (functions, methods...), so there is no file complexity to show.");
            report.endSection();
        } else {
            addGraphOverall(report, overall);
            addGraphPerExtension(report, files.getFileComplexityDistributionPerExtension());
            addGraphsPerLogicalComponents(report, files.getFileComplexityDistributionPerLogicalDecomposition());
            addMostComplexFiles(report, files.getMostComplexFiles());
        }

        addAboutSection(report);
    }

    private void addGraphOverall(RichTextReport report, RiskDistributionStats distribution) {
        report.startSection("File Complexity Overall", "");
        report.startUnorderedList();
        report.addListItem("There are " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + " files with units, with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code.");
        report.startUnorderedList();
        addBandItem(report, distribution.getVeryHighRiskCount(), distribution.getVeryHighRiskValue(), "very complex", thresholds.getVeryHighRiskLabel());
        addBandItem(report, distribution.getHighRiskCount(), distribution.getHighRiskValue(), "complex", thresholds.getHighRiskLabel());
        addBandItem(report, distribution.getMediumRiskCount(), distribution.getMediumRiskValue(), "medium complex", thresholds.getMediumRiskLabel());
        addBandItem(report, distribution.getLowRiskCount(), distribution.getLowRiskValue(), "slightly complex", thresholds.getLowRiskLabel());
        addBandItem(report, distribution.getNegligibleRiskCount(), distribution.getNegligibleRiskValue(), "simple", thresholds.getNegligibleRiskLabel());
        report.endUnorderedList();
        int filesWithoutUnits = countMainFilesWithoutUnits();
        if (filesWithoutUnits > 0) {
            report.addListItem(RichTextRenderingUtils.renderNumberStrong(filesWithoutUnits)
                    + " main files have no units (e.g. languages without unit analysis, or files without functions) and are not classified.");
        }
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionChart(distribution, labels));
        report.addLineBreak();
        report.addLineBreak();
        report.addHtmlContent("explore: ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles.html#main_mccabe_coloring'>grouped by folders</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles.html#main_mccabe_coloring_categories'>grouped by complexity</a>");
        report.endSection();
    }

    private void addBandItem(RichTextReport report, int count, int linesOfCode, String name, String range) {
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(count) + " " + name + " files (McCabe index sum "
                + range + "; " + RichTextRenderingUtils.renderNumberStrong(linesOfCode) + " lines of code)");
    }

    private int countMainFilesWithoutUnits() {
        return (int) codeAnalysisResults.getFilesAnalysisResults().getAllFiles().stream()
                .filter(file -> file.getUnitsCount() == 0).count();
    }

    private void addGraphPerExtension(RichTextReport report, List<RiskDistributionStats> distributions) {
        report.startSection("File Complexity per Extension", "");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(distributions, labels));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report, List<FileComplexityDistributionPerLogicalDecomposition> decompositions) {
        report.startSection("File Complexity per Logical Decomposition", "");
        decompositions.forEach(decomposition -> {
            report.startSubSectionText(decomposition.getName(), "");
            report.startScrollingDiv();
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(decomposition.getDistributionPerComponent(), labels));
            report.endDiv();
            report.endSection();
        });
        report.endSection();
    }

    private void addMostComplexFiles(RichTextReport report, List<SourceFile> files) {
        report.startSection("Most Complex Files (Top " + files.size() + ")", "");
        report.addParagraph("The McCabe index of a file grows with its size: <b>per 100 lines</b> tells a large but plain file "
                + "from a dense one, and <b>most complex unit</b> whether the complexity sits in one unit or is spread over many.");
        Map<String, Integer> mostComplexUnit = mostComplexUnitPerFile();
        boolean linkToFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        StringBuilder table = new StringBuilder();
        // Long folder paths wrap here (the shared file cell keeps them on one line), so the numeric
        // columns stay in view.
        table.append("<style>.sk-file-complexity-table td:first-child > div { display: flex; }"
                + " .sk-file-complexity-table td:first-child > div > div:last-child { min-width: 0; }"
                + " .sk-file-complexity-table td:first-child > div > div:last-child div { white-space: normal !important; word-break: break-all; }</style>\n");
        table.append("<div style='width: 100%; overflow-x: auto;'>\n");
        table.append("<table class='sk-data-table sk-file-complexity-table' style='width: 100%'>\n");
        table.append("<tr><th>File</th><th># lines</th><th># units</th><th>McCabe index<br>(sum)</th>"
                + "<th>per 100<br>lines</th><th>most complex<br>unit</th></tr>\n");
        files.forEach(file -> {
            int linesOfCode = file.getLinesOfCode();
            int mcCabe = file.getUnitsMcCabeIndexSum();
            table.append("<tr>\n");
            table.append(FilesReportUtils.fileNameCell(file, linkToFiles));
            table.append("<td style='text-align: center'>" + linesOfCode + "</td>\n");
            table.append("<td style='text-align: center'>" + file.getUnitsCount() + "</td>\n");
            table.append("<td style='text-align: center'><b>" + mcCabe + "</b></td>\n");
            table.append("<td style='text-align: center'>" + (linesOfCode > 0 ? String.valueOf(Math.round(100.0 * mcCabe / linesOfCode)) : "-") + "</td>\n");
            table.append("<td style='text-align: center'>" + mostComplexUnit.getOrDefault(file.getRelativePath(), 0) + "</td>\n");
            table.append("</tr>\n");
        });
        table.append("</table>\n</div>\n");
        report.addHtmlContent(table.toString());
        report.endSection();
    }

    private Map<String, Integer> mostComplexUnitPerFile() {
        Map<String, Integer> max = new HashMap<>();
        codeAnalysisResults.getUnitsAnalysisResults().getAllUnits().forEach(unit -> {
            if (unit.getSourceFile() != null) {
                max.merge(unit.getSourceFile().getRelativePath(), unit.getMcCabeIndex(), Math::max);
            }
        });
        return max;
    }

    private void addAboutSection(RichTextReport report) {
        report.startSection("About This Analysis", "");
        report.startUnorderedList();
        report.addListItem("File complexity is the sum of the McCabe indexes (conditional complexity) of the units (methods, functions...) in a file.");
        report.addListItem("Files are classified in five categories based on that sum: " +
                thresholds.getNegligibleRiskLabel() + " (simple files), " +
                thresholds.getLowRiskLabel() + " (slightly complex files), " +
                thresholds.getMediumRiskLabel() + " (medium complex files), " +
                thresholds.getHighRiskLabel() + " (complex files), " +
                thresholds.getVeryHighRiskLabel() + " (very complex files). " +
                "The default bands are five times the unit complexity bands, as a file is a group of units.");
        report.addListItem("Only main code files with units are classified. Files without units (languages without unit analysis, " +
                "or files without functions) have no complexity to measure and are left out rather than counted as simple.");
        report.addListItem("A very complex file holds many decisions, which often means it has too many responsibilities: " +
                "a candidate to split, and a place where changes need more testing.");
        report.endUnorderedList();
        report.startDetailsBlock("Learn more...");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='https://en.wikipedia.org/wiki/Cyclomatic_complexity'>Cyclomatic Complexity</a>, wikipedia.org");
        report.addListItem("<a target='_blank' href='https://sourcemaking.com/antipatterns/the-blob'>The Blob Software Development Anti-Pattern</a>, sourcemaking.com");
        report.endUnorderedList();
        report.endDetailsBlock();
        report.endSection();
    }
}
