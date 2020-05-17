/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.LandscapeGroup;

import java.util.ArrayList;
import java.util.List;

public class LandscapeGroupAnalysisResults {
    private LandscapeGroup group = new LandscapeGroup();
    private List<ProjectAnalysisResults> projectsAnalysisResults = new ArrayList<>();
    private List<LandscapeGroupAnalysisResults> subGroupsAnalysisResults = new ArrayList<>();

    public LandscapeGroup getGroup() {
        return group;
    }

    public void setGroup(LandscapeGroup group) {
        this.group = group;
    }

    public List<ProjectAnalysisResults> getProjectsAnalysisResults() {
        return projectsAnalysisResults;
    }

    public void setProjectsAnalysisResults(List<ProjectAnalysisResults> projectsAnalysisResults) {
        this.projectsAnalysisResults = projectsAnalysisResults;
    }

    public List<LandscapeGroupAnalysisResults> getSubGroupsAnalysisResults() {
        return subGroupsAnalysisResults;
    }

    public void setSubGroupsAnalysisResults(List<LandscapeGroupAnalysisResults> subGroupsAnalysisResults) {
        this.subGroupsAnalysisResults = subGroupsAnalysisResults;
    }

    @JsonIgnore
    public int getProjectsCount() {
        int projectsInGroup = this.getProjectsAnalysisResults().size();
        int projectsInSubGroups[] = {0};

        this.getSubGroupsAnalysisResults().forEach(subGroupsAnalysisResults -> {
            projectsInSubGroups[0] += subGroupsAnalysisResults.getProjectsCount();
        });

        return projectsInGroup + projectsInSubGroups[0];
    }

    @JsonIgnore
    public int getMainLoc() {
        int locInGroup[] = {0};
        this.getProjectsAnalysisResults().forEach(projectsAnalysisResults -> {
            locInGroup[0] += projectsAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
        });

        int locInSubGroups[] = {0};

        this.getSubGroupsAnalysisResults().forEach(subGroupsAnalysisResults -> {
            locInSubGroups[0] += subGroupsAnalysisResults.getMainLoc();
        });

        return locInGroup[0] + locInSubGroups[0];
    }

    @JsonIgnore
    public int getMainFileCount() {
        int locInGroup[] = {0};
        this.getProjectsAnalysisResults().forEach(projectsAnalysisResults -> {
            locInGroup[0] += projectsAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getFilesCount();
        });

        int locInSubGroups[] = {0};

        this.getSubGroupsAnalysisResults().forEach(subGroupsAnalysisResults -> {
            locInSubGroups[0] += subGroupsAnalysisResults.getMainFileCount();
        });

        return locInGroup[0] + locInSubGroups[0];
    }
}

