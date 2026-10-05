package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleOfThumbEstimatesTest {
    @Test
    void rendersAClosedDetailsWithTheLinesOfCode() {
        String html = RuleOfThumbEstimates.html(new long[]{200000, 50000, 1000, 200, 34}, 7, 14, null);
        assertTrue(html.trim().startsWith("<details class=\"sk-est\" id=\"sk-est\""));
        assertFalse(html.contains(" open"), "closed by default");
        assertTrue(html.contains("data-main-loc=\"200000\""));
        assertTrue(html.contains("data-test-loc=\"50000\""));
        assertTrue(html.contains("data-build-loc=\"1000\""));
        assertTrue(html.contains("data-generated-loc=\"200\""));
        assertTrue(html.contains("data-other-loc=\"34\""));
        assertTrue(html.contains("data-tokens-min=\"7\""));
        assertTrue(html.contains("data-tokens-max=\"14\""));
        assertTrue(html.contains("value=\"10000\""), "10,000 lines of code per man-year");
        assertTrue(html.contains("id=\"sk-est-maintenance\"") && html.contains("value=\"15\""), "15% maintenance");
        assertFalse(html.contains("${"), "every placeholder is filled");
    }

    @Test
    void sanitizesTheTokenRange() {
        String html = RuleOfThumbEstimates.html(new long[]{-5, 0, 0, 0, 0}, 20, 0, null);
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

        String html = RuleOfThumbEstimates.html(new long[]{21000, 2100, 210, 0, 0}, 7, 14, windows);
        assertTrue(html.contains("data-default-window=\"365\""));
        assertTrue(html.contains("&quot;id&quot;:&quot;365&quot;"), "the windows are an escaped JSON attribute");
        assertTrue(html.contains("&quot;build&quot;:210"), "build & deployment per window");
        assertFalse(html.contains("${"));
    }

    @Test
    void aRepositoryHasNoWindows() {
        assertTrue(RuleOfThumbEstimates.html(new long[]{1, 1, 1, 1, 1}, 7, 14, null).contains("data-windows=\"\""));
    }

    private static ContributionTimeSlot day(String date, int added, int deleted) {
        ContributionTimeSlot slot = new ContributionTimeSlot();
        slot.setTimeSlot(date);
        slot.setLinesAdded(added);
        slot.setLinesDeleted(deleted);
        return slot;
    }

    @Test
    void churnSumsAddedAndDeletedPerPeriodAndScope() {
        DateUtils.setDateParam("2026-10-05");
        DateUtils.reset();
        CodeAnalysisResults results = repository(1000, 100, 0, "2026-10-01");
        results.getContributorsAnalysisResults().getContributorsPerDayByScope().put("main",
                Arrays.asList(day("2026-10-01", 10, 5), day("2026-08-01", 100, 0), day("2025-12-01", 1000, 0), day("2024-01-01", 9999, 0)));
        results.getContributorsAnalysisResults().getContributorsPerDayByScope().put("test", Arrays.asList(day("2026-09-30", 3, 2)));

        long[][] churn = RuleOfThumbEstimates.churn(results.getContributorsAnalysisResults());
        assertArrayEquals(new long[]{15, 5, 0, 0, 0}, churn[0], "past 30 days");
        assertArrayEquals(new long[]{115, 5, 0, 0, 0}, churn[1], "past 3 months");
        assertArrayEquals(new long[]{1115, 5, 0, 0, 0}, churn[2], "past year");

        String html = RuleOfThumbEstimates.html(new long[]{1000, 100, 0, 0, 0}, churn, 7, 14, null);
        assertTrue(html.contains("data-churn=\"{&quot;30&quot;:[15,5,0,0,0],&quot;90&quot;:[115,5,0,0,0],&quot;365&quot;:[1115,5,0,0,0]}\""));

        List<RuleOfThumbEstimates.Window> windows = RuleOfThumbEstimates.windows(Arrays.asList(results, results));
        assertArrayEquals(new long[]{2230, 10, 0, 0, 0}, windows.get(3).getChurn()[2], "the landscape sums the repositories");
    }
}
