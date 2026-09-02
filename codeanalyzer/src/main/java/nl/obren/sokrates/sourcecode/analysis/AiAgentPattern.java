package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A named AI coding agent recognised in commit co-author trailers: a display name plus regexes
 * matched (entirely, case-insensitively) against the trailer value ("Name <email>") and against
 * the email part alone.
 */
public class AiAgentPattern {
    private String name = "";
    private List<String> patterns = new ArrayList<>();

    public AiAgentPattern() {
    }

    public AiAgentPattern(String name, String... patterns) {
        this.name = name;
        this.patterns = new ArrayList<>(Arrays.asList(patterns));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    @JsonIgnore
    public boolean matches(String value, String email) {
        return RegexUtils.matchesAnyPatternIgnoreCase(value == null ? "" : value, patterns)
                || RegexUtils.matchesAnyPatternIgnoreCase(email == null ? "" : email, patterns);
    }
}
