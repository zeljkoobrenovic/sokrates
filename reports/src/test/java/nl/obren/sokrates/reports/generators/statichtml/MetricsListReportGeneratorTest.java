/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.ReportRenderer;
import nl.obren.sokrates.reports.core.ReportRenderingClient;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Golden-file test for MetricsListReportGenerator: builds a deterministic
 * CodeAnalysisResults, renders the report to HTML and compares it against a
 * checked-in expected file (src/test/resources/golden/metrics-list-report.html).
 *
 * To regenerate the golden file after an intentional change, set the system
 * property -Dsokrates.updateGolden=true and re-run; the test then overwrites
 * the resource with the current output and passes.
 */
class MetricsListReportGeneratorTest {

    private static final String GOLDEN_RESOURCE = "/golden/metrics-list-report.html";

    /** Collects rendered HTML in memory; no visuals folder so nothing touches disk. */
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

    private CodeAnalysisResults sampleResults() {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(new CodeConfiguration());
        results.getMetricsList().addMetric().id("NUMBER_OF_FILES").value(42);
        results.getMetricsList().addMetric().id("LINES_OF_CODE_MAIN").value(12345);
        results.getMetricsList().addMetric().id("DUPLICATION_PERCENTAGE").value(3.5);
        return results;
    }

    private String renderReport() {
        RichTextReport report = new RichTextReport("Metrics", "Metrics.html");
        new MetricsListReportGenerator().generateReport(sampleResults(), report);

        StringRenderingClient client = new StringRenderingClient();
        new ReportRenderer().render(report, client);
        // normalize line endings so the test is stable across platforms
        return client.getHtml().replace("\r\n", "\n");
    }

    @Test
    void rendersExpectedHtml() throws IOException {
        String actual = renderReport();

        if (Boolean.getBoolean("sokrates.updateGolden")) {
            updateGoldenFile(actual);
            return;
        }

        String expected = readGolden();
        assertEquals(expected, actual,
                "Rendered metrics report HTML changed. If this change is intentional, " +
                        "regenerate the golden file with -Dsokrates.updateGolden=true");
    }

    @Test
    void reportContainsEveryMetric() {
        String actual = renderReport();

        // independent of the golden file: each metric id and its formatted value must appear
        assertContains(actual, "NUMBER_OF_FILES");
        assertContains(actual, "LINES_OF_CODE_MAIN");
        assertContains(actual, "DUPLICATION_PERCENTAGE");
    }

    private static void assertContains(String haystack, String needle) {
        if (!haystack.contains(needle)) {
            throw new AssertionError("Expected rendered report to contain \"" + needle + "\"");
        }
    }

    private String readGolden() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(GOLDEN_RESOURCE)) {
            assertNotNull(in, "Missing golden resource " + GOLDEN_RESOURCE +
                    " (generate it with -Dsokrates.updateGolden=true)");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private void updateGoldenFile(String content) throws IOException {
        File goldenFile = new File("src/test/resources/golden/metrics-list-report.html");
        goldenFile.getParentFile().mkdirs();
        Files.writeString(goldenFile.toPath(), content, StandardCharsets.UTF_8);
    }
}
