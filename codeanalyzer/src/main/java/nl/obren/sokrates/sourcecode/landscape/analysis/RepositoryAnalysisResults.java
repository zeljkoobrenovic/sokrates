/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;

import java.util.ArrayList;
import java.util.List;

public class RepositoryAnalysisResults {
    private SokratesRepositoryLink sokratesRepositoryLink;
    private CodeAnalysisResults analysisResults;

    @JsonIgnore
    List<FileExport> files = new ArrayList<>();

    public RepositoryAnalysisResults() {
    }

    public RepositoryAnalysisResults(SokratesRepositoryLink sokratesRepositoryLink, CodeAnalysisResults analysisResults, List<FileExport> files) {
        this.sokratesRepositoryLink = sokratesRepositoryLink;
        this.analysisResults = analysisResults;
        this.files = files;
    }

    public SokratesRepositoryLink getSokratesRepositoryLink() {
        return sokratesRepositoryLink;
    }

    public void setSokratesRepositoryLink(SokratesRepositoryLink sokratesRepositoryLink) {
        this.sokratesRepositoryLink = sokratesRepositoryLink;
    }

    public CodeAnalysisResults getAnalysisResults() {
        return analysisResults;
    }

    public void setAnalysisResults(CodeAnalysisResults analysisResults) {
        this.analysisResults = analysisResults;
    }

    public List<FileExport> getFiles() {
        return files;
    }

    public void setFiles(List<FileExport> files) {
        this.files = files;
    }
}
