package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.sourcecode.landscape.ProjectTag;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorProjectInfo;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;

import java.util.ArrayList;
import java.util.List;

public class TagStats {
    private ProjectTag tag;
    private List<ProjectAnalysisResults> projectsAnalysisResults = new ArrayList<>();

    public TagStats(ProjectTag tag) {
        this.tag = tag;
    }

    public ProjectTag getTag() {
        return tag;
    }

    public void setTag(ProjectTag tag) {
        this.tag = tag;
    }

    public List<ProjectAnalysisResults> getProjectsAnalysisResults() {
        return projectsAnalysisResults;
    }

    public void setProjectsAnalysisResults(List<ProjectAnalysisResults> projectsAnalysisResults) {
        this.projectsAnalysisResults = projectsAnalysisResults;
    }
}
