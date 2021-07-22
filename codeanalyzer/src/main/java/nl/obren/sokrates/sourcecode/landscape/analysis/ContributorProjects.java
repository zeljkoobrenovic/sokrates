package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        String path = projectAnalysisResults.getAnalysisResults().getMetadata().getName();
        ContributorProjectInfo projectByPath = getProjectByPath(path);
        if (projectByPath != null) {
            if (firstCommitDate.compareTo(projectByPath.getFirstCommitDate()) < 0) {
                projectByPath.setFirstCommitDate(firstCommitDate);
            }
            if (latestCommitDate.compareTo(projectByPath.getLatestCommitDate()) > 0) {
                projectByPath.setLatestCommitDate(latestCommitDate);
            }
            projectByPath.setCommits30Days(projectByPath.getCommits30Days() + commits30Days);
            projectByPath.setCommits90Days(projectByPath.getCommits90Days() + commits90Days);
            projectByPath.setCommitsCount(projectByPath.getCommitsCount() + commitsCount);
            commitDates.forEach(date -> {
                if (!projectByPath.getCommitDates().contains(date)) {
                    projectByPath.getCommitDates().add(date);
                }
            });
        } else {
            projects.add(new ContributorProjectInfo(projectAnalysisResults, firstCommitDate, latestCommitDate,
                    commitsCount, commits30Days, commits90Days, commitDates));
        }
    }

    public void addProject(ContributorProjectInfo project) {
        projects.add(project);
    }

    @JsonIgnore
    private ContributorProjectInfo getProjectByPath(String path) {
        for (ContributorProjectInfo project : projects) {
            if (project.getProjectAnalysisResults().getAnalysisResults().getMetadata().getName().equalsIgnoreCase(path)) {
                return project;
            }
        }
        return null;
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
