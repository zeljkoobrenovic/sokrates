/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class FilePairChangedTogether {
    private SourceFile sourceFile1;
    private SourceFile sourceFile2;
    private int commitsCountFile1;
    private int commitsCountFile2;
    private String latestCommit = "";

    private List<String> commits = new ArrayList<>();

    public FilePairChangedTogether() {
    }

    public FilePairChangedTogether(SourceFile sourceFile1, SourceFile sourceFile2) {
        this.sourceFile1 = sourceFile1;
        this.sourceFile2 = sourceFile2;
    }

    public SourceFile getSourceFile1() {
        return sourceFile1;
    }

    public void setSourceFile1(SourceFile sourceFile1) {
        this.sourceFile1 = sourceFile1;
    }

    public SourceFile getSourceFile2() {
        return sourceFile2;
    }

    public void setSourceFile2(SourceFile sourceFile2) {
        this.sourceFile2 = sourceFile2;
    }

    public List<String> getCommits() {
        return commits;
    }

    public void setCommits(List<String> commits) {
        this.commits = commits;
    }

    public int getCommitsCountFile1() {
        return commitsCountFile1;
    }

    public void setCommitsCountFile1(int commitsCountFile1) {
        this.commitsCountFile1 = commitsCountFile1;
    }

    public int getCommitsCountFile2() {
        return commitsCountFile2;
    }

    public void setCommitsCountFile2(int commitsCountFile2) {
        this.commitsCountFile2 = commitsCountFile2;
    }

    public String getLatestCommit() {
        return latestCommit;
    }

    public void setLatestCommit(String latestCommit) {
        this.latestCommit = latestCommit;
    }
}
