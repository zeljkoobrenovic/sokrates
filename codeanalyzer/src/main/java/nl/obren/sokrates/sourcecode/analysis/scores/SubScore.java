/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.scores;

/**
 * One measured aspect of a maintainability score: the measured value (e.g. 12.5 for "12.5% duplicated"), its
 * 0–10 score, the weight it has in the total, and its drag — how much higher the total would be if this
 * sub-score were a perfect 10 — which ranks what to improve first.
 */
public class SubScore {
    private String key = "";
    private String label = "";
    private double measure;
    private String measureText = "";
    private double score;
    private double weight;
    private double drag;

    public SubScore() {
    }

    public SubScore(String key, String label, double measure, String measureText, double score) {
        this.key = key;
        this.label = label;
        this.measure = measure;
        this.measureText = measureText;
        this.score = score;
    }

    public SubScore withWeight(double weight) {
        SubScore copy = new SubScore(key, label, measure, measureText, score);
        copy.weight = weight;
        return copy;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getMeasure() {
        return measure;
    }

    public void setMeasure(double measure) {
        this.measure = measure;
    }

    public String getMeasureText() {
        return measureText;
    }

    public void setMeasureText(String measureText) {
        this.measureText = measureText;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getDrag() {
        return drag;
    }

    public void setDrag(double drag) {
        this.drag = drag;
    }
}
