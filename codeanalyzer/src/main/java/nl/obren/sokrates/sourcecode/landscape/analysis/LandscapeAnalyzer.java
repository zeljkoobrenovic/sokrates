/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LandscapeAnalyzer {
    private static final Log LOG = LogFactory.getLog(LandscapeAnalyzer.class);

    private File landscapeConfigurationFile;
    private LandscapeConfiguration landscapeConfiguration;

    public LandscapeAnalysisResults analyze(File landscapeConfigFile) {
        this.landscapeConfigurationFile = landscapeConfigFile;

        LandscapeAnalysisResults landscapeAnalysisResults = new LandscapeAnalysisResults();

        Set<String> repositoryNames = new HashSet<>();

        try {
            String json = FileUtils.readFileToString(landscapeConfigurationFile, StandardCharsets.UTF_8);
            File infoFile = new File(landscapeConfigFile.getParentFile(), "info.json");
            String jsonInfo = infoFile.exists() ? FileUtils.readFileToString(infoFile, StandardCharsets.UTF_8) : null;
            this.landscapeConfiguration = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);
            if (jsonInfo != null) {
                LandscapeInfo info = (LandscapeInfo) new JsonMapper().getObject(jsonInfo, LandscapeInfo.class);
                landscapeConfiguration.setSubLandscapes(info.getSubLandscapes());
                landscapeConfiguration.setRepositories(info.getRepositories());
            }
            landscapeAnalysisResults.setConfiguration(landscapeConfiguration);
            landscapeConfiguration.getRepositories().forEach(link -> {
                LOG.info("Analysing " + link.getAnalysisResultsPath() + "...");
                CodeAnalysisResults repositoryAnalysisResults = this.getRepositoryAnalysisResults(link);
                if (repositoryAnalysisResults != null) {
                    String repositoryName = repositoryAnalysisResults.getMetadata().getName();
                    if (!landscapeConfiguration.isIncludeOnlyOneRepositoryWithSameName() || !repositoryNames.contains(repositoryName)) {
                        repositoryNames.add(repositoryName);
                        List<String> files = this.getRepositoryFiles(link);
                        landscapeAnalysisResults.getRepositoryAnalysisResults().add(new RepositoryAnalysisResults(link, repositoryAnalysisResults, files));
                        repositoryAnalysisResults.getContributorsAnalysisResults().getContributors().forEach(contributor -> {
                            contributor.getCommitDates().forEach(commitDate -> {
                                if (landscapeAnalysisResults.getFirstCommitDate() == "" || commitDate.compareTo(landscapeAnalysisResults.getFirstCommitDate()) < 0) {
                                    landscapeAnalysisResults.setFirstCommitDate(commitDate);
                                }
                                if (landscapeAnalysisResults.getLatestCommitDate() == "" || commitDate.compareTo(landscapeAnalysisResults.getLatestCommitDate()) > 0) {
                                    landscapeAnalysisResults.setLatestCommitDate(commitDate);
                                    DateUtils.setLatestCommitDate(commitDate);
                                }
                            });
                        });
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        updatePeopleDependencies(landscapeAnalysisResults);

        return landscapeAnalysisResults;
    }

    private void updatePeopleDependencies(LandscapeAnalysisResults landscapeAnalysisResults) {
        LOG.info("Updating people dependencies....");
        List<ContributorRepositories> contributors = landscapeAnalysisResults.getContributors();
        LOG.info("Updating people dependencies in past 30d....");
        List<ComponentDependency> peopleDependencies30Days = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 30);
        landscapeAnalysisResults.setPeopleDependencies30Days(peopleDependencies30Days);
        List<ComponentDependency> peopleRepositoryDependencies30Days = ContributorConnectionUtils.getPeopleRepositoryDependencies(contributors, 0, 30);
        landscapeAnalysisResults.setPeopleRepositoryDependencies30Days(peopleRepositoryDependencies30Days);
        LOG.info("Updating people dependencies in past 90d....");
        List<ComponentDependency> peopleDependencies90Days = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 90);
        landscapeAnalysisResults.setPeopleDependencies90Days(peopleDependencies90Days);
        LOG.info("Updating people dependencies in past 180d....");
        List<ComponentDependency> peopleDependencies180Days = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 180);
        landscapeAnalysisResults.setPeopleDependencies180Days(peopleDependencies180Days);

        List<ContributorConnections> connectionsViaRepositories30Days = ContributorConnectionUtils.getConnectionsViaRepositories(contributors, peopleDependencies30Days, 0, 30);
        List<ContributorConnections> connectionsViaRepositories90Days = ContributorConnectionUtils.getConnectionsViaRepositories(contributors, peopleDependencies90Days, 0, 90);
        List<ContributorConnections> connectionsViaRepositories180Days = ContributorConnectionUtils.getConnectionsViaRepositories(contributors, peopleDependencies180Days, 0, 180);

        landscapeAnalysisResults.setConnectionsViaRepositories30Days(connectionsViaRepositories30Days);
        landscapeAnalysisResults.setConnectionsViaRepositories90Days(connectionsViaRepositories90Days);
        landscapeAnalysisResults.setConnectionsViaRepositories180Days(connectionsViaRepositories180Days);

        landscapeAnalysisResults.setcIndex30Days(ContributorConnectionUtils.getCIndex(connectionsViaRepositories30Days));
        landscapeAnalysisResults.setpIndex30Days(ContributorConnectionUtils.getPIndex(connectionsViaRepositories30Days));

        landscapeAnalysisResults.setcIndex90Days(ContributorConnectionUtils.getCIndex(connectionsViaRepositories90Days));
        landscapeAnalysisResults.setpIndex90Days(ContributorConnectionUtils.getPIndex(connectionsViaRepositories90Days));

        landscapeAnalysisResults.setcIndex180Days(ContributorConnectionUtils.getCIndex(connectionsViaRepositories180Days));
        landscapeAnalysisResults.setpIndex180Days(ContributorConnectionUtils.getPIndex(connectionsViaRepositories180Days));

        landscapeAnalysisResults.setcMean30Days(ContributorConnectionUtils.getCMean(connectionsViaRepositories30Days));
        landscapeAnalysisResults.setpMean30Days(ContributorConnectionUtils.getPMean(connectionsViaRepositories30Days));

        landscapeAnalysisResults.setcMean90Days(ContributorConnectionUtils.getCMean(connectionsViaRepositories90Days));
        landscapeAnalysisResults.setpMean90Days(ContributorConnectionUtils.getPMean(connectionsViaRepositories90Days));

        landscapeAnalysisResults.setcMean180Days(ContributorConnectionUtils.getCMean(connectionsViaRepositories180Days));
        landscapeAnalysisResults.setpMean180Days(ContributorConnectionUtils.getPMean(connectionsViaRepositories180Days));

        landscapeAnalysisResults.setcMedian30Days(ContributorConnectionUtils.getCMedian(connectionsViaRepositories30Days));
        landscapeAnalysisResults.setpMedian30Days(ContributorConnectionUtils.getPMedian(connectionsViaRepositories30Days));

        landscapeAnalysisResults.setcMedian90Days(ContributorConnectionUtils.getCMedian(connectionsViaRepositories90Days));
        landscapeAnalysisResults.setpMedian90Days(ContributorConnectionUtils.getPMedian(connectionsViaRepositories90Days));

        landscapeAnalysisResults.setcMedian180Days(ContributorConnectionUtils.getCMedian(connectionsViaRepositories180Days));
        landscapeAnalysisResults.setpMedian180Days(ContributorConnectionUtils.getPMedian(connectionsViaRepositories180Days));

        landscapeAnalysisResults.setC2cConnectionsCount30Days(peopleDependencies30Days.size());
        landscapeAnalysisResults.setC2pConnectionsCount30Days(connectionsViaRepositories30Days.stream().mapToInt(c -> c.getConnectionsCount()).sum());

        LOG.info("Adding history....");
        addHistory(landscapeAnalysisResults);
        LOG.info("Done updating people dependencies.");
    }

    private void addHistory(LandscapeAnalysisResults landscapeAnalysisResults) {
        List<ContributorRepositories> contributors = landscapeAnalysisResults.getContributors();
        for (int i = 0; i < 12; i++) {
            int daysAgo1 = i * 30;
            int daysAgo2 = (i + 1) * 30;
            List<ComponentDependency> peopleDependencies30Days = ContributorConnectionUtils.getPeopleDependencies(contributors, daysAgo1, daysAgo2);
            List<ContributorConnections> connectionsViaRepositories30Days = ContributorConnectionUtils.getConnectionsViaRepositories(contributors, peopleDependencies30Days, daysAgo1, daysAgo2);
            landscapeAnalysisResults.getcIndex30DaysHistory().add(ContributorConnectionUtils.getCIndex(connectionsViaRepositories30Days));
            landscapeAnalysisResults.getpIndex30DaysHistory().add(ContributorConnectionUtils.getPIndex(connectionsViaRepositories30Days));
            landscapeAnalysisResults.getcMean30DaysHistory().add(ContributorConnectionUtils.getCMean(connectionsViaRepositories30Days));
            landscapeAnalysisResults.getpMean30DaysHistory().add(ContributorConnectionUtils.getPMean(connectionsViaRepositories30Days));
            landscapeAnalysisResults.getcMedian30DaysHistory().add(ContributorConnectionUtils.getCMedian(connectionsViaRepositories30Days));
            landscapeAnalysisResults.getpMedian30DaysHistory().add(ContributorConnectionUtils.getPMedian(connectionsViaRepositories30Days));
            int connectionSum = connectionsViaRepositories30Days.stream().mapToInt(c -> c.getConnectionsCount()).sum();
            landscapeAnalysisResults.getConnectionsViaRepositories30DaysCountHistory().add((double) connectionSum);
            landscapeAnalysisResults.getPeopleDependenciesCount30DaysHistory().add((double) peopleDependencies30Days.size());
            landscapeAnalysisResults.getActiveContributors30DaysHistory().add((double) ContributorConnectionUtils.getContributorsActiveInPeriodCount(contributors, daysAgo1, daysAgo2));
        }
    }

    private CodeAnalysisResults getRepositoryAnalysisResults(SokratesRepositoryLink sokratesRepositoryLink) {
        return getRepositoryAnalysisResults(getRepositoryAnalysisFile(sokratesRepositoryLink));
    }

    private File getRepositoryAnalysisFile(SokratesRepositoryLink sokratesRepositoryLink) {
        String analysisRoot = landscapeConfiguration.getAnalysisRoot();
        if (analysisRoot.startsWith(".")) {
            analysisRoot = Paths.get(landscapeConfigurationFile.getParentFile().getParentFile().getPath(), analysisRoot).toFile().getPath();
        }
        return Paths.get(analysisRoot, sokratesRepositoryLink.getAnalysisResultsPath()).toFile();
    }

    private CodeAnalysisResults getRepositoryAnalysisResults(File repositoryAnalysisResultsFile) {
        try {
            String json = FileUtils.readFileToString(repositoryAnalysisResultsFile, StandardCharsets.UTF_8);
            CodeAnalysisResults codeAnalysisResults = (CodeAnalysisResults) new JsonMapper().getObject(json, CodeAnalysisResults.class);
            // codeAnalysisResults.setUnitsAnalysisResults(new UnitsAnalysisResults());
            // codeAnalysisResults.setFilesAnalysisResults(new FilesAnalysisResults());
            codeAnalysisResults.setAllDependencies(new ArrayList<>());
            codeAnalysisResults.setFilesExcludedByExtension(new ArrayList<>());
            if (codeAnalysisResults.getMainAspectAnalysisResults().getAspect() != null) {
                codeAnalysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles().clear();
                codeAnalysisResults.getTestAspectAnalysisResults().getAspect().getSourceFiles().clear();
                codeAnalysisResults.getGeneratedAspectAnalysisResults().getAspect().getSourceFiles().clear();
                codeAnalysisResults.getBuildAndDeployAspectAnalysisResults().getAspect().getSourceFiles().clear();
                codeAnalysisResults.getOtherAspectAnalysisResults().getAspect().getSourceFiles().clear();
            }

            return codeAnalysisResults;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private List<String> getRepositoryFiles(SokratesRepositoryLink sokratesRepositoryLink) {
        List<String> files = new ArrayList<>();

        files.addAll(getRepositoryFilesByScope(sokratesRepositoryLink, "aspect_main.txt"));
        files.addAll(getRepositoryFilesByScope(sokratesRepositoryLink, "aspect_test.txt"));
        files.addAll(getRepositoryFilesByScope(sokratesRepositoryLink, "aspect_generated.txt"));
        files.addAll(getRepositoryFilesByScope(sokratesRepositoryLink, "aspect_build_and_deployment.txt"));
        files.addAll(getRepositoryFilesByScope(sokratesRepositoryLink, "aspect_other.txt"));
        files.addAll(getRepositoryFilesByScope(sokratesRepositoryLink, "excluded_files_ignored_extensions.txt"));
        files.addAll(getRepositoryFilesByScope(sokratesRepositoryLink, "excluded_files_ignored_rules.txt"));

        return files;
    }

    private List<String> getRepositoryFilesByScope(SokratesRepositoryLink sokratesRepositoryLink, String scopeFile) {
        try {
            File txtDataFolder = new File(getRepositoryAnalysisFile(sokratesRepositoryLink).getParentFile(), "text");
            File mainFile = new File(txtDataFolder, scopeFile);
            if (mainFile.exists()) {
                LOG.info(mainFile.getPath());
                return FileUtils.readLines(mainFile, StandardCharsets.UTF_8).stream()
                        .skip(1)
                        .map(line -> line.replaceAll("\t.*", ""))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

}
