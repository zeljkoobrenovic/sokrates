/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

public class ReferenceAnalysisResult {
    private String label = "DATE";
    private String analysisResultsPath = "reports/history/DATE/analysisResults.zip";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAnalysisResultsPath() {
        return analysisResultsPath;
    }

    public void setAnalysisResultsPath(String analysisResultsPath) {
        this.analysisResultsPath = analysisResultsPath;
    }
}
