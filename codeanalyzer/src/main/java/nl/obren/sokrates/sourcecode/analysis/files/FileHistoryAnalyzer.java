/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileAgeDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.filehistory.*;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileChangeDistribution;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution.Types.FIRST_MODIFIED;
import static nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution.Types.LAST_MODIFIED;

public class FileHistoryAnalyzer extends Analyzer {
    private static final Log LOG = LogFactory.getLog(FileHistoryAnalyzer.class);

    private CodeConfiguration codeConfiguration;
    private MetricsList metricsList;
    private File sokratesFolder;
    private FilesHistoryAnalysisResults analysisResults;

    public FileHistoryAnalyzer(CodeAnalysisResults results, File sokratesFolder) {
        this.analysisResults = results.getFilesHistoryAnalysisResults();
        this.codeConfiguration = results.getCodeConfiguration();
        this.metricsList = results.getMetricsList();
        this.sokratesFolder = sokratesFolder;
    }

    public void analyze() {
        if (codeConfiguration.getFileHistoryAnalysis().filesHistoryImportPathExists(sokratesFolder)) {

            List<FileModificationHistory> history = codeConfiguration.getFileHistoryAnalysis().getHistory(sokratesFolder);
            analysisResults.setHistory(history);

            if (history.size() > 0) {
                summarize(history);

                LOG.info("Enriching files with age...");
                enrichFilesWithAge(history);
                LOG.info("Analyzing file age...");
                analyzeFilesAge();
                int maxDays = codeConfiguration.getAnalysis().getMaxTemporalDependenciesDepthDays();
                if (maxDays > 180) {
                    LOG.info("Analyzing files changed together (all time=" + maxDays + " days)...");
                    analyzeFilesChangedTogether(history);
                }
                if (maxDays >= 30) {
                    LOG.info("Analyzing files changed together in past 30 days...");
                    analyzeFilesChangedTogether30Days(history);
                }
                if (maxDays >= 90) {
                    LOG.info("Analyzing files changed together in past 90 days...");
                    analyzeFilesChangedTogether90Days(history);
                }
                if (maxDays >= 180) {
                    LOG.info("Analyzing files changed together in past 180 days...");
                    analyzeFilesChangedTogether180Days(history);
                }
            }
        }
    }

    private void summarize(List<FileModificationHistory> history) {
        FileHistoryComponentsHelper helper = new FileHistoryComponentsHelper();
        List<String> uniqueDates = helper.getUniqueDates(history);

        if (uniqueDates.size() > 1) {
            String firstDateString = uniqueDates.get(0);
            String latestDateString = uniqueDates.get(uniqueDates.size() - 1);

            Date firstDate = FileHistoryUtils.getDateFromString(firstDateString);
            Date latestDate = FileHistoryUtils.getDateFromString(latestDateString);

            int daysBetween = FileHistoryUtils.daysBetween(firstDate, latestDate);
            Date today = new Date();
            int totalAge = FileHistoryUtils.daysBetween(firstDate, today);

            int weeks = daysBetween / 7;
            int estimatedWorkingDays = weeks * 5;

            int activeDays = uniqueDates.size();

            analysisResults.setFirstDate(firstDateString);
            analysisResults.setLatestDate(latestDateString);
            analysisResults.setAgeInDays(totalAge);
            analysisResults.setDaysBetweenFirstAndLastDate(daysBetween);
            analysisResults.setWeeks(weeks);
            analysisResults.setEstimatedWorkindDays(estimatedWorkingDays);
            analysisResults.setActiveDays(activeDays);

            metricsList.addMetric().id("FILE_CHANGE_HISTORY_TOTAL_AGE_DAYS")
                    .description("The age of the repository in days")
                    .value(totalAge);

            metricsList.addMetric().id("FILE_CHANGE_HISTORY_ACTIVE_DAYS")
                    .description("The number of days with at least one file change")
                    .value(activeDays);

            metricsList.addMetric().id("FILE_CHANGE_HISTORY_WEEKS")
                    .description("The number of weeks")
                    .value(weeks);

            metricsList.addMetric().id("FILE_CHANGE_HISTORY_ESTIMATED_WORKING_DAYS")
                    .description("The number of estimated working days in the period")
                    .value(estimatedWorkingDays);
        }
    }

