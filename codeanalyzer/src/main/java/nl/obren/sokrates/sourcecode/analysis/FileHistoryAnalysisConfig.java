/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.contributors.ContributorsImport;
import nl.obren.sokrates.sourcecode.contributors.GitContributorsUtil;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.GitHistoryUtil;
import nl.obren.sokrates.sourcecode.githistory.AuthorCommit;
import nl.obren.sokrates.sourcecode.githistory.CommitsPerExtension;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileHistoryAnalysisConfig {
    // A path to commit history file
    private String importPath = "../" + GitHistoryUtils.GIT_HISTORY_FILE_NAME;

    // An optional list of regex expression used to ignore commits from contributors
    private List<String> ignoreContributors = new ArrayList<>();

    // An optional list of regex expression used to detect bots
    private List<String> bots = new ArrayList<>(Arrays.asList(".*\\[bot\\].*", ".*[-]bot[@].*"));

    // If true, contributors IDs (e.g. emails) will be replaced with anonymous IDs (e.g. Contributor 1, Contributor 2)
    private boolean anonymizeContributors = false;

    // An optional list of string transformation used to transform contributor IDs (e.g. to remove domain from email)
    private List<OperationStatement> transformContributorEmails = new ArrayList<>();

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
        return new GitHistoryUtil().importGitLsFilesExport(getFilesHistoryFile(sokratesConfigFolder), this);
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
    public ContributorsImport getContributors(File sokratesConfigFolder, FileHistoryAnalysisConfig config) {
        ProcessingStopwatch.start("analysis/contributors/loading/import");
        ContributorsImport contributorsImport = GitContributorsUtil.importGitContributorsExport(getContributorsFile(sokratesConfigFolder), config);
        ProcessingStopwatch.end("analysis/contributors/loading/import");
        ProcessingStopwatch.start("analysis/contributors/loading/ignore filtering");
        List<Contributor> contributors = contributorsImport.getContributors()
                .stream()
                .filter(c -> !GitHistoryUtils.shouldIgnore(c.getEmail(), ignoreContributors))
                .collect(Collectors.toList());
        contributorsImport.setContributors(contributors);
        ProcessingStopwatch.end("analysis/contributors/loading/ignore filtering");
        return contributorsImport;
    }

    @JsonIgnore
    public List<CommitsPerExtension> getCommitsPerExtension(File sokratesConfigFolder, FileHistoryAnalysisConfig config) {
        return GitContributorsUtil.getCommitsPerExtension(getContributorsFile(sokratesConfigFolder), config);
    }

    public List<String> getIgnoreContributors() {
        return ignoreContributors;
    }

    public void setIgnoreContributors(List<String> ignoreContributors) {
        this.ignoreContributors = ignoreContributors;
    }

    public List<String> getBots() {
        return bots;
    }

    public void setBots(List<String> bots) {
        this.bots = bots;
    }

    public List<OperationStatement> getTransformContributorEmails() {
        return transformContributorEmails;
    }

    public void setTransformContributorEmails(List<OperationStatement> transformContributorEmails) {
        this.transformContributorEmails = transformContributorEmails;
    }

    public boolean isAnonymizeContributors() {
        return anonymizeContributors;
    }

    public void setAnonymizeContributors(boolean anonymizeContributors) {
        this.anonymizeContributors = anonymizeContributors;
    }

}
