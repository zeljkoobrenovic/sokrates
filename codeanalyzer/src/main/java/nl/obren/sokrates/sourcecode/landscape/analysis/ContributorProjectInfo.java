package nl.obren.sokrates.sourcecode.landscape.analysis;

import java.util.ArrayList;
import java.util.List;

public class ContributorProjectInfo {
    private ProjectAnalysisResults projectAnalysisResults = new ProjectAnalysisResults();
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private int commitsCount = 0;
    private List<String> commitDates = new ArrayList<>();

    public ContributorProjectInfo() {
    }

    public ContributorProjectInfo(ProjectAnalysisResults projectAnalysisResults, String firstCommitDate, String latestCommitDate, int commitsCount, List<String> commitDates) {
        this.projectAnalysisResults = projectAnalysisResults;
        this.firstCommitDate = firstCommitDate;
        this.latestCommitDate = latestCommitDate;
        this.commitsCount = commitsCount;
        this.commitDates = commitDates;
    }

    public ProjectAnalysisResults getProjectAnalysisResults() {
        return projectAnalysisResults;
    }

    public void setProjectAnalysisResults(ProjectAnalysisResults projectAnalysisResults) {
        this.projectAnalysisResults = projectAnalysisResults;
    }

    public String getFirstCommitDate() {
        return firstCommitDate;
    }

    public void setFirstCommitDate(String firstCommitDate) {
        this.firstCommitDate = firstCommitDate;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }

    public List<String> getCommitDates() {
        return commitDates;
    }

    public void setCommitDates(List<String> commitDates) {
        this.commitDates = commitDates;
    }
}
