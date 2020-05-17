/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.SokratesProjectLink;

public class ProjectAnalysisResults {
    private SokratesProjectLink sokratesProjectLink;
    private CodeAnalysisResults analysisResults;

    public ProjectAnalysisResults() {
    }

    public ProjectAnalysisResults(SokratesProjectLink sokratesProjectLink, CodeAnalysisResults analysisResults) {
        this.sokratesProjectLink = sokratesProjectLink;
        this.analysisResults = analysisResults;
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
}
