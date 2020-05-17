/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;

import java.util.ArrayList;
import java.util.List;

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
    public int getMainFileCount() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getFilesCount();
        });
        return count[0];
    }

    @JsonIgnore
    public int getMainLoc() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }
}
