/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.age.FileLastModifiedInfo;
import nl.obren.sokrates.sourcecode.age.utils.GitLsFileUtil;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileAgeDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAgeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

public class FileAgeAnalyzer extends Analyzer {
    private CodeConfiguration codeConfiguration;
    private MetricsList metricsList;
    private FilesAgeAnalysisResults analysisResults;

    public FileAgeAnalyzer(CodeAnalysisResults results) {
        this.analysisResults = results.getFilesAgeAnalysisResults();
        this.codeConfiguration = results.getCodeConfiguration();
        this.metricsList = results.getMetricsList();
    }

    public void analyze() {
        String filesAgeImportPath = codeConfiguration.getAnalysis().getFilesAgeImportPath();
        if (StringUtils.isNotBlank(filesAgeImportPath)) {
            List<FileLastModifiedInfo> ages = GitLsFileUtil.importGitLsFilesExport(new File(filesAgeImportPath));
            if (ages.size() > 0) {
                enrichFilesWithAge(ages);
                analyzeFilesAge();
            }
        }
    }

    private void analyzeFilesAge() {
        SourceFileAgeDistribution fileAgeDistribution = new SourceFileAgeDistribution();
        List<SourceFile> allFiles = codeConfiguration.getMain().getSourceFiles();
        List<SourceFile> sourceFiles = allFiles;
        SourceFileAgeDistribution overallDistribution = fileAgeDistribution.getOverallDistribution(sourceFiles);
        analysisResults.setOverallFileAgeDistribution(overallDistribution);
        analysisResults.setFileAgeDistributionPerExtension(SourceFileAgeDistribution.getFileAgeRiskDistributionPerExtension(sourceFiles));
        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            FileAgeDistributionPerLogicalDecomposition distributionPerLogicalDecomposition = new FileAgeDistributionPerLogicalDecomposition();
            distributionPerLogicalDecomposition.setName(logicalDecomposition.getName());
            distributionPerLogicalDecomposition.setFileAgeDistributionPerComponent(SourceFileAgeDistribution.getFileAgeRiskDistributionPerComponent(sourceFiles, logicalDecomposition));
            analysisResults.getFileAgeDistributionPerLogicalDecomposition().add(distributionPerLogicalDecomposition);
        });
        analysisResults.setAllFiles(allFiles);
        addOldestFiles(allFiles, analysisResults, 50);
        addYoungestFiles(allFiles, analysisResults, 50);

        addMetrics(overallDistribution);
    }

    private void enrichFilesWithAge(List<FileLastModifiedInfo> ages) {
        codeConfiguration.getMain().getSourceFiles().forEach(sourceFile -> {
            Optional<FileLastModifiedInfo> any = ages.stream().filter(f -> f.getPath().equalsIgnoreCase(sourceFile.getRelativePath())).findAny();
            if (any.isPresent()) {
                sourceFile.setAgeInDays(any.get().ageInDays());
            }
        });
    }

    private void addMetrics(SourceFileAgeDistribution overallDistribution) {
        metricsList.addSystemMetric().id("AGE_OF_FILES_IN_DAYS_0_30").value(overallDistribution.getLowRiskCount())
                .description("Number of files 30 or less days old");
        metricsList.addSystemMetric().id("AGE_OF_FILES_IN_DAYS_31_90").value(overallDistribution.getMediumRiskCount())
                .description("Number of files 31 to 90 days old");
        metricsList.addSystemMetric().id("AGE_OF_FILES_IN_DAYS_91_180").value(overallDistribution.getHighRiskCount())
                .description("Number of files 91 to 180 days old");
        metricsList.addSystemMetric().id("AGE_OF_FILES_IN_DAYS_181_PLUS").value(overallDistribution.getVeryHighRiskCount())
                .description("Number of files more than 181 days old");

        metricsList.addSystemMetric().id("LINES_OF_CODE_IN_FILES_AGE_0_30").value(overallDistribution.getLowRiskCount())
                .description("Lines of code in all files 30 or less days old");
        metricsList.addSystemMetric().id("LINES_OF_CODE_AGE_31_90").value(overallDistribution.getMediumRiskCount())
                .description("Lines of code in all files 31 to 60 days old");
        metricsList.addSystemMetric().id("LINES_OF_CODE_AGE_91_180").value(overallDistribution.getHighRiskCount())
                .description("Lines of code in files 91 to 180 days old");
        metricsList.addSystemMetric().id("LINES_OF_CODE_AGE_181_PLUS").value(overallDistribution.getVeryHighRiskCount())
                .description("Lines of code in all files more than 181 days old");
    }

    private void addOldestFiles(List<SourceFile> sourceFiles, FilesAgeAnalysisResults filesAgeAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getAgeInDays(), o1.getAgeInDays()));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesAgeAnalysisResults.getOldestFiles().add(sourceFile);
        });
    }

    private void addYoungestFiles(List<SourceFile> sourceFiles, FilesAgeAnalysisResults filesAgeAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(SourceFile::getAgeInDays));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesAgeAnalysisResults.getYoungestFiles().add(sourceFile);
        });
    }
}
