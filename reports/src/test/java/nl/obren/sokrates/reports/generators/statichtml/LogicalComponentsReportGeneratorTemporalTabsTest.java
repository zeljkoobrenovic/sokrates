package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.ReportRenderer;
import nl.obren.sokrates.reports.core.ReportRenderingClient;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class LogicalComponentsReportGeneratorTemporalTabsTest {
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
    }

    private static LogicalDecompositionAnalysisResults decomposition(String name) {
        LogicalDecompositionAnalysisResults results = new LogicalDecompositionAnalysisResults(name);
        LogicalDecomposition decomposition = new LogicalDecomposition(name);
        results.setLogicalDecomposition(decomposition);
        return results;
    }

    @Test
    public void eachDecompositionTabShowsOnlyItsOwnTemporalDependencies() {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.getLogicalDecompositionsAnalysisResults().add(decomposition("primary"));
        results.getLogicalDecompositionsAnalysisResults().add(decomposition("secondary"));
        Contributor contributor = new Contributor("dev@example.com");
        contributor.setCommitsCount(1);
        results.getContributorsAnalysisResults().getContributors().add(contributor);

        RichTextReport report = new RichTextReport("Components", "Components.html");
        new LogicalComponentsReportGenerator(results, true).addCodeOrganizationToReport(report);

        StringRenderingClient client = new StringRenderingClient();
        new ReportRenderer().render(report, client);
        String html = client.html.toString();

        // One temporal-dependencies section per decomposition tab (it used to be decompositions² —
        // every tab repeated the sections of all decompositions).
        assertEquals(2, StringUtils.countMatches(html, "Dependencies between components in same commits"));
    }
}
