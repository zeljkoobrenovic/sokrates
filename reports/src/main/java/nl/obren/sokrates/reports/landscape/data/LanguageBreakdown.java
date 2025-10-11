package nl.obren.sokrates.reports.landscape.data;

/**
 * Represents a language's contribution to a repository with both absolute LOC and percentage
 */
public class LanguageBreakdown {
    private String language;
    private int linesOfCode;
    private double percentage;

    public LanguageBreakdown() {
    }

    public LanguageBreakdown(String language, int linesOfCode, double percentage) {
        this.language = language;
        this.linesOfCode = linesOfCode;
        this.percentage = percentage;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
