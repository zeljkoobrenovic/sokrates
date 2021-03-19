package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.contributors.Contributor;

import java.util.ArrayList;
import java.util.List;

public class ContributorProjects {
    private Contributor contributor;
    private List<ContributorProjectInfo> projects = new ArrayList<>();

    public ContributorProjects(Contributor contributor) {
        this.contributor = contributor;
    }

    public void addProject(ProjectAnalysisResults projectAnalysisResults, String firstCommitDate, String latestCommitDate,
                           int commitsCount, int commits30Days, int commits90Days, List<String> commitDates) {
        projects.add(new ContributorProjectInfo(projectAnalysisResults, firstCommitDate, latestCommitDate,
                commitsCount, commits30Days, commits90Days, commitDates));
    }

    public void addProject(ContributorProjectInfo project) {
        projects.add(project);
    }

    public Contributor getContributor() {
        return contributor;
    }

    public void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public List<ContributorProjectInfo> getProjects() {
        return projects;
    }

    public void setProjects(List<ContributorProjectInfo> projects) {
        this.projects = projects;
    }
}
