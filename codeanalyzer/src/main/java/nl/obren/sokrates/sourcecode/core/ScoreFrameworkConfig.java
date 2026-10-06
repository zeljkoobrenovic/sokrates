/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A user-defined maintainability score framework ({@code analysis.maintainabilityScores.customFramework}), used
 * instead of the built-in one only when {@code analysis.maintainabilityScores.useCustomFramework} is true. It lists
 * the sub-scores (built-in measures by key, or any analysis metric by id), each with its own anchors and Human and AI
 * weights, and the rules that combine them. Values left out fall back to the built-in ones.
 */
public class ScoreFrameworkConfig {
    // The sub-scores; only these count
    private List<SubScoreConfig> subScores = new ArrayList<>();

    // The total is capped at the weakest sub-score + this margin; null = the built-in 4, negative = no cap
    private Double weakestLinkMargin;

    // Sub-score keys that never cap the total; null = the built-in ["knowledge"]
    private List<String> capExcludes;

    // Lowest totals for the grades A, B, C and D (below D: E); null = the built-in [8, 6.5, 5, 3.5]
    private List<Double> gradeThresholds;

    public static class SubScoreConfig {
        // A built-in measure (volume, duplication, unitSize, ...) or, with "metric", your own key
        private String key = "";

        // The label shown; empty = the built-in label, or the key
        private String label = "";

        // An analysis metric id (e.g. LINES_OF_CODE_TEST); the sub-score then measures its value
        private String metric = "";

        // What the metric's value is, shown after it (e.g. "TODO comments"); empty = the metric id
        private String description = "";

        // [measure, score 0-10] points, in any order; required for a metric, else replaces the built-in anchors
        private List<List<Double>> anchors = new ArrayList<>();

        private double humanWeight = 1;
        private double aiWeight = 1;

        // Why the sub-score matters for people / for AI agents, shown when its row is opened; empty = the built-in text
        private String whyHuman = "";
        private String whyAi = "";

        public SubScoreConfig() {
        }

        public SubScoreConfig(String key, double humanWeight, double aiWeight) {
            this.key = key;
            this.humanWeight = humanWeight;
            this.aiWeight = aiWeight;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key != null ? key : "";
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label != null ? label : "";
        }

        public String getMetric() {
            return metric;
        }

        public void setMetric(String metric) {
            this.metric = metric != null ? metric : "";
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description != null ? description : "";
        }

        public List<List<Double>> getAnchors() {
            return anchors;
        }

        public void setAnchors(List<List<Double>> anchors) {
            this.anchors = anchors != null ? anchors : new ArrayList<>();
        }

        public SubScoreConfig anchors(double[]... points) {
            this.anchors = new ArrayList<>();
            for (double[] point : points) {
                this.anchors.add(Arrays.asList(point[0], point[1]));
            }
            return this;
        }

        public double getHumanWeight() {
            return humanWeight;
        }

        public void setHumanWeight(double humanWeight) {
            this.humanWeight = humanWeight;
        }

        public double getAiWeight() {
            return aiWeight;
        }

        public String getWhyHuman() {
            return whyHuman;
        }

        public void setWhyHuman(String whyHuman) {
            this.whyHuman = whyHuman != null ? whyHuman : "";
        }

        public String getWhyAi() {
            return whyAi;
        }

        public void setWhyAi(String whyAi) {
            this.whyAi = whyAi != null ? whyAi : "";
        }

        public void setAiWeight(double aiWeight) {
            this.aiWeight = aiWeight;
        }
    }

    public List<SubScoreConfig> getSubScores() {
        return subScores;
    }

    public void setSubScores(List<SubScoreConfig> subScores) {
        this.subScores = subScores != null ? subScores : new ArrayList<>();
    }

    public Double getWeakestLinkMargin() {
        return weakestLinkMargin;
    }

    public void setWeakestLinkMargin(Double weakestLinkMargin) {
        this.weakestLinkMargin = weakestLinkMargin;
    }

    public List<String> getCapExcludes() {
        return capExcludes;
    }

    public void setCapExcludes(List<String> capExcludes) {
        this.capExcludes = capExcludes;
    }

    public List<Double> getGradeThresholds() {
        return gradeThresholds;
    }

    public void setGradeThresholds(List<Double> gradeThresholds) {
        this.gradeThresholds = gradeThresholds;
    }
}
