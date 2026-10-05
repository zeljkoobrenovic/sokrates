package nl.obren.sokrates.cli;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End to end: the AI scanner findings in a repository's reports/ai-insights/ get their page, index tab
 * and sidebar items in the repository report - also when a -postAnalysis "agent" writes them after the
 * reports were generated (the reports are generated again), and on a re-run that skips the agent (the
 * kept findings travel into the fresh clone instead of being replaced with the reports folder).
 */
class RepositoryAiInsightsTest {
    private static final String FINDINGS = "{\\\"scanner\\\":\\\"tech-stack-scan\\\",\\\"analyzed_at\\\":\\\"2026-10-01T00:00:00Z\\\",\\\"findings\\\":["
            + "{\\\"id\\\":\\\"tech-stack-scan/langs/ts\\\",\\\"group\\\":\\\"langs\\\",\\\"title\\\":\\\"TypeScript only\\\",\\\"severity\\\":\\\"low\\\",\\\"confidence\\\":\\\"certain\\\"}]}";
    private static final String HOOK = "mkdir -p \"$SOKRATES_REPORTS_FOLDER/ai-insights\" && printf '%s' \"" + FINDINGS + "\" > \"$SOKRATES_REPORTS_FOLDER/ai-insights/tech-stack-scan.json\"";

    @Test
    void findingsWrittenByTheAgentAreLinkedFromTheRepositoryReport(@TempDir Path tmp) throws Exception {
        File bare = bareRepoWithHistory(tmp, "alpha");
        File output = tmp.resolve("out/alpha").toFile();

        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", bare.toURI().toString(), "-destFolder", output.getPath(),
                "-postAnalysis", HOOK});
        assertLinked(output);

        // Same head: the agent is skipped, and the report still links the findings of the earlier run.
        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", bare.toURI().toString(), "-destFolder", output.getPath(),
                "-postAnalysis", HOOK});
        assertTrue(new File(output, "reports/ai-insights/tech-stack-scan.json").exists(), "the kept findings survive a skipped agent");
        assertLinked(output);

        // Without findings: no page, no sidebar group.
        File plain = tmp.resolve("out/plain").toFile();
        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", bareRepoWithHistory(tmp, "beta").toURI().toString(),
                "-destFolder", plain.getPath()});
        assertFalse(new File(plain, "reports/explorers/ai-insights.html").exists());
        assertFalse(FileUtils.readFileToString(new File(plain, "reports/html/index.html"), UTF_8).contains("AI Insights"));
    }

    private static void assertLinked(File output) throws Exception {
        File page = new File(output, "reports/explorers/ai-insights.html");
        assertTrue(page.exists(), "the AI Insights page");
        assertTrue(new File(output, "reports/explorers/ai-insights-icons/tech-stack-scan.png").exists(), "the scanner's icon");
        String index = FileUtils.readFileToString(new File(output, "reports/html/index.html"), UTF_8);
        assertTrue(index.contains("data-sk-nav='ai-insights/tech-stack-scan'"), "a sidebar item per scanner");
        assertTrue(index.contains("data-sk-frame-src='../explorers/ai-insights.html?view=tech-stack-scan'"));
        assertTrue(index.indexOf(">AI Insights<") < index.indexOf(">Index<"), "the AI Insights group comes before Index");
        assertTrue(index.contains("data-sk-src='../explorers/ai-insights.html?view=overview'"), "the tab's frame");
        // The analysis report pages carry the same sidebar.
        assertTrue(FileUtils.readFileToString(new File(output, "reports/html/FileSize.html"), UTF_8).contains("data-sk-nav='ai-insights/overview'"));
    }

    private static File bareRepoWithHistory(Path tmp, String name) throws Exception {
        File work = tmp.resolve("work-" + name).toFile();
        File bare = tmp.resolve(name + ".git").toFile();
        Git.init().setBare(true).setDirectory(bare).call().close();
        File source = new File(work, "src/" + name + ".ts");
        FileUtils.write(source, "export function " + name + "(x: number): number { return x + 1; }\n", UTF_8);
        try (Git git = Git.init().setDirectory(work).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("initial").setAuthor(name, name + "@example.com").setCommitter(name, name + "@example.com").call();
            FileUtils.write(source, "export function " + name + "(x: number): number { return x + 2; }\n", UTF_8);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("tweak").setAuthor(name, name + "@example.com").setCommitter(name, name + "@example.com").call();
            git.remoteAdd().setName("origin").setUri(new URIish(bare.toURI().toString())).call();
            git.push().setRemote("origin").setPushAll().call();
        }
        return bare;
    }
}
