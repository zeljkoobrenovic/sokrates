/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.EstimateAssumptionsConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The configured assumptions ({@code analysis.estimateAssumptions}) as JSON for the estimate pages, which use them
 * as their defaults. Empty ({@code {}}) unless the configuration enables them, so the pages keep their built-in
 * defaults. The JSON is safe inside a script block ({@code <}, {@code >} and {@code &} as unicode escapes).
 */
public class EstimateAssumptions {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static EstimateAssumptionsConfig enabledConfig(CodeAnalysisResults results) {
        if (results == null || results.getCodeConfiguration() == null) return null;
        EstimateAssumptionsConfig config = results.getCodeConfiguration().getAnalysis().getEstimateAssumptions();
        return config != null && config.isEnabled() ? config : null;
    }

    /** The Rule-of-thumb estimates' values, keyed by the page's input ids. */
    public static String ruleOfThumbJson(CodeAnalysisResults results) {
        EstimateAssumptionsConfig config = enabledConfig(results);
        Map<String, Object> values = new LinkedHashMap<>();
        if (config != null) {
            EstimateAssumptionsConfig.RuleOfThumb ruleOfThumb = config.getRuleOfThumb();
            putPositive(values, "loc-per-my", ruleOfThumb.getLinesOfCodePerManYear());
            putPositive(values, "cost-per-my", ruleOfThumb.getCostPerManYear());
            if (ruleOfThumb.getCurrency() != null) values.put("currency", StringUtils.left(ruleOfThumb.getCurrency(), 4));
            putPositive(values, "maintenance", ruleOfThumb.getMaintenancePercentage());
        }
        return json(values);
    }

    /** The AI Cost Estimator's values, keyed by its assumption ids (the page checks them). */
    public static String aiCostEstimatorJson(CodeAnalysisResults results) {
        EstimateAssumptionsConfig config = enabledConfig(results);
        return json(config != null ? config.getAiCostEstimator() : new LinkedHashMap<>());
    }

    private static void putPositive(Map<String, Object> values, String key, Double value) {
        if (value != null && value >= 0 && !value.isInfinite() && !value.isNaN()) values.put(key, value);
    }

    static String json(Map<String, Object> values) {
        try {
            return MAPPER.writeValueAsString(values)
                    .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
