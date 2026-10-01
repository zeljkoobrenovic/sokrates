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
 * End to end: a -postAnalysis "agent" writes a findings file into the clone's
 * _sokrates/reports/ai-insights/, it travels with the kept analysis, and the landscape built over
 * the repositories gets an AI Insights tab, page and data entry — and none of them without findings.
 */
class LandscapeAiInsightsTest {
    private static final String FINDINGS = "{\\\"scanner\\\":\\\"tech-stack-scan\\\",\\\"analyzed_at\\\":\\\"2026-10-01T00:00:00Z\\\",\\\"findings\\\":["
            + "{\\\"id\\\":\\\"tech-stack-scan/langs/ts\\\",\\\"group\\\":\\\"langs\\\",\\\"title\\\":\\\"TypeScript only\\\",\\\"severity\\\":\\\"low\\\",\\\"confidence\\\":\\\"certain\\\"}]}";
    private static final String HOOK = "mkdir -p \"$SOKRATES_REPORTS_FOLDER/ai-insights\" && printf '%s' \"" + FINDINGS + "\" > \"$SOKRATES_REPORTS_FOLDER/ai-insights/tech-stack-scan.json\"";

    @Test
    void findingsOfTheRepositoriesShowUpAsALandscapeTab(@TempDir Path tmp) throws Exception {
        File alpha = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File beta = bareRepoWithHistory(tmp, "beta", "bob@example.com");
        File root = tmp.resolve("landscape").toFile();

        // Only alpha gets the "agent" (the hook is bounded to one repository).
        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", root.getPath(), "-url", alpha.toURI().toString(), "-url", beta.toURI().toString(),
                "-postAnalysis", HOOK, "-aiMaxRepos", "1"});

        assertTrue(new File(root, "alpha/reports/ai-insights/tech-stack-scan.json").exists(), "the findings travel with the kept analysis");
        assertFalse(new File(root, "beta/reports/ai-insights").exists());
        File landscape = new File(root, "_sokrates_landscape");
        String index = FileUtils.readFileToString(new File(landscape, "index.html"), UTF_8);
        assertTrue(index.contains("AI Insights (1)"), "the tab counts the findings");
        assertTrue(new File(landscape, "ai-insights.html").exists(), "the client-rendered page");
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(new File(landscape, "data/data.zip"))) {
            String json = new String(zip.getInputStream(zip.getEntry("ai-insights.json")).readAllBytes(), UTF_8);
            assertTrue(json.contains("\"title\" : \"TypeScript only\""), json);
            assertTrue(json.contains("\"url\" : \"../alpha/reports/ai-insights/index.html#tech-stack-scan%2Flangs%2Fts\""), json);
            assertTrue(json.contains("\"name\" : \"alpha\""));
            assertFalse(json.contains("\"name\" : \"beta\""), "repositories without findings are not listed");
        }

        // A landscape without any findings has no tab and no page.
        File plain = tmp.resolve("plain").toFile();
        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", plain.getPath(), "-url", beta.toURI().toString()});
        assertFalse(FileUtils.readFileToString(new File(plain, "_sokrates_landscape/index.html"), UTF_8).contains("AI Insights"));
        assertFalse(new File(plain, "_sokrates_landscape/ai-insights.html").exists());
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
