/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.SokratesProjectLink;

import java.util.ArrayList;
import java.util.List;

public class ProjectAnalysisResults {
    private SokratesProjectLink sokratesProjectLink;
    private CodeAnalysisResults analysisResults;

    @JsonIgnore
    List<String> files = new ArrayList<>();

    public ProjectAnalysisResults() {
    }

    public ProjectAnalysisResults(SokratesProjectLink sokratesProjectLink, CodeAnalysisResults analysisResults, List<String> files) {
        this.sokratesProjectLink = sokratesProjectLink;
        this.analysisResults = analysisResults;
        this.files = files;
    }

    public SokratesProjectLink getSokratesProjectLink() {
        return sokratesProjectLink;
    }

    public void setSokratesProjectLink(SokratesProjectLink sokratesProjectLink) {
        this.sokratesProjectLink = sokratesProjectLink;
    }

    public CodeAnalysisResults getAnalysisResults() {
        return analysisResults;
    }

    public void setAnalysisResults(CodeAnalysisResults analysisResults) {
        this.analysisResults = analysisResults;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }
}
