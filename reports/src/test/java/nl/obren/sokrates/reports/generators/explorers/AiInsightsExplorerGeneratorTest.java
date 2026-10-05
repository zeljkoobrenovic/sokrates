package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.reports.core.ReportNavigation;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class AiInsightsExplorerGeneratorTest {

    private static void write(File reports, String name, String json) throws Exception {
        FileUtils.write(new File(reports, "ai-insights/" + name), json, UTF_8);
    }

    private static String findings(String scanner, String... severities) {
        return "{\"scanner\": \"" + scanner + "\", \"findings\": [" + Arrays.stream(severities)
                .map(s -> "{\"id\": \"" + scanner + "/" + s + "\", \"title\": \"t\", \"severity\": \"" + s + "\"}")
                .collect(Collectors.joining(", ")) + "]}";
    }

    @Test
    void readsTheFindingsFilesInTheScannersOrder(@TempDir Path tmp) throws Exception {
        File reports = tmp.toFile();
        write(reports, "security-scan.json", findings("security-scan", "high"));
        write(reports, "functionality-scan.json", findings("functionality-scan", "info"));
        write(reports, "zzz-custom-scan.json", findings("zzz-custom-scan"));
        write(reports, "combined-report.json", "{\"scanner\": \"combined\", \"findings\": []}");
        write(reports, "notes.json", "{\"title\": \"not a findings file\"}");
        write(reports, "broken.json", "{");
        write(reports, "index.html", "<html></html>");

        AiInsightsExplorerGenerator insights = AiInsightsExplorerGenerator.load(reports);

        // the bundled scanners.json order (basic before deep dives), unknown scanners last
        assertEquals(List.of("functionality-scan", "security-scan", "zzz-custom-scan"), insights.getScanners());
    }

    @Test
    void noFindingsMeansNoPageAndNoSidebarGroup(@TempDir Path tmp) throws Exception {
        AiInsightsExplorerGenerator insights = AiInsightsExplorerGenerator.load(tmp.toFile());
        assertTrue(insights.isEmpty());

        ReportNavigation navigation = new ReportNavigation("Repo", "index.html#overview");
        insights.addNavigation(navigation);
        insights.export(tmp.toFile());
        assertTrue(navigation.getGroups().isEmpty());
        assertFalse(new File(tmp.toFile(), "explorers/ai-insights.html").exists());
    }

    @Test
    void basicScannersAndDeepDivesGetTheirOwnSidebarGroups(@TempDir Path tmp) throws Exception {
        File reports = tmp.toFile();
        write(reports, "functionality-scan.json", findings("functionality-scan", "info"));
        write(reports, "security-scan.json", findings("security-scan", "high", "medium", "info"));

        ReportNavigation navigation = new ReportNavigation("Repo", "index.html#overview");
        AiInsightsExplorerGenerator.load(reports).addNavigation(navigation);

        assertEquals(List.of("AI Insights", "AI Deep Dives"),
                navigation.getGroups().stream().map(ReportNavigation.Group::getLabel).collect(Collectors.toList()));
        assertEquals(List.of("ai-insights/overview", "ai-insights/attention", "ai-insights/functionality-scan"),
                navigation.getGroups().get(0).getItems().stream().map(ReportNavigation.Item::getId).collect(Collectors.toList()));
        assertEquals("Security", navigation.labelOf("ai-insights/security-scan"));
        assertEquals("3 findings, 1 high, 1 medium.", navigation.subtitleOf("ai-insights/security-scan"));
        assertEquals("AI Insights", navigation.titleOf("ai-insights/overview"));

        String html = navigation.render("overview");
        assertTrue(html.contains("data-sk-frame-src='../explorers/ai-insights.html?view=security-scan'"), html);
        // a regular item with its icon, not an indented sub-item; the scanner's icon is the header icon
        assertFalse(html.contains("sk-nav-sub"));
        assertTrue(html.contains("data-sk-icon-src='../explorers/ai-insights-icons/security-scan.png' data-sk-icon-invert='true'"), html);
    }

    @Test
    void aScannerIdIsDataInTheSidebar(@TempDir Path tmp) throws Exception {
        File reports = tmp.toFile();
        write(reports, "x.json", findings("x'><img src=x onerror=alert(1)>"));

        ReportNavigation navigation = new ReportNavigation("Repo", "index.html#overview");
        AiInsightsExplorerGenerator.load(reports).addNavigation(navigation);
        String html = navigation.render("overview");

        assertFalse(html.contains("<img src=x"), html);
        assertTrue(html.contains("?view=x%27%3E%3Cimg+src%3Dx+onerror%3Dalert%281%29%3E"), html);
    }

    @Test
    void exportsThePageWithTheIconsOfTheScannersPresent(@TempDir Path tmp) throws Exception {
        File reports = tmp.toFile();
        write(reports, "security-scan.json", findings("security-scan", "high"));
        write(reports, "zzz-custom-scan.json", findings("zzz-custom-scan", "low"));

        AiInsightsExplorerGenerator.load(reports).export(reports);

        String page = FileUtils.readFileToString(new File(reports, "explorers/ai-insights.html"), UTF_8);
        assertTrue(page.contains("sokratesInflate(\""), "the data is embedded compressed");
        assertFalse(page.contains("${data}") || page.contains("${sokrates-theme}") || page.contains("${sokrates-inflate-lib}"));
        File icons = new File(reports, "explorers/ai-insights-icons");
        assertEquals(List.of("attention.png", "overview.png", "security-scan.png"),
                Arrays.stream(icons.list()).sorted().collect(Collectors.toList()), "no icon for an unknown scanner");
    }
}
