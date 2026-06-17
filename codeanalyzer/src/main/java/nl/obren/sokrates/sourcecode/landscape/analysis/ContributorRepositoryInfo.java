package nl.obren.sokrates.sourcecode.landscape.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContributorRepositoryInfo {
    private RepositoryAnalysisResults repositoryAnalysisResults = new RepositoryAnalysisResults();
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private int commitsCount = 0;
    private int commits30Days;
    private int commits90Days;
    private int commits180Days;
    private int commits365Days;
    private List<String> commitDates = new ArrayList<>();
    // Per-day commit counts (date -> commits) for THIS repository; commitDates above is the distinct
    // dates. Lets the activity visuals be sized by commits rather than commit days. May be empty for
    // older analysisResults.json (consumers fall back to commitDates).
    private Map<String, Integer> commitsPerDate = new LinkedHashMap<>();

    public ContributorRepositoryInfo() {
    }

    public ContributorRepositoryInfo(RepositoryAnalysisResults repositoryAnalysisResults, String firstCommitDate, String latestCommitDate,
                                     int commitsCount, int commits30Days, int commits90Days, int commits180Days, int commits365Days, List<String> commitDates) {
        this(repositoryAnalysisResults, firstCommitDate, latestCommitDate, commitsCount, commits30Days, commits90Days,
                commits180Days, commits365Days, commitDates, new LinkedHashMap<>());
    }

    public ContributorRepositoryInfo(RepositoryAnalysisResults repositoryAnalysisResults, String firstCommitDate, String latestCommitDate,
                                     int commitsCount, int commits30Days, int commits90Days, int commits180Days, int commits365Days,
                                     List<String> commitDates, Map<String, Integer> commitsPerDate) {
        this.repositoryAnalysisResults = repositoryAnalysisResults;
        this.firstCommitDate = firstCommitDate;
        this.latestCommitDate = latestCommitDate;
        this.commitsCount = commitsCount;
        this.commits30Days = commits30Days;
        this.commits90Days = commits90Days;
        this.commits180Days = commits180Days;
        this.commits365Days = commits365Days;
        this.commitDates = commitDates;
        this.commitsPerDate = commitsPerDate != null ? commitsPerDate : new LinkedHashMap<>();
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

    public Map<String, Integer> getCommitsPerDate() {
        return commitsPerDate;
    }

    public void setCommitsPerDate(Map<String, Integer> commitsPerDate) {
        this.commitsPerDate = commitsPerDate != null ? commitsPerDate : new LinkedHashMap<>();
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

    public int getCommits180Days() {
        return commits180Days;
    }

    public void setCommits180Days(int commits180Days) {
        this.commits180Days = commits180Days;
    }

    public int getCommits365Days() {
        return commits365Days;
    }

    public void setCommits365Days(int commits365Days) {
        this.commits365Days = commits365Days;
    }
}
