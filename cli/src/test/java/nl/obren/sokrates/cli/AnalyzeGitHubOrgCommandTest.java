package nl.obren.sokrates.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.obren.sokrates.cli.git.GitHubOrg;
import nl.obren.sokrates.cli.git.GitHubOrgClient;
import nl.obren.sokrates.cli.git.GitHubRepo;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * analyzeGitHubOrg against a fake GitHub API whose repositories are local bare repositories:
 * selection (forks/archived dropped), the per-organization folder + landscape with metadata from
 * the profile, repos.txt, -listOnly, -prune, user edits surviving re-runs, and the parent landscape
 * once there are two organizations. Offline.
 */
class AnalyzeGitHubOrgCommandTest {

    /** A canned GitHub: login -> profile, login -> repositories. */
    private static class FakeGitHub extends GitHubOrgClient {
        final Map<String, GitHubOrg> orgs = new LinkedHashMap<>();
        final Map<String, List<GitHubRepo>> repos = new LinkedHashMap<>();

        @Override
        public GitHubOrg fetchOrg(String login) throws IOException {
            if (!orgs.containsKey(login)) throw new IOException("GitHub API returned 404 for " + login);
            return orgs.get(login);
        }

        @Override
        public List<GitHubRepo> listRepos(String login) {
            return new ArrayList<>(repos.getOrDefault(login, List.of()));
        }
    }

