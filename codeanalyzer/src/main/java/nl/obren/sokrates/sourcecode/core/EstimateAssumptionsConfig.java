/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configured starting values for the assumptions of the report's estimate pages
 * ({@code analysis.estimateAssumptions}): the At a Glance "Rule-of-thumb estimates" and the AI Cost Estimator.
 * Only applied when {@code enabled} is true; otherwise the pages start from their built-in defaults. Either way a
 * viewer can still change the values in the page (remembered in the browser).
 */
public class EstimateAssumptionsConfig {
    // If false (default), the configured values are ignored and the pages use their built-in defaults
    private boolean enabled = false;

    // The rule-of-thumb estimates (a field left out keeps its default)
    private RuleOfThumb ruleOfThumb = new RuleOfThumb();

    // AI Cost Estimator assumption id -> value: a number, [min, max] for a range, or a string (currency, r-level)
    private Map<String, Object> aiCostEstimator = new LinkedHashMap<>();

    public static class RuleOfThumb {
        private Double linesOfCodePerManYear;
        private Double costPerManYear;
        private String currency;
        private Double maintenancePercentage;

        public Double getLinesOfCodePerManYear() {
            return linesOfCodePerManYear;
        }

        public void setLinesOfCodePerManYear(Double linesOfCodePerManYear) {
            this.linesOfCodePerManYear = linesOfCodePerManYear;
        }

        public Double getCostPerManYear() {
            return costPerManYear;
        }

        public void setCostPerManYear(Double costPerManYear) {
            this.costPerManYear = costPerManYear;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public Double getMaintenancePercentage() {
            return maintenancePercentage;
        }

        public void setMaintenancePercentage(Double maintenancePercentage) {
            this.maintenancePercentage = maintenancePercentage;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RuleOfThumb getRuleOfThumb() {
        return ruleOfThumb;
    }

    public void setRuleOfThumb(RuleOfThumb ruleOfThumb) {
        this.ruleOfThumb = ruleOfThumb != null ? ruleOfThumb : new RuleOfThumb();
    }

    public Map<String, Object> getAiCostEstimator() {
        return aiCostEstimator;
    }

    public void setAiCostEstimator(Map<String, Object> aiCostEstimator) {
        this.aiCostEstimator = aiCostEstimator != null ? aiCostEstimator : new LinkedHashMap<>();
    }
}
