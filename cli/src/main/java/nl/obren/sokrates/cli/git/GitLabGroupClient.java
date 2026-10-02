package nl.obren.sokrates.cli.git;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The GitLab REST API (v4) calls behind analyzeGitLabGroup: a group's profile and the projects of
 * the group and all its subgroups (paginated, 100 per page), on gitlab.com or a self-hosted
 * instance ({@code baseUrl}). A path that is not a group falls back to a user (projects owned by
 * that user). {@link GitRepoCloner#ENV_TOKEN} is sent as {@code PRIVATE-TOKEN} when set — required
 * for private groups/projects. The HTTP layer is an injectable {@link HttpFetcher}.
 * <p>
 * A project's {@link CodeHostRepo#getFolderPath() folder path} is its path relative to the group
 * (subgroup folders kept), so projects with the same name in different subgroups do not collide.
 */
public class GitLabGroupClient implements CodeHostOrgClient {
    public static final String DEFAULT_BASE_URL = "https://gitlab.com";
    public static final int PAGE_SIZE = 100;

    private static final Log LOG = LogFactory.getLog(GitLabGroupClient.class);

    private final HttpFetcher fetcher;
    private final String apiBase;

    public GitLabGroupClient(String baseUrl) {
        this(HttpFetcher.create(Map.of("PRIVATE-TOKEN", StringUtils.defaultString(System.getenv(GitRepoCloner.ENV_TOKEN)))), baseUrl);
    }

    public GitLabGroupClient(HttpFetcher fetcher, String baseUrl) {
        this.fetcher = fetcher;
        this.apiBase = StringUtils.stripEnd(StringUtils.defaultIfBlank(baseUrl, DEFAULT_BASE_URL), "/") + "/api/v4";
    }

    @Override
    public String hostLabel() {
        return "GitLab";
    }

    /** The group's profile (login = its full path, e.g. {@code gitlab-org/ci-cd}); a user when the path is a username. */
    @Override
    public CodeHostOrg fetchOrg(String path) throws IOException {
        HttpFetcher.Response response = fetcher.get(apiBase + "/groups/" + encode(path) + "?with_projects=false");
        if (response.status == 200) {
            JsonNode json = parse(response.body);
            CodeHostOrg org = new CodeHostOrg();
            org.setLogin(json.path("full_path").asText(path));
            org.setName(json.path("name").asText(""));
            org.setDescription(json.path("description").asText(""));
            org.setHtmlUrl(json.path("web_url").asText(""));
            org.setAvatarUrl(json.path("avatar_url").asText(""));
            org.setLinkLabel(hostLabel());
            return org;
        }
        if (response.status == 404 && !path.contains("/")) {
            HttpFetcher.Response users = fetcher.get(apiBase + "/users?username=" + encode(path));
            check(users, path);
            JsonNode list = parse(users.body);
            if (list.isArray() && list.size() > 0) {
                JsonNode json = list.get(0);
                CodeHostOrg org = new CodeHostOrg();
                org.setLogin(json.path("username").asText(path));
                org.setName(json.path("name").asText(""));
                org.setDescription(json.path("bio").asText(""));
                org.setHtmlUrl(json.path("web_url").asText(""));
                org.setAvatarUrl(json.path("avatar_url").asText(""));
                org.setUser(true);
                org.setUserId(json.path("id").asText(""));
                org.setLinkLabel(hostLabel());
                return org;
            }
        }
        check(response, path);
        throw new IOException("GitLab API returned " + response.status + " for " + path);
    }

    /** The projects of the group and its subgroups (or owned by the user), in API order. */
    @Override
    public List<CodeHostRepo> listRepos(String path) throws IOException {
        CodeHostOrg org = fetchOrg(path);
        String groupPath = org.getLogin();
        String listPath = org.isUser()
                ? "/users/" + org.getUserId() + "/projects?"
                : "/groups/" + encode(groupPath) + "/projects?include_subgroups=true&";
        List<CodeHostRepo> repos = new ArrayList<>();
        for (int page = 1; ; page++) {
            HttpFetcher.Response response = fetcher.get(apiBase + listPath + "per_page=" + PAGE_SIZE + "&page=" + page);
            check(response, path);
            JsonNode json = parse(response.body);
            if (!json.isArray()) {
                throw new IOException("Unexpected GitLab API response for " + path + " (not a list of projects).");
            }
            json.forEach(node -> repos.add(toRepo(node, org.isUser() ? org.getLogin() : groupPath)));
            LOG.info("GitLab: " + path + " page " + page + " -> " + json.size() + " projects (" + repos.size() + " so far)");
            if (json.size() < PAGE_SIZE) {
                break;
            }
        }
        return repos;
    }

    /** From one element of a projects list; {@code namespacePath} is stripped from the project's path to get its folder path. */
    static CodeHostRepo toRepo(JsonNode json, String namespacePath) {
        CodeHostRepo repo = new CodeHostRepo();
        repo.setName(json.path("path").asText(json.path("name").asText("")));
        String fullPath = json.path("path_with_namespace").asText("");
        repo.setFullName(fullPath);
        String prefix = namespacePath + "/";
        repo.setFolderPath(fullPath.startsWith(prefix) ? fullPath.substring(prefix.length()) : repo.getName());
        repo.setCloneUrl(json.path("http_url_to_repo").asText(""));
        repo.setHtmlUrl(json.path("web_url").asText(""));
        repo.setDescription(json.path("description").asText(""));
        repo.setAvatarUrl(json.path("avatar_url").asText(""));
        repo.setDefaultBranch(json.path("default_branch").asText(""));
        repo.setPushedAt(json.path("last_activity_at").asText(""));
        repo.setFork(json.has("forked_from_project") && !json.path("forked_from_project").isNull());
        repo.setArchived(json.path("archived").asBoolean(false));
        repo.setPrivateRepo("private".equals(json.path("visibility").asText("")));
        return repo;
    }

    private static String encode(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    private static void check(HttpFetcher.Response response, String path) throws IOException {
        if (response.status == 200) {
            return;
        }
        String hint = "";
        if (response.status == 404) {
            hint = " (no such group or user, or it is not visible with the current credentials)";
        } else if (response.status == 401) {
            hint = " (invalid " + GitRepoCloner.ENV_TOKEN + ")";
        } else if (response.status == 403 || response.status == 429) {
            hint = " (rate limit or missing permissions; set " + GitRepoCloner.ENV_TOKEN + " to a token with read_api access)";
        }
        throw new IOException("GitLab API returned " + response.status + " for " + path + hint);
    }

    private static JsonNode parse(String body) throws IOException {
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readTree(body);
    }
}
