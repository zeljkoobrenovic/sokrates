package nl.obren.sokrates.sourcecode.landscape.analysis;

public class ContributorProjectInfo {
    private ProjectAnalysisResults projectAnalysisResults = new ProjectAnalysisResults();
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private int commitsCount = 0;

    public ContributorProjectInfo() {
    }

    public ContributorProjectInfo(ProjectAnalysisResults projectAnalysisResults, String firstCommitDate, String latestCommitDate, int commitsCount) {
        this.projectAnalysisResults = projectAnalysisResults;
        this.firstCommitDate = firstCommitDate;
        this.latestCommitDate = latestCommitDate;
        this.commitsCount = commitsCount;
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
}