    private void analyzeFilesChangedTogether(List<FileModificationHistory> history) {
        FilePairsChangedTogether filePairsChangedTogether = new FilePairsChangedTogether(codeConfiguration.getAnalysis().getMaxTemporalDependenciesDepthDays());
        filePairsChangedTogether.populate(codeConfiguration.getMain(), history);
        analysisResults.setFilePairsChangedTogether(filePairsChangedTogether.getFilePairsList());
    }

    private void analyzeFilesChangedTogether30Days(List<FileModificationHistory> history) {
        FilePairsChangedTogether filePairsChangedTogether = new FilePairsChangedTogether(30);
        filePairsChangedTogether.populate(codeConfiguration.getMain(), history);
        analysisResults.setFilePairsChangedTogether30Days(filePairsChangedTogether.getFilePairsList());
    }

    private void analyzeFilesChangedTogether90Days(List<FileModificationHistory> history) {
        FilePairsChangedTogether filePairsChangedTogether = new FilePairsChangedTogether(90);
        filePairsChangedTogether.populate(codeConfiguration.getMain(), history);
        analysisResults.setFilePairsChangedTogether90Days(filePairsChangedTogether.getFilePairsList());
    }

    private void analyzeFilesChangedTogether180Days(List<FileModificationHistory> history) {
        FilePairsChangedTogether filePairsChangedTogether = new FilePairsChangedTogether(180);
        filePairsChangedTogether.populate(codeConfiguration.getMain(), history);
        analysisResults.setFilePairsChangedTogether180Days(filePairsChangedTogether.getFilePairsList());
    }

    private void analyzeFilesAge() {
        List<SourceFile> allFiles = codeConfiguration.getMain().getSourceFiles();
        List<SourceFile> sourceFiles = allFiles;

        Thresholds fileAgeThresholds = codeConfiguration.getAnalysis().getFileAgeThresholds();
        Thresholds fileUpdateFrequencyThresholds = codeConfiguration.getAnalysis().getFileUpdateFrequencyThresholds();
        Thresholds fileContributorCountThresholds = codeConfiguration.getAnalysis().getFileContributorsCountThresholds();

        if (sourceFiles != null) {
            sourceFiles.stream().filter(f -> f.getFileModificationHistory() == null).forEach(sourceFile -> {
                analysisResults.setFilesWithoutCommitHistoryCount(analysisResults.getFilesWithoutCommitHistoryCount() + 1);
                analysisResults.setFilesWithoutCommitHistoryLinesOfCode(analysisResults.getFilesWithoutCommitHistoryLinesOfCode() + sourceFile.getLinesOfCode());
            });
        }

        SourceFileAgeDistribution lastModifiedDistribution = new SourceFileAgeDistribution(fileAgeThresholds, LAST_MODIFIED).getOverallLastModifiedDistribution(sourceFiles);
        SourceFileAgeDistribution firstModifiedDistribution = new SourceFileAgeDistribution(fileAgeThresholds, FIRST_MODIFIED).getOverallFirstModifiedDistribution(sourceFiles);
        SourceFileChangeDistribution changeDistribution = new SourceFileChangeDistribution(fileUpdateFrequencyThresholds).getOverallDistribution(sourceFiles);
        SourceFileChangeDistribution contributorCountDistribution = new SourceFileChangeDistribution(fileContributorCountThresholds).getOverallContributorsCountDistribution(sourceFiles);

        analysisResults.setOverallFileLastModifiedDistribution(lastModifiedDistribution);
        analysisResults.setOverallFileFirstModifiedDistribution(firstModifiedDistribution);
        analysisResults.setOverallFileChangeDistribution(changeDistribution);
        analysisResults.setOverallContributorsCountDistribution(contributorCountDistribution);

        analysisResults.setChangeDistributionPerExtension(
                new SourceFileChangeDistribution(fileUpdateFrequencyThresholds).getDistributionPerExtension(sourceFiles));
        analysisResults.setFirstModifiedDistributionPerExtension(
                new SourceFileAgeDistribution(fileAgeThresholds, FIRST_MODIFIED).getFileAgeRiskDistributionPerExtension(sourceFiles));
        analysisResults.setLastModifiedDistributionPerExtension(
                new SourceFileAgeDistribution(fileAgeThresholds, LAST_MODIFIED).getFileAgeRiskDistributionPerExtension(sourceFiles));

        codeConfiguration.getLogicalDecompositions().forEach(logicalDecomposition -> {
            addLogicalDecompositions(logicalDecomposition);
        });

        analysisResults.setAllFiles(allFiles);

        int maxTopListSize = codeConfiguration.getAnalysis().getMaxTopListSize();
        addOldestFiles(allFiles, analysisResults, maxTopListSize);
        addYoungestFiles(allFiles, analysisResults, maxTopListSize);
        addMostRecentlyChangedFiles(allFiles, analysisResults, maxTopListSize);
        addMostPreviouslyChangedFiles(allFiles, analysisResults, maxTopListSize);
        addMostChangedFiles(allFiles, analysisResults, maxTopListSize);
        addFilesWithMostContributors(allFiles, analysisResults, maxTopListSize);
        addFilesWithLeastContributors(allFiles, analysisResults, maxTopListSize);

        addMetrics(lastModifiedDistribution);
    }

