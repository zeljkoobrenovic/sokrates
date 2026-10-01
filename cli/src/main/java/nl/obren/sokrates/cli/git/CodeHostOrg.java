package nl.obren.sokrates.cli.git;

import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import org.apache.commons.lang3.StringUtils;

/**
 * An organization on a code host — a GitHub organization or user, a GitLab group or user — as the
 * organization-level analysis commands see it: what they need to title, describe, link and brand
 * the organization's landscape. {@code login} is the host's identifier (GitHub login, GitLab full
 * path) and names the output folder.
 */
public class CodeHostOrg {
    private String login = "";
    private String name = "";
    private String description = "";
    private String htmlUrl = "";
    private String avatarUrl = "";
    private String linkLabel = "Repository host";
    private boolean user = false;
    private String userId = "";

    public CodeHostOrg() {
    }

    public CodeHostOrg(String login, String name, String description, String htmlUrl, String avatarUrl) {
        this.login = login;
        this.name = name;
        this.description = description;
        this.htmlUrl = htmlUrl;
        this.avatarUrl = avatarUrl;
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
            metadata.getLinks().add(new Link(linkLabel, htmlUrl));
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

    /** The host's numeric user id when {@link #isUser()} (GitLab lists a user's projects by id). */
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /** The label of the link to the organization page ("GitHub", "GitLab"). */
    public String getLinkLabel() {
        return linkLabel;
    }

    public void setLinkLabel(String linkLabel) {
        this.linkLabel = linkLabel;
    }
}
