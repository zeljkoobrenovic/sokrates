package nl.obren.sokrates.sourcecode.githistory;

public class ContributorPerExtensionStats {
    private String contributor = "";
    private int commitsCount30Days = 0;
    private int commitsCount90Days = 0;
    private int commitsCount = 0;

    public ContributorPerExtensionStats() {
    }

    public ContributorPerExtensionStats(String contributor) {
        this.contributor = contributor;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public void setCommitsCount30Days(int commitsCount30Days) {
        this.commitsCount30Days = commitsCount30Days;
    }

    public int getCommitsCount90Days() {
        return commitsCount90Days;
    }

    public void setCommitsCount90Days(int commitsCount90Days) {
        this.commitsCount90Days = commitsCount90Days;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }
}
