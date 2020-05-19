/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LandscapeVisualsGenerator {
    private File landscapeReportsFolder;

    public LandscapeVisualsGenerator(File landscapeReportsFolder) {
        this.landscapeReportsFolder = landscapeReportsFolder;
    }

    public void exportVisuals(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        exportProjects(landscapeAnalysisResults);
        exportLanguages(landscapeAnalysisResults);
    }

    private void exportProjects(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        landscapeAnalysisResults.getAllProjects().forEach(projectAnalysisResults -> {
            CodeAnalysisResults analysisResults = projectAnalysisResults.getAnalysisResults();

            String name = analysisResults.getCodeConfiguration().getMetadata().getName();
            int linesOfCode = analysisResults.getMainAspectAnalysisResults().getLinesOfCode();

            items.add(new VisualizationItem(name, linesOfCode));
        });
        exportVisuals("projects", items);
    }

    private void exportLanguages(LandscapeAnalysisResults landscapeAnalysisResults) throws IOException {
        List<VisualizationItem> items = new ArrayList<>();
        landscapeAnalysisResults.getLinesOfCodePerExtension().forEach(metric -> {
            items.add(new VisualizationItem(metric.getName().replace("*.", ""), metric.getValue().intValue()));
        });
        exportVisuals("extensions", items);
    }

    public void exportVisuals(String nameSuffix, List<VisualizationItem> items) throws IOException {
        File folder = Paths.get(landscapeReportsFolder.getPath(), "visuals").toFile();
        folder.mkdirs();
        FileUtils.write(new File(folder, "bubble_chart_" + nameSuffix + ".html"), new VisualizationTemplate().renderBubbleChart(items), UTF_8);
        FileUtils.write(new File(folder, "tree_map_" + nameSuffix + ".html"), new VisualizationTemplate().renderTreeMap(items), UTF_8);
    }
}
