/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import java.io.File;

public class SokratesProjectLink {
    private String analysisResultsPath = "";
    private String htmlReportsRoot = "";
    private String note = "";

    public SokratesProjectLink() {
    }

    public SokratesProjectLink(String analysisResultsPath) {
        this.analysisResultsPath = analysisResultsPath;
        File reportsRoot = new File(analysisResultsPath).getParentFile().getParentFile();
        this.htmlReportsRoot = new File(reportsRoot, "html").getPath();
    }

    public String getAnalysisResultsPath() {
        return analysisResultsPath;
    }

    public void setAnalysisResultsPath(String analysisResultsPath) {
        this.analysisResultsPath = analysisResultsPath;
    }

    public String getHtmlReportsRoot() {
        return htmlReportsRoot;
    }

    public void setHtmlReportsRoot(String htmlReportsRoot) {
        this.htmlReportsRoot = htmlReportsRoot;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
