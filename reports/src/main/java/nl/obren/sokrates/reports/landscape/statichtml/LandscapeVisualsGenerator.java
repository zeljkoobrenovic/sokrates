/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LandscapeVisualsGenerator {
    private File landscapeReportsFolder;

    public LandscapeVisualsGenerator(File landscapeReportsFolder) {
        this.landscapeReportsFolder = landscapeReportsFolder;
    }

    public void exportVisuals(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        exportProjects(landscapeAnalysisResults);
        exportContributors(landscapeAnalysisResults);
        exportRecentContributors(landscapeAnalysisResults);
        exportLanguages(landscapeAnalysisResults);
        exportContributorPerLanguage(landscapeAnalysisResults);
    }

    private void exportProjects(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        landscapeAnalysisResults.getAllProjects().forEach(projectAnalysisResults -> {
            CodeAnalysisResults analysisResults = projectAnalysisResults.getAnalysisResults();

            String name = analysisResults.getMetadata().getName();
            int linesOfCode = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();

            items.add(new VisualizationItem(name, linesOfCode));
        });
        exportVisuals("projects", items);
    }

    private void exportContributors(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        landscapeAnalysisResults.getContributors().forEach(contributorProject -> {
            String name = contributorProject.getContributor().getEmail();
            items.add(new VisualizationItem(name, contributorProject.getContributor().getCommitsCount()));
        });
        exportVisuals("contributors", items);
    }

    private void exportRecentContributors(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        landscapeAnalysisResults.getContributors().stream().filter(c -> c.getContributor().getCommitsCount30Days() > 0).forEach(contributorProject -> {
            String name = contributorProject.getContributor().getEmail();
            items.add(new VisualizationItem(name, contributorProject.getContributor().getCommitsCount30Days()));
        });
        exportVisuals("contributors_30_days", items);
    }

    private void exportLanguages(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        landscapeAnalysisResults.getMainLinesOfCodePerExtension().forEach(metric -> {
            items.add(new VisualizationItem(metric.getName().replace("*.", ""), metric.getValue().intValue()));
        });
        exportVisuals("extensions", items);
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

    public void exportVisuals(String nameSuffix, List<VisualizationItem> items) throws IOException {
        System.out.println("Exporting visuals for " + nameSuffix + ".");
        File folder = Paths.get(landscapeReportsFolder.getPath(), "visuals").toFile();
        folder.mkdirs();
        FileUtils.write(new File(folder, "bubble_chart_" + nameSuffix + ".html"), new VisualizationTemplate().renderBubbleChart(items), UTF_8);
        FileUtils.write(new File(folder, "tree_map_" + nameSuffix + ".html"), new VisualizationTemplate().renderTreeMap(items), UTF_8);
    }

    private List<String> getMainExtensions(LandscapeAnalysisResults landscapeAnalysisResults) {
        return landscapeAnalysisResults.getMainLinesOfCodePerExtension().stream().map(l -> l.getName().replace("*.", "").trim()).collect(Collectors.toList());
    }
}
