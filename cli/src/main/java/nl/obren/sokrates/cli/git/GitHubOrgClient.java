package nl.obren.sokrates.cli.git;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The GitHub REST API calls behind analyzeGitHubOrg: the organization's profile and the list of its
 * repositories (paginated, 100 per page). A login that is not an organization falls back to the
 * user endpoints, so personal accounts work too. {@link GitRepoCloner#ENV_TOKEN} is sent as a
 * bearer token when set — required for private repositories and for the higher rate limit (5000
 * requests/hour instead of 60). The HTTP layer is an injectable {@link HttpFetcher} so the parsing
 * and pagination are unit-tested offline.
 */
public class GitHubOrgClient {
    public static final String DEFAULT_API_BASE = "https://api.github.com";
    public static final int PAGE_SIZE = 100;

    private static final Log LOG = LogFactory.getLog(GitHubOrgClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    /** One GET: the status code and body of the response. */
    public interface HttpFetcher {
        Response get(String url) throws IOException;
    }

    public static class Response {
        public final int status;
        public final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private final HttpFetcher fetcher;
    private final String apiBase;

    public GitHubOrgClient() {
        this(defaultFetcher(), DEFAULT_API_BASE);
    }

    public GitHubOrgClient(HttpFetcher fetcher) {
        this(fetcher, DEFAULT_API_BASE);
    }

    public GitHubOrgClient(HttpFetcher fetcher, String apiBase) {
        this.fetcher = fetcher;
        this.apiBase = StringUtils.stripEnd(apiBase, "/");
    }

    /** The organization's profile; a personal account when {@code login} is not an organization. */
    public GitHubOrg fetchOrg(String login) throws IOException {
        Response response = fetcher.get(apiBase + "/orgs/" + login);
        boolean user = false;
        if (response.status == 404) {
            response = fetcher.get(apiBase + "/users/" + login);
            user = true;
        }
        check(response, login);
        GitHubOrg org = GitHubOrg.fromJson(parse(response.body), user);
        if (StringUtils.isBlank(org.getLogin())) {
            org.setLogin(login);
        }
        return org;
    }

    /** Every repository of the organization (or user) the token can see, in API order. */
    public List<GitHubRepo> listRepos(String login) throws IOException {
        List<GitHubRepo> repos = new ArrayList<>();
        String path = "/orgs/" + login + "/repos?type=all";
        for (int page = 1; ; page++) {
            Response response = fetcher.get(apiBase + path + "&per_page=" + PAGE_SIZE + "&page=" + page);
            if (response.status == 404 && page == 1 && path.startsWith("/orgs/")) {
                path = "/users/" + login + "/repos?type=owner";
                response = fetcher.get(apiBase + path + "&per_page=" + PAGE_SIZE + "&page=" + page);
            }
            check(response, login);
            JsonNode json = parse(response.body);
            if (!json.isArray()) {
                throw new IOException("Unexpected GitHub API response for " + login + " (not a list of repositories).");
            }
            json.forEach(node -> repos.add(GitHubRepo.fromJson(node)));
            LOG.info("GitHub: " + login + " page " + page + " -> " + json.size() + " repositories (" + repos.size() + " so far)");
            if (json.size() < PAGE_SIZE) {
                break;
            }
        }
        return repos;
    }

    private static void check(Response response, String login) throws IOException {
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

    private static HttpFetcher defaultFetcher() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build();
        return url -> {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "sokrates");
            String token = System.getenv(GitRepoCloner.ENV_TOKEN);
            if (StringUtils.isNotBlank(token)) {
                request.header("Authorization", "Bearer " + token);
            }
            try {
                HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
                return new Response(response.statusCode(), response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while calling " + url, e);
            }
        };
    }
}
