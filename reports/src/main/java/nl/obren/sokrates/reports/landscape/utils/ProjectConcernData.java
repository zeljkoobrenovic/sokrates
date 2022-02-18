package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;

public class ProjectConcernData {
    private String projectName;
    private AspectAnalysisResults concern;
    private ProjectAnalysisResults project;

    public ProjectConcernData(String projectName, AspectAnalysisResults concern, ProjectAnalysisResults project) {
        this.projectName = projectName;
        this.concern = concern;
        this.project = project;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public AspectAnalysisResults getConcern() {
        return concern;
    }

    public void setConcern(AspectAnalysisResults concern) {
        this.concern = concern;
    }

    public ProjectAnalysisResults getProject() {
        return project;
    }

    public void setProject(ProjectAnalysisResults project) {
        this.project = project;
    }
}
