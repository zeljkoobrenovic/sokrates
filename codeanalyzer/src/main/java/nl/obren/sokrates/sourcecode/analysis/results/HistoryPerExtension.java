package nl.obren.sokrates.sourcecode.analysis.results;

import java.util.HashSet;
import java.util.Set;

public class HistoryPerExtension {
    private String extension = "";
    private String year = "";
    private int commitsCount = 0;
    private Set<String> contributors = new HashSet<>();

    public HistoryPerExtension() {
    }

    public HistoryPerExtension(String extension, String year, int commitsCount) {
        this.extension = extension;
        this.year = year;
        this.commitsCount = commitsCount;
        this.contributors = contributors;
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

    public Set<String> getContributors() {
        return contributors;
    }

    public void setContributors(Set<String> contributors) {
        this.contributors = contributors;
    }
}
