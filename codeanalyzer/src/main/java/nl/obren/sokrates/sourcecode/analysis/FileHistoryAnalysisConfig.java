/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.GitLsFileUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class FileHistoryAnalysisConfig {
    private String importPath = "../git-history.txt";

    public String getImportPath() {
        return importPath;
    }

    public void setImportPath(String importPath) {
        this.importPath = importPath;
    }

    @JsonIgnore
    public File getFilesHistoryFile(File sokratesConfigFolder) {
        if (new File(importPath).exists()) {
            return new File(importPath);
        } else {
            return new File(sokratesConfigFolder, importPath);
        }
    }

    @JsonIgnore
    public List<FileModificationHistory> getHistory(File sokratesConfigFolder) {
        return GitLsFileUtil.importGitLsFilesExport(getFilesHistoryFile(sokratesConfigFolder));
    }

    @JsonIgnore
    public boolean filesHistoryImportPathExists(File sokratesConfigFolder) {
        if (StringUtils.isBlank(importPath)) {
            return false;
        }

        return getFilesHistoryFile(sokratesConfigFolder).exists()
                && getHistory(sokratesConfigFolder).size() > 0;
    }
}
