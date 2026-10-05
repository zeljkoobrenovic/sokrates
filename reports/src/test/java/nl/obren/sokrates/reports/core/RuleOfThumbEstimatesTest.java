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
    void rendersAClosedDetailsWithTheMainLinesOfCode() {
        String html = RuleOfThumbEstimates.html(200000, null);
        assertTrue(html.trim().startsWith("<details class=\"sk-est\" id=\"sk-est\""));
        assertFalse(html.contains(" open"), "closed by default");
        assertTrue(html.contains("data-main-loc=\"200000\""));
        assertTrue(html.contains("value=\"10000\""), "10,000 lines of code per man-year");
        assertTrue(html.contains("id=\"sk-est-maintenance\"") && html.contains("value=\"15\""), "15% maintenance");
        assertFalse(html.contains("token"), "AI token costs have their own page");
        assertFalse(html.contains("${"), "every placeholder is filled");
    }

    @Test
    void negativeLinesOfCodeBecomeZero() {
        assertTrue(RuleOfThumbEstimates.html(-5, null).contains("data-main-loc=\"0\""));
    }

    @AfterEach
    void resetDates() {
        DateUtils.setDateParam(null);
        DateUtils.reset();
    }

    private static CodeAnalysisResults repository(int main, String latestCommitDate) {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.getMainAspectAnalysisResults().setLinesOfCode(main);
        results.getContributorsAnalysisResults().setLatestCommitDate(latestCommitDate);
        return results;
    }

    @Test
    void landscapeWindowsSumTheRepositoriesByLatestCommit() {
        DateUtils.setDateParam("2026-10-05");
        DateUtils.reset();
        List<RuleOfThumbEstimates.Window> windows = RuleOfThumbEstimates.windows(Arrays.asList(
                repository(1000, "2026-10-01"),
                repository(20000, "2026-03-01"),
                repository(300000, "2023-01-01"),
                repository(5, "")));

        assertEquals(Arrays.asList("30", "90", "180", "365", "730", "all"),
                windows.stream().map(RuleOfThumbEstimates.Window::getId).collect(java.util.stream.Collectors.toList()));
        assertEquals(1, windows.get(0).getRepositories());
        assertEquals(1000, windows.get(0).getMainLoc());
        assertEquals(2, windows.get(3).getRepositories(), "past year");
        assertEquals(21000, windows.get(3).getMainLoc());
        assertEquals(4, windows.get(5).getRepositories(), "all time includes repositories without history");
        assertEquals(321005, windows.get(5).getMainLoc());

        String html = RuleOfThumbEstimates.html(21000, windows);
        assertTrue(html.contains("data-default-window=\"365\""));
        assertTrue(html.contains("&quot;id&quot;:&quot;365&quot;"), "the windows are an escaped JSON attribute");
        assertTrue(html.contains("&quot;main&quot;:21000"));
        assertFalse(html.contains("${"));
    }

    @Test
    void aRepositoryHasNoWindows() {
        assertTrue(RuleOfThumbEstimates.html(1, null).contains("data-windows=\"\""));
    }
}
