package nl.obren.sokrates.cli;

import nl.obren.sokrates.cli.git.GitRepoCloner;
import nl.obren.sokrates.cli.git.GitRepoMetadata;
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
    void clonesAnalyzesKeepsOnlyTheAnalysisAndReusesTheConfigOnRerun(@TempDir Path tmp) throws Exception {
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
        File output = tmp.resolve("analyses/remote").toFile();

        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", url, "-destFolder", output.getPath()});

        File config = new File(output, "config.json");
        assertTrue(config.exists(), "the configuration should be kept directly in -destFolder (no _sokrates subfolder)");
        File reports = new File(output, "reports");
        assertTrue(new File(reports, "index.html").exists(), "reports should be kept in <destFolder>/reports");
        assertTrue(FileUtils.readFileToString(new File(reports, "html/Contributors.html"), UTF_8).contains("ada@example.com"));
        assertFalse(new File(output, "src").exists(), "the source code must not be kept");
        assertFalse(new File(output, ".git").exists(), "the clone must not be kept");
        assertFalse(new File(output, "git-history.txt").exists(), "only the analysis is kept");
        assertFalse(new File(output, "_sokrates").exists(), "no _sokrates subfolder in the output");

        // The report is named after the git remote (remote.git), not any folder.
        String configText = FileUtils.readFileToString(config, UTF_8);
        assertTrue(configText.contains("\"name\" : \"remote\""), "the name should come from the origin URL");
        assertFalse(configText.contains("\"href\" : \"https://"), "a file:// remote has no browsable link");

        // A new commit on the remote + an edited kept config: the re-run clones again, reuses the config.
        try (Git git = Git.open(work)) {
            FileUtils.write(new File(work, "src/b.ts"), "export function b(): number { return 2; }\n", UTF_8);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("second").setAuthor("Bob", "bob@example.com").setCommitter("Bob", "bob@example.com").call();
            git.push().setRemote("origin").setPushAll().call();
        }
        FileUtils.write(config, configText.replace("\"name\" : \"remote\"", "\"name\" : \"kept-name\""), UTF_8);

        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", url, "-destFolder", output.getPath()});

        assertTrue(FileUtils.readFileToString(new File(reports, "html/Contributors.html"), UTF_8).contains("bob@example.com"), "the re-run should analyze the new commit");
        assertTrue(FileUtils.readFileToString(config, UTF_8).contains("kept-name"), "the kept configuration must be reused");
        assertTrue(FileUtils.readFileToString(new File(reports, "html/index.html"), UTF_8).contains("kept-name"), "the reports should use the kept configuration");
        assertFalse(new File(output, "src").exists());
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
        // The default output folder is relative to the working directory, which tests must not change:
        // resolve it the same way the command does (owner/name for hosted repos, name for local ones).
        assertEquals("named-after-url", GitRepoMetadata.fromUrl(bare.toURI().toString()).outputFolderName());
        assertEquals("junit-team/junit4", GitRepoMetadata.fromUrl("https://github.com/junit-team/junit4.git").outputFolderName());
        File output = tmp.resolve("named-after-url").toFile();
        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", bare.toURI().toString(), "-destFolder", output.getPath(), "-depth", "1"});
        assertTrue(new File(output, "reports/index.html").exists(), "a shallow clone should analyze too");
        assertTrue(new File(output, "config.json").exists());
    }
}
