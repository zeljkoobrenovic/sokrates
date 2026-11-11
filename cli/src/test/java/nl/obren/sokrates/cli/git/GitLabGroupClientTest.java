package nl.obren.sokrates.cli.git;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The GitLab API client against canned responses: group + subgroup projects, pagination, the user fallback, self-hosted base URLs. */
class GitLabGroupClientTest {

    private static String projectJson(String pathWithNamespace, boolean fork, boolean archived) {
        String path = pathWithNamespace.substring(pathWithNamespace.lastIndexOf('/') + 1);
        return "{\"id\":1,\"name\":\"" + path.toUpperCase() + "\",\"path\":\"" + path + "\",\"path_with_namespace\":\"" + pathWithNamespace + "\","
                + "\"http_url_to_repo\":\"https://gitlab.example.com/" + pathWithNamespace + ".git\",\"web_url\":\"https://gitlab.example.com/" + pathWithNamespace + "\","
                + "\"description\":\"d\",\"avatar_url\":" + (fork ? "\"https://gitlab.example.com/p.png\"" : "null") + ",\"default_branch\":\"main\",\"last_activity_at\":\"2026-09-30T10:00:00.000Z\",\"archived\":" + archived + ",\"visibility\":\"private\""
                + (fork ? ",\"forked_from_project\":{\"id\":9}" : "") + "}";
    }

    private static String page(String namespace, int from, int count) {
        List<String> items = new ArrayList<>();
        for (int i = from; i < from + count; i++) {
            items.add(projectJson(namespace + "/p" + i, false, false));
        }
        return "[" + String.join(",", items) + "]";
    }

    @Test
    void listsGroupAndSubgroupProjectsWithFolderPathsRelativeToTheGroup() throws IOException {
        List<String> requested = new ArrayList<>();
        GitLabGroupClient client = new GitLabGroupClient(url -> {
            requested.add(url);
            if (url.equals("https://gitlab.example.com/api/v4/groups/acme%2Ftools?with_projects=false")) {
                return new HttpFetcher.Response(200, "{\"id\":7,\"name\":\"Acme Tools\",\"path\":\"tools\",\"full_path\":\"acme/tools\",\"description\":\"Internal tools\",\"web_url\":\"https://gitlab.example.com/groups/acme/tools\",\"avatar_url\":\"https://gitlab.example.com/avatar.png\"}");
            }
            if (url.equals("https://gitlab.example.com/api/v4/groups/acme%2Ftools/projects?include_subgroups=true&per_page=100&page=1")) {
                return new HttpFetcher.Response(200, "[" + projectJson("acme/tools/alpha", false, false) + "," + projectJson("acme/tools/sub/beta", true, false) + "," + projectJson("acme/tools/old", false, true) + "]");
            }
            return new HttpFetcher.Response(500, "unexpected " + url);
        }, "https://gitlab.example.com/");

        CodeHostOrg org = client.fetchOrg("acme/tools");
        assertEquals("acme/tools", org.getLogin());
        assertEquals("Acme Tools", org.displayName());
        assertEquals("Internal tools", org.getDescription());
        assertEquals("https://gitlab.example.com/groups/acme/tools", org.getHtmlUrl());
        assertEquals("GitLab", org.getLinkLabel());
        assertFalse(org.isUser());

        List<CodeHostRepo> repos = client.listRepos("acme/tools");
        assertEquals(3, repos.size());
        CodeHostRepo alpha = repos.get(0);
        assertEquals("alpha", alpha.getName());
        assertEquals("acme/tools/alpha", alpha.getFullName());
        assertEquals("alpha", alpha.getFolderPath());
        assertEquals("https://gitlab.example.com/acme/tools/alpha.git", alpha.getCloneUrl());
        assertEquals("2026-09-30T10:00:00.000Z", alpha.getPushedAt());
        assertTrue(alpha.isPrivateRepo());
        assertFalse(alpha.isFork());
        CodeHostRepo beta = repos.get(1);
        assertEquals("sub/beta", beta.getFolderPath(), "a subgroup project keeps its subgroup folder");
        assertTrue(beta.isFork(), "forked_from_project marks a fork");
        assertEquals("https://gitlab.example.com/p.png", beta.getAvatarUrl());
        assertEquals("", alpha.getAvatarUrl(), "a null avatar_url is blank (the command falls back to the group's)");
        assertEquals("d", alpha.getDescription());
        assertTrue(repos.get(2).isArchived());
    }

    @Test
    void paginatesAndFallsBackToAUser() throws IOException {
        Map<String, String> responses = Map.of(
                "https://gitlab.com/api/v4/users?username=jane", "[{\"id\":42,\"username\":\"jane\",\"name\":\"Jane Doe\",\"bio\":\"Builds things\",\"web_url\":\"https://gitlab.com/jane\",\"avatar_url\":\"https://gitlab.com/a.png\"}]",
                "https://gitlab.com/api/v4/users/42/projects?per_page=100&page=1", page("jane", 1, 100),
                "https://gitlab.com/api/v4/users/42/projects?per_page=100&page=2", page("jane", 101, 2));
        GitLabGroupClient client = new GitLabGroupClient(url -> responses.containsKey(url)
                ? new HttpFetcher.Response(200, responses.get(url))
                : new HttpFetcher.Response(404, "{\"message\":\"404 Group Not Found\"}"), GitLabGroupClient.DEFAULT_BASE_URL);

        CodeHostOrg user = client.fetchOrg("jane");
        assertTrue(user.isUser());
        assertEquals("42", user.getUserId());
        assertEquals("Jane Doe", user.displayName());
        assertEquals("Builds things", user.getDescription());

        List<CodeHostRepo> repos = client.listRepos("jane");
        assertEquals(102, repos.size());
        assertEquals("p101", repos.get(100).getFolderPath());

        IOException missing = assertThrows(IOException.class, () -> client.fetchOrg("no/such"));
        assertTrue(missing.getMessage().contains("404"), missing.getMessage());
        GitLabGroupClient forbidden = new GitLabGroupClient(url -> new HttpFetcher.Response(403, "{}"), "https://gitlab.com");
        IOException e = assertThrows(IOException.class, () -> forbidden.fetchOrg("acme"));
        assertTrue(e.getMessage().contains(GitRepoCloner.ENV_TOKEN), e.getMessage());
    }
}
