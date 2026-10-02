package nl.obren.sokrates.cli.git;

import org.apache.commons.lang3.StringUtils;

/**
 * One repository of a code-host organization (GitHub repository, GitLab project) as listed by the
 * host's API — the subset of fields the organization-level analysis commands filter on and clone
 * from. {@code pushedAt} is the last push/activity instant (ISO-8601).
 */
public class CodeHostRepo {
    private String name = "";
    private String folderPath = "";
    private String fullName = "";
    private String cloneUrl = "";
    private String htmlUrl = "";
    private String description = "";
    private String avatarUrl = "";
    private String language = "";
    private String defaultBranch = "";
    private String pushedAt = "";
    private int sizeKb = 0;
    private boolean fork = false;
    private boolean archived = false;
    private boolean privateRepo = false;

    public CodeHostRepo() {
    }

    public CodeHostRepo(String name, String cloneUrl) {
        this.name = name;
        this.cloneUrl = cloneUrl;
    }

    public String getName() {
        return name;
    }

    /**
     * Where the analysis is kept, relative to the organization folder: the name, unless the host
     * nests repositories (GitLab subgroups: {@code subgroup/project}).
     */
    public String getFolderPath() {
        return StringUtils.isNotBlank(folderPath) ? folderPath : name;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
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

    /** The owner's (GitHub) or project's (GitLab) avatar; the repository report's logo. */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
