/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSizeAnalyzer extends Analyzer {
    private CodeConfiguration codeConfiguration;
    private MetricsList metricsList;
    private FilesAnalysisResults analysisResults;

    public FileSizeAnalyzer(CodeAnalysisResults results) {
        this.analysisResults = results.getFilesAnalysisResults();
        this.codeConfiguration = results.getCodeConfiguration();
        this.metricsList = results.getMetricsList();
    }

    public void analyze() {
        SourceFileSizeDistribution fileSizeDistribution = new SourceFileSizeDistribution();
        List<SourceFile> allFiles = codeConfiguration.getMain().getSourceFiles();
        List<SourceFile> sourceFiles = allFiles;
        SourceFileSizeDistribution overallDistribution = fileSizeDistribution.getOverallDistribution(sourceFiles);
        analysisResults.setOveralFileSizeDistribution(overallDistribution);
        analysisResults.setFileSizeDistributionPerExtension(SourceFileSizeDistribution.getFileSizeRiskDistributionPerExtension(sourceFiles));
        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            FileDistributionPerLogicalDecomposition distributionPerLogicalDecomposition = new FileDistributionPerLogicalDecomposition();
            distributionPerLogicalDecomposition.setName(logicalDecomposition.getName());
            distributionPerLogicalDecomposition.setFileSizeDistributionPerComponent(SourceFileSizeDistribution.getFileSizeRiskDistributionPerComponent(sourceFiles, logicalDecomposition));
            analysisResults.getFileSizeDistributionPerLogicalDecomposition().add(distributionPerLogicalDecomposition);
        });
        analysisResults.setAllFiles(allFiles);
        addLongestFiles(allFiles, analysisResults, 50);

        addMetrics(overallDistribution);
    }

    private void addMetrics(SourceFileSizeDistribution overallDistribution) {
        metricsList.addSystemMetric().id("NUMBER_OF_FILES_FILE_SIZE_0_200").value(overallDistribution.getLowRiskCount())
                .description("Number of files with 200 or less lines of code");
        metricsList.addSystemMetric().id("NUMBER_OF_FILES_FILE_SIZE_201_500").value(overallDistribution.getMediumRiskCount())
                .description("Number of files with 200 to 500 lines of code");
        metricsList.addSystemMetric().id("NUMBER_OF_FILES_FILE_SIZE_501_1000").value(overallDistribution.getHighRiskCount())
                .description("Number of files with 500 to 1000 lines of code");
        metricsList.addSystemMetric().id("NUMBER_OF_FILES_FILE_SIZE_1001_PLUS").value(overallDistribution.getVeryHighRiskCount())
                .description("Number of files with more than 1000 lines of code");

        metricsList.addSystemMetric().id("LINES_OF_CODE_FILE_SIZE_FILES_0_200").value(overallDistribution.getLowRiskCount())
                .description("Lines of code in all files with 500 to 1000 lines of code");
        metricsList.addSystemMetric().id("LINES_OF_CODE_FILE_SIZE_201_500").value(overallDistribution.getMediumRiskCount())
                .description("Lines of code in all files with 500 to 1000 lines of code");
        metricsList.addSystemMetric().id("LINES_OF_CODE_FILE_SIZE_501_1000").value(overallDistribution.getHighRiskCount())
                .description("Lines of code in files with 500 to 1000 lines of code");
        metricsList.addSystemMetric().id("LINES_OF_CODE_FILE_SIZE_1001_PLUS").value(overallDistribution.getVeryHighRiskCount())
                .description("Lines of code in all files with more than 1000 lines of code");
    }

    private void addLongestFiles(List<SourceFile> sourceFiles, FilesAnalysisResults filesAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesAnalysisResults.getLongestFiles().add(sourceFile);
        });
    }
}
