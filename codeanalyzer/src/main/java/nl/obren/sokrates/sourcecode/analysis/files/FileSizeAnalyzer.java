/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
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
        SourceFileSizeDistribution fileSizeDistribution = new SourceFileSizeDistribution(codeConfiguration.getAnalysis().getFileSizeThresholds());
        List<SourceFile> allFiles = codeConfiguration.getMain().getSourceFiles();
        List<SourceFile> sourceFiles = allFiles;
        SourceFileSizeDistribution overallDistribution = fileSizeDistribution.getOverallDistribution(sourceFiles);
        analysisResults.setOverallFileSizeDistribution(overallDistribution);
        analysisResults.setFileSizeDistributionPerExtension(SourceFileSizeDistribution.getFileSizeRiskDistributionPerExtension(sourceFiles, codeConfiguration.getAnalysis().getFileSizeThresholds()));
        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            FileDistributionPerLogicalDecomposition distributionPerLogicalDecomposition = new FileDistributionPerLogicalDecomposition();
            distributionPerLogicalDecomposition.setName(logicalDecomposition.getName());
            distributionPerLogicalDecomposition.setFileSizeDistributionPerComponent(SourceFileSizeDistribution.getFileSizeRiskDistributionPerComponent(logicalDecomposition, codeConfiguration.getAnalysis().getFileSizeThresholds()));
            analysisResults.getFileSizeDistributionPerLogicalDecomposition().add(distributionPerLogicalDecomposition);
        });
        analysisResults.setAllFiles(allFiles);
        addLongestFiles(allFiles, analysisResults, 50);

        addMetrics(overallDistribution);
    }

    private void addMetrics(SourceFileSizeDistribution overallDistribution) {
        String negligiblePrefix = "NEGLIGIBLE_RISK_FILE_SIZE_";
        String lowPrefix = "LOW_RISK_FILE_SIZE_";
        String mediumPrefix = "MEDIUM_RISK_FILE_SIZE_";
        String highPrefix = "HIGH_RISK_FILE_SIZE_";
        String veryHighPrefix = "VERY_HIGH_RISK_FILE_SIZE_";

        String negligibleDescription = " files with " + overallDistribution.getLowRiskThreshold() + " or less lines of code";
        String lowDescription = " files with " + overallDistribution.getLowRiskThreshold() + " to " + overallDistribution.getMediumRiskThreshold() + " lines of code";
        String mediumDescription = " files with " + overallDistribution.getMediumRiskThreshold() + " to " + overallDistribution.getHighRiskThreshold() + " lines of code";
        String highDescription = " files with " + overallDistribution.getHighRiskThreshold() + " to " + overallDistribution.getVeryHighRiskThreshold() + " lines of code";
        String veryHighDescription = " files with more than " + overallDistribution.getVeryHighRiskThreshold() + " lines of code";

        String countDescriptionPrefix = "Number of ";
        metricsList.addSystemMetric().id(negligiblePrefix + "COUNT").value(overallDistribution.getNegligibleRiskCount())
                .description(countDescriptionPrefix + negligibleDescription);
        metricsList.addSystemMetric().id(lowPrefix + "COUNT").value(overallDistribution.getLowRiskCount())
                .description(countDescriptionPrefix + lowDescription);
        metricsList.addSystemMetric().id(mediumPrefix + "COUNT").value(overallDistribution.getMediumRiskCount())
                .description(countDescriptionPrefix + mediumDescription);
        metricsList.addSystemMetric().id(highPrefix + "COUNT").value(overallDistribution.getHighRiskCount())
                .description(countDescriptionPrefix + highDescription);
        metricsList.addSystemMetric().id(veryHighPrefix + "COUNT").value(overallDistribution.getVeryHighRiskCount())
                .description(countDescriptionPrefix + veryHighDescription);

        String locDescriptionPrefix = "Lines of code in ";
        metricsList.addSystemMetric().id(negligiblePrefix + "LOC").value(overallDistribution.getNegligibleRiskValue())
                .description(locDescriptionPrefix + negligibleDescription);
        metricsList.addSystemMetric().id(lowPrefix + "LOC").value(overallDistribution.getLowRiskValue())
                .description(locDescriptionPrefix + lowDescription);
        metricsList.addSystemMetric().id(mediumPrefix + "LOC").value(overallDistribution.getMediumRiskValue())
                .description(locDescriptionPrefix + mediumDescription);
        metricsList.addSystemMetric().id(highPrefix + "LOC").value(overallDistribution.getHighRiskValue())
                .description(locDescriptionPrefix + highDescription);
        metricsList.addSystemMetric().id(veryHighPrefix + "LOC").value(overallDistribution.getVeryHighRiskValue())
                .description(locDescriptionPrefix + veryHighDescription);
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
