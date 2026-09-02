package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CustomTab;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ReportFileExporterTest {

    @Test
    void extractTitle() {
        assertEquals(ReportFileExporter.extractTitle("ABC"), "ABC");
        assertEquals(ReportFileExporter.extractTitle("<div>ABC</div>"), "ABC");
        assertEquals(ReportFileExporter.extractTitle("<div>ABC</div> <div><img></div>"), "ABC");
        assertEquals(ReportFileExporter.extractTitle(" <div>ABC </div> <div><img>  </div>"), "ABC");
    }

    @Test
    void getCustomTabsSkipsInvalidEntries() {
        CodeConfiguration configuration = new CodeConfiguration();
        configuration.setCustomTabs(Arrays.asList(
                new CustomTab("Docs", "https://example.com/docs"),
                new CustomTab("", "https://example.com/no-label"),
                new CustomTab("No link", " "),
                null));
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(configuration);

        assertEquals(1, ReportFileExporter.getCustomTabs(results).size());
        assertEquals("Docs", ReportFileExporter.getCustomTabs(results).get(0).getLabel());

        results.setCodeConfiguration(new CodeConfiguration());
        assertTrue(ReportFileExporter.getCustomTabs(results).isEmpty());
        configuration.setCustomTabs(null);
        assertTrue(configuration.getCustomTabs().isEmpty());
    }

    @Test
    void customTabIdsAndIframes() {
        assertEquals("custom-tab-1", ReportFileExporter.customTabId(0));
        assertEquals("custom-tab-2", ReportFileExporter.customTabId(1));

        String iframe = ReportFileExporter.customTabIframe(new CustomTab("Docs", " ../custom/page.html?a=1&b=2 "));
        assertTrue(iframe.startsWith("<iframe src='../custom/page.html?a=1&amp;b=2'"));
        assertTrue(iframe.contains("width: 100%"));
        assertTrue(iframe.contains("height: calc(100vh - 220px)"));
    }

    @Test
    void addOrReplaceCustomTabKeepsLabelsUnique() {
        CodeConfiguration configuration = new CodeConfiguration();
        assertFalse(configuration.addOrReplaceCustomTab(new CustomTab("Docs", "a.html")));
        assertFalse(configuration.addOrReplaceCustomTab(new CustomTab("Dashboards", "b.html")));
        assertTrue(configuration.addOrReplaceCustomTab(new CustomTab(" docs ", "c.html")));

        assertEquals(2, configuration.getCustomTabs().size());
        assertEquals(" docs ", configuration.getCustomTabs().get(0).getLabel());
        assertEquals("c.html", configuration.getCustomTabs().get(0).getIframeLink());
        assertEquals("b.html", configuration.getCustomTabs().get(1).getIframeLink());
    }
}
