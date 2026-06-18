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
    // Per-window line churn by this contributor in this repository, kept split into lines added and
    // deleted (mirroring the commit windows). 0 when the git history carried no churn columns.
    private int churnAdded;
    private int churnDeleted;
    private int churnAdded30Days;
    private int churnDeleted30Days;
    private int churnAdded90Days;
    private int churnDeleted90Days;
    private int churnAdded365Days;
    private int churnDeleted365Days;
    // Per-day line churn (date -> lines) for THIS repository, split added/deleted, mirroring
    // commitsPerDate, so the individual report can draw a stacked per-slot churn bar. May be empty
    // for older analyses.
    private Map<String, Integer> churnAddedPerDate = new LinkedHashMap<>();
    private Map<String, Integer> churnDeletedPerDate = new LinkedHashMap<>();

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

    public int getChurnAdded() {
        return churnAdded;
    }

    public void setChurnAdded(int churnAdded) {
        this.churnAdded = churnAdded;
    }

    public int getChurnDeleted() {
        return churnDeleted;
    }

    public void setChurnDeleted(int churnDeleted) {
        this.churnDeleted = churnDeleted;
    }

    public int getChurnAdded30Days() {
        return churnAdded30Days;
    }

    public void setChurnAdded30Days(int churnAdded30Days) {
        this.churnAdded30Days = churnAdded30Days;
    }

    public int getChurnDeleted30Days() {
        return churnDeleted30Days;
    }

    public void setChurnDeleted30Days(int churnDeleted30Days) {
        this.churnDeleted30Days = churnDeleted30Days;
    }

    public int getChurnAdded90Days() {
        return churnAdded90Days;
    }

    public void setChurnAdded90Days(int churnAdded90Days) {
        this.churnAdded90Days = churnAdded90Days;
    }

    public int getChurnDeleted90Days() {
        return churnDeleted90Days;
    }

    public void setChurnDeleted90Days(int churnDeleted90Days) {
        this.churnDeleted90Days = churnDeleted90Days;
    }

    public int getChurnAdded365Days() {
        return churnAdded365Days;
    }

    public void setChurnAdded365Days(int churnAdded365Days) {
        this.churnAdded365Days = churnAdded365Days;
    }

    public int getChurnDeleted365Days() {
        return churnDeleted365Days;
    }

    public void setChurnDeleted365Days(int churnDeleted365Days) {
        this.churnDeleted365Days = churnDeleted365Days;
    }

    // Combined-churn convenience getters (added + deleted), used where the split isn't needed
    // (e.g. the repositories explorer's total-churn column).
    public int getChurn() {
        return churnAdded + churnDeleted;
    }

    public int getChurn30Days() {
        return churnAdded30Days + churnDeleted30Days;
    }

    public int getChurn90Days() {
        return churnAdded90Days + churnDeleted90Days;
    }

    public int getChurn365Days() {
        return churnAdded365Days + churnDeleted365Days;
    }

    // Accumulates one contributor-in-repository's churn windows into this aggregate, keeping added and
    // deleted separate (mirrors the commit-window accumulation in ContributorRepositories.addRepository).
    public void addChurn(int added, int deleted, int added30, int deleted30, int added90, int deleted90, int added365, int deleted365) {
        this.churnAdded += added;
        this.churnDeleted += deleted;
        this.churnAdded30Days += added30;
        this.churnDeleted30Days += deleted30;
        this.churnAdded90Days += added90;
        this.churnDeleted90Days += deleted90;
        this.churnAdded365Days += added365;
        this.churnDeleted365Days += deleted365;
    }

    public Map<String, Integer> getChurnAddedPerDate() {
        return churnAddedPerDate;
    }

    public void setChurnAddedPerDate(Map<String, Integer> churnAddedPerDate) {
        this.churnAddedPerDate = churnAddedPerDate != null ? churnAddedPerDate : new LinkedHashMap<>();
    }

    public Map<String, Integer> getChurnDeletedPerDate() {
        return churnDeletedPerDate;
    }

    public void setChurnDeletedPerDate(Map<String, Integer> churnDeletedPerDate) {
        this.churnDeletedPerDate = churnDeletedPerDate != null ? churnDeletedPerDate : new LinkedHashMap<>();
    }

    // Merges per-day added/deleted churn into this aggregate (mirrors commitsPerDate merging).
    public void addChurnPerDate(Map<String, Integer> added, Map<String, Integer> deleted) {
        if (added != null) {
            added.forEach((date, count) -> churnAddedPerDate.merge(date, count, Integer::sum));
        }
        if (deleted != null) {
            deleted.forEach((date, count) -> churnDeletedPerDate.merge(date, count, Integer::sum));
        }
    }
}
