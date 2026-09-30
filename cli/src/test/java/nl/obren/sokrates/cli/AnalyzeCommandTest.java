package nl.obren.sokrates.cli;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test of the one-shot {@code analyze} command: from a bare git repository with source
 * files it must extract the history, create the configuration and generate the reports, and on a
 * second run it must keep the (possibly edited) configuration.
 */
class AnalyzeCommandTest {

    @Test
    void analyzeRunsHistoryExtractionInitAndReportsInOneGo(@TempDir Path tmp) throws Exception {
        File repo = tmp.resolve("repo").toFile();
        File src = new File(repo, "src");
        FileUtils.write(new File(src, "a.ts"), "export function a(x: number): number { return x + 1; }\n", UTF_8);
        FileUtils.write(new File(src, "b.ts"), "export function b(y: string): string { return y.trim(); }\n", UTF_8);
        try (Git git = Git.init().setDirectory(repo).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("initial").setAuthor("Ada", "ada@example.com").setCommitter("Ada", "ada@example.com").call();
            FileUtils.write(new File(src, "a.ts"), "export function a(x: number): number { return x + 2; }\n", UTF_8);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("tweak").setAuthor("Ada", "ada@example.com").setCommitter("Ada", "ada@example.com").call();
        }

        new CommandLineInterface().run(new String[]{"analyze", "-srcRoot", repo.getPath()});

        File history = new File(repo, "git-history.txt");
        assertTrue(history.exists(), "analyze should extract git-history.txt");
        String historyText = FileUtils.readFileToString(history, UTF_8);
        assertTrue(historyText.contains("ada@example.com"), "history should hold the commit author: " + historyText);
        assertTrue(historyText.contains("src/a.ts"), "history should hold the changed file: " + historyText);

        File config = new File(repo, "_sokrates/config.json");
        assertTrue(config.exists(), "analyze should create " + config);
        File reports = new File(repo, "_sokrates/reports");
        assertTrue(new File(reports, "index.html").exists(), "analyze should generate reports");
        assertTrue(new File(reports, "html/Contributors.html").exists(), "analyze should generate the contributors report");
        assertTrue(FileUtils.readFileToString(new File(reports, "html/Contributors.html"), UTF_8).contains("ada@example.com"),
                "contributors report should be fed by the extracted history");

        // A second run keeps the configuration (so user edits survive) and skips extraction when asked.
        String edited = FileUtils.readFileToString(config, UTF_8).replace("\"name\" : \"Repo\"", "\"name\" : \"edited-name\"");
        assertNotEquals(FileUtils.readFileToString(config, UTF_8), edited, "the fixture config should carry the folder name");
        FileUtils.write(config, edited, UTF_8);
        assertTrue(history.delete());

        new CommandLineInterface().run(new String[]{"analyze", "-srcRoot", repo.getPath(), "-skipGitHistory"});

        assertFalse(history.exists(), "-skipGitHistory must not re-extract the history");
        assertTrue(FileUtils.readFileToString(config, UTF_8).contains("edited-name"), "an existing config must be kept");
        assertTrue(new File(reports, "index.html").exists());
    }

    @Test
    void analyzeWithoutGitStillProducesReports(@TempDir Path tmp) throws Exception {
        File root = tmp.resolve("plain").toFile();
        FileUtils.write(new File(root, "src/main.py"), "def main():\n    return 1\n", UTF_8);

        new CommandLineInterface().run(new String[]{"analyze", "-srcRoot", root.getPath()});

        assertFalse(new File(root, "git-history.txt").exists(), "no history without a .git folder");
        assertTrue(new File(root, "_sokrates/config.json").exists());
        assertTrue(new File(root, "_sokrates/reports/index.html").exists());
    }
}
