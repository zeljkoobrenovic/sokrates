package nl.obren.sokrates.cli.git;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The GitHub API client against canned responses: pagination, the user fallback and errors. */
class GitHubOrgClientTest {

    private static String repoJson(int i, boolean fork) {
        return "{\"name\":\"repo" + i + "\",\"full_name\":\"acme/repo" + i + "\",\"clone_url\":\"https://github.com/acme/repo" + i + ".git\","
                + "\"html_url\":\"https://github.com/acme/repo" + i + "\",\"fork\":" + fork + ",\"archived\":false,\"private\":false,"
                + "\"pushed_at\":\"2026-09-0" + (i % 9 + 1) + "T00:00:00Z\",\"size\":" + (i * 10) + ",\"language\":\"Java\",\"default_branch\":\"main\"}";
    }

    private static String page(int from, int count) {
        List<String> items = new ArrayList<>();
        for (int i = from; i < from + count; i++) {
            items.add(repoJson(i, i == from));
        }
        return "[" + String.join(",", items) + "]";
    }

    @Test
    void listsAllPagesOfAnOrganization() throws IOException {
        List<String> requested = new ArrayList<>();
        GitHubOrgClient client = new GitHubOrgClient(url -> {
            requested.add(url);
            if (url.endsWith("page=1")) return new HttpFetcher.Response(200, page(1, 100));
            if (url.endsWith("page=2")) return new HttpFetcher.Response(200, page(101, 3));
            return new HttpFetcher.Response(500, "unexpected");
        });

        List<CodeHostRepo> repos = client.listRepos("acme");

        assertEquals(103, repos.size());
        assertEquals(List.of("https://api.github.com/orgs/acme/repos?type=all&per_page=100&page=1",
                "https://api.github.com/orgs/acme/repos?type=all&per_page=100&page=2"), requested);
        CodeHostRepo first = repos.get(0);
        assertEquals("repo1", first.getName());
        assertEquals("acme/repo1", first.getFullName());
        assertEquals("https://github.com/acme/repo1.git", first.getCloneUrl());
        assertTrue(first.isFork());
        assertEquals("2026-09-02T00:00:00Z", first.getPushedAt());
        assertEquals("Java", first.getLanguage());
        assertFalse(repos.get(1).isFork());
    }

    @Test
    void fallsBackToTheUserEndpointsForAPersonalAccount() throws IOException {
        Map<String, String> responses = Map.of(
                "https://api.github.com/users/jane", "{\"login\":\"jane\",\"name\":\"Jane Doe\",\"bio\":\"Builds things\",\"html_url\":\"https://github.com/jane\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/1\"}",
                "https://api.github.com/users/jane/repos?type=owner&per_page=100&page=1", page(1, 2));
        GitHubOrgClient client = new GitHubOrgClient(url -> responses.containsKey(url)
                ? new HttpFetcher.Response(200, responses.get(url))
                : new HttpFetcher.Response(404, "{\"message\":\"Not Found\"}"));

        CodeHostOrg org = client.fetchOrg("jane");
        assertTrue(org.isUser());
        assertEquals("Jane Doe", org.displayName());
        assertEquals("Builds things", org.getDescription(), "a user's bio stands in for the description");
        assertEquals("https://github.com/jane", org.getHtmlUrl());
        assertEquals(2, client.listRepos("jane").size());
    }

    @Test
    void organizationProfileAndErrorsAreReported() throws IOException {
        GitHubOrgClient client = new GitHubOrgClient(url -> url.endsWith("/orgs/acme")
                ? new HttpFetcher.Response(200, "{\"login\":\"acme\",\"name\":\"Acme Corp\",\"description\":\"We make things\",\"html_url\":\"https://github.com/acme\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/2\"}")
                : new HttpFetcher.Response(403, "{\"message\":\"API rate limit exceeded\"}"));

        CodeHostOrg org = client.fetchOrg("acme");
        assertFalse(org.isUser());
        assertEquals("Acme Corp", org.displayName());
        assertEquals("We make things", org.getDescription());

        IOException e = assertThrows(IOException.class, () -> client.listRepos("acme"));
        assertTrue(e.getMessage().contains("403"), e.getMessage());
        assertTrue(e.getMessage().contains(GitRepoCloner.ENV_TOKEN), e.getMessage());

        GitHubOrgClient missing = new GitHubOrgClient(url -> new HttpFetcher.Response(404, "{}"));
        IOException notFound = assertThrows(IOException.class, () -> missing.fetchOrg("nobody"));
        assertTrue(notFound.getMessage().contains("404"), notFound.getMessage());
    }
}
