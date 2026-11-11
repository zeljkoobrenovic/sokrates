/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.ReportRenderer;
import nl.obren.sokrates.reports.core.ReportRenderingClient;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ControlStatus;
import nl.obren.sokrates.sourcecode.analysis.results.GoalsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.Range;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;
import nl.obren.sokrates.sourcecode.metrics.MetricsWithGoal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Golden-file tests for ControlsReportGenerator: an empty-state report (no goals
 * defined) and a populated report with a single control. Compared against files
 * in src/test/resources/golden/. Regenerate with -Dsokrates.updateGolden=true.
 */
class ControlsReportGeneratorTest {

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

    /**
     * The report's "show more" blocks get ids from a global static counter in
     * RichTextRenderingUtils that never resets, so the rendered html depends on how
     * many such blocks were rendered earlier in the JVM. Reset it before each test so
     * the golden comparison is independent of test execution order.
     */
    @BeforeEach
    void resetShowMoreBlockCounter() throws Exception {
        Class<?> clazz = Class.forName(
                "nl.obren.sokrates.common.renderingutils.RichTextRenderingUtils");
        Field field = clazz.getDeclaredField("showMoreBlockId");
        field.setAccessible(true);
        field.setInt(null, 1);
    }

    private String render(CodeAnalysisResults results) {
        RichTextReport report = new RichTextReport("Controls", "Controls.html");
        new ControlsReportGenerator().generateReport(results, report);

        StringRenderingClient client = new StringRenderingClient();
        new ReportRenderer().render(report, client);
        return client.getHtml().replace("\r\n", "\n");
    }

    @Test
    void rendersEmptyStateWhenNoGoals() throws IOException {
        assertGolden(render(new CodeAnalysisResults()), "controls-report-empty.html");
    }

    @Test
    void rendersPopulatedControl() throws IOException {
        CodeAnalysisResults results = new CodeAnalysisResults();

        MetricsWithGoal goal = new MetricsWithGoal();
        goal.setGoal("Keep duplication low");
        goal.setDescription("Duplication should stay within tolerance");

        Metric metric = new Metric().id("DUPLICATION_PERCENTAGE").value(3);

        MetricRangeControl control = new MetricRangeControl(
                "DUPLICATION_PERCENTAGE", "Duplication percentage", new Range("0", "5", "1"));

        ControlStatus status = new ControlStatus();
        status.setStatus("OK");
        status.setMetric(metric);
        status.setControl(control);

        GoalsAnalysisResults goalResults = new GoalsAnalysisResults();
        goalResults.setMetricsWithGoal(goal);
        goalResults.setControlStatuses(List.of(status));

        results.getControlResults().setGoalsAnalysisResults(List.of(goalResults));

        assertGolden(render(results), "controls-report-populated.html");
    }

    @Test
    void getColorMapsStatusToTrafficLight() {
        assertEquals("darkgreen", ControlsReportGenerator.getColor("OK"));
        assertEquals("darkgreen", ControlsReportGenerator.getColor("ok"));
        assertEquals("crimson", ControlsReportGenerator.getColor("FAILED"));
        assertEquals("grey", ControlsReportGenerator.getColor("IGNORED"));
        assertEquals("orange", ControlsReportGenerator.getColor("something-else"));
    }

    private void assertGolden(String actual, String goldenFileName) throws IOException {
        String resource = "/golden/" + goldenFileName;

        if (Boolean.getBoolean("sokrates.updateGolden")) {
            File goldenFile = new File("src/test/resources/golden/" + goldenFileName);
            goldenFile.getParentFile().mkdirs();
            Files.writeString(goldenFile.toPath(), actual, StandardCharsets.UTF_8);
            return;
        }

        try (InputStream in = getClass().getResourceAsStream(resource)) {
            assertNotNull(in, "Missing golden resource " + resource +
                    " (generate it with -Dsokrates.updateGolden=true)");
            String expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertEquals(expected, actual,
                    "Rendered controls report HTML changed. If intentional, regenerate with " +
                            "-Dsokrates.updateGolden=true");
        }
    }
}
