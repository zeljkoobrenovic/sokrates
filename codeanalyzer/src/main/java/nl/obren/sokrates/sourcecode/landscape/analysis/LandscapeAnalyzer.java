/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.SokratesProjectLink;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LandscapeAnalyzer {
    private File landscapeConfigurationFile;
    private LandscapeConfiguration landscapeConfiguration;

    public LandscapeAnalysisResults analyze(File landscapeConfigFile) {
        this.landscapeConfigurationFile = landscapeConfigFile;

        LandscapeAnalysisResults landscapeAnalysisResults = new LandscapeAnalysisResults();

        try {
            String json = FileUtils.readFileToString(landscapeConfigurationFile, StandardCharsets.UTF_8);
            this.landscapeConfiguration = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);
            landscapeAnalysisResults.setConfiguration(landscapeConfiguration);
            landscapeConfiguration.getProjects().forEach(link -> {
                CodeAnalysisResults projectAnalysisResults = this.getProjectAnalysisResults(link);
                if (projectAnalysisResults != null) {
                    landscapeAnalysisResults.getProjectAnalysisResults().add(new ProjectAnalysisResults(link, projectAnalysisResults));
                }
                projectAnalysisResults.getContributorsAnalysisResults().getContributors().forEach(contributor -> {
                    contributor.getCommitDates().forEach(commitDate -> {
                        if (landscapeAnalysisResults.getLatestCommitDate() == "" || commitDate.compareTo(landscapeAnalysisResults.getLatestCommitDate()) > 0) {
                            landscapeAnalysisResults.setLatestCommitDate(commitDate);
                        }
                    });
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        updatePeopleDependencies(landscapeAnalysisResults);

        return landscapeAnalysisResults;
    }

    private void updatePeopleDependencies(LandscapeAnalysisResults landscapeAnalysisResults) {
        List<ContributorProjects> contributors = landscapeAnalysisResults.getContributors();
        List<ComponentDependency> peopleDependencies30Days = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 30);
        landscapeAnalysisResults.setPeopleDependencies30Days(peopleDependencies30Days);
        List<ComponentDependency> peopleDependencies90Days = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 90);
        landscapeAnalysisResults.setPeopleDependencies90Days(peopleDependencies90Days);
        List<ComponentDependency> peopleDependencies180Days = ContributorConnectionUtils.getPeopleDependencies(contributors, 0, 180);
        landscapeAnalysisResults.setPeopleDependencies180Days(peopleDependencies180Days);

        List<ContributorConnections> connectionsViaProjects30Days = ContributorConnectionUtils.getConnectionsViaProjects(contributors, peopleDependencies30Days, 0, 30);
        List<ContributorConnections> connectionsViaProjects90Days = ContributorConnectionUtils.getConnectionsViaProjects(contributors, peopleDependencies90Days, 0, 90);
        List<ContributorConnections> connectionsViaProjects180Days = ContributorConnectionUtils.getConnectionsViaProjects(contributors, peopleDependencies180Days, 0, 180);

        landscapeAnalysisResults.setConnectionsViaProjects30Days(connectionsViaProjects30Days);
        landscapeAnalysisResults.setConnectionsViaProjects90Days(connectionsViaProjects90Days);
        landscapeAnalysisResults.setConnectionsViaProjects180Days(connectionsViaProjects180Days);

        landscapeAnalysisResults.setcIndex30Days(ContributorConnectionUtils.getCIndex(connectionsViaProjects30Days));
        landscapeAnalysisResults.setpIndex30Days(ContributorConnectionUtils.getPIndex(connectionsViaProjects30Days));

        landscapeAnalysisResults.setcIndex90Days(ContributorConnectionUtils.getCIndex(connectionsViaProjects90Days));
        landscapeAnalysisResults.setpIndex90Days(ContributorConnectionUtils.getPIndex(connectionsViaProjects90Days));

        landscapeAnalysisResults.setcIndex180Days(ContributorConnectionUtils.getCIndex(connectionsViaProjects180Days));
        landscapeAnalysisResults.setpIndex180Days(ContributorConnectionUtils.getPIndex(connectionsViaProjects180Days));

        landscapeAnalysisResults.setcMean30Days(ContributorConnectionUtils.getCMean(connectionsViaProjects30Days));
        landscapeAnalysisResults.setpMean30Days(ContributorConnectionUtils.getPMean(connectionsViaProjects30Days));

        landscapeAnalysisResults.setcMean90Days(ContributorConnectionUtils.getCMean(connectionsViaProjects90Days));
        landscapeAnalysisResults.setpMean90Days(ContributorConnectionUtils.getPMean(connectionsViaProjects90Days));

        landscapeAnalysisResults.setcMean180Days(ContributorConnectionUtils.getCMean(connectionsViaProjects180Days));
        landscapeAnalysisResults.setpMean180Days(ContributorConnectionUtils.getPMean(connectionsViaProjects180Days));

        landscapeAnalysisResults.setcMedian30Days(ContributorConnectionUtils.getCMedian(connectionsViaProjects30Days));
        landscapeAnalysisResults.setpMedian30Days(ContributorConnectionUtils.getPMedian(connectionsViaProjects30Days));

        landscapeAnalysisResults.setcMedian90Days(ContributorConnectionUtils.getCMedian(connectionsViaProjects90Days));
        landscapeAnalysisResults.setpMedian90Days(ContributorConnectionUtils.getPMedian(connectionsViaProjects90Days));

        landscapeAnalysisResults.setcMedian180Days(ContributorConnectionUtils.getCMedian(connectionsViaProjects180Days));
        landscapeAnalysisResults.setpMedian180Days(ContributorConnectionUtils.getPMedian(connectionsViaProjects180Days));

        landscapeAnalysisResults.setC2cConnectionsCount30Days(peopleDependencies30Days.size());
        landscapeAnalysisResults.setC2pConnectionsCount30Days(connectionsViaProjects30Days.stream().mapToInt(c -> c.getConnectionsCount()).sum());

        addHistory(landscapeAnalysisResults);
    }

    private void addHistory(LandscapeAnalysisResults landscapeAnalysisResults) {
        List<ContributorProjects> contributors = landscapeAnalysisResults.getContributors();
        for (int i = 0; i < 12; i++) {
            int daysAgo1 = i * 30;
            int daysAgo2 = (i + 1) * 30;
            List<ComponentDependency> peopleDependencies30Days = ContributorConnectionUtils.getPeopleDependencies(contributors, daysAgo1, daysAgo2);
            List<ContributorConnections> connectionsViaProjects30Days = ContributorConnectionUtils.getConnectionsViaProjects(contributors, peopleDependencies30Days, daysAgo1, daysAgo2);
            landscapeAnalysisResults.getcIndex30DaysHistory().add(ContributorConnectionUtils.getCIndex(connectionsViaProjects30Days));
            landscapeAnalysisResults.getpIndex30DaysHistory().add(ContributorConnectionUtils.getPIndex(connectionsViaProjects30Days));
            landscapeAnalysisResults.getcMean30DaysHistory().add(ContributorConnectionUtils.getCMean(connectionsViaProjects30Days));
            landscapeAnalysisResults.getpMean30DaysHistory().add(ContributorConnectionUtils.getPMean(connectionsViaProjects30Days));
            landscapeAnalysisResults.getcMedian30DaysHistory().add(ContributorConnectionUtils.getCMedian(connectionsViaProjects30Days));
            landscapeAnalysisResults.getpMedian30DaysHistory().add(ContributorConnectionUtils.getPMedian(connectionsViaProjects30Days));
            int connectionSum = connectionsViaProjects30Days.stream().mapToInt(c -> c.getConnectionsCount()).sum();
            landscapeAnalysisResults.getConnectionsViaProjects30DaysCountHistory().add((double) connectionSum);
            landscapeAnalysisResults.getPeopleDependenciesCount30DaysHistory().add((double) peopleDependencies30Days.size());
            landscapeAnalysisResults.getActiveContributors30DaysHistory().add((double) ContributorConnectionUtils.getContributorsActiveInPeriodCount(contributors, daysAgo1, daysAgo2));
        }
    }

    private CodeAnalysisResults getProjectAnalysisResults(SokratesProjectLink sokratesProjectLink) {
        return getProjectAnalysisResults(getProjectAnalysisFile(sokratesProjectLink));
    }

    private File getProjectAnalysisFile(SokratesProjectLink sokratesProjectLink) {
        String analysisRoot = landscapeConfiguration.getAnalysisRoot();
        if (analysisRoot.startsWith("..")) {
            analysisRoot = Paths.get(landscapeConfigurationFile.getParentFile().getPath(), analysisRoot).toFile().getPath();
        }
        return Paths.get(analysisRoot, sokratesProjectLink.getAnalysisResultsPath()).toFile();
    }

    private CodeAnalysisResults getProjectAnalysisResults(File projectAnalysisResultsFile) {
        try {
            String json = FileUtils.readFileToString(projectAnalysisResultsFile, StandardCharsets.UTF_8);
            CodeAnalysisResults codeAnalysisResults = (CodeAnalysisResults) new JsonMapper().getObject(json, CodeAnalysisResults.class);
            codeAnalysisResults.setUnitsAnalysisResults(new UnitsAnalysisResults());
            codeAnalysisResults.setFilesAnalysisResults(new FilesAnalysisResults());
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
}
