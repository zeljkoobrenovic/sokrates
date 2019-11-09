package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.FilesReportUtils;
import nl.obren.sokrates.reports.utils.PieChartUtils;
import nl.obren.sokrates.reports.utils.RiskDistributionStatsReportUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;

import java.util.Arrays;
import java.util.List;

public class FileSizeReportGenerator {
    private CodeAnalysisResults codeAnalysisResults;
    private List<String> labels = Arrays.asList("1001+", "501-1000", "201-500", "1-200");

    public FileSizeReportGenerator(CodeAnalysisResults codeAnalysisResults) {
        this.codeAnalysisResults = codeAnalysisResults;
    }

    public void addFileSizeToReport(RichTextReport report) {
        report.startSection("Intro", "");
        report.startUnorderedList();
        report.addListItem("File size measurements show the distribution of size of files.");
        report.addListItem("Files are classified in four categories based on their size (lines of code): " +
                "1-200 (small files), 200-500 (medium size files), 501-1000 (long files), 1001+ (very long files).");
        report.endUnorderedList();
        report.endSection();

        addGraphOverall(report, codeAnalysisResults.getFilesAnalysisResults().getOveralFileSizeDistribution());
        addGraphPerExtension(report, codeAnalysisResults.getFilesAnalysisResults().getFileSizeDistributionPerExtension());
        addGraphsPerLogicalComponents(report, codeAnalysisResults.getFilesAnalysisResults().getFileSizeDistributionPerLogicalDecomposition());

        report.startSection("Alternative Visuals", "");
        report.startUnorderedList();
        report.addListItem("<a target='_blank' href='visuals/files_3d.html'>3D view of all files</a>");
        report.endUnorderedList();
        report.endSection();

        addLongestFilesList(report);
    }

    private void addGraphOverall(RichTextReport report, SourceFileSizeDistribution distribution) {
        report.startSection("File Size Overall", "");
        report.startUnorderedList();
        report.addListItem("There are " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalCount())
                + " files with " + RichTextRenderingUtils.renderNumberStrong(distribution.getTotalValue())
                + " lines of code in files" +
                ".");
        report.startUnorderedList();
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskCount())
                + " very long files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getVeryHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskCount())
                + " long files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getHighRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskCount())
                + " medium size files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getMediumRiskValue())
                + " lines of code)");
        report.addListItem(RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskCount())
                + " small files (" + RichTextRenderingUtils.renderNumberStrong(distribution.getLowRiskValue())
                + " lines of code)");
        report.endUnorderedList();
        report.endUnorderedList();
        report.addHtmlContent(PieChartUtils.getRiskDistributionPieChart(distribution, labels));
        report.endSection();
    }

    private void addGraphPerExtension(RichTextReport report, List<RiskDistributionStats> sourceFileSizeDistribution) {
        report.startSection("File Size per Extension", "");
        report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(sourceFileSizeDistribution, labels));
        report.endSection();
    }

    private void addGraphsPerLogicalComponents(RichTextReport report, List<FileDistributionPerLogicalDecomposition> fileDistributionPerLogicalDecompositions) {
        report.startSection("File Size per Logical Decomposition", "");
        fileDistributionPerLogicalDecompositions.forEach(logicalDecomposition -> {
            report.startSubSection("" + logicalDecomposition.getName() + "", "");
            report.addHtmlContent(RiskDistributionStatsReportUtils.getRiskDistributionPerKeySvgBarChart(logicalDecomposition.getFileSizeDistributionPerComponent(), labels));
            report.endSection();
        });
        report.endSection();
    }

    private void addLongestFilesList(RichTextReport report) {
        List<SourceFile> longestFiles = codeAnalysisResults.getFilesAnalysisResults().getLongestFiles();
        report.startSection("Longest Files (Top " + longestFiles.size() + ")", "");
        report.addHtmlContent(FilesReportUtils.getFilesTable(longestFiles).toString());
        report.endSection();
    }
}