    private static GitHubRepo repo(String org, String name, File bare, int daysAgo, boolean fork, boolean archived) {
        GitHubRepo repo = new GitHubRepo(name, bare.toURI().toString());
        repo.setFullName(org + "/" + name);
        repo.setHtmlUrl("https://github.com/" + org + "/" + name);
        repo.setPushedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS).toString());
        repo.setFork(fork);
        repo.setArchived(archived);
        return repo;
    }

    private static LandscapeConfiguration landscapeConfig(File orgRoot) throws IOException {
        return new JsonMapper().getObject(FileUtils.readFileToString(new File(orgRoot, "_sokrates_landscape/config.json"), UTF_8),
                new TypeReference<LandscapeConfiguration>() {
                });
    }

    @Test
    void oneLandscapePerOrganizationWithMetadataFromGitHub(@TempDir Path tmp) throws Exception {
        File alpha = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File beta = bareRepoWithHistory(tmp, "beta", "bob@example.com");
        File gamma = bareRepoWithHistory(tmp, "gamma", "cy@example.com");
        File delta = bareRepoWithHistory(tmp, "delta", "dee@example.com");
        File root = tmp.resolve("landscapes").toFile();

        FakeGitHub gitHub = new FakeGitHub();
        gitHub.orgs.put("acme", new GitHubOrg("acme", "Acme Corp", "We make things", "https://github.com/acme", "https://avatars.example.com/acme.png"));
        gitHub.repos.put("acme", List.of(
                repo("acme", "alpha", alpha, 1, false, false),
                repo("acme", "beta", beta, 2, true, false),      // fork -> skipped
                repo("acme", "gamma", gamma, 3, false, true)));  // archived -> skipped

        CommandLineInterface cli = new CommandLineInterface();
        cli.setGitHubOrgClient(gitHub);
        cli.run(new String[]{"analyzeGitHubOrg", "-org", "acme", "-analysisRoot", root.getPath()});

        File acme = new File(root, "acme");
        assertTrue(new File(acme, "alpha/config.json").exists(), "the selected repository is analyzed into <root>/<org>/<repository>");
        assertTrue(new File(acme, "alpha/reports/index.html").exists());
        assertFalse(new File(acme, "beta").exists(), "forks are skipped");
        assertFalse(new File(acme, "gamma").exists(), "archived repositories are skipped");
        assertFalse(new File(acme, "alpha/src").exists(), "no source is kept");

        List<String> reposTxt = FileUtils.readLines(new File(acme, "repos.txt"), UTF_8);
        assertTrue(reposTxt.get(0).startsWith("# Acme Corp (https://github.com/acme): 1 of 3 repositories selected on "), reposTxt.get(0));
        assertEquals(List.of(alpha.toURI().toString()), reposTxt.stream().filter(l -> !l.startsWith("#")).toList());

        assertTrue(new File(acme, "_sokrates_landscape/index.html").exists(), "a landscape per organization");
        LandscapeConfiguration config = landscapeConfig(acme);
        assertEquals("Acme Corp", config.getMetadata().getName());
        assertEquals("We make things", config.getMetadata().getDescription());
        assertEquals("https://avatars.example.com/acme.png", config.getMetadata().getLogoLink());
        assertEquals(1, config.getMetadata().getLinks().size());
        assertEquals("https://github.com/acme", config.getMetadata().getLinks().get(0).getHref());
        assertFalse(new File(root, "_sokrates_landscape").exists(), "no parent landscape for a single organization");

        // Re-run with a second organization: the user's name edit survives, a repository that is no
        // longer selected is pruned, and a parent landscape appears over the two organizations.
        config.getMetadata().setName("My Acme");
        FileUtils.write(new File(acme, "_sokrates_landscape/config.json"), new nl.obren.sokrates.common.io.JsonGenerator().generate(config), UTF_8);
        gitHub.repos.put("acme", List.of(repo("acme", "delta", delta, 1, false, false)));
        gitHub.orgs.put("globex", new GitHubOrg("globex", "", "", "https://github.com/globex", ""));
        gitHub.repos.put("globex", List.of(repo("globex", "beta", beta, 1, false, false)));

        cli.run(new String[]{"analyzeGitHubOrg", "-org", "acme", "-org", "globex", "-org", "missing", "-analysisRoot", root.getPath(), "-prune",
                "-setName", "All orgs"});

        assertTrue(new File(acme, "delta/config.json").exists());
        assertFalse(new File(acme, "alpha").exists(), "-prune deletes the analysis of a repository no longer selected");
        assertEquals("My Acme", landscapeConfig(acme).getMetadata().getName(), "a user-set name is kept");
        File globex = new File(root, "globex");
        assertTrue(new File(globex, "beta/config.json").exists());
        assertEquals("globex", landscapeConfig(globex).getMetadata().getName(), "the login names an organization without a display name");
        assertFalse(new File(root, "missing").exists(), "an organization the API does not know is skipped");

        File parentIndex = new File(root, "_sokrates_landscape/index.html");
        assertTrue(parentIndex.exists(), "two organizations -> a parent landscape");
        String parent = FileUtils.readFileToString(parentIndex, UTF_8);
        // Folder sub-landscapes are labelled by their folder (the login), as everywhere else.
        assertTrue(parent.contains("acme/_sokrates_landscape/index.html") && parent.contains("globex/_sokrates_landscape/index.html"),
                "the parent links both organization landscapes");
        assertEquals("All orgs", landscapeConfig(root).getMetadata().getName(), "-setName names the parent");
    }

    @Test
    void listOnlyWritesTheSelectionWithoutAnalyzing(@TempDir Path tmp) throws Exception {
        File root = tmp.resolve("landscapes").toFile();
        FakeGitHub gitHub = new FakeGitHub();
        gitHub.orgs.put("acme", new GitHubOrg("acme", "Acme", "", "https://github.com/acme", ""));
        gitHub.repos.put("acme", List.of(
                repo("acme", "fresh", new File(tmp.toFile(), "fresh.git"), 2, false, false),
                repo("acme", "fresher", new File(tmp.toFile(), "fresher.git"), 1, false, false),
                repo("acme", "stale", new File(tmp.toFile(), "stale.git"), 400, false, false)));

        CommandLineInterface cli = new CommandLineInterface();
        cli.setGitHubOrgClient(gitHub);
        cli.run(new String[]{"analyzeGitHubOrg", "-org", "https://github.com/acme/", "-analysisRoot", root.getPath(), "-listOnly",
                "-pushedWithinDays", "30", "-maxRepos", "1"});

        File acme = new File(root, "acme");
        String[] names = acme.list() == null ? new String[0] : acme.list();
        java.util.Arrays.sort(names);
        assertEquals("[repos.txt]", java.util.Arrays.toString(names), "nothing but the list is written");
        List<String> lines = FileUtils.readLines(new File(acme, "repos.txt"), UTF_8);
        assertTrue(lines.get(1).contains("pushed within 30 days") && lines.get(1).contains("at most 1 repositories"), lines.get(1));
        assertEquals(List.of(new File(tmp.toFile(), "fresher.git").toURI().toString()), lines.stream().filter(l -> !l.startsWith("#")).toList(),
                "the most recently pushed repository within the window");
    }

    @Test
    void relativeAnalysisRootFindsTheRepositories(@TempDir Path tmp) throws Exception {
        // The default -analysisRoot is "." so the organization root is a relative "./<org>"; the
        // landscape must still find its repositories (its stored analysisRoot is normalized to ".").
        File alpha = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File root = new File("target/analyze-github-org-relative-" + System.nanoTime());
        try {
            FakeGitHub gitHub = new FakeGitHub();
            gitHub.orgs.put("acme", new GitHubOrg("acme", "Acme", "", "https://github.com/acme", ""));
            gitHub.repos.put("acme", List.of(repo("acme", "alpha", alpha, 1, false, false)));
            CommandLineInterface cli = new CommandLineInterface();
            cli.setGitHubOrgClient(gitHub);

            cli.run(new String[]{"analyzeGitHubOrg", "-org", "acme", "-analysisRoot", root.getPath(), "-dataOnly"});
            cli.run(new String[]{"analyzeGitHubOrg", "-org", "acme", "-analysisRoot", root.getPath(), "-dataOnly"}); // re-run: the landscape's own data.zip exists now

            File acme = new File(root, "acme");
            assertEquals(".", landscapeConfig(acme).getAnalysisRoot());
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(new File(acme, "_sokrates_landscape/data/data.zip"))) {
                String repositories = new String(zip.getInputStream(zip.getEntry("repositories.json")).readAllBytes(), UTF_8);
                assertTrue(repositories.contains("\"name\" : \"alpha\""), "the landscape lists the repository (local origin -> plain name): " + repositories);
            }
            String info = FileUtils.readFileToString(new File(acme, "_sokrates_landscape/info.json"), UTF_8);
            assertFalse(info.contains("_sokrates_landscape/data"), "the landscape's own data.zip is not registered as a repository");
        } finally {
            FileUtils.deleteDirectory(root);
        }
    }

    private static File bareRepoWithHistory(Path tmp, String name, String email) throws Exception {
        File work = tmp.resolve("work-" + name).toFile();
        File bare = tmp.resolve(name + ".git").toFile();
        Git.init().setBare(true).setDirectory(bare).call().close();
        File source = new File(work, "src/" + name + ".ts");
        FileUtils.write(source, "export function " + name + "(x: number): number { return x + 1; }\n", UTF_8);
        try (Git git = Git.init().setDirectory(work).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("initial").setAuthor(name, email).setCommitter(name, email).call();
            FileUtils.write(source, "export function " + name + "(x: number): number { return x + 2; }\n", UTF_8);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("tweak").setAuthor(name, email).setCommitter(name, email).call();
            git.remoteAdd().setName("origin").setUri(new URIish(bare.toURI().toString())).call();
            git.push().setRemote("origin").setPushAll().call();
        }
        return bare;
    }
}
