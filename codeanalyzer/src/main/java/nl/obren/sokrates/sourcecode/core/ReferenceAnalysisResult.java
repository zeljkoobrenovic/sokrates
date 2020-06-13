/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import java.io.File;

public class ReferenceAnalysisResult {
    private String label = "DATE";
    private File analysisResultsZipFile;

    public ReferenceAnalysisResult() {
    }

    public ReferenceAnalysisResult(String label, File analysisResultsZipFile) {
        this.label = label;
        this.analysisResultsZipFile = analysisResultsZipFile;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public File getAnalysisResultsZipFile() {
        return analysisResultsZipFile;
    }

    public void setAnalysisResultsZipFile(File analysisResultsZipFile) {
        this.analysisResultsZipFile = analysisResultsZipFile;
    }
}
