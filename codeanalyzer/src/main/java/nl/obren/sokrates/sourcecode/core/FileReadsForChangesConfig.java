/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

/**
 * Settings of the File Size report's "Large Files That Change Often" section and the matching Highlights tile and
 * list ({@code analysis.fileReadsForChanges}): the main files read most for changes (lines × changes in the window),
 * with the tokens to read them.
 */
public class FileReadsForChangesConfig {
    // If false, the File Size section and the Highlights tile and list are not shown
    private boolean enabled = true;

    // The days of git history whose commits count as changes (0 or less: the whole history)
    private int windowDays = 365;

    // The number of files listed
    private int maxFiles = 20;

    // Files above this many lines count as large in the Highlights tile and list (about what an AI coding agent reads at once)
    private int largeFileLines = 2000;

    // Tokens per line of code, shown as a range from min to max (about 4 characters per token; newer tokenizers count more)
    private int tokensPerLineMin = 7;
    private int tokensPerLineMax = 14;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public int getLargeFileLines() {
        return largeFileLines;
    }

    public void setLargeFileLines(int largeFileLines) {
        this.largeFileLines = largeFileLines;
    }

    public int getTokensPerLineMin() {
        return tokensPerLineMin;
    }

    public void setTokensPerLineMin(int tokensPerLineMin) {
        this.tokensPerLineMin = tokensPerLineMin;
    }

    public int getTokensPerLineMax() {
        return tokensPerLineMax;
    }

    public void setTokensPerLineMax(int tokensPerLineMax) {
        this.tokensPerLineMax = tokensPerLineMax;
    }
}
