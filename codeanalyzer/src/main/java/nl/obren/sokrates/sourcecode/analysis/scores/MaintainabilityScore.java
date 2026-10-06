/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.scores;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    // The total is capped at the weakest sub-score + this margin (negative: no cap)
    private double capMargin = 4;
    // Coverage: how many sub-scores count in this score (weight > 0), and the labels of those that could not be
    // measured (their analysis did not run); 0 = unknown (an analysis from before coverage was recorded)
    private int subScoresTotal;
    private List<String> notMeasured = new ArrayList<>();

    public int getSubScoresTotal() {
        return subScoresTotal;
    }

    public void setSubScoresTotal(int subScoresTotal) {
        this.subScoresTotal = subScoresTotal;
    }

    public List<String> getNotMeasured() {
        return notMeasured;
    }

    public void setNotMeasured(List<String> notMeasured) {
        this.notMeasured = notMeasured != null ? notMeasured : new ArrayList<>();
    }

    /** False only when some sub-score of the score could not be measured. */
    @JsonIgnore
    public boolean isFullyMeasured() {
        return subScoresTotal <= 0 || subScores.size() >= subScoresTotal;
    }

    /** "8/10", or "" when the coverage is unknown. */
    @JsonIgnore
    public String getCoverageShort() {
        return subScoresTotal <= 0 ? "" : subScores.size() + "/" + subScoresTotal;
    }

    /** "measured on 8 of 10 sub-scores (not measured: Change entropy, Context per change)", or "". */
    @JsonIgnore
    public String getCoverageText() {
        if (subScoresTotal <= 0) {
            return "";
        }
        String text = String.format(Locale.US, "measured on %d of %d sub-scores", subScores.size(), subScoresTotal);
        return notMeasured.isEmpty() ? text : text + " (not measured: " + String.join(", ", notMeasured) + ")";
    }

    public double getCapMargin() {
        return capMargin;
    }

    public void setCapMargin(double capMargin) {
        this.capMargin = capMargin;
    }

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
