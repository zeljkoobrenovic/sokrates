package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextFragment;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.SymbolicLink;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The analysis scope section names the symbolic links the walk did not follow.
 *
 * <p>The line sits beside "N files are excluded from analyses", not inside it: that list's two
 * entries sum exactly to the excluded total, and a link was never walked, so it is not an excluded
 * file and must not join that arithmetic.
 */
class OverviewReportGeneratorSymbolicLinksTest {

    @Test
    void theSkippedLinksAreNamedWithALinkToTheirList() {
        CodeAnalysisResults results = resultsWithLinks(
                new SymbolicLink("scripts/fp-report", "/elsewhere/bin/fp-report", false),
                new SymbolicLink("CLAUDE.md", "AGENTS.md", true));

        String html = scopeSectionOf(results);

        assertTrue(html.contains("symbolic links were not followed"), html);
        assertTrue(html.contains("text/symbolic_links.txt"), html);
        assertTrue(html.contains("<b>2</b>"), html);
    }

    @Test
    void aSingleLinkIsNamedInTheSingular() {
        CodeAnalysisResults results = resultsWithLinks(new SymbolicLink("CLAUDE.md", "AGENTS.md", true));

        assertTrue(scopeSectionOf(results).contains("symbolic link was not followed"));
    }

    @Test
    void aRepositoryWithoutSymbolicLinksGetsNoSuchLine() {
        // The report for a repository that has no links must be the report it was before this line
        // existed - the whole basis for calling this change free of side effects.
        String html = scopeSectionOf(resultsWithLinks());

        assertFalse(html.contains("symbolic link"), html);
        assertFalse(html.contains("symbolic_links.txt"), html);
    }

    @Test
    void anAbsentListIsTreatedAsNoLinks() {
        // Nothing sets the list outside the analysis run; rendering must not depend on that.
        CodeAnalysisResults results = resultsWithLinks();
        results.setSkippedSymbolicLinks(null);

        assertFalse(scopeSectionOf(results).contains("symbolic link"));
    }

    private CodeAnalysisResults resultsWithLinks(SymbolicLink... links) {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setCodeConfiguration(CodeConfiguration.getDefaultConfiguration());
        // Set by BasicsAnalyzer on every real run; unset on a bare results object.
        results.setFilesExcludedByExtension(new ArrayList<>());
        Arrays.asList(results.getMainAspectAnalysisResults(), results.getTestAspectAnalysisResults(),
                results.getGeneratedAspectAnalysisResults(), results.getBuildAndDeployAspectAnalysisResults(),
                results.getOtherAspectAnalysisResults())
                .forEach(aspect -> aspect.setAspect(new NamedSourceCodeAspect("scope")));
        results.setSkippedSymbolicLinks(Arrays.asList(links));
        return results;
    }

    private String scopeSectionOf(CodeAnalysisResults results) {
        RichTextReport report = new RichTextReport("", "");
        new OverviewReportGenerator(results, null).addScopeAnalysisToReport(report);
        return report.getRichTextFragments().stream()
                .map(RichTextFragment::getFragment).collect(Collectors.joining("\n"));
    }
}
