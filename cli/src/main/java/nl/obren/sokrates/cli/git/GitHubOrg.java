package nl.obren.sokrates.cli.git;

import com.fasterxml.jackson.databind.JsonNode;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import org.apache.commons.lang3.StringUtils;

/**
 * A GitHub organization (or user account) as returned by {@code GET /orgs/{login}} (or
 * {@code /users/{login}}): what analyzeGitHubOrg needs to title, describe, link and brand the
 * organization's landscape.
 */
public class GitHubOrg {
    private String login = "";
    private String name = "";
    private String description = "";
    private String htmlUrl = "";
    private String avatarUrl = "";
    private boolean user = false;

    public GitHubOrg() {
    }

    public GitHubOrg(String login, String name, String description, String htmlUrl, String avatarUrl) {
        this.login = login;
        this.name = name;
        this.description = description;
        this.htmlUrl = htmlUrl;
        this.avatarUrl = avatarUrl;
    }

    public static GitHubOrg fromJson(JsonNode json, boolean user) {
        GitHubOrg org = new GitHubOrg();
        org.login = json.path("login").asText("");
        org.name = json.path("name").asText("");
        org.description = json.path("description").asText("");
        if (user && StringUtils.isBlank(org.description)) {
            org.description = json.path("bio").asText("");
        }
        org.htmlUrl = json.path("html_url").asText("");
        org.avatarUrl = json.path("avatar_url").asText("");
        org.user = user;
        return org;
    }

    /** The display name: the organization's name, or its login when it has none. */
    public String displayName() {
        return StringUtils.isNotBlank(name) ? name.trim() : login;
    }

    /**
     * Fills the blank fields of a landscape's metadata from the organization: name, description,
     * logo (the avatar) and — only when the landscape has no links yet — a link to the organization
     * page. Fields the user already set are left alone, so edits survive re-runs.
     */
    public boolean applyTo(Metadata metadata) {
        boolean changed = false;
        if (StringUtils.isBlank(metadata.getName()) && StringUtils.isNotBlank(displayName())) {
            metadata.setName(displayName());
            changed = true;
        }
        if (StringUtils.isBlank(metadata.getDescription()) && StringUtils.isNotBlank(description)) {
            metadata.setDescription(description.trim());
            changed = true;
        }
        if (StringUtils.isBlank(metadata.getLogoLink()) && StringUtils.isNotBlank(avatarUrl)) {
            metadata.setLogoLink(avatarUrl);
            changed = true;
        }
        if (metadata.getLinks().isEmpty() && StringUtils.isNotBlank(htmlUrl)) {
            metadata.getLinks().add(new Link("GitHub", htmlUrl));
            changed = true;
        }
        return changed;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /** True when the login is a personal account rather than an organization. */
    public boolean isUser() {
        return user;
    }

    public void setUser(boolean user) {
        this.user = user;
    }
}
