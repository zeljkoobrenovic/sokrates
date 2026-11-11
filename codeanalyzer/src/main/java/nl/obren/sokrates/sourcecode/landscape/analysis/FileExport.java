package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonInclude;
import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;

public class FileExport {
    private String repository;
    private String path;

    private String scope;

    private int linesOfCode;

    // Git-history fields; 0 / "" when no file history is available for the analysis.
    private int commitsCount;
    private int recentCommitsCount30Days;
    private int recentCommitsCount90Days;
    private String latestCommitDate = "";
    // Age/freshness/churn fields (-1 = unknown, i.e. no history), mirroring the File Age and
    // File Change Frequency reports.
    private int ageDays = -1;          // days since first update
    private int freshnessDays = -1;    // days since latest update
    private int contributorsCount;
    // Total lines touched (added + deleted) by commits to this file, in the last 30 / 90 days and all
    // time. 0 when no churn data is available.
    private int churn30Days;
    private int churn90Days;
    private int churnTotal;
    // Distinct contributors of the commits in the last 30 / 90 days, and the file's units (functions,
    // methods) and their McCabe index sum. Null (left out of the JSON) when not computed, e.g. in the
    // landscape's file list, so the explorer does not offer those columns.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer contributorsCount30Days;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer contributorsCount90Days;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer unitsCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer mcCabeIndexSum;
    // Relative URL (from the explorers/ folder) of this file's cached source page, or "" when none
    // was saved (only a referenced subset of files is cached).
    private String sourceFileLink = "";

    public FileExport() {
    }

    public Integer getContributorsCount30Days() {
        return contributorsCount30Days;
    }

    public void setContributorsCount30Days(Integer contributorsCount30Days) {
        this.contributorsCount30Days = contributorsCount30Days;
    }

    public Integer getContributorsCount90Days() {
        return contributorsCount90Days;
    }

    public void setContributorsCount90Days(Integer contributorsCount90Days) {
        this.contributorsCount90Days = contributorsCount90Days;
    }

    public Integer getUnitsCount() {
        return unitsCount;
    }

    public void setUnitsCount(Integer unitsCount) {
        this.unitsCount = unitsCount;
    }

    public Integer getMcCabeIndexSum() {
        return mcCabeIndexSum;
    }

    public void setMcCabeIndexSum(Integer mcCabeIndexSum) {
        this.mcCabeIndexSum = mcCabeIndexSum;
    }

    public FileExport(String repository, String path, String scope, int linesOfCode) {
        this.repository = repository;
        this.path = path;
        this.scope = scope;
        this.linesOfCode = linesOfCode;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }

    public int getRecentCommitsCount30Days() {
        return recentCommitsCount30Days;
    }

    public void setRecentCommitsCount30Days(int recentCommitsCount30Days) {
        this.recentCommitsCount30Days = recentCommitsCount30Days;
    }

    public int getRecentCommitsCount90Days() {
        return recentCommitsCount90Days;
    }

    public void setRecentCommitsCount90Days(int recentCommitsCount90Days) {
        this.recentCommitsCount90Days = recentCommitsCount90Days;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }

    public int getAgeDays() {
        return ageDays;
    }

    public void setAgeDays(int ageDays) {
        this.ageDays = ageDays;
    }

    public int getFreshnessDays() {
        return freshnessDays;
    }

    public void setFreshnessDays(int freshnessDays) {
        this.freshnessDays = freshnessDays;
    }

    public int getContributorsCount() {
        return contributorsCount;
    }

    public void setContributorsCount(int contributorsCount) {
        this.contributorsCount = contributorsCount;
    }

    public int getChurn30Days() {
        return churn30Days;
    }

    public void setChurn30Days(int churn30Days) {
        this.churn30Days = churn30Days;
    }

    public int getChurn90Days() {
        return churn90Days;
    }

    public void setChurn90Days(int churn90Days) {
        this.churn90Days = churn90Days;
    }

    public int getChurnTotal() {
        return churnTotal;
    }

    public void setChurnTotal(int churnTotal) {
        this.churnTotal = churnTotal;
    }

    public String getSourceFileLink() {
        return sourceFileLink;
    }

    public void setSourceFileLink(String sourceFileLink) {
        this.sourceFileLink = sourceFileLink;
    }

    /**
     * The file's language, derived from its path extension. Serialized into the explorer JSON
     * so the files explorer can render a matching language icon.
     */
    public String getMainLang() {
        return path != null ? ExtensionGroupExtractor.getExtension(path).toLowerCase().trim() : "";
    }
}
