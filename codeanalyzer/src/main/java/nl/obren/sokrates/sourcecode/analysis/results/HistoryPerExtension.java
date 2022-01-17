package nl.obren.sokrates.sourcecode.analysis.results;

public class HistoryPerExtension {
    private String extension = "";
    private String year = "";
    private int commitsCount = 0;
    private int contributorsCount = 0;

    public HistoryPerExtension() {
    }

    public HistoryPerExtension(String extension, String year, int commitsCount, int contributorsCount) {
        this.extension = extension;
        this.year = year;
        this.commitsCount = commitsCount;
        this.contributorsCount = contributorsCount;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }

    public int getContributorsCount() {
        return contributorsCount;
    }

    public void setContributorsCount(int contributorsCount) {
        this.contributorsCount = contributorsCount;
    }
}
