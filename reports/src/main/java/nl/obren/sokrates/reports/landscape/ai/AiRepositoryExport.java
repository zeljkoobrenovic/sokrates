package nl.obren.sokrates.reports.landscape.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A repository with AI scanner results: its scanners, its finding counts per severity, and where its own explorer is. */
public class AiRepositoryExport {
    private String name = "";
    /** The repository's AI Insights explorer (index.html), relative to the landscape folder. */
    private String insightsUrl = "";
    /** The repository's Sokrates report, relative to the landscape folder. */
    private String reportUrl = "";
    private String scannedAt = "";
    private List<AiScannerExport> scanners = new ArrayList<>();
    private Map<String, Integer> findingsBySeverity = new LinkedHashMap<>();
    private int findings = 0;
    private int attention = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInsightsUrl() {
        return insightsUrl;
    }

    public void setInsightsUrl(String insightsUrl) {
        this.insightsUrl = insightsUrl;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    /** The latest analyzed_at over the repository's scanners. */
    public String getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(String scannedAt) {
        this.scannedAt = scannedAt;
    }

    public List<AiScannerExport> getScanners() {
        return scanners;
    }

    public void setScanners(List<AiScannerExport> scanners) {
        this.scanners = scanners;
    }

    public Map<String, Integer> getFindingsBySeverity() {
        return findingsBySeverity;
    }

    public void setFindingsBySeverity(Map<String, Integer> findingsBySeverity) {
        this.findingsBySeverity = findingsBySeverity;
    }

    public int getFindings() {
        return findings;
    }

    public void setFindings(int findings) {
        this.findings = findings;
    }

    public int getAttention() {
        return attention;
    }

    public void setAttention(int attention) {
        this.attention = attention;
    }
}