    private void addLogicalDecompositions(LogicalDecomposition logicalDecomposition) {
        FileAgeDistributionPerLogicalDecomposition change = new FileAgeDistributionPerLogicalDecomposition();
        change.setName(logicalDecomposition.getName());
        change.setDistributionPerComponent(
                new SourceFileChangeDistribution(codeConfiguration.getAnalysis().getFileUpdateFrequencyThresholds()).getRiskDistributionPerComponent(logicalDecomposition));

        analysisResults.getChangeDistributionPerLogicalDecomposition().add(change);

        Thresholds thresholds = codeConfiguration.getAnalysis().getFileAgeThresholds();

        FileAgeDistributionPerLogicalDecomposition firstModified = new FileAgeDistributionPerLogicalDecomposition();
        firstModified.setName(logicalDecomposition.getName());
        firstModified.setDistributionPerComponent(
                new SourceFileAgeDistribution(thresholds, FIRST_MODIFIED).getFileAgeRiskDistributionPerComponent(logicalDecomposition));

        analysisResults.getFirstModifiedDistributionPerLogicalDecomposition().add(firstModified);

        FileAgeDistributionPerLogicalDecomposition lastModified = new FileAgeDistributionPerLogicalDecomposition();
        lastModified.setName(logicalDecomposition.getName());

        lastModified.setDistributionPerComponent(
                new SourceFileAgeDistribution(thresholds, LAST_MODIFIED).getFileAgeRiskDistributionPerComponent(logicalDecomposition));

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
        metricsList.addSystemMetric().id("FILE_AGE_NEGLIGIBLE_RISK_COUNT").value(overallDistribution.getNegligibleRiskCount())
                .description("Number of files " + overallDistribution.getNegligibleRiskLabel() + " days old");
        metricsList.addSystemMetric().id("FILE_AGE_LOW_RISK_COUNT").value(overallDistribution.getLowRiskCount())
                .description("Number of files " + overallDistribution.getLowRiskLabel() + " days old");
        metricsList.addSystemMetric().id("FILE_AGE_MEDIUM_RISK_COUNT").value(overallDistribution.getMediumRiskCount())
                .description("Number of files " + overallDistribution.getMediumRiskLabel() + " days old");
        metricsList.addSystemMetric().id("FILE_AGE_HIGH_RISK_COUNT").value(overallDistribution.getHighRiskCount())
                .description("Number of files " + overallDistribution.getHighRiskLabel() + " days old");
        metricsList.addSystemMetric().id("FILE_AGE_VERY_HIGH_RISK_COUNT").value(overallDistribution.getVeryHighRiskCount())
                .description("Number of files " + overallDistribution.getVeryHighRiskLabel() + " days old");

        metricsList.addSystemMetric().id("FILE_AGE_NEGLIGIBLE_RISK_LOC").value(overallDistribution.getNegligibleRiskValue())
                .description("Number of files " + overallDistribution.getNegligibleRiskLabel() + " days old");
        metricsList.addSystemMetric().id("FILE_AGE_LOW_RISK_LOC").value(overallDistribution.getLowRiskValue())
                .description("Number of files " + overallDistribution.getLowRiskLabel() + " days old");
        metricsList.addSystemMetric().id("FILE_AGE_MEDIUM_RISK_LOC").value(overallDistribution.getMediumRiskValue())
                .description("Number of files " + overallDistribution.getMediumRiskLabel() + " days old");
        metricsList.addSystemMetric().id("FILE_AGE_HIGH_RISK_LOC").value(overallDistribution.getHighRiskValue())
                .description("Number of files " + overallDistribution.getHighRiskLabel() + " days old");
        metricsList.addSystemMetric().id("FILE_AGE_VERY_HIGH_RISK_LOC").value(overallDistribution.getVeryHighRiskValue())
                .description("Number of files " + overallDistribution.getVeryHighRiskLabel() + " days old");
    }

