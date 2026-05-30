/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.ReportRenderer;
import nl.obren.sokrates.reports.core.ReportRenderingClient;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Golden-file test for FindingsReportGenerator covering the common case where no
 * manual findings file exists: the report should render the intro plus a
 * "no findings" note. Compared against
 * src/test/resources/golden/findings-report-empty.html.
 *
 * Regenerate the golden file with -Dsokrates.updateGolden=true.
 */
class FindingsReportGeneratorTest {

    private static final String GOLDEN_RESOURCE = "/golden/findings-report-empty.html";

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

    private String renderReport() throws IOException {
        // point the generator at an empty temp folder so it hits the "no findings" branch
        File configDir = Files.createTempDirectory("sokrates-findings").toFile();
        File configFile = new File(configDir, "config.json");

        RichTextReport report = new RichTextReport("Findings", "Findings.html");
        new FindingsReportGenerator(configFile).generateReport(new CodeAnalysisResults(), report);

        StringRenderingClient client = new StringRenderingClient();
        new ReportRenderer().render(report, client);
        return client.getHtml().replace("\r\n", "\n");
    }

    @Test
    void rendersExpectedHtmlWhenNoFindings() throws IOException {
        String actual = renderReport();

        if (Boolean.getBoolean("sokrates.updateGolden")) {
            File goldenFile = new File("src/test/resources/golden/findings-report-empty.html");
            goldenFile.getParentFile().mkdirs();
            Files.writeString(goldenFile.toPath(), actual, StandardCharsets.UTF_8);
            return;
        }

        try (InputStream in = getClass().getResourceAsStream(GOLDEN_RESOURCE)) {
            assertNotNull(in, "Missing golden resource " + GOLDEN_RESOURCE +
                    " (generate it with -Dsokrates.updateGolden=true)");
            String expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertEquals(expected, actual,
                    "Rendered findings report HTML changed. If intentional, regenerate with " +
                            "-Dsokrates.updateGolden=true");
        }
    }
}
