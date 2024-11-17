/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LandscapeVisualsGenerator {
    private static final Log LOG = LogFactory.getLog(LandscapeVisualsGenerator.class);

    private File landscapeReportsFolder;
    private final TeamsConfig teamsConfig;

    public LandscapeVisualsGenerator(File landscapeReportsFolder, TeamsConfig teamsConfig) {
        this.landscapeReportsFolder = landscapeReportsFolder;
        this.teamsConfig = teamsConfig;
    }

    public void exportVisuals(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        exportRepositories(landscapeAnalysisResults);
        exportContributors(landscapeAnalysisResults.getContributors(), "contributors");
        exportContributors(landscapeAnalysisResults.getTeams(teamsConfig), "teams");
        exportRecentContributors(landscapeAnalysisResults.getContributors(), "contributors");
        exportRecentContributors(landscapeAnalysisResults.getTeams(teamsConfig), "teams");
        exportLanguages(landscapeAnalysisResults);
        exportContributorPerLanguage(landscapeAnalysisResults);
        exportTeamsPerLanguage(landscapeAnalysisResults);
    }

    private void exportRepositories(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> itemsLinesOfCode = new ArrayList<>();
        List<VisualizationItem> itemsCommits = new ArrayList<>();
        List<VisualizationItem> itemsAge = new ArrayList<>();
        List<VisualizationItem> itemsContributors = new ArrayList<>();
        landscapeAnalysisResults.getAllRepositories().forEach(repositoryAnalysisResults -> {
            CodeAnalysisResults analysisResults = repositoryAnalysisResults.getAnalysisResults();

            String name = analysisResults.getMetadata().getName();

            itemsLinesOfCode.add(new VisualizationItem(name, analysisResults.getMainAspectAnalysisResults().getLinesOfCode()));
            itemsCommits.add(new VisualizationItem(name, analysisResults.getContributorsAnalysisResults().getCommitsCount30Days()));
            itemsCommits.add(new VisualizationItem(name, analysisResults.getFilesHistoryAnalysisResults().getAgeInDays()));
            if (analysisResults.getContributorsAnalysisResults().getContributorsPerMonth().size() > 0) {
                itemsContributors.add(new VisualizationItem(name, analysisResults.getContributorsAnalysisResults().getContributorsPerMonth().get(0).getContributorsCount()));
            }
        });
        exportVisuals("repositories_loc", itemsLinesOfCode);
        exportVisuals("repositories_commits", itemsCommits);
        exportVisuals("repositories_age", itemsCommits);
        exportVisuals("repositories_contributors", itemsContributors);
    }

    private void exportContributors(List<ContributorRepositories> contributors, String prefix) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        contributors.forEach(contributorRepository -> {
            String name = contributorRepository.getContributor().getEmail();
            items.add(new VisualizationItem(name, contributorRepository.getContributor().getCommitsCount()));
        });
        exportVisuals(prefix, items);
    }

    private void exportRecentContributors(List<ContributorRepositories> contributors, String prefix) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        contributors.stream().filter(c -> c.getContributor().getCommitsCount30Days() > 0).forEach(contributorRepository -> {
            String name = contributorRepository.getContributor().getEmail();
            items.add(new VisualizationItem(name, contributorRepository.getContributor().getCommitsCount30Days()));
        });
        exportVisuals(prefix + "_30_days", items);
    }

    private void exportLanguages(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        List<NumericMetric> mainLinesOfCodePerExtension = getMergedMainLocPerExtension(landscapeAnalysisResults);
        mainLinesOfCodePerExtension.forEach(metric -> {
            items.add(new VisualizationItem(metric.getName().replace("*.", ""), metric.getValue().intValue()));
        });
        exportVisuals("extensions", items);
    }

    private List<NumericMetric> getMergedMainLocPerExtension(LandscapeAnalysisResults landscapeAnalysisResults) {
        return LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, landscapeAnalysisResults.getMainLinesOfCodePerExtension());
    }

    private void exportContributorPerLanguage(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        List<String> mainExtensions = getMainExtensions(landscapeAnalysisResults);
        List<CommitsPerExtension> contributorsPerExtension = landscapeAnalysisResults.getContributorsPerExtension()
                .stream().filter(c -> mainExtensions.contains(c.getExtension())).collect(Collectors.toList());
        contributorsPerExtension.stream().filter(e -> e.getCommitters30Days().size() > 0).forEach(commitsPerExtension -> {
            items.add(new VisualizationItem(commitsPerExtension.getExtension().replace("*.", ""), commitsPerExtension.getCommitters30Days().size()));
        });
        exportVisuals("extensions_contributors_30d", items);
    }

    private void exportTeamsPerLanguage(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        List<String> mainExtensions = getMainExtensions(landscapeAnalysisResults);
        List<CommitsPerExtension> contributorsPerExtension = landscapeAnalysisResults.getContributorsPerExtension()
                .stream().filter(c -> mainExtensions.contains(c.getExtension())).collect(Collectors.toList());
        contributorsPerExtension.stream().filter(e -> e.getTeams30Days(teamsConfig).size() > 0).forEach(commitsPerExtension -> {
            items.add(new VisualizationItem(commitsPerExtension.getExtension().replace("*.", ""), commitsPerExtension.getTeams30Days(teamsConfig).size()));
        });
        exportVisuals("extensions_teams_30d", items);
    }

    public void exportVisuals(String nameSuffix, List<VisualizationItem> items) throws IOException {
        LOG.info("Exporting visuals for " + nameSuffix + ".");
        File folder = Paths.get(landscapeReportsFolder.getPath(), "visuals").toFile();
        folder.mkdirs();
        FileUtils.write(new File(folder, "bubble_chart_" + nameSuffix + ".html"), new VisualizationTemplate().renderBubbleChart(items), UTF_8);
        FileUtils.write(new File(folder, "tree_map_" + nameSuffix + ".html"), new VisualizationTemplate().renderTreeMap(items), UTF_8);
    }

    private List<String> getMainExtensions(LandscapeAnalysisResults landscapeAnalysisResults) {
        return getMergedMainLocPerExtension(landscapeAnalysisResults).stream().map(l -> l.getName().replace("*.", "").trim()).collect(Collectors.toList());
    }
}
