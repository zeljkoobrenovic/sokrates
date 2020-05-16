/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import java.io.File;

public class SokratesProjectLink {
    private String analysisResultsPath = "";
    private String htmlReportsIndexPath = "";
    private String note = "";

    public SokratesProjectLink() {
    }

    public SokratesProjectLink(String analysisResultsPath) {
        this.analysisResultsPath = analysisResultsPath;
        File reportsRoot = new File(analysisResultsPath).getParentFile().getParentFile();
        File htmlRoot = new File(reportsRoot, "html");
        this.htmlReportsIndexPath = new File(htmlRoot, "index.html").getPath();
    }

    public String getAnalysisResultsPath() {
        return analysisResultsPath;
    }

    public void setAnalysisResultsPath(String analysisResultsPath) {
        this.analysisResultsPath = analysisResultsPath;
    }

    public String getHtmlReportsIndexPath() {
        return htmlReportsIndexPath;
    }

    public void setHtmlReportsIndexPath(String htmlReportsIndexPath) {
        this.htmlReportsIndexPath = htmlReportsIndexPath;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
