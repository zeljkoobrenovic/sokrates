/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;

public class SubLandscapeLink {
    private String name = "";
    private String indexFilePath = "";
    // True for virtual landscapes: their files live inside the landscape's own folder
    // (landscapes/<name>/_sokrates_landscape/...), so paths resolve relative to the landscape
    // folder without the repositoryReportsUrlPrefix, and `label` is used for display.
    private boolean virtual = false;
    private String label = "";
    @JsonIgnore
    private LandscapeAnalysisResults landscapeAnalysisResults;

    public SubLandscapeLink() {
    }

    public SubLandscapeLink(String name, String indexFilePath) {
        this.name = name;
        this.indexFilePath = indexFilePath;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    @JsonIgnore
    public LandscapeAnalysisResults getLandscapeAnalysisResults() {
        return landscapeAnalysisResults;
    }

    @JsonIgnore
    public void setLandscapeAnalysisResults(LandscapeAnalysisResults landscapeAnalysisResults) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
    }
}
