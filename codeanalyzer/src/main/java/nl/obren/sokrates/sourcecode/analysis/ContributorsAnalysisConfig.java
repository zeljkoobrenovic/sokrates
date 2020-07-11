/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.contributors.ContributorsImport;
import nl.obren.sokrates.sourcecode.contributors.GitContributorsUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;

public class ContributorsAnalysisConfig {
    // private int activeThresholdDays = 180; // 6 months
    private String importPath = "../git-contributors-log.txt";

    public String getImportPath() {
        return importPath;
    }

    public void setImportPath(String importPath) {
        this.importPath = importPath;
    }

    @JsonIgnore
    public File getContributorsFile(File sokratesConfigFolder) {
        if (new File(importPath).exists()) {
            return new File(importPath);
        } else {
            return new File(sokratesConfigFolder, importPath);
        }
    }

    @JsonIgnore
    public ContributorsImport getContributors(File sokratesConfigFolder) {
        return GitContributorsUtil.importGitContributorsExport(getContributorsFile(sokratesConfigFolder));
    }

    @JsonIgnore
    public boolean filesHistoryImportPathExists(File sokratesConfigFolder) {
        if (StringUtils.isBlank(importPath)) {
            return false;
        }

        return getContributorsFile(sokratesConfigFolder).exists()
                && getContributors(sokratesConfigFolder).getContributors().size() > 0;
    }
}
