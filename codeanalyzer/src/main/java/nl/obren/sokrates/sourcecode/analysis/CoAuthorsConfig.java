package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * How commit-message trailers (from the optional git-commit-trailers.txt sidecar) are turned into
 * co-author information: which trailer keys name a co-author, and which co-author values are AI
 * coding agents (Claude Code, Copilot, Cursor, ...). Part of fileHistoryAnalysis in config.json.
 */
public class CoAuthorsConfig {
    // If false, co-author / AI agent analysis is skipped entirely: the trailers sidecar is not read,
    // commits get no co-authors, and the related report column/row are not shown.
    private boolean enabled = true;

    // Trailer keys (case-insensitive) whose value names a co-author
    private List<String> trailerKeys = defaultTrailerKeys();

    // Named AI agents; a co-author matching one of an agent's patterns is attributed to that agent
    private List<AiAgentPattern> aiAgents = defaultAiAgents();

    public static List<String> defaultTrailerKeys() {
        return new ArrayList<>(Arrays.asList("Co-authored-by", "Assisted-by", "Generated-by", "Message-Signature"));
    }

    public static List<AiAgentPattern> defaultAiAgents() {
        return new ArrayList<>(Arrays.asList(
                new AiAgentPattern("Claude Code", ".*noreply@anthropic\\.com.*", ".*generated with \\[claude code\\].*", ".*claude.*"),
                new AiAgentPattern("GitHub Copilot", ".*copilot.*"),
                new AiAgentPattern("Cursor", ".*cursoragent.*", ".*@cursor\\.com.*"),
                new AiAgentPattern("OpenAI Codex", ".*codex.*"),
                new AiAgentPattern("Aider", ".*aider.*"),
                new AiAgentPattern("Gemini", ".*gemini.*"),
                new AiAgentPattern("Devin", ".*devin-ai.*"),
                new AiAgentPattern("Jules", ".*google-labs-jules.*")
        ));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getTrailerKeys() {
        return trailerKeys;
    }

    public void setTrailerKeys(List<String> trailerKeys) {
        this.trailerKeys = trailerKeys;
    }

    public List<AiAgentPattern> getAiAgents() {
        return aiAgents;
    }

    public void setAiAgents(List<AiAgentPattern> aiAgents) {
        this.aiAgents = aiAgents;
    }

    @JsonIgnore
    public boolean isCoAuthorKey(String key) {
        if (key == null || trailerKeys == null) {
            return false;
        }
        return trailerKeys.stream().anyMatch(k -> k != null && k.equalsIgnoreCase(key.trim()));
    }

    /**
     * The name of the first AI agent whose patterns match the trailer value or its email, or null
     * for a human co-author.
     */
    @JsonIgnore
    public String classify(String value, String email) {
        if (aiAgents == null) {
            return null;
        }
        for (AiAgentPattern agent : aiAgents) {
            if (agent != null && agent.matches(value, email)) {
                return agent.getName();
            }
        }
        return null;
    }
}
