package nl.obren.sokrates.cli.git;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Report metadata derived from a repository's git remote, so that a clone analyzed in a folder with
 * a meaningless name (the Docker image mounts the code base at {@code /code}, which used to give
 * every report the title "Code") is still titled after the repository, and linked to it.
 * <p>
 * From the {@code origin} URL alone: the report name ({@code owner/repository} for hosted repositories, see
 * {@link #reportName()}), the browsable web URL
 * (https form, {@code .git} stripped; scp-style {@code git@host:org/repo.git} handled) and, on
 * github.com, the owner's avatar as the logo. From the GitHub REST API, best effort (5 s timeout,
 * skipped when {@link #ENV_OFFLINE} is set): the repository description and the owner's avatar
 * URL. Everything is optional: callers fill only the metadata fields the user did not set.
 */
public class GitRepoMetadata {
    public static final String ENV_OFFLINE = "SOKRATES_OFFLINE";

    private static final Log LOG = LogFactory.getLog(GitRepoMetadata.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String remoteUrl;
    private final String webUrl;
    private final String host;
    private final String owner;
    private final String name;
    private String description = "";
    private String logoLink = "";

    private GitRepoMetadata(String remoteUrl, String webUrl, String host, String owner, String name) {
        this.remoteUrl = remoteUrl;
        this.webUrl = webUrl;
        this.host = host;
        this.owner = owner;
        this.name = name;
    }

    /**
     * The metadata of the repository at {@code root}, from its {@code origin} remote; {@code null}
     * when {@code root} is not a git repository or has no origin. Does not touch the network.
     */
    public static GitRepoMetadata fromLocalRepository(File root) {
        File gitDir = new File(root, ".git");
        if (!gitDir.exists()) {
            return null;
        }
        try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).setMustExist(true).build()) {
            String url = repository.getConfig().getString("remote", "origin", "url");
            return StringUtils.isBlank(url) ? null : fromUrl(url);
        } catch (Exception e) {
            LOG.info("Could not read the git remote of " + root.getPath() + ": " + e.getMessage());
            return null;
        }
    }

    /** Pure URL parsing (no network): name, owner, host and browsable URL. {@code null} for a URL without a path. */
    public static GitRepoMetadata fromUrl(String url) {
        String cleaned = StringUtils.stripEnd(url.trim(), "/");
        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.substring(0, cleaned.length() - ".git".length());
        }
        String host;
        String path;
        if (cleaned.matches("^[A-Za-z][A-Za-z0-9+.-]*://.*")) {
            // https://host/org/repo, ssh://git@host/org/repo, file:///path/repo
            String rest = cleaned.substring(cleaned.indexOf("://") + 3);
            int slash = rest.indexOf('/');
            String authority = slash >= 0 ? rest.substring(0, slash) : rest;
            path = slash >= 0 ? rest.substring(slash + 1) : "";
            host = authority.contains("@") ? authority.substring(authority.indexOf('@') + 1) : authority;
            if (host.contains(":")) {
                host = host.substring(0, host.indexOf(':'));
            }
        } else if (cleaned.matches("^[^/]+@[^:]+:.*") || cleaned.matches("^[^/:]+:[^/].*")) {
            // git@host:org/repo (scp-style)
            String authority = cleaned.substring(0, cleaned.indexOf(':'));
            path = cleaned.substring(cleaned.indexOf(':') + 1);
            host = authority.contains("@") ? authority.substring(authority.indexOf('@') + 1) : authority;
        } else {
            // a plain local path
            host = "";
            path = cleaned;
        }
        path = StringUtils.strip(path, "/");
        if (path.isEmpty()) {
            return null;
        }
        String[] segments = path.split("/");
        String name = segments[segments.length - 1];
        String owner = segments.length >= 2 ? segments[segments.length - 2] : "";
        boolean web = StringUtils.isNotBlank(host) && !"localhost".equals(host) && !host.startsWith("127.");
        String webUrl = web ? "https://" + host + "/" + path : "";
        GitRepoMetadata metadata = new GitRepoMetadata(url, webUrl, host.toLowerCase(), owner, name);
        if (metadata.isGitHub() && StringUtils.isNotBlank(owner)) {
            metadata.logoLink = "https://github.com/" + owner + ".png?size=200";
        }
        return metadata;
    }

    /**
     * The report name and the folder analyzeGitRepo keeps the reports in: {@code <owner>/<name>} for a
     * hosted repository (e.g. {@code junit-team/junit4}), just {@code <name>} for a local one. The owner
     * keeps repositories with the same name from different owners apart — the landscape identifies a
     * repository by its report name — and its list renders {@code owner/name} with the owner greyed.
     */
    public String reportName() {
        return StringUtils.isNotBlank(webUrl) && StringUtils.isNotBlank(owner) ? owner + "/" + name : name;
    }

    /** Same as {@link #reportName()}: the output folder mirrors the report name. */
    public String outputFolderName() {
        return reportName();
    }

    public boolean isGitHub() {
        return "github.com".equals(host);
    }

    /**
     * Completes the description (and a canonical avatar URL) from the GitHub REST API. Best effort:
     * any failure, a non-GitHub host or {@link #ENV_OFFLINE} leaves the URL-derived values as they
     * are. The optional {@link GitRepoCloner#ENV_TOKEN} raises the API rate limit (60/h anonymous).
     */
    public GitRepoMetadata fetchDetails() {
        if (!isGitHub() || StringUtils.isBlank(owner) || StringUtils.isNotBlank(System.getenv(ENV_OFFLINE))) {
            return this;
        }
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + owner + "/" + name))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "sokrates");
            String token = System.getenv(GitRepoCloner.ENV_TOKEN);
            if (StringUtils.isNotBlank(token)) {
                request.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build()
                    .send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.info("GitHub API returned " + response.statusCode() + " for " + owner + "/" + name + "; using the URL-derived metadata only.");
                return this;
            }
            JsonNode json = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readTree(response.body());
            String apiDescription = json.path("description").asText("");
            if (StringUtils.isNotBlank(apiDescription)) {
                description = apiDescription.trim();
            }
            String avatar = json.path("owner").path("avatar_url").asText("");
            if (StringUtils.isNotBlank(avatar)) {
                logoLink = avatar;
            }
        } catch (Exception e) {
            LOG.info("Could not fetch GitHub details for " + owner + "/" + name + " (" + e.getMessage() + "); using the URL-derived metadata only.");
        }
        return this;
    }

    /**
     * Fills the fields of {@code metadata} the user has not set: the name when it is blank or still
     * the folder-derived default, description/logo when blank, and the repository link when there
     * is no link yet (a link to the same URL counts as already there). Returns whether anything changed.
     */
    public boolean applyTo(Metadata metadata, String folderDefaultName) {
        boolean changed = false;
        String reportName = reportName();
        if (StringUtils.isBlank(metadata.getName()) || metadata.getName().equals(folderDefaultName)) {
            if (!reportName.equals(metadata.getName())) {
                metadata.setName(reportName);
                changed = true;
            }
        }
        if (StringUtils.isBlank(metadata.getDescription()) && StringUtils.isNotBlank(description)) {
            metadata.setDescription(description);
            changed = true;
        }
        if (StringUtils.isBlank(metadata.getLogoLink()) && StringUtils.isNotBlank(logoLink)) {
            metadata.setLogoLink(logoLink);
            changed = true;
        }
        if (StringUtils.isNotBlank(webUrl) && metadata.getLinks().stream().noneMatch(l -> webUrl.equals(l.getHref()))) {
            if (metadata.getLinks().isEmpty()) {
                metadata.getLinks().add(new Link(linkLabel(), webUrl));
                changed = true;
            }
        }
        return changed;
    }

    public String linkLabel() {
        if (isGitHub()) return "GitHub";
        if ("gitlab.com".equals(host)) return "GitLab";
        if ("bitbucket.org".equals(host)) return "Bitbucket";
        return "Repository";
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public String getHost() {
        return host;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLogoLink() {
        return logoLink;
    }
}
