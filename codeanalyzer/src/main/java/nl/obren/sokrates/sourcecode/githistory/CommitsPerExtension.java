/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.githistory;

import java.util.ArrayList;
import java.util.List;

public class CommitsPerExtension {
    private String extension = "";
    private int commitsCount = 0;
    private List<String> committers = new ArrayList<>();
    private int filesCount = 0;
    private int commitsCount30Days = 0;
    private List<String> committers30Days = new ArrayList<>();
    private int filesCount30Days = 0;
    private int commitsCount90Days = 0;
    private List<String> committers90Days = new ArrayList<>();
    private int filesCount90Days = 0;

    public CommitsPerExtension() {
    }

    public CommitsPerExtension(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public void setCommitsCount30Days(int commitsCount30Days) {
        this.commitsCount30Days = commitsCount30Days;
    }

    public int getFilesCount30Days() {
        return filesCount30Days;
    }

    public void setFilesCount30Days(int filesCount30Days) {
        this.filesCount30Days = filesCount30Days;
    }

    public int getCommitsCount90Days() {
        return commitsCount90Days;
    }

    public void setCommitsCount90Days(int commitsCount90Days) {
        this.commitsCount90Days = commitsCount90Days;
    }

    public int getFilesCount90Days() {
        return filesCount90Days;
    }

    public void setFilesCount90Days(int filesCount90Days) {
        this.filesCount90Days = filesCount90Days;
    }

    public List<String> getCommitters() {
        return committers;
    }

    public void setCommitters(List<String> committers) {
        this.committers = committers;
    }

    public List<String> getCommitters30Days() {
        return committers30Days;
    }

    public void setCommitters30Days(List<String> committers30Days) {
        this.committers30Days = committers30Days;
    }

    public List<String> getCommitters90Days() {
        return committers90Days;
    }

    public void setCommitters90Days(List<String> committers90Days) {
        this.committers90Days = committers90Days;
    }
}