    private void addOldestFiles(List<SourceFile> sourceFiles, FilesHistoryAnalysisResults filesHistoryAnalysisResults, int sampleSize) {
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
            filesHistoryAnalysisResults.getOldestFiles().add(sourceFile);
        });
    }

    private void addYoungestFiles(List<SourceFile> sourceFiles, FilesHistoryAnalysisResults filesHistoryAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : o.getFileModificationHistory().daysSinceFirstUpdate())));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesHistoryAnalysisResults.getYoungestFiles().add(sourceFile);
        });
    }

    private void addMostRecentlyChangedFiles(List<SourceFile> sourceFiles, FilesHistoryAnalysisResults filesHistoryAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : o.getFileModificationHistory().daysSinceLatestUpdate())));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesHistoryAnalysisResults.getMostRecentlyChangedFiles().add(sourceFile);
        });
    }

    private void addMostPreviouslyChangedFiles(List<SourceFile> sourceFiles, FilesHistoryAnalysisResults filesHistoryAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : o.getFileModificationHistory().daysSinceLatestUpdate())));
        Collections.reverse(files);
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesHistoryAnalysisResults.getMostPreviouslyChangedFiles().add(sourceFile);
        });
    }

    private void addMostChangedFiles(List<SourceFile> sourceFiles, FilesHistoryAnalysisResults filesHistoryAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, (o1, o2) -> Integer.compare(o2.getLinesOfCode(), o1.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : o.getFileModificationHistory().getDates().size())));
        Collections.reverse(files);
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesHistoryAnalysisResults.getMostChangedFiles().add(sourceFile);
        });
    }

    private void addFilesWithMostContributors(List<SourceFile> sourceFiles, FilesHistoryAnalysisResults filesHistoryAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, Comparator.comparingInt(o -> (o.getFileModificationHistory() == null ? 0 : -o.getFileModificationHistory().getDates().size())));
        Collections.sort(files, (o1, o2) -> getCountContributors(o2) - getCountContributors(o1));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesHistoryAnalysisResults.getFilesWithMostContributors().add(sourceFile);
        });
    }

    private void addFilesWithLeastContributors(List<SourceFile> sourceFiles, FilesHistoryAnalysisResults filesHistoryAnalysisResults, int sampleSize) {
        List<SourceFile> files = new ArrayList<>(sourceFiles);
        Collections.sort(files, Comparator.comparingInt(o -> -o.getLinesOfCode()));
        Collections.sort(files, Comparator.comparingInt(this::getCountContributors));
        int index[] = {0};
        files.forEach(sourceFile -> {
            if (index[0]++ >= sampleSize) {
                return;
            }
            filesHistoryAnalysisResults.getFilesWithLeastContributors().add(sourceFile);
        });
    }

    private int getCountContributors(SourceFile sourceFile) {
        return sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().countContributors() : 0;
    }
}
