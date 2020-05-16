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
    private List<LandscapeGroupAnalysisResults> groupsAnalysisResults = new ArrayList<>();

    public LandscapeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(LandscapeConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<LandscapeGroupAnalysisResults> getGroupsAnalysisResults() {
        return groupsAnalysisResults;
    }

    public void setGroupsAnalysisResults(List<LandscapeGroupAnalysisResults> groupsAnalysisResults) {
        this.groupsAnalysisResults = groupsAnalysisResults;
    }

    @JsonIgnore
    public int getProjectsCount() {
        int count[] = {0};
        this.groupsAnalysisResults.forEach(groupAnalysisResult -> count[0] += groupAnalysisResult.getProjectsCount());
        return count[0];
    }

    @JsonIgnore
    public int getMainFileCount() {
        int count[] = {0};
        this.groupsAnalysisResults.forEach(groupAnalysisResult -> count[0] += groupAnalysisResult.getMainFileCount());
        return count[0];
    }

    @JsonIgnore
    public int getMainLoc() {
        int count[] = {0};
        this.groupsAnalysisResults.forEach(groupAnalysisResult -> count[0] += groupAnalysisResult.getMainLoc());
        return count[0];
    }
}
