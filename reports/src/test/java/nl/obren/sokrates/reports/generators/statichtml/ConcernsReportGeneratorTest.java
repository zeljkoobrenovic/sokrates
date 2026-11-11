package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextFragment;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ConcernsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcernsReportGeneratorTest {

    private AspectAnalysisResults concern(String name, int loc) {
        AspectAnalysisResults concern = new AspectAnalysisResults(name);
        concern.setAspect(new NamedSourceCodeAspect(name));
        concern.setLinesOfCode(loc);
        concern.setFilesCount(1);
        return concern;
    }

    @Test
    void concernNamesAreEscaped() {
        // Meta-concern names are extracted from source lines, so they are repository-controlled.
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(CodeConfiguration.getDefaultConfiguration());
        results.getMainAspectAnalysisResults().setLinesOfCode(100);
        ConcernsAnalysisResults group = new ConcernsAnalysisResults("general");
        group.getConcerns().add(concern("TODO <img src=x onerror=XSS>", 10));
        group.getConcerns().add(concern("plain", 20));
        results.getConcernsAnalysisResults().add(group);

        RichTextReport report = new RichTextReport("", "");
        new ConcernsReportGenerator(results).addConcernsToReport(report);

        String html = report.getRichTextFragments().stream().map(RichTextFragment::getFragment).collect(Collectors.joining("\n"));
        assertFalse(html.contains("<img src=x"), html);
        assertTrue(html.contains("TODO &lt;img src=x onerror=XSS&gt;"), html);
        assertTrue(html.contains("<li>plain</li>"), html);
    }
}
