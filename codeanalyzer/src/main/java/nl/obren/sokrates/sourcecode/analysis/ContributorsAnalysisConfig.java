/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.contributors.ContributorsImport;
import nl.obren.sokrates.sourcecode.contributors.GitContributorsUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContributorsAnalysisConfig {
    // private int activeThresholdDays = 180; // 6 months
    private String importPath = "../git-contributors-log.txt";
    private List<String> ignoreContributors = new ArrayList<>();

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
        ContributorsImport contributorsImport = GitContributorsUtil.importGitContributorsExport(getContributorsFile(sokratesConfigFolder));
        List<Contributor> contributors = contributorsImport.getContributors().stream().filter(c -> !shouldIgnore(c)).collect(Collectors.toList());
        contributorsImport.setContributors(contributors);
        return contributorsImport;
    }

    private boolean shouldIgnore(Contributor contributor) {
        for (String ignorePattern : ignoreContributors) {
            if (RegexUtils.matchesEntirely(ignorePattern, contributor.getId())) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean filesHistoryImportPathExists(File sokratesConfigFolder) {
        if (StringUtils.isBlank(importPath)) {
            return false;
        }

        return getContributorsFile(sokratesConfigFolder).exists()
                && getContributors(sokratesConfigFolder).getContributors().size() > 0;
    }

    public List<String> getIgnoreContributors() {
        return ignoreContributors;
    }

    public void setIgnoreContributors(List<String> ignoreContributors) {
        this.ignoreContributors = ignoreContributors;
    }
}
