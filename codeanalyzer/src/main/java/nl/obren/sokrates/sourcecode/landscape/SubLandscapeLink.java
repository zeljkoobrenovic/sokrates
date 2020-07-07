/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

public class SubLandscapeLink {
    private String name = "";
    private String indexFilePath = "";

    public SubLandscapeLink() {
    }

    public SubLandscapeLink(String name, String indexFilePath) {
        this.name = name;
        this.indexFilePath = indexFilePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndexFilePath() {
        return indexFilePath;
    }

    public void setIndexFilePath(String indexFilePath) {
        this.indexFilePath = indexFilePath;
    }
}
