package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.reports.core.RichTextFragment;
import nl.obren.sokrates.reports.core.RichTextReport;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationDiagramGeneratorTest {

    @Test
    void pointTitlesAreEscaped() {
        RichTextReport report = new RichTextReport("", "");
        CorrelationDiagramGenerator<String> generator = new CorrelationDiagramGenerator<>(report,
                Arrays.asList("src/<img src=x onerror=XSS>.ts", "src/normal.ts"));

        generator.addCorrelations("Size vs. Churn", "lines", "churn", s -> s.length(), s -> 2 * s.length(), s -> s);

        String html = report.getRichTextFragments().stream().map(RichTextFragment::getFragment).collect(Collectors.joining("\n"));
        assertFalse(html.contains("<img"), html);
        assertTrue(html.contains("<title>src/&lt;img src=x onerror=XSS&gt;.ts\n"), html);
        assertTrue(html.contains("<title>src/normal.ts\n"), html);
    }
}
