package nl.obren.sokrates.cli.git;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The GitHub REST API calls behind analyzeGitHubOrg: the organization's profile and the list of its
 * repositories (paginated, 100 per page). A login that is not an organization falls back to the
 * user endpoints, so personal accounts work too. {@link GitRepoCloner#ENV_TOKEN} is sent as a
 * bearer token when set — required for private repositories and for the higher rate limit (5000
 * requests/hour instead of 60). The HTTP layer is an injectable {@link HttpFetcher}.
 */
public class GitHubOrgClient implements CodeHostOrgClient {
    public static final String DEFAULT_API_BASE = "https://api.github.com";
    public static final int PAGE_SIZE = 100;

    private static final Log LOG = LogFactory.getLog(GitHubOrgClient.class);

    private final HttpFetcher fetcher;
    private final String apiBase;

    public GitHubOrgClient() {
        this(HttpFetcher.create(Map.of(
                "Accept", "application/vnd.github+json",
                "Authorization", StringUtils.isNotBlank(System.getenv(GitRepoCloner.ENV_TOKEN)) ? "Bearer " + System.getenv(GitRepoCloner.ENV_TOKEN) : "")), DEFAULT_API_BASE);
    }

    public GitHubOrgClient(HttpFetcher fetcher) {
        this(fetcher, DEFAULT_API_BASE);
    }

    public GitHubOrgClient(HttpFetcher fetcher, String apiBase) {
        this.fetcher = fetcher;
        this.apiBase = StringUtils.stripEnd(apiBase, "/");
    }

    @Override
    public String hostLabel() {
        return "GitHub";
    }

    /** The organization's profile; a personal account when {@code login} is not an organization. */
    @Override
    public CodeHostOrg fetchOrg(String login) throws IOException {
        HttpFetcher.Response response = fetcher.get(apiBase + "/orgs/" + login);
        boolean user = false;
        if (response.status == 404) {
            response = fetcher.get(apiBase + "/users/" + login);
            user = true;
        }
        check(response, login);
        JsonNode json = parse(response.body);
        CodeHostOrg org = new CodeHostOrg();
        org.setLogin(json.path("login").asText(login));
        org.setName(json.path("name").asText(""));
        org.setDescription(json.path("description").asText(""));
        if (user && StringUtils.isBlank(org.getDescription())) {
            org.setDescription(json.path("bio").asText(""));
        }
        org.setHtmlUrl(json.path("html_url").asText(""));
        org.setAvatarUrl(json.path("avatar_url").asText(""));
        org.setUser(user);
        org.setLinkLabel(hostLabel());
        return org;
    }

    /** Every repository of the organization (or user) the token can see, in API order. */
    @Override
    public List<CodeHostRepo> listRepos(String login) throws IOException {
        List<CodeHostRepo> repos = new ArrayList<>();
        String path = "/orgs/" + login + "/repos?type=all";
        for (int page = 1; ; page++) {
            HttpFetcher.Response response = fetcher.get(apiBase + path + "&per_page=" + PAGE_SIZE + "&page=" + page);
            if (response.status == 404 && page == 1 && path.startsWith("/orgs/")) {
                path = "/users/" + login + "/repos?type=owner";
                response = fetcher.get(apiBase + path + "&per_page=" + PAGE_SIZE + "&page=" + page);
            }
            check(response, login);
            JsonNode json = parse(response.body);
            if (!json.isArray()) {
                throw new IOException("Unexpected GitHub API response for " + login + " (not a list of repositories).");
            }
            json.forEach(node -> repos.add(toRepo(node)));
            LOG.info("GitHub: " + login + " page " + page + " -> " + json.size() + " repositories (" + repos.size() + " so far)");
            if (json.size() < PAGE_SIZE) {
                break;
            }
        }
        return repos;
    }

    /** From one element of the {@code /orgs/{org}/repos} (or {@code /users/{user}/repos}) response. */
    static CodeHostRepo toRepo(JsonNode json) {
        CodeHostRepo repo = new CodeHostRepo();
        repo.setName(json.path("name").asText(""));
        repo.setFullName(json.path("full_name").asText(""));
        repo.setCloneUrl(json.path("clone_url").asText(""));
        repo.setHtmlUrl(json.path("html_url").asText(""));
        repo.setDescription(json.path("description").asText(""));
        repo.setAvatarUrl(json.path("owner").path("avatar_url").asText(""));
        repo.setLanguage(json.path("language").asText(""));
        repo.setDefaultBranch(json.path("default_branch").asText(""));
        repo.setPushedAt(json.path("pushed_at").asText(""));
        repo.setSizeKb(json.path("size").asInt(0));
        repo.setFork(json.path("fork").asBoolean(false));
        repo.setArchived(json.path("archived").asBoolean(false));
        repo.setPrivateRepo(json.path("private").asBoolean(false));
        return repo;
    }

    private static void check(HttpFetcher.Response response, String login) throws IOException {
        if (response.status == 200) {
            return;
        }
        String hint = "";
        if (response.status == 404) {
            hint = " (no such organization or user, or it is not visible with the current credentials)";
        } else if (response.status == 401) {
            hint = " (invalid " + GitRepoCloner.ENV_TOKEN + ")";
        } else if (response.status == 403 || response.status == 429) {
            hint = " (rate limit or missing permissions; set " + GitRepoCloner.ENV_TOKEN + " to a token with read access)";
        }
        throw new IOException("GitHub API returned " + response.status + " for " + login + hint);
    }

    private static JsonNode parse(String body) throws IOException {
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readTree(body);
    }
}
