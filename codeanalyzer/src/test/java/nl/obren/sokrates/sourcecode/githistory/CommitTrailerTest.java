package nl.obren.sokrates.sourcecode.githistory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommitTrailerTest {
    @Test
    void parseTrailerLine() {
        CommitTrailer trailer = CommitTrailer.parse("Co-authored-by: Claude <noreply@anthropic.com>");
        assertNotNull(trailer);
        assertEquals("Co-authored-by", trailer.getKey());
        assertEquals("Claude <noreply@anthropic.com>", trailer.getValue());
        assertEquals("Claude", trailer.getName());
        assertEquals("noreply@anthropic.com", trailer.getEmail());
        assertTrue(trailer.hasKey("co-authored-by"));
        assertFalse(trailer.hasKey("Signed-off-by"));
    }

    @Test
    void parseRejectsNonTrailers() {
        assertNull(CommitTrailer.parse("Fix the bug"));
        assertNull(CommitTrailer.parse("http://example.com"));
        assertNull(CommitTrailer.parse("Key:no-space"));
        assertNull(CommitTrailer.parse(""));
        assertNull(CommitTrailer.parse(null));
    }

    @Test
    void nameAndEmailWithoutAngleBrackets() {
        CommitTrailer trailer = CommitTrailer.parse("Generated-by: GitHub Copilot");
        assertEquals("GitHub Copilot", trailer.getName());
        assertEquals("", trailer.getEmail());
        CommitTrailer upper = CommitTrailer.parse("Co-authored-by: Jane <Jane@Acme.COM>");
        assertEquals("jane@acme.com", upper.getEmail());
    }

    @Test
    void extractTrailersFromClaudeCodeStyleMessage() {
        String message = "Add addCustomTab command\n\n"
                + "Some explanation\nover two lines.\n\n"
                + "🤖 Generated with [Claude Code](https://claude.com/claude-code)\n\n"
                + "Co-authored-by: Claude <noreply@anthropic.com>\n"
                + "Signed-off-by: Jane Doe <jane@acme.com>\n";
        List<CommitTrailer> trailers = GitHistoryUtils.extractTrailers(message);
        assertEquals(2, trailers.size());
        assertEquals("Co-authored-by: Claude <noreply@anthropic.com>", trailers.get(0).toString());
        assertEquals("Signed-off-by: Jane Doe <jane@acme.com>", trailers.get(1).toString());
    }

    @Test
    void extractTrailersIgnoresSubjectOnlyAndBodyOnlyMessages() {
        assertTrue(GitHistoryUtils.extractTrailers("Fix: something").isEmpty());
        assertTrue(GitHistoryUtils.extractTrailers("Note: not a trailer, it is the subject").isEmpty());
        assertTrue(GitHistoryUtils.extractTrailers("Subject\n\nJust a body paragraph.").isEmpty());
        assertTrue(GitHistoryUtils.extractTrailers("").isEmpty());
        assertTrue(GitHistoryUtils.extractTrailers(null).isEmpty());
    }

    @Test
    void extractTrailersFromMixedFinalParagraphAndFoldedLines() {
        String message = "Subject\r\n\r\nSee the docs.\r\nCo-authored-by: A <a@x.com>\r\n"
                + "Reviewed-by: B <b@x.com>\r\n  and C <c@x.com>\r\n\r\n";
        List<CommitTrailer> trailers = GitHistoryUtils.extractTrailers(message);
        assertEquals(2, trailers.size());
        assertEquals("A <a@x.com>", trailers.get(0).getValue());
        assertEquals("B <b@x.com> and C <c@x.com>", trailers.get(1).getValue());
    }

    @Test
    void extractMessageSignaturesFindsToolSignatureLinesOnly() {
        String message = "Generated with care: fix subject line is never a signature\n\n"
                + "Body text; goldens regenerated with -Dsokrates.updateGolden=true.\n"
                + "<summary>; regenerated with the new flag\n\n"
                + "🤖 Generated with [Claude Code](https://claude.ai/code)\n"
                + "- Made with Cursor\n"
                + "🤖 Generated with [Claude Code](https://claude.ai/code)\n\n"
                + "Co-authored-by: Claude <noreply@anthropic.com>\n";
        List<String> signatures = GitHistoryUtils.extractMessageSignatures(message);
        assertEquals(List.of("🤖 Generated with [Claude Code](https://claude.ai/code)", "- Made with Cursor"), signatures);
        assertTrue(GitHistoryUtils.extractMessageSignatures("Made with love").isEmpty());
        assertTrue(GitHistoryUtils.extractMessageSignatures(null).isEmpty());
    }
}
