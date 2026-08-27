package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CoAuthorsConfigTest {
    @Test
    void defaultTrailerKeysAreCaseInsensitive() {
        CoAuthorsConfig config = new CoAuthorsConfig();
        assertTrue(config.isCoAuthorKey("Co-authored-by"));
        assertTrue(config.isCoAuthorKey("Co-Authored-By"));
        assertTrue(config.isCoAuthorKey("generated-by"));
        assertFalse(config.isCoAuthorKey("Signed-off-by"));
        assertFalse(config.isCoAuthorKey("Committer"));
        assertFalse(config.isCoAuthorKey(null));
    }

    @Test
    void defaultAgentsClassifyCommonAgentTrailers() {
        CoAuthorsConfig config = new CoAuthorsConfig();
        assertEquals("Claude Code", config.classify("Claude <noreply@anthropic.com>", "noreply@anthropic.com"));
        assertEquals("Claude Code", config.classify("Claude Fable 5 <noreply@anthropic.com>", "noreply@anthropic.com"));
        assertEquals("GitHub Copilot", config.classify("Copilot <223556219+Copilot@users.noreply.github.com>", "223556219+copilot@users.noreply.github.com"));
        assertEquals("Cursor", config.classify("Cursor Agent <cursoragent@cursor.com>", "cursoragent@cursor.com"));
        assertEquals("OpenAI Codex", config.classify("Codex <codex@openai.com>", "codex@openai.com"));
        assertEquals("Aider", config.classify("aider (gpt-4o) <aider@aider.chat>", "aider@aider.chat"));
        assertEquals("Devin", config.classify("devin-ai-integration[bot] <158243242+devin-ai-integration[bot]@users.noreply.github.com>", ""));
        assertNull(config.classify("Jane Doe <jane@acme.com>", "jane@acme.com"));
        assertEquals("Claude Code", config.classify("🤖 Generated with [Claude Code](https://claude.com/claude-code)", ""));
        assertTrue(config.isCoAuthorKey("Message-Signature"));
    }

    @Test
    void customAgentsAndKeysReplaceDefaults() {
        CoAuthorsConfig config = new CoAuthorsConfig();
        config.setTrailerKeys(Arrays.asList("Paired-with"));
        config.setAiAgents(Arrays.asList(new AiAgentPattern("Acme Bot", ".*@bots\\.acme\\.com")));
        assertTrue(config.isCoAuthorKey("paired-with"));
        assertFalse(config.isCoAuthorKey("Co-authored-by"));
        assertEquals("Acme Bot", config.classify("Acme <x@bots.acme.com>", "x@bots.acme.com"));
        assertNull(config.classify("Claude <noreply@anthropic.com>", "noreply@anthropic.com"));
    }

    @Test
    void roundTripsThroughJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        FileHistoryAnalysisConfig config = new FileHistoryAnalysisConfig();
        String json = mapper.writeValueAsString(config);
        assertTrue(json.contains("\"coAuthors\""));
        assertTrue(json.contains("\"enabled\":true"));
        assertTrue(json.contains("\"trailerKeys\""));
        assertTrue(json.contains("\"aiAgents\""));
        assertFalse(json.contains("coAuthorKey"));

        FileHistoryAnalysisConfig read = mapper.readValue(json, FileHistoryAnalysisConfig.class);
        assertEquals(CoAuthorsConfig.defaultAiAgents().size(), read.getCoAuthors().getAiAgents().size());
        assertEquals("Claude Code", read.getCoAuthors().classify("Claude <noreply@anthropic.com>", "noreply@anthropic.com"));

        // Older config.json without the block keeps the defaults
        FileHistoryAnalysisConfig legacy = mapper.readValue("{\"importPath\":\"../git-history.txt\"}", FileHistoryAnalysisConfig.class);
        assertTrue(legacy.getCoAuthors().isCoAuthorKey("Co-authored-by"));
        assertTrue(legacy.getCoAuthors().isEnabled());
        FileHistoryAnalysisConfig disabled = mapper.readValue("{\"coAuthors\":{\"enabled\":false}}", FileHistoryAnalysisConfig.class);
        assertFalse(disabled.getCoAuthors().isEnabled());
    }
}
