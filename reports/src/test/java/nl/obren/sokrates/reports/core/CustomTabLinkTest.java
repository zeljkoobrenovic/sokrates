package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.core.CustomTab;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomTabLinkTest {

    @Test
    void onlyHttpHttpsAndRelativeLinksAreOpenedOrEmbedded() {
        assertEquals("https://grafana.example.com/d/1", ReportFileExporter.customTabLink(" https://grafana.example.com/d/1 "));
        assertEquals("http://example.com", ReportFileExporter.customTabLink("http://example.com"));
        assertEquals("Duplication.html", ReportFileExporter.customTabLink("Duplication.html"));
        assertEquals("../explorers/files-explorer.html", ReportFileExporter.customTabLink("../explorers/files-explorer.html"));
        assertNull(ReportFileExporter.customTabLink("javascript:alert(1)"));
        assertNull(ReportFileExporter.customTabLink("JavaScript:alert(1)"));
        assertNull(ReportFileExporter.customTabLink("data:text/html,<b>x</b>"));
        assertNull(ReportFileExporter.customTabLink("  "));
        assertNull(ReportFileExporter.customTabLink(null));
    }

    @Test
    void theSubtitleNamesTheHostOfAnAbsoluteLink() {
        assertEquals("Embedded from grafana.example.com", ReportFileExporter.customTabSubtitle("https://grafana.example.com/d/1"));
        assertEquals("Embedded page", ReportFileExporter.customTabSubtitle("Duplication.html"));
        assertEquals("Embedded page", ReportFileExporter.customTabSubtitle(null));
    }

    @Test
    void anUnsafeLinkIsNotEmbedded() {
        CustomTab bad = new CustomTab();
        bad.setLabel("Bad");
        bad.setIframeLink("javascript:alert(1)");
        assertFalse(ReportFileExporter.customTabIframe(bad).contains("<iframe"));

        CustomTab good = new CustomTab();
        good.setLabel("Good");
        good.setIframeLink("https://example.com/?a=1&b='2'");
        assertEquals("<iframe src='https://example.com/?a=1&amp;b=&#39;2&#39;' style='width: 100%; border: none; height: calc(100vh - 220px); overflow: hidden;'></iframe>",
                ReportFileExporter.customTabIframe(good));
    }

    @Test
    void theHeaderLinkOpensInANewTabWithoutOpener() {
        assertEquals("<a class='sk-open-new-tab' target='_blank' rel='noopener' href='https://a.example/?x=1&amp;y=2'>Open in new tab ↗</a>",
                ReportNavigation.openInNewTabHtml("https://a.example/?x=1&y=2"));
    }
}
