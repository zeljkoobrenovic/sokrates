/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.*;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileAgeDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.*;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileChangeDistribution;

import java.util.*;
import java.util.stream.Collectors;

public class FileChurnReportGenerator {
    public static final String THE_NUMBER_OF_FILE_CHANGES = "File Change Frequency";
    public static final String THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION = "The number of recorded file updates";

    private CodeAnalysisResults codeAnalysisResults;
    private List<String> changeFrequencyLabels = Arrays.asList("101+", "51-100", "21-50", "6-20", "1-5 updates");
    private int graphCounter = 1;

    public FileChurnReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileHistoryToReport(RichTextReport report) {
        report.addParagraph("File change frequency measurements show the distribution of the number of file updates (days with at least one commit).");

        addOverallSections(report);

        addGraphsPerExtension(report);
        addGraphsPerLogicalComponents(report);
        addMostChangedFilesList(report);
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
        report.startSection("File Change Frequency Overall", "");
        addGraphOverallChange(report, codeAnalysisResults.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution(),
                THE_NUMBER_OF_FILE_CHANGES + " Overall", THE_NUMBER_OF_FILE_CHANGES_DESCRIPTION);
        report.addParagraph("<a href='../data/text/mainFilesWithHistory.txt' target='_blank'>Detailed data...</a>");
        report.endSection();
    }

    private void addGraphOverallChange(RichTextReport report, SourceFileChangeDistribution distribution, String title, String subtitle) {
        report.startSubSection(title, subtitle);
        report.startUnorderedList();
        report.addListItem("There are <b>"
                + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + "</b> files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " files changed more than 100 times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + " files 51 to 100 times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " files changed 21 to 50 times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " files changed 6 to 20 times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskCount())
                + " files changed 5 or fewer times (" + RichTextRenderingUtils.renderNumberStrong(distribution.getNegligibleRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, changeFrequencyLabels, Palette.getHeatPalette()));
        report.endSection();
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
        boolean cacheSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles();
        report.addHtmlContent(FilesReportUtils.getFilesTable(youngestFiles, cacheSourceFiles, true, false).toString());
        report.endSection();
    }
}
