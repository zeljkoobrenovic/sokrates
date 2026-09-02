package nl.obren.sokrates.reports.generators.explorers;

import com.fasterxml.jackson.annotation.JsonInclude;
import nl.obren.sokrates.sourcecode.githistory.CoAuthor;

/**
 * One co-author of a commit row in the commits explorer (commits-explorer.html), from the
 * git-commit-trailers.txt sidecar resolved via CoAuthorsConfig. {@code agent} is the AI agent
 * name ("Claude Code", ...; "bot" when only the bots list matched) and is omitted for people.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CoAuthorExport {
    private String name = "";
    private String email = "";
    private String agent = null;

    public CoAuthorExport() {
    }

    public CoAuthorExport(CoAuthor coAuthor) {
        this.name = coAuthor.getName();
        this.email = coAuthor.getEmail();
        this.agent = coAuthor.getAgent();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }
}
