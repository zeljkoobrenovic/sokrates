/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.findings;

public class Findings {
    private String summary = "";
    private String content = "";

    private Runnable saveCallback;

    public Findings(Runnable saveCallback) {
        this.saveCallback = saveCallback;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void save() {
        if (this.saveCallback != null) {
            this.saveCallback.run();
        }
    }
}
