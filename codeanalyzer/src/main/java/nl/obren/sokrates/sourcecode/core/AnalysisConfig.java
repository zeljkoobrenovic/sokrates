/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.AnalyzerOverride;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AnalysisConfig {
    private boolean skipDuplication = false;
    private boolean skipDependencies = false;
    private boolean cacheSourceFiles = true;
    private boolean saveDailyHistory = false;
    private List<AnalyzerOverride> analyzerOverrides = new ArrayList<>();
    private String filesHistoryImportPath = "";

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

    public String getFilesHistoryImportPath() {
        return filesHistoryImportPath;
    }

    public void setFilesHistoryImportPath(String filesHistoryImportPath) {
        this.filesHistoryImportPath = filesHistoryImportPath;
    }

    @JsonIgnore
    public File getFilesHistoryFile(File sokratesFolder) {
        if (new File(filesHistoryImportPath).exists()) {
            return new File(filesHistoryImportPath);
        } else {
            return new File(sokratesFolder, filesHistoryImportPath);
        }
    }

    @JsonIgnore
    public boolean filesHistoryImportPathExists(File sokratesFolder) {
        if (StringUtils.isBlank(filesHistoryImportPath)) {
            return false;
        }

        return getFilesHistoryFile(sokratesFolder).exists();
    }
}
