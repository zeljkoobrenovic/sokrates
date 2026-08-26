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
    public void chartWithSummaryHasNoSummaryColumnsButLinkedIconsWithTooltips() {
        RichTextReport report = new RichTextReport("t", "t.html");
        List<ContributionTimeSlot> slots = new ArrayList<>(Arrays.asList(new ContributionTimeSlot("2025", Thresholds.defaultCommitFilesCountThresholds())));
        slots.get(0).setCommitsCount(5);
        ContributorsReportUtils.addContributorsPerTimeSlot(report, slots, 20, true, true, 8, false, summary());
        StringRenderingClient client = new StringRenderingClient();
        new ReportRenderer().render(report, client);
        String html = client.getHtml();

        assertFalse(html.contains(">30 days<"));
        assertFalse(html.contains(">all time<"));
        assertTrue(html.contains("href='Commits.html'"));
        assertTrue(html.contains("href='Contributors.html'"));
        assertTrue(html.contains("href='FileChurn.html'"));
        assertTrue(html.contains("30 days: 1,000"));
    }
}
