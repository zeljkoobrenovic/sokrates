package nl.obren.sokrates.cli;

import nl.obren.sokrates.cli.git.GitRepoCloner;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test of {@code analyzeGitRepo} against a local bare repository (no network): the first
 * run clones and analyzes, the second run fetches the new commit and re-analyzes while keeping the
 * configuration.
 */
class AnalyzeGitRepoCommandTest {

    @Test
    void folderNameIsDerivedFromTheUrl() {
        assertEquals("junit4", GitRepoCloner.folderNameFromUrl("https://github.com/junit-team/junit4"));
        assertEquals("junit4", GitRepoCloner.folderNameFromUrl("https://github.com/junit-team/junit4.git"));
        assertEquals("junit4", GitRepoCloner.folderNameFromUrl("https://github.com/junit-team/junit4.git/"));
        assertEquals("sokrates", GitRepoCloner.folderNameFromUrl("git@github.com:zeljkoobrenovic/sokrates.git"));
        assertEquals("repo", GitRepoCloner.folderNameFromUrl("file:///tmp/repos/repo.git"));
        assertEquals("my_repo", GitRepoCloner.folderNameFromUrl("https://host/x/my repo"));
    }

    @Test
    void clonesAnalyzesAndUpdatesOnRerun(@TempDir Path tmp) throws Exception {
        // A working repository that pushes to a bare "remote" — the URL analyzeGitRepo clones from.
        File work = tmp.resolve("work").toFile();
        File bare = tmp.resolve("remote.git").toFile();
        Git.init().setBare(true).setDirectory(bare).call().close();
        FileUtils.write(new File(work, "src/a.ts"), "export function a(x: number): number { return x + 1; }\n", UTF_8);
        try (Git git = Git.init().setDirectory(work).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("initial").setAuthor("Ada", "ada@example.com").setCommitter("Ada", "ada@example.com").call();
            // The history extractor skips the parentless root commit, so a second commit is needed
            // for git-history.txt to have lines (and the contributors report to exist).
            FileUtils.write(new File(work, "src/a.ts"), "export function a(x: number): number { return x + 2; }\n", UTF_8);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("tweak").setAuthor("Ada", "ada@example.com").setCommitter("Ada", "ada@example.com").call();
            git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(bare.toURI().toString())).call();
            git.push().setRemote("origin").setPushAll().call();
        }
        String url = bare.toURI().toString();
        File clone = tmp.resolve("clones/analyzed").toFile();

        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", url, "-destFolder", clone.getPath()});

        assertTrue(new File(clone, "src/a.ts").exists(), "the repository should be cloned into -destFolder");
        assertTrue(new File(clone, "git-history.txt").exists(), "the clone's history should be extracted");
        File config = new File(clone, "_sokrates/config.json");
        assertTrue(config.exists(), "analyze should create the configuration in the clone");
        File reports = new File(clone, "_sokrates/reports");
        assertTrue(new File(reports, "index.html").exists(), "reports should be generated in the clone");
        assertTrue(FileUtils.readFileToString(new File(reports, "html/Contributors.html"), UTF_8).contains("ada@example.com"));

        // A new commit on the remote + an edited config: the re-run fetches the commit and keeps the config.
        try (Git git = Git.open(work)) {
            FileUtils.write(new File(work, "src/b.ts"), "export function b(): number { return 2; }\n", UTF_8);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("second").setAuthor("Bob", "bob@example.com").setCommitter("Bob", "bob@example.com").call();
            git.push().setRemote("origin").setPushAll().call();
        }
        FileUtils.write(config, FileUtils.readFileToString(config, UTF_8).replace("\"name\" : \"Analyzed\"", "\"name\" : \"kept-name\""), UTF_8);

        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", url, "-destFolder", clone.getPath()});

        assertTrue(new File(clone, "src/b.ts").exists(), "the re-run should fetch the new commit");
        assertTrue(FileUtils.readFileToString(new File(clone, "git-history.txt"), UTF_8).contains("bob@example.com"), "history should include the new commit");
        assertTrue(FileUtils.readFileToString(config, UTF_8).contains("kept-name"), "an existing configuration must be kept");
    }

    @Test
    void defaultDestinationIsNamedAfterTheUrl(@TempDir Path tmp) throws Exception {
        File bare = tmp.resolve("named-after-url.git").toFile();
        File work = tmp.resolve("work").toFile();
        Git.init().setBare(true).setDirectory(bare).call().close();
        FileUtils.write(new File(work, "main.py"), "def main():\n    return 1\n", UTF_8);
        try (Git git = Git.init().setDirectory(work).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("initial").setAuthor("Ada", "ada@example.com").setCommitter("Ada", "ada@example.com").call();
            git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(bare.toURI().toString())).call();
            git.push().setRemote("origin").setPushAll().call();
        }
        // The default destination is relative to the working directory, which tests must not change:
        // resolve it the same way the command does and check it is the URL's repository name.
        assertEquals("named-after-url", GitRepoCloner.folderNameFromUrl(bare.toURI().toString()));
        File clone = tmp.resolve("named-after-url").toFile();
        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", bare.toURI().toString(), "-destFolder", clone.getPath(), "-depth", "1"});
        assertTrue(new File(clone, "_sokrates/reports/index.html").exists(), "a shallow clone should analyze too");
    }
}
