package nl.obren.sokrates.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.obren.sokrates.cli.git.CodeHostOrg;
import nl.obren.sokrates.cli.git.CodeHostOrgClient;
import nl.obren.sokrates.cli.git.CodeHostRepo;
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
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * analyzeGitLabGroup against a fake GitLab whose projects are local bare repositories: a group
 * given as a URL, a subgroup project kept in its subgroup folder, the group landscape with metadata
 * from the profile (GitLab link), repos.txt, and -prune reaching into subgroup folders. Offline.
 */
class AnalyzeGitLabGroupCommandTest {

    private static class FakeGitLab implements CodeHostOrgClient {
        CodeHostOrg group;
        List<CodeHostRepo> projects = new ArrayList<>();
        String askedFor;

        @Override
        public CodeHostOrg fetchOrg(String login) throws IOException {
            askedFor = login;
            if (!group.getLogin().equals(login)) throw new IOException("GitLab API returned 404 for " + login);
            return group;
        }

        @Override
        public List<CodeHostRepo> listRepos(String login) {
            return new ArrayList<>(projects);
        }

        @Override
        public String hostLabel() {
            return "GitLab";
        }
    }

    private static CodeHostRepo project(String folderPath, File bare) {
        String name = folderPath.substring(folderPath.lastIndexOf('/') + 1);
        CodeHostRepo repo = new CodeHostRepo(name, bare.toURI().toString());
        repo.setFullName("acme/tools/" + folderPath);
        repo.setFolderPath(folderPath);
        repo.setPushedAt(Instant.now().minus(1, ChronoUnit.DAYS).toString());
        return repo;
    }

    @Test
    void groupLandscapeWithSubgroupProjectsAndPrune(@TempDir Path tmp) throws Exception {
        File alpha = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File beta = bareRepoWithHistory(tmp, "beta", "bob@example.com");
        File root = tmp.resolve("landscapes").toFile();

        FakeGitLab gitLab = new FakeGitLab();
        gitLab.group = new CodeHostOrg("acme/tools", "Acme Tools", "Internal tools", "https://gitlab.example.com/groups/acme/tools", "https://gitlab.example.com/avatar.png");
        gitLab.group.setLinkLabel("GitLab");
        gitLab.projects.add(project("alpha", alpha));
        gitLab.projects.add(project("sub/beta", beta));

        CommandLineInterface cli = new CommandLineInterface();
        cli.setGitLabGroupClient(gitLab);
        cli.run(new String[]{"analyzeGitLabGroup", "-group", "https://gitlab.example.com/acme/tools/", "-analysisRoot", root.getPath()});

        assertEquals("acme/tools", gitLab.askedFor, "a group URL is reduced to its path");
        File group = new File(root, "acme/tools");
        assertTrue(new File(group, "alpha/config.json").exists());
        assertTrue(new File(group, "sub/beta/config.json").exists(), "a subgroup project keeps its subgroup folder");
        assertTrue(new File(group, "sub/beta/reports/index.html").exists());
        assertTrue(new File(group, "_sokrates_landscape/index.html").exists(), "one landscape per group");

        LandscapeConfiguration config = new JsonMapper().getObject(FileUtils.readFileToString(new File(group, "_sokrates_landscape/config.json"), UTF_8),
                new TypeReference<LandscapeConfiguration>() {
                });
        assertEquals("Acme Tools", config.getMetadata().getName());
        assertEquals("Internal tools", config.getMetadata().getDescription());
        assertEquals("https://gitlab.example.com/avatar.png", config.getMetadata().getLogoLink());
        assertEquals("GitLab", config.getMetadata().getLinks().get(0).getLabel());
        assertEquals("https://gitlab.example.com/groups/acme/tools", config.getMetadata().getLinks().get(0).getHref());
        assertEquals(".", config.getAnalysisRoot());
        List<String> reposTxt = FileUtils.readLines(new File(group, "repos.txt"), UTF_8);
        assertTrue(reposTxt.get(2).contains("analyzeGitLabGroup"), reposTxt.get(2));
        assertEquals(List.of(alpha.toURI().toString(), beta.toURI().toString()).size(), reposTxt.stream().filter(l -> !l.startsWith("#")).count());
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(new File(group, "_sokrates_landscape/data/data.zip"))) {
            String repositories = new String(zip.getInputStream(zip.getEntry("repositories.json")).readAllBytes(), UTF_8);
            assertTrue(repositories.contains("\"name\" : \"alpha\"") && repositories.contains("\"name\" : \"beta\""), repositories);
        }

        // The subgroup project disappears from the selection: -prune deletes it inside its subgroup folder.
        gitLab.projects.remove(1);
        cli.run(new String[]{"analyzeGitLabGroup", "-group", "acme/tools", "-analysisRoot", root.getPath(), "-prune", "-dataOnly"});
        assertFalse(new File(group, "sub/beta").exists(), "-prune reaches into subgroup folders");
        assertTrue(new File(group, "alpha/config.json").exists());
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
