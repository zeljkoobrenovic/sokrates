/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

public class CleaningResult {
    private String content;
    int currentIndex;

    public CleaningResult() {
    }

    public CleaningResult(String content, int currentIndex) {
        this.content = content;
        this.currentIndex = currentIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }
}
