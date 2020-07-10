package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.contributors.Contributor;

import java.util.ArrayList;
import java.util.List;

public class ContributorProject {
    private Contributor contributor;
    private List<ProjectAnalysisResults> projects = new ArrayList<>();
    private List<Integer> projectsCommits = new ArrayList<>();

    public ContributorProject(Contributor contributor) {
        this.contributor = contributor;
    }

    public void addProject(ProjectAnalysisResults project, int commits) {
        projects.add(project);
        projectsCommits.add(commits);
    }

    public Contributor getContributor() {
        return contributor;
    }

    public void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public List<ProjectAnalysisResults> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectAnalysisResults> projects) {
        this.projects = projects;
    }

    public List<Integer> getProjectsCommits() {
        return projectsCommits;
    }

    public void setProjectsCommits(List<Integer> projectsCommits) {
        this.projectsCommits = projectsCommits;
    }
}
