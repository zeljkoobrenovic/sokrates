/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LandscapeAnalysisResults {
    private LandscapeConfiguration configuration = new LandscapeConfiguration();
    private List<ProjectAnalysisResults> projectAnalysisResults = new ArrayList<>();

    public LandscapeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(LandscapeConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<ProjectAnalysisResults> getProjectAnalysisResults() {
        return projectAnalysisResults;
    }

    public void setProjectAnalysisResults(List<ProjectAnalysisResults> projectAnalysisResults) {
        this.projectAnalysisResults = projectAnalysisResults;
    }

    @JsonIgnore
    public List<ProjectAnalysisResults> getAllProjects() {
        return this.projectAnalysisResults;
    }

    @JsonIgnore
    public int getProjectsCount() {
        return projectAnalysisResults.size();
    }

    @JsonIgnore
    public int getMainLoc() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public List<NumericMetric> getLinesOfCodePerExtension() {
        List<NumericMetric> linesOfCodePerExtension = new ArrayList<>();
        getProjectAnalysisResults().forEach(projectAnalysisResults -> {
            AspectAnalysisResults main = projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults();
            List<NumericMetric> projectLinesOfCodePerExtension = main.getLinesOfCodePerExtension();
            projectLinesOfCodePerExtension.forEach(metric -> {
                String id = metric.getName();
                Optional<NumericMetric> existingMetric = linesOfCodePerExtension.stream().filter(c -> c.getName().equalsIgnoreCase(id)).findAny();
                if (existingMetric.isPresent()) {
                    existingMetric.get().setValue(existingMetric.get().getValue().intValue() + metric.getValue().intValue());
                } else {
                    linesOfCodePerExtension.add(metric);
                }
            });
        });

        Collections.sort(linesOfCodePerExtension, (a, b) -> b.getValue().intValue() - a.getValue().intValue());
        return linesOfCodePerExtension;
    }

}
