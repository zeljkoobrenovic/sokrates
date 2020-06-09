/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

public class DependencyEvidence {
    private String pathFrom = "";
    private String evidence;

    public DependencyEvidence() {
    }

    public DependencyEvidence(String pathFrom, String evidence) {
        this.pathFrom = pathFrom;
        this.evidence = evidence;
    }

    public String getPathFrom() {
        return pathFrom;
    }

    public void setPathFrom(String pathFrom) {
        this.pathFrom = pathFrom;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}
