package nl.obren.sokrates.sourcecode.landscape.analysis;

import java.util.ArrayList;
import java.util.List;

public class ContributorRepositoryInfo {
    private RepositoryAnalysisResults repositoryAnalysisResults = new RepositoryAnalysisResults();
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private int commitsCount = 0;
    private int commits30Days;
    private int commits90Days;
    private List<String> commitDates = new ArrayList<>();

    public ContributorRepositoryInfo() {
    }

    public ContributorRepositoryInfo(RepositoryAnalysisResults repositoryAnalysisResults, String firstCommitDate, String latestCommitDate,
                                     int commitsCount, int commits30Days, int commits90Days, List<String> commitDates) {
        this.repositoryAnalysisResults = repositoryAnalysisResults;
        this.firstCommitDate = firstCommitDate;
        this.latestCommitDate = latestCommitDate;
        this.commitsCount = commitsCount;
        this.commits30Days = commits30Days;
        this.commits90Days = commits90Days;
        this.commitDates = commitDates;
    }

    public RepositoryAnalysisResults getRepositoryAnalysisResults() {
        return repositoryAnalysisResults;
    }

    public void setRepositoryAnalysisResults(RepositoryAnalysisResults repositoryAnalysisResults) {
        this.repositoryAnalysisResults = repositoryAnalysisResults;
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

    public int getCommits30Days() {
        return commits30Days;
    }

    public void setCommits30Days(int commits30Days) {
        this.commits30Days = commits30Days;
    }

    public int getCommits90Days() {
        return commits90Days;
    }

    public void setCommits90Days(int commits90Days) {
        this.commits90Days = commits90Days;
    }
}
