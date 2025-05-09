/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.utils.CorrelationDiagramGenerator;
import nl.obren.sokrates.reports.utils.FilesReportUtils;
import nl.obren.sokrates.reports.utils.PieChartUtils;
import nl.obren.sokrates.reports.utils.RiskDistributionStatsReportUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileAgeDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileChangeDistribution;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.List;
import java.util.stream.Collectors;

public class FileChurnReportGenerator {
    public static final String THE_NUMBER_OF_FILE_CHANGES = "File Change Frequency";
    public static final String THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION = "The number of recorded file updates";
    public static final String THE_CONTRIBUTORS_COUNT = "Contributors Count Frequency";
    public static final String THE_CONTRIBUTORS_COUNT_DESCRIPTION = "The number of file contributors";
    private final Thresholds thresholds;
    private final Thresholds thresholdsContributors;

    private CodeAnalysisResults codeAnalysisResults;
    private List<String> changeFrequencyLabels;
    private List<String> contributorsCountFrequencyLabels;
    private int graphCounter = 1;

    public FileChurnReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
        thresholds = codeAnalysisResults.getCodeConfiguration().getAnalysis().getFileUpdateFrequencyThresholds();
        thresholdsContributors = codeAnalysisResults.getCodeConfiguration().getAnalysis().getFileContributorsCountThresholds();
        changeFrequencyLabels = thresholds.getLabels();
        contributorsCountFrequencyLabels = thresholdsContributors.getLabels();
    }

    public void addFileHistoryToReport(RichTextReport report) {
        report.addParagraph("File change frequency (churn) shows the distribution of file updates " +
                "(days with at least one commit).", "margin-top: 12px; color: grey");

        addOverallSections(report);

        addGraphsPerExtension(report);
        addGraphsPerLogicalComponents(report);
        addMostChangedFilesList(report);
        addFilesWithMostContributors(report);
        addFilesWithLeastContributors(report);
        addCorrelations(report);
    }


    private void addGraphsPerExtension(RichTextReport report) {
        FilesHistoryAnalysisResults ageAnalysisResults = codeAnalysisResults.getFilesHistoryAnalysisResults();

        String extensions = codeAnalysisResults.getCodeConfiguration().getExtensions().stream().collect(Collectors.joining(", "));
        report.startSection("File Change Frequency per File Extension", extensions);

        addChangesGraphPerExtension(report, ageAnalysisResults.getChangeDistributionPerExtension(),
                THE_NUMBER_OF_FILE_CHANGES + " per Extension", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);

        report.endSection();
    }

    private void addOverallSections(RichTextReport report) {
        report.startSection("Overview", "");
        report.startSubSection("File Change Frequency Overall", "");
        addGraphOverallChange(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution());
        report.addLineBreak();
        report.addHtmlContent("explore: ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles_main_update_frequency_coloring.html'>grouped by folders</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles_main_update_frequency_coloring_categories.html'>grouped by update frequency</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='../data/text/mainFilesWithHistory.txt'>data</a>");
        report.endSection();

        report.startSubSection("Contributors Count Frequency Overall", "");
        addGraphOverallContributorCount(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallContributorsCountDistribution());
        report.addLineBreak();
        report.addHtmlContent("explore: ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles_main_contributors_count_coloring.html'>grouped by folders</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='visuals/zoomable_circles_main_contributors_count_coloring_categories.html'>grouped by contributors count</a>");
        report.addHtmlContent(" | ");
        report.addHtmlContent("<a target='_blank' href='../data/text/mainFilesWithHistory.txt'>data</a>");
        report.endSection();
        report.endSection();
    }

    private void addGraphOverallChange(RichTextReport report, SourceFileChangeDistribution distribution) {
        report.startUnorderedList();
        report.addListItem("There are <b>"
                + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + "</b> files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " " + (distribution.getVeryHighRiskCount() == 1 ? "file" : "files") + " changed more than "
                + thresholds.getVeryHigh()
                + " times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + " " + (distribution.getHighRiskCount() == 1 ? "file" : "files") + " changed " + thresholds.getHighRiskLabel() + " times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " " + (distribution.getMediumRiskCount() == 1 ? "file" : "files") + " changed " + thresholds.getMediumRiskLabel() + " times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " " + (distribution.getLowRiskCount() == 1 ? "file" : "files") + " changed " + thresholds.getLowRiskLabel() + " times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " " + (distribution.getNegligibleRiskCount() == 1 ? "file" : "files") + " changed " + thresholds.getNegligibleRiskLabel() + " times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionChart(distribution, changeFrequencyLabels, Palette.getHeatPalette()));
    }

    private void addGraphOverallContributorCount(RichTextReport report, SourceFileChangeDistribution distribution) {
        report.startUnorderedList();
        report.addListItem("There are <b>"
                + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + "</b> files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " " + (distribution.getVeryHighRiskCount() == 1 ? "file" : "files") + " changed by more than "
                + thresholdsContributors.getVeryHigh()
                + " contributors (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + " " + (distribution.getHighRiskCount() == 1 ? "file" : "files") + " changed by " + thresholdsContributors.getHighRiskLabel() + " contributors (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " " + (distribution.getMediumRiskCount() == 1 ? "file" : "files") + " changed by " + thresholdsContributors.getMediumRiskLabel() + " contributors (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " " + (distribution.getLowRiskCount() == 1 ? "file" : "files") + " changed by " + thresholdsContributors.getLowRiskLabel() + " contributors (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " " + (distribution.getNegligibleRiskCount() == 1 ? "file" : "files") + " changed by " + thresholdsContributors.getNegligibleRiskLabel()
                + " " + (thresholdsContributors.getLow() == 1 ? "contributor" : "contributors") + " (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionChart(distribution, contributorsCountFrequencyLabels, Palette.getHeatPalette()));
    }

    private void addChangesGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileAgeDistribution, String title, String subtitle) {
        report.startSubSection(title, subtitle);
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(sourceFileAgeDistribution, changeFrequencyLabels, Palette.getHeatPalette()));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report) {
        String components = codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().stream().map(c -> c.getName()).collect(Collectors.joining(", "));

        report.startSection("File Change Frequency per Logical Decomposition", components);

        addChangesPerLogicalDecomposition(report);

        report.endSection();
    }

    private LogicalDecomposition getLogicalDecompositionByName(String name) {
        for (LogicalDecomposition logicalDecomposition : this.codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions()) {
            if (logicalDecomposition.getName().equalsIgnoreCase(name)) {
                return logicalDecomposition;
            }
        }

        return null;
    }

    private void addChangesPerLogicalDecomposition(RichTextReport report) {
        int logicalDecompositionCounter[] = {0};
        codeAnalysisResults.getCodeConfiguration().getLogicalDecompositions().forEach(logicalDecomposition -> {
            logicalDecompositionCounter[0]++;

            String name = logicalDecomposition.getName();
            codeAnalysisResults.getFilesHistoryAnalysisResults().getChangeDistributionPerLogicalDecomposition().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name)).forEach(distribution -> {
                        addChangeDetailsForLogicalDecomposition(report, distribution);
                    });
        });
    }

    private void addChangeDetailsForLogicalDecomposition(RichTextReport report, FileAgeDistributionPerLogicalDecomposition logicalDecomposition) {
        report.startSubSection("" + logicalDecomposition.getName()
                + " (" + THE_NUMBER_OF_FILE_CHANGES.toLowerCase() + ")", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        report.startScrollingDiv();
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getDistributionPerComponent(), changeFrequencyLabels, Palette.getHeatPalette()));
        report.endDiv();
        report.endSection();

    }

    private void addMostChangedFilesList(RichTextReport report) {
        List<SourceFile> youngestFiles = codeAnalysisResults.getFilesHistoryAnalysisResults().getMostChangedFiles();
        report.startSection("Most Frequently Changed Files (Top " + youngestFiles.size() + ")", "");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        report.addParagraph("<a href='../data/text/mainFilesWithHistory.txt' target='_blank'>See data for all files...</a>");
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true, false, 500).toString());
        report.endSection();
    }

    private void addFilesWithMostContributors(RichTextReport report) {
        List<SourceFile> files = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilesWithMostContributors();
        report.startSection("Files With Most Contributors (Top " + files.size() + ")", "Based on the number of unique email addresses found in commits.");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        report.addParagraph("<a href='../data/text/mainFilesWithHistory.txt' target='_blank'>See data for all files...</a>");
        report.addHtmlContent(FilesReportUtils.getFilesTable(files, cacheSourceFiles, true, false, 500).toString());
        report.endSection();
    }

    private void addFilesWithLeastContributors(RichTextReport report) {
        List<SourceFile> files = codeAnalysisResults.getFilesHistoryAnalysisResults().getFilesWithLeastContributors();
        report.startSection("Files With Least Contributors (Top " + files.size() + ")", "Based on the number of unique email addresses found in commits.");
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
        report.addParagraph("<a href='../data/text/mainFilesWithHistory.txt' target='_blank'>See data for all files...</a>");
        report.addHtmlContent(FilesReportUtils.getFilesTable(files, cacheSourceFiles, true, false, 500).toString());
        report.endSection();
    }

    private void addCorrelations(RichTextReport report) {
        if (!codeAnalysisResults.getCodeConfiguration().getAnalysis().isSkipCorrelations() && codeAnalysisResults.getContributorsAnalysisResults().getContributors().size() > 0) {
            report.startSection("Correlations", "");
            CorrelationDiagramGenerator<FileModificationHistory> correlationDiagramGenerator = new CorrelationDiagramGenerator<>(report,
                    codeAnalysisResults.getFilesHistoryAnalysisResults().getHistory(Integer.MAX_VALUE));

            ProcessingStopwatch.start("reporting/file update frequency/correlations");
            correlationDiagramGenerator.addCorrelations("File Size vs. Number of Changes", "lines of code", "# changes",
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode;
                    },
                    p -> p.getDates().size(),
                    p -> p.getPath());

            correlationDiagramGenerator.addCorrelations("Number of Contributors vs. Number of Changes", "# contributors", "# changes",
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode > 0 ? p.countContributors() : 0;
                    },
                    p -> p.getDates().size(),
                    p -> p.getPath());

            correlationDiagramGenerator.addCorrelations("Number of Contributors vs. File Size", "# contributors", "lines of code",
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode > 0 ? p.countContributors() : 0;
                    },
                    p -> {
                        SourceFile sourceFileByRelativePath = codeAnalysisResults.getFilesAnalysisResults().getSourceFileByRelativePath(p.getPath());
                        int linesOfCode = sourceFileByRelativePath != null ? sourceFileByRelativePath.getLinesOfCode() : 0;
                        return linesOfCode;
                    },
                    p -> p.getPath());

            ProcessingStopwatch.end("reporting/file update frequency/correlations");
            report.endSection();
        }
    }
}
