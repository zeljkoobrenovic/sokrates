package nl.obren.sokrates.sourcecode.githistory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

/**
 * A commit co-author resolved from a trailer (see CoAuthorsConfig): a person (agent == null, with
 * the email normalised the same way as commit authors) or an AI agent (agent = configured name).
 */
public class CoAuthor {
    public static final String BOT_AGENT = "bot";

    private String name = "";
    private String email = "";
    private String agent = null;

    public CoAuthor() {
    }

    public CoAuthor(String name, String email, String agent) {
        this.name = name;
        this.email = email;
        this.agent = agent;
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

    @JsonIgnore
    public boolean isAi() {
        return StringUtils.isNotBlank(agent);
    }

    /**
     * Identity used to dedupe co-authors within a commit: the agent name for agents, else the
     * email (falling back to the name when the trailer had no email).
     */
    @JsonIgnore
    public String getKey() {
        if (isAi()) {
            return agent;
        }
        return StringUtils.isNotBlank(email) ? email : name;
    }
}
