package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextFragment;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContributorsReportUtilsTileTest {

    @Test
    void tileTitleAndLabelEscapeBothQuotes() {
        // The tile's title is single-quoted, so an apostrophe in the email must be escaped too
        // (StringEscapeUtils.escapeHtml4 leaves it alone).
        Contributor contributor = new Contributor("pwn'/onmouseover='XSS\"x@example.com");
        contributor.setCommitsCount(3);
        contributor.setFirstCommitDate("2026-01-01");
        contributor.setLatestCommitDate("2026-02-01");

        RichTextReport report = new RichTextReport("", "");
        ContributorsReportUtils.addContributor(report, 10, 10, contributor);

        String html = report.getRichTextFragments().stream().map(RichTextFragment::getFragment).collect(Collectors.joining("\n"));
        assertFalse(html.contains("onmouseover='XSS"), html);
        assertFalse(html.contains("XSS\"x"), html);
        assertTrue(html.contains("title='pwn&#39;/onmouseover=&#39;XSS&quot;x@example.com 3 commits"), html);
        assertTrue(html.contains(">\npwn&#39;/onmouseover=&#39;XSS&quot;x@example.com\n<") || html.contains("pwn&#39;/onmouseover=&#39;XSS&quot;x@example.com</div>"), html);
    }
}
