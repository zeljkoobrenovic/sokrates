/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.contributors.ContributorsImport;
import nl.obren.sokrates.sourcecode.contributors.GitContributorsUtil;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.GitHistoryUtil;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileHistoryAnalysisConfig {
    private String importPath = "../git-history.txt";
    private List<String> ignoreContributors = new ArrayList<>();

    public String getImportPath() {
        return importPath;
    }

    public void setImportPath(String importPath) {
        this.importPath = importPath;
    }

    @JsonIgnore
    public File getFilesHistoryFile(File sokratesConfigFolder) {
        return new File(sokratesConfigFolder, importPath);
    }

    @JsonIgnore
    public List<FileModificationHistory> getHistory(File sokratesConfigFolder) {
        return new GitHistoryUtil().importGitLsFilesExport(getFilesHistoryFile(sokratesConfigFolder), ignoreContributors);
    }

    @JsonIgnore
    public boolean filesHistoryImportPathExists(File sokratesConfigFolder) {
        if (StringUtils.isBlank(importPath)) {
            return false;
        }

        return getFilesHistoryFile(sokratesConfigFolder).exists()
                && getHistory(sokratesConfigFolder).size() > 0;
    }

    @JsonIgnore
    public File getContributorsFile(File sokratesConfigFolder) {
        return new File(sokratesConfigFolder, importPath);
    }

    @JsonIgnore
    public ContributorsImport getContributors(File sokratesConfigFolder) {
        ContributorsImport contributorsImport = GitContributorsUtil.importGitContributorsExport(getContributorsFile(sokratesConfigFolder));
        List<Contributor> contributors = contributorsImport.getContributors()
                .stream()
                .filter(c -> !GitHistoryUtils.shouldIgnore(c.getEmail(), ignoreContributors))
                .collect(Collectors.toList());
        contributorsImport.setContributors(contributors);
        return contributorsImport;
    }

    @JsonIgnore
    public List<CommitsPerExtension> getCommitsPerExtension(File sokratesConfigFolder) {
        return GitContributorsUtil.getCommitsPerExtension(getContributorsFile(sokratesConfigFolder));
    }

    public List<String> getIgnoreContributors() {
        return ignoreContributors;
    }

    public void setIgnoreContributors(List<String> ignoreContributors) {
        this.ignoreContributors = ignoreContributors;
    }
}
