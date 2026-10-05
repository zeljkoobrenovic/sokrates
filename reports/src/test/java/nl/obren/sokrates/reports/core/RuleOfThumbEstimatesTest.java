package nl.obren.sokrates.reports.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleOfThumbEstimatesTest {
    @Test
    void rendersAClosedDetailsWithTheLinesOfCode() {
        String html = RuleOfThumbEstimates.html(200000, 50000, 1234, 7, 14);
        assertTrue(html.trim().startsWith("<details class=\"sk-est\" id=\"sk-est\""));
        assertFalse(html.contains(" open"), "closed by default");
        assertTrue(html.contains("data-main-loc=\"200000\""));
        assertTrue(html.contains("data-test-loc=\"50000\""));
        assertTrue(html.contains("data-other-loc=\"1234\""));
        assertTrue(html.contains("data-tokens-min=\"7\""));
        assertTrue(html.contains("data-tokens-max=\"14\""));
        assertTrue(html.contains("value=\"10000\""), "10,000 lines of code per man-year");
        assertTrue(html.contains("id=\"sk-est-maintenance\"") && html.contains("value=\"15\""), "15% maintenance");
        assertFalse(html.contains("${"), "every placeholder is filled");
    }

    @Test
    void sanitizesTheTokenRange() {
        String html = RuleOfThumbEstimates.html(-5, 0, 0, 20, 0);
        assertTrue(html.contains("data-main-loc=\"0\""));
        assertTrue(html.contains("data-tokens-min=\"1\""));
        assertTrue(html.contains("data-tokens-max=\"20\""));
    }
}
