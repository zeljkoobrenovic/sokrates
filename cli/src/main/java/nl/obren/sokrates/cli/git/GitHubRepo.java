package nl.obren.sokrates.cli.git;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One repository of a GitHub organization (or user), as listed by the GitHub REST API — the subset
 * of fields analyzeGitHubOrg filters on and clones from.
 */
public class GitHubRepo {
    private String name = "";
    private String fullName = "";
    private String cloneUrl = "";
    private String htmlUrl = "";
    private String description = "";
    private String language = "";
    private String defaultBranch = "";
    private String pushedAt = "";
    private int sizeKb = 0;
    private boolean fork = false;
    private boolean archived = false;
    private boolean privateRepo = false;

    public GitHubRepo() {
    }

    public GitHubRepo(String name, String cloneUrl) {
        this.name = name;
        this.cloneUrl = cloneUrl;
    }

    /** From one element of the {@code /orgs/{org}/repos} (or {@code /users/{user}/repos}) response. */
    public static GitHubRepo fromJson(JsonNode json) {
        GitHubRepo repo = new GitHubRepo();
        repo.name = json.path("name").asText("");
        repo.fullName = json.path("full_name").asText("");
        repo.cloneUrl = json.path("clone_url").asText("");
        repo.htmlUrl = json.path("html_url").asText("");
        repo.description = json.path("description").asText("");
        repo.language = json.path("language").asText("");
        repo.defaultBranch = json.path("default_branch").asText("");
        repo.pushedAt = json.path("pushed_at").asText("");
        repo.sizeKb = json.path("size").asInt(0);
        repo.fork = json.path("fork").asBoolean(false);
        repo.archived = json.path("archived").asBoolean(false);
        repo.privateRepo = json.path("private").asBoolean(false);
        return repo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public void setCloneUrl(String cloneUrl) {
        this.cloneUrl = cloneUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    /** ISO-8601 instant of the last push (e.g. {@code 2026-09-30T14:03:11Z}); empty when unknown. */
    public String getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(String pushedAt) {
        this.pushedAt = pushedAt;
    }

    public int getSizeKb() {
        return sizeKb;
    }

    public void setSizeKb(int sizeKb) {
        this.sizeKb = sizeKb;
    }

    public boolean isFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isPrivateRepo() {
        return privateRepo;
    }

    public void setPrivateRepo(boolean privateRepo) {
        this.privateRepo = privateRepo;
    }

    @Override
    public String toString() {
        return fullName.isEmpty() ? name : fullName;
    }
}
