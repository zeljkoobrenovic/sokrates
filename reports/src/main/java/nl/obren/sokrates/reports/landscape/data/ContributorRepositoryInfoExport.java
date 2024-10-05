package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositoryInfo;

import java.util.ArrayList;
import java.util.List;

public class ContributorRepositoryInfoExport {
    private String name;
    private String description;
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private int commitsCount = 0;
    private int commits30Days;
    private int commits90Days;
    private int commits180Days;
    private int commits365Days;
    private List<String> commitDates = new ArrayList<>();

    public ContributorRepositoryInfoExport(ContributorRepositoryInfo contributorRepositoryInfo) {
        Metadata metadata = contributorRepositoryInfo.getRepositoryAnalysisResults().getAnalysisResults().getMetadata();
        this.name = metadata.getName();
        this.description = metadata.getDescription();
        this.firstCommitDate = contributorRepositoryInfo.getFirstCommitDate();
        this.latestCommitDate = contributorRepositoryInfo.getLatestCommitDate();
        this.commitsCount = contributorRepositoryInfo.getCommitsCount();
        this.commits30Days = contributorRepositoryInfo.getCommits30Days();
        this.commits90Days = contributorRepositoryInfo.getCommits90Days();
        this.commits180Days = contributorRepositoryInfo.getCommits180Days();
        this.commits365Days = contributorRepositoryInfo.getCommits365Days();
        this.commitDates = contributorRepositoryInfo.getCommitDates();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
