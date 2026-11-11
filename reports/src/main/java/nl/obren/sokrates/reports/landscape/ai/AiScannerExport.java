package nl.obren.sokrates.reports.landscape.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

/** One scanner's run on one repository: when, how many findings, and its summary (shortened). */
public class AiScannerExport {
    private String scanner = "";
    private String version = "";
    private String analyzedAt = "";
    private int findings = 0;
    private int attention = 0;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String summary = "";
    // the scanner's page of the repository (its report's AI Insights page, or the standalone explorer)
    private String url = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getScanner() {
        return scanner;
    }

    public void setScanner(String scanner) {
        this.scanner = scanner;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(String analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    public int getFindings() {
        return findings;
    }

    public void setFindings(int findings) {
        this.findings = findings;
    }

    /** Findings with a severity above info. */
    public int getAttention() {
        return attention;
    }

    public void setAttention(int attention) {
        this.attention = attention;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
