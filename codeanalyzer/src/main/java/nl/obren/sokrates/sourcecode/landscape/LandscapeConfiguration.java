/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.Metadata;

import java.util.ArrayList;
import java.util.List;

public class LandscapeConfiguration {
    private Metadata metadata = new Metadata();
    private String analysisRoot = "";

    private List<LandscapeGroup> groups = new ArrayList<>();

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public List<LandscapeGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<LandscapeGroup> groups) {
        this.groups = groups;
    }

    public String getAnalysisRoot() {
        return analysisRoot;
    }

    public void setAnalysisRoot(String analysisRoot) {
        this.analysisRoot = analysisRoot;
    }
}
