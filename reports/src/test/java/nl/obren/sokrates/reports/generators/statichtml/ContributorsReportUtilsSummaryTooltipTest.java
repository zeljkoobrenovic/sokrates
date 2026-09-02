package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.ReportRenderer;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.core.ReportRenderingClient;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContributorsReportUtilsSummaryTooltipTest {
    private static class StringRenderingClient implements ReportRenderingClient {
        private final StringBuilder html = new StringBuilder();

        @Override
        public void append(String text) {
            html.append(text);
        }

        @Override
        public File getVisualsExportFolder() {
            return null;
        }

        String getHtml() {
            return html.toString();
        }
    }

    private static ContributorsReportUtils.ActivitySummary summary() {
        ContributorsReportUtils.WindowTotals[] w = new ContributorsReportUtils.WindowTotals[3];
        for (int i = 0; i < 3; i++) {
            w[i] = new ContributorsReportUtils.WindowTotals();
            w[i].added = 10 * (i + 1);
            w[i].deleted = i + 1;
            w[i].fileUpdates = 100 * (i + 1);
            w[i].commits = 1000 * (i + 1);
            w[i].contributors = i + 1;
            w[i].aiCommits = 250 * (i + 1);
        }
        return new ContributorsReportUtils.ActivitySummary(w);
    }

    @Test
    public void tooltipListsEveryWindow() {
        assertEquals("number of commits\n\n30 days: 1,000\n90 days: 2,000\nall time: 3,000",
                ContributorsReportUtils.summaryTooltip("number of commits", summary(), ContributorsReportUtils.SummaryMetric.COMMITS));
        assertEquals("line churn\n\n30 days: +10 / -1 lines\n90 days: +20 / -2 lines\nall time: +30 / -3 lines",
                ContributorsReportUtils.summaryTooltip("line churn", summary(), ContributorsReportUtils.SummaryMetric.CHURN));
    }

    @Test
    public void chartWithSummaryHasOnly30DayColumnAndLinkedIconsWithTooltips() {
        RichTextReport report = new RichTextReport("t", "t.html");
        List<ContributionTimeSlot> slots = new ArrayList<>(Arrays.asList(new ContributionTimeSlot("2025", Thresholds.defaultCommitFilesCountThresholds())));
        slots.get(0).setCommitsCount(5);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, slots, 20, true, true, 8, false, summary());
        StringRenderingClient client = new StringRenderingClient();
        new ReportRenderer().render(report, client);
        String html = client.getHtml();

        // Only the 30-day total is a real column; 90 days / all time live in the icon tooltips.
        assertTrue(html.contains(">30 days<"));
        assertFalse(html.contains(">90 days<"));
        assertFalse(html.contains(">all time<"));
        assertTrue(html.contains(">" + nl.obren.sokrates.common.utils.FormattingUtils.getSmallTextForNumber(1000) + "</span>"));
        assertTrue(html.contains("href='Commits.html'"));
        assertTrue(html.contains("href='Contributors.html'"));
        assertTrue(html.contains("href='FileChurn.html'"));
        assertTrue(html.contains("30 days: 1,000"));
    }

    @Test
    public void aiCommitsTooltipShowsShareOfCommits() {
        assertEquals("ai commits\n\n30 days: 250 of 1,000 (25%)\n90 days: 500 of 2,000 (25%)\nall time: 750 of 3,000 (25%)",
                ContributorsReportUtils.summaryTooltip("ai commits", summary(), ContributorsReportUtils.SummaryMetric.AI_COMMITS));
    }

    private static String render(List<ContributionTimeSlot> slots) {
        RichTextReport report = new RichTextReport("t", "t.html");
        ContributorsReportUtils.addContributorsPerTimeSlot(report, slots, 20, true, true, 8, false, summary());
        StringRenderingClient client = new StringRenderingClient();
        new ReportRenderer().render(report, client);
        return client.getHtml();
    }

    @Test
    public void aiCommitsRowOnlyWhenSomeSlotHasAiCoAuthoredCommits() {
        List<ContributionTimeSlot> slots = new ArrayList<>(Arrays.asList(
                new ContributionTimeSlot("2025", Thresholds.defaultCommitFilesCountThresholds()),
                new ContributionTimeSlot("2026", Thresholds.defaultCommitFilesCountThresholds())));
        slots.get(0).setCommitsCount(5);
        slots.get(1).setCommitsCount(8);
        assertFalse(render(slots).contains("AI coding agent co-author"));

        // addContributorsPerTimeSlot sorts the list in place (newest first), so look the slot up by name.
        slots.stream().filter(s -> s.getTimeSlot().equals("2026")).findFirst().get().setAiCoAuthoredCommitsCount(2);
        String html = render(slots);
        assertTrue(html.contains("AI coding agent co-author"));
        assertTrue(html.contains("2026: 2 of 8 commits (25%)"));
        assertTrue(html.contains("2025: 0 of 5 commits (0%)"));
        assertTrue(html.contains("30 days: 250 of 1,000 (25%)"));
    }
}
