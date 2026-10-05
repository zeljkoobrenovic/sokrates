package nl.obren.sokrates.reports.core;

import nl.obren.sokrates.reports.generators.explorers.AiInsightsExplorerGenerator;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CustomTab;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class ReportFileExporterTest {

    @Test
    void extractTitle() {
        assertEquals(ReportHtmlWriter.extractTitle("ABC"), "ABC");
        assertEquals(ReportHtmlWriter.extractTitle("<div>ABC</div>"), "ABC");
        assertEquals(ReportHtmlWriter.extractTitle("<div>ABC</div> <div><img></div>"), "ABC");
        assertEquals(ReportHtmlWriter.extractTitle(" <div>ABC </div> <div><img>  </div>"), "ABC");
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

        assertEquals(1, ReportFileExporter.getCustomTabs(results, null).size());
        assertEquals("Docs", ReportFileExporter.getCustomTabs(results, null).get(0).getLabel());

        results.setCodeConfiguration(new CodeConfiguration());
        assertTrue(ReportFileExporter.getCustomTabs(results, null).isEmpty());
        configuration.setCustomTabs(null);
        assertTrue(configuration.getCustomTabs().isEmpty());
    }

    @Test
    void theStandaloneAiExplorerTabIsDroppedWhenTheReportShowsTheFindings(@TempDir Path tmp) throws Exception {
        CodeConfiguration configuration = new CodeConfiguration();
        configuration.setCustomTabs(Arrays.asList(
                new CustomTab("AI Insights*", "../ai-insights/index.html"),
                new CustomTab("Docs", "https://example.com/docs")));
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(configuration);

        AiInsightsExplorerGenerator none = AiInsightsExplorerGenerator.load(tmp.toFile());
        assertEquals(2, ReportFileExporter.getCustomTabs(results, none).size(), "kept while the report has no findings of its own");

        FileUtils.write(new File(tmp.toFile(), "ai-insights/tech-stack-scan.json"), "{\"scanner\": \"tech-stack-scan\", \"findings\": []}", UTF_8);
        AiInsightsExplorerGenerator some = AiInsightsExplorerGenerator.load(tmp.toFile());
        assertEquals(Arrays.asList("Docs"), ReportFileExporter.getCustomTabs(results, some).stream().map(CustomTab::getLabel).collect(Collectors.toList()));

        assertTrue(ReportFileExporter.embedsStandaloneAiExplorer(new CustomTab("x", " ai-insights/index.html#view=attention ")));
        assertFalse(ReportFileExporter.embedsStandaloneAiExplorer(new CustomTab("x", "../my-ai-insights/index.html")));
        assertFalse(ReportFileExporter.embedsStandaloneAiExplorer(new CustomTab("x", "../ai-insights/other.html")));
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
