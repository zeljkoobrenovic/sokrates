/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.scores;

import java.util.ArrayList;
import java.util.List;

/**
 * A 0–10 score (one decimal) with its A–E grade and the weighted sub-scores it was computed from, in a fixed
 * order (see {@link MaintainabilityScoresAnalyzer#KEYS}).
 */
public class MaintainabilityScore {
    private double value;
    private String grade = "";
    private List<SubScore> subScores = new ArrayList<>();
    // The label of the sub-score whose weakest-link cap set the total, or "" when the geometric mean did
    private String cappedBy = "";

    public String getCappedBy() {
        return cappedBy;
    }

    public void setCappedBy(String cappedBy) {
        this.cappedBy = cappedBy != null ? cappedBy : "";
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public List<SubScore> getSubScores() {
        return subScores;
    }

    public void setSubScores(List<SubScore> subScores) {
        this.subScores = subScores != null ? subScores : new ArrayList<>();
    }
}
