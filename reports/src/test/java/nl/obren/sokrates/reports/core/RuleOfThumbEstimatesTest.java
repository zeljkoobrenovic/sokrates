package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

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

    @AfterEach
    void resetDates() {
        DateUtils.setDateParam(null);
        DateUtils.reset();
    }

    private static CodeAnalysisResults repository(int main, int test, int build, String latestCommitDate) {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.getMainAspectAnalysisResults().setLinesOfCode(main);
        results.getTestAspectAnalysisResults().setLinesOfCode(test);
        results.getBuildAndDeployAspectAnalysisResults().setLinesOfCode(build);
        results.getContributorsAnalysisResults().setLatestCommitDate(latestCommitDate);
        return results;
    }

    @Test
    void landscapeWindowsSumTheRepositoriesByLatestCommit() {
        DateUtils.setDateParam("2026-10-05");
        DateUtils.reset();
        List<RuleOfThumbEstimates.Window> windows = RuleOfThumbEstimates.windows(Arrays.asList(
                repository(1000, 100, 10, "2026-10-01"),
                repository(20000, 2000, 200, "2026-03-01"),
                repository(300000, 0, 0, "2023-01-01"),
                repository(5, 0, 0, "")));

        assertEquals(Arrays.asList("30", "90", "180", "365", "730", "all"),
                windows.stream().map(RuleOfThumbEstimates.Window::getId).collect(java.util.stream.Collectors.toList()));
        assertEquals(1, windows.get(0).getRepositories());
        assertEquals(1000, windows.get(0).getMainLoc());
        assertEquals(2, windows.get(3).getRepositories(), "past year");
        assertEquals(21000, windows.get(3).getMainLoc());
        assertEquals(4, windows.get(5).getRepositories(), "all time includes repositories without history");
        assertEquals(321005, windows.get(5).getMainLoc());

        String html = RuleOfThumbEstimates.html(21000, 2100, 210, 7, 14, windows);
        assertTrue(html.contains("data-default-window=\"365\""));
        assertTrue(html.contains("&quot;id&quot;:&quot;365&quot;"), "the windows are an escaped JSON attribute");
        assertTrue(html.contains("&quot;other&quot;:210"));
        assertFalse(html.contains("${"));
    }

    @Test
    void aRepositoryHasNoWindows() {
        assertTrue(RuleOfThumbEstimates.html(1, 1, 1, 7, 14).contains("data-windows=\"\""));
    }
}
