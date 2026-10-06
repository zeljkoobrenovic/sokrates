/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settings of the maintainability scores ({@code analysis.maintainabilityScores}): whether they are computed and
 * shown, an optional own score framework ({@link ScoreFrameworkConfig}, behind the explicit
 * {@code useCustomFramework} switch), and optional weight overrides per sub-score key for the Human and the AI score (a key left out keeps its default
 * weight, 0 leaves the sub-score out of that score). The keys and default weights are in
 * {@code MaintainabilityScoresAnalyzer}.
 */
public class MaintainabilityScoresConfig {
    // If false, no scores are computed, shown or exported
    private boolean enabled = true;

    // If false, the scores are computed and kept in the data (analysisResults.json, metrics) but not shown in the report
    private boolean show = true;

    // Sub-score key -> weight overrides for the Human score
    private Map<String, Double> humanWeights = new LinkedHashMap<>();

    // Sub-score key -> weight overrides for the AI score
    private Map<String, Double> aiWeights = new LinkedHashMap<>();

    // If true, customFramework replaces the built-in sub-scores, weights (humanWeights/aiWeights are then ignored),
    // cap and grades
    private boolean useCustomFramework = false;

    // Your own score framework, used only when useCustomFramework is true
    private ScoreFrameworkConfig customFramework = new ScoreFrameworkConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    public Map<String, Double> getHumanWeights() {
        return humanWeights;
    }

    public void setHumanWeights(Map<String, Double> humanWeights) {
        this.humanWeights = humanWeights != null ? humanWeights : new LinkedHashMap<>();
    }

    public Map<String, Double> getAiWeights() {
        return aiWeights;
    }

    public void setAiWeights(Map<String, Double> aiWeights) {
        this.aiWeights = aiWeights != null ? aiWeights : new LinkedHashMap<>();
    }

    public boolean isUseCustomFramework() {
        return useCustomFramework;
    }

    public void setUseCustomFramework(boolean useCustomFramework) {
        this.useCustomFramework = useCustomFramework;
    }

    public ScoreFrameworkConfig getCustomFramework() {
        return customFramework;
    }

    public void setCustomFramework(ScoreFrameworkConfig customFramework) {
        this.customFramework = customFramework != null ? customFramework : new ScoreFrameworkConfig();
    }
}
