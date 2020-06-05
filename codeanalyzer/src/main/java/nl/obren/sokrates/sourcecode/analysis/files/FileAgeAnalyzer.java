/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.age.FileModificationHistory;
import nl.obren.sokrates.sourcecode.age.utils.GitLsFileUtil;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileAgeDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAgeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileChangeDistribution;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

import static nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution.Types.FIRST_MODIFIED;
import static nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution.Types.LAST_MODIFIED;

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
        String filesAgeImportPath = codeConfiguration.getAnalysis().getFilesHistoryImportPath();
        if (StringUtils.isNotBlank(filesAgeImportPath)) {
            List<FileModificationHistory> ages = GitLsFileUtil.importGitLsFilesExport(new File(filesAgeImportPath));
            if (ages.size() > 0) {
                enrichFilesWithAge(ages);
                analyzeFilesAge();
            }
        }
    }

    private void analyzeFilesAge() {
        List<SourceFile> allFiles = codeConfiguration.getMain().getSourceFiles();
        List<SourceFile> sourceFiles = allFiles;
        SourceFileAgeDistribution lastModifiedDistribution = new SourceFileAgeDistribution(LAST_MODIFIED).getOverallLastModifiedDistribution(sourceFiles);
        SourceFileAgeDistribution firstModifiedDistribution = new SourceFileAgeDistribution(LAST_MODIFIED).getOverallFirstModifiedDistribution(sourceFiles);
        SourceFileChangeDistribution changeDistribution = new SourceFileChangeDistribution().getOverallDistribution(sourceFiles);

        analysisResults.setOverallFileLastModifiedDistribution(lastModifiedDistribution);
        analysisResults.setOverallFileFirstModifiedDistribution(firstModifiedDistribution);
        analysisResults.setOverallFileChangeDistribution(changeDistribution);

        analysisResults.setChangeDistributionPerExtension(
                new SourceFileChangeDistribution().getDistributionPerExtension(sourceFiles));
        analysisResults.setFirstModifiedDistributionPerExtension(
                new SourceFileAgeDistribution(FIRST_MODIFIED).getFileAgeRiskDistributionPerExtension(sourceFiles));
        analysisResults.setLastModifiedDistributionPerExtension(
                new SourceFileAgeDistribution(LAST_MODIFIED).getFileAgeRiskDistributionPerExtension(sourceFiles));

        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            addLogicalDecompositions(logicalDecomposition);
        });

        analysisResults.setAllFiles(allFiles);
        addOldestFiles(allFiles, analysisResults, 50);
        addYoungestFiles(allFiles, analysisResults, 50);
        addMostRecentlyChangedFiles(allFiles, analysisResults, 50);
        addMostPreviouslyChangedFiles(allFiles, analysisResults, 50);
        addMostChangedFiles(allFiles, analysisResults, 50);

        addMetrics(lastModifiedDistribution);
    }

    private void addLogicalDecompositions(LogicalDecomposition logicalDecomposition) {
        FileAgeDistributionPerLogicalDecomposition change = new FileAgeDistributionPerLogicalDecomposition();
        change.setName(logicalDecomposition.getName());
        change.setDistributionPerComponent(
                new SourceFileChangeDistribution().getRiskDistributionPerComponent(logicalDecomposition));

        analysisResults.getChangeDistributionPerLogicalDecomposition().add(change);

        FileAgeDistributionPerLogicalDecomposition firstModified = new FileAgeDistributionPerLogicalDecomposition();
        firstModified.setName(logicalDecomposition.getName());
        firstModified.setDistributionPerComponent(
                new SourceFileAgeDistribution(FIRST_MODIFIED).getFileAgeRiskDistributionPerComponent(logicalDecomposition));

        analysisResults.getFirstModifiedDistributionPerLogicalDecomposition().add(firstModified);

        FileAgeDistributionPerLogicalDecomposition lastModified = new FileAgeDistributionPerLogicalDecomposition();
        lastModified.setName(logicalDecomposition.getName());

        lastModified.setDistributionPerComponent(
                new SourceFileAgeDistribution(LAST_MODIFIED).getFileAgeRiskDistributionPerComponent(logicalDecomposition));

        analysisResults.getLastModifiedDistributionPerLogicalDecomposition().add(lastModified);
    }

    private void enrichFilesWithAge(List<FileModificationHistory> ages) {
        codeConfiguration.getMain().getSourceFiles().forEach(sourceFile -> {
            Optional<FileModificationHistory> any = ages.stream().filter(f -> f.getPath().equalsIgnoreCase(sourceFile.getRelativePath())).findAny();
            if (any.isPresent()) {
                sourceFile.setFileModificationHistory(any.get());
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
        Collections.sort(files, (o1, o2) ->
                (o2.getFileModificationHistory() == null ? 0 : o2.getFileModificationHistory().daysSinceFirstUpdate()) -
                        (o1.getFileModificationHistory() == null ? 0 : o1.getFileModificationHistory().daysSinceFirstUpdate()));
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
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : o.getFileModificationHistory().daysSinceFirstUpdate())));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesAgeAnalysisResults.getYoungestFiles().add(sourceFile);
        });
    }

    private void addMostRecentlyChangedFiles(List<SourceFile> sourceFiles, FilesAgeAnalysisResults filesAgeAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : o.getFileModificationHistory().daysSinceLatestUpdate())));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesAgeAnalysisResults.getMostRecentlyChangedFiles().add(sourceFile);
        });
    }

    private void addMostPreviouslyChangedFiles(List<SourceFile> sourceFiles, FilesAgeAnalysisResults filesAgeAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : o.getFileModificationHistory().daysSinceLatestUpdate())));
        Collections.reverse(files);
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesAgeAnalysisResults.getMostPreviouslyChangedFiles().add(sourceFile);
        });
    }

    private void addMostChangedFiles(List<SourceFile> sourceFiles, FilesAgeAnalysisResults filesAgeAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : o.getFileModificationHistory().getDates().size())));
        Collections.reverse(files);
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesAgeAnalysisResults.getMostChangedFiles().add(sourceFile);
        });
    }
}
