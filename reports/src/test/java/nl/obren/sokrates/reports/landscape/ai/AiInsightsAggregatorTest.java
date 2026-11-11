package nl.obren.sokrates.reports.landscape.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class AiInsightsAggregatorTest {
    private static final String TECH_STACK = "{\"scanner\":\"tech-stack-scan\",\"scanner_version\":\"1.1\",\"analyzed_at\":\"2026-09-30T10:00:00Z\",\"summary\":\"A Rust workspace.\",\"findings\":["
            + "{\"id\":\"tech-stack-scan/frameworks/tokio\",\"group\":\"frameworks\",\"title\":\"Tokio runtime\",\"description\":\"Async runtime.\",\"severity\":\"info\",\"confidence\":\"certain\",\"tags\":[\"rust\",\"async\"]},"
            + "{\"id\":\"tech-stack-scan/deps/old lib\",\"group\":\"deps\",\"title\":\"Outdated library\",\"description\":\"" + "x".repeat(500) + "\",\"severity\":\"High\",\"confidence\":\"likely\",\"recommendation\":\"Upgrade it.\"}]}";
    private static final String SECURITY = "{\"scanner\":\"security-scan\",\"analyzed_at\":\"2026-10-01T10:00:00Z\",\"findings\":["
            + "{\"id\":\"security-scan/secrets/token\",\"group\":\"secrets\",\"title\":\"Token in tree\",\"severity\":\"critical\",\"confidence\":\"certain\"}]}";
    private static final String COMBINED = "{\"scanner\":\"combined\",\"findings\":[{\"id\":\"x\",\"title\":\"dup\",\"severity\":\"critical\"}]}";

    private static RepositoryAnalysisResults repository(String name, String relativePath) {
        CodeAnalysisResults analysis = new CodeAnalysisResults();
        analysis.getMetadata().setName(name);
        return new RepositoryAnalysisResults(new SokratesRepositoryLink(relativePath + "/reports/data/analysisResults.json"), analysis, null);
    }

    @Test
    void aggregatesTheScannersOfEveryRepositoryWithResults(@TempDir Path tmp) throws Exception {
        File root = tmp.toFile();
        File landscape = new File(root, "_sokrates_landscape");
        FileUtils.write(new File(root, "acme/alpha/reports/ai-insights/tech-stack-scan.json"), TECH_STACK, UTF_8);
        FileUtils.write(new File(root, "acme/alpha/reports/ai-insights/security-scan.json"), SECURITY, UTF_8);
        FileUtils.write(new File(root, "acme/alpha/reports/ai-insights/combined-report.json"), COMBINED, UTF_8);
        FileUtils.write(new File(root, "acme/alpha/reports/ai-insights/index.html"), "<html></html>", UTF_8);
        FileUtils.write(new File(root, "acme/beta/reports/data/data.zip"), "", UTF_8);   // analyzed, never scanned
        FileUtils.write(new File(root, "acme/gamma/reports/ai-insights/broken.json"), "{not json", UTF_8);

        AiInsightsLandscapeExport export = AiInsightsAggregator.aggregate(
                List.of(repository("acme/alpha", "acme/alpha"), repository("acme/beta", "acme/beta"), repository("acme/gamma", "acme/gamma")), landscape, "../");

        assertEquals(1, export.getRepositories().size(), "only repositories with readable scanner results");
        AiRepositoryExport alpha = export.getRepositories().get(0);
        assertEquals("acme/alpha", alpha.getName());
        assertEquals("../acme/alpha/reports/ai-insights/index.html", alpha.getInsightsUrl());
        assertEquals("../acme/alpha/reports/index.html", alpha.getReportUrl());
        assertEquals("2026-10-01T10:00:00Z", alpha.getScannedAt(), "the latest scan date");
        assertEquals(List.of("security-scan", "tech-stack-scan"), alpha.getScanners().stream().map(AiScannerExport::getScanner).toList(), "combined-report.json is skipped");
        assertEquals(3, alpha.getFindings());
        assertEquals(2, alpha.getAttention(), "findings above info");
        assertEquals(1, alpha.getFindingsBySeverity().get("critical"));
        assertEquals(1, alpha.getFindingsBySeverity().get("high"));
        assertEquals("A Rust workspace.", alpha.getScanners().get(1).getSummary());

        assertEquals(List.of("critical", "high", "info"), export.getFindings().stream().map(AiFindingExport::getSeverity).toList(), "severity order");
        AiFindingExport high = export.getFindings().get(1);
        assertEquals("high", high.getSeverity(), "severities are normalized to lower case");
        assertEquals(AiInsightsAggregator.DESCRIPTION_LIMIT, high.getDescription().length(), "long descriptions are shortened");
        assertTrue(high.getDescription().endsWith("…"));
        assertEquals("Upgrade it.", high.getRecommendation());
        // a space as %20, not "+": the pages decode their fragment with decodeURIComponent
        assertEquals("../acme/alpha/reports/ai-insights/index.html#tech-stack-scan%2Fdeps%2Fold%20lib", high.getUrl(), "a deep link into the repository's explorer");
        assertEquals("../acme/alpha/reports/ai-insights/index.html#view=tech-stack-scan", alpha.getScanners().get(1).getUrl(), "the scanner's view");
        AiFindingExport info = export.getFindings().get(2);
        assertEquals(List.of("rust", "async"), info.getTags());
        assertEquals("tech-stack-scan", info.getScanner());

        assertEquals(1, AiInsightsAggregator.totalsBySeverity(export).get("critical"));
        assertEquals(0, AiInsightsAggregator.totalsBySeverity(export).get("medium"));
        // The export round-trips as JSON (it is embedded in the page and written to data/).
        String json = new ObjectMapper().writeValueAsString(export);
        assertTrue(json.contains("\"url\":\"../acme/alpha/reports/ai-insights/index.html#security-scan%2Fsecrets%2Ftoken\""));
        assertFalse(json.contains("\"empty\""), "the isEmpty helper is not a data field");
    }

    @Test
    void linksGoToTheRepositoryReportWhenItRendersTheFindings(@TempDir Path tmp) throws Exception {
        File root = tmp.toFile();
        FileUtils.write(new File(root, "acme/alpha/reports/ai-insights/tech-stack-scan.json"), TECH_STACK, UTF_8);
        FileUtils.write(new File(root, "acme/alpha/reports/explorers/ai-insights.html"), "<html></html>", UTF_8);

        AiInsightsLandscapeExport export = AiInsightsAggregator.aggregate(
                List.of(repository("acme/alpha", "acme/alpha")), new File(root, "_sokrates_landscape"), "../");

        AiRepositoryExport alpha = export.getRepositories().get(0);
        assertEquals("../acme/alpha/reports/html/index.html#ai-insights/overview", alpha.getInsightsUrl());
        assertEquals("../acme/alpha/reports/html/index.html#ai-insights/tech-stack-scan", alpha.getScanners().get(0).getUrl());
        AiFindingExport high = export.getFindings().get(0);
        assertEquals("../acme/alpha/reports/html/index.html#ai-insights/tech-stack-scan/tech-stack-scan%2Fdeps%2Fold%20lib", high.getUrl(),
                "the scanner's page in the report, opened on the finding");
        assertEquals("../acme/alpha/reports/index.html", alpha.getReportUrl());
    }

    @Test
    void repositoriesWithoutResultsGiveAnEmptyExport(@TempDir Path tmp) {
        AiInsightsLandscapeExport export = AiInsightsAggregator.aggregate(List.of(repository("x", "x")), new File(tmp.toFile(), "_sokrates_landscape"), "../");
        assertTrue(export.isEmpty());
        assertTrue(export.getRepositories().isEmpty());
    }
}
