package nl.obren.sokrates.sourcecode.core;

import nl.obren.sokrates.sourcecode.analysis.AnalyzerOverride;

import java.util.ArrayList;
import java.util.List;

public class AnalysisConfig {
    private boolean skipDuplication = false;
    private boolean skipDependencies = false;
    private boolean cacheSourceFiles = true;
    private boolean saveDailyHistory = true;
    private List<AnalyzerOverride> analyzerOverrides = new ArrayList<>();

    public boolean isSkipDuplication() {
        return skipDuplication;
    }

    public void setSkipDuplication(boolean skipDuplication) {
        this.skipDuplication = skipDuplication;
    }

    public boolean isSkipDependencies() {
        return skipDependencies;
    }

    public void setSkipDependencies(boolean skipDependencies) {
        this.skipDependencies = skipDependencies;
    }

    public List<AnalyzerOverride> getAnalyzerOverrides() {
        return analyzerOverrides;
    }

    public void setAnalyzerOverrides(List<AnalyzerOverride> analyzerOverrides) {
        this.analyzerOverrides = analyzerOverrides;
    }

    public boolean isSaveDailyHistory() {
        return saveDailyHistory;
    }

    public void setSaveDailyHistory(boolean saveDailyHistory) {
        this.saveDailyHistory = saveDailyHistory;
    }

    public boolean isCacheSourceFiles() {
        return cacheSourceFiles;
    }

    public void setCacheSourceFiles(boolean cacheSourceFiles) {
        this.cacheSourceFiles = cacheSourceFiles;
    }
}
