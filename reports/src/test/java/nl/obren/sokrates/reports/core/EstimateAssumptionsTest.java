package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.EstimateAssumptionsConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class EstimateAssumptionsTest {
    private static CodeAnalysisResults results(boolean enabled) {
        CodeConfiguration configuration = new CodeConfiguration();
        EstimateAssumptionsConfig config = configuration.getAnalysis().getEstimateAssumptions();
        config.setEnabled(enabled);
        config.getRuleOfThumb().setLinesOfCodePerManYear(8000.0);
        config.getRuleOfThumb().setCurrency("<b>");
        config.getRuleOfThumb().setMaintenancePercentage(-1.0);
        config.getAiCostEstimator().put("p-in", 2.5);
        config.getAiCostEstimator().put("steps", Arrays.asList(5, 15));
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(configuration);
        return results;
    }

    @Test
    void ignoredUnlessEnabled() {
        assertFalse(new CodeConfiguration().getAnalysis().getEstimateAssumptions().isEnabled(), "off by default");
        assertEquals("{}", EstimateAssumptions.ruleOfThumbJson(results(false)));
        assertEquals("{}", EstimateAssumptions.aiCostEstimatorJson(results(false)));
        assertEquals("{}", EstimateAssumptions.aiCostEstimatorJson(new CodeAnalysisResults()));
    }

    @Test
    void enabledValuesAreScriptSafeJson() {
        assertEquals("{\"loc-per-my\":8000.0,\"currency\":\"\\u003cb\\u003e\"}", EstimateAssumptions.ruleOfThumbJson(results(true)),
                "page ids; a negative value is left out; markup escaped");
        assertEquals("{\"p-in\":2.5,\"steps\":[5,15]}", EstimateAssumptions.aiCostEstimatorJson(results(true)));
    }

    @Test
    void theRuleOfThumbPageCarriesThemAsAnEscapedAttribute() {
        String html = RuleOfThumbEstimates.html(1000, null, EstimateAssumptions.ruleOfThumbJson(results(true)));
        assertTrue(html.contains("data-assumptions=\"{&quot;loc-per-my&quot;:8000.0,"));
        assertFalse(RuleOfThumbEstimates.html(1000, null).contains("${"));
    }

    @Test
    void readFromConfigJson() throws IOException {
        String json = "{\"analysis\": {\"estimateAssumptions\": {\"enabled\": true, \"ruleOfThumb\": {\"costPerManYear\": 120000},"
                + " \"aiCostEstimator\": {\"r-level\": \"medium\", \"tool\": [500, 2000]}}}}";
        CodeConfiguration configuration = (CodeConfiguration) new JsonMapper().getObject(json, CodeConfiguration.class);
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(configuration);
        assertEquals("{\"cost-per-my\":120000.0}", EstimateAssumptions.ruleOfThumbJson(results));
        assertEquals("{\"r-level\":\"medium\",\"tool\":[500,2000]}", EstimateAssumptions.aiCostEstimatorJson(results));
    }
}
