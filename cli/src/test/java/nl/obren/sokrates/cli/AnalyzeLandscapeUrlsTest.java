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
 * analyzeLandscape with -urls / -url: each repository is analyzed with the analyzeGitRepo step into
 * <analysisRoot>/<name>, a bad URL is skipped, and the landscape is built over the results. Offline:
 * the "remote" repositories are local bare repositories.
 */
class AnalyzeLandscapeUrlsTest {

    @Test
    void analyzesEveryUrlThenBuildsTheLandscape(@TempDir Path tmp) throws Exception {
        File alphaRemote = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File betaRemote = bareRepoWithHistory(tmp, "beta", "bob@example.com");
        File root = tmp.resolve("landscape").toFile();
        File urlsFile = tmp.resolve("repos.txt").toFile();
        FileUtils.write(urlsFile, String.join("\n",
                "# repositories of the landscape",
                alphaRemote.toURI().toString(),
                "",
                "file:///" + tmp.resolve("does-not-exist.git").toString().replace('\\', '/'),
                ""), UTF_8);

        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", root.getPath(),
                "-urls", urlsFile.getPath(), "-url", betaRemote.toURI().toString()});

        assertTrue(new File(root, "alpha/config.json").exists(), "alpha should be analyzed into <root>/alpha");
        assertTrue(new File(root, "alpha/reports/index.html").exists());
        assertTrue(new File(root, "beta/config.json").exists(), "the -url repository should be analyzed too");
        assertTrue(new File(root, "beta/reports/index.html").exists());
        assertFalse(new File(root, "does-not-exist").exists(), "a repository whose clone fails is skipped");
        assertFalse(new File(root, "alpha/src").exists(), "no source is kept");

        File landscapeIndex = new File(root, "_sokrates_landscape/index.html");
        assertTrue(landscapeIndex.exists(), "the landscape should be built after the repositories");
        String landscape = FileUtils.readFileToString(landscapeIndex, UTF_8);
        assertTrue(landscape.contains("alpha"), "landscape should list alpha");
        assertTrue(landscape.contains("beta"), "landscape should list beta");

        // Each repository's viewer holds only its own files (one exporter instance serves the whole run).
        String betaViewer = FileUtils.readFileToString(new File(root, "beta/reports/src/viewer.html"), UTF_8);
        assertFalse(betaViewer.contains("alpha.ts"), "beta's viewer must not carry alpha's sources");
    }

    @Test
    void dataOnlyKeepsJustTheDataZips(@TempDir Path tmp) throws Exception {
        File alphaRemote = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File betaRemote = bareRepoWithHistory(tmp, "beta", "bob@example.com");
        File root = tmp.resolve("landscape").toFile();

        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", root.getPath(),
                "-url", alphaRemote.toURI().toString(), "-url", betaRemote.toURI().toString(), "-dataOnly"});

        // Each repository: config.json + reports/data/data.zip, nothing else.
        for (String repo : new String[]{"alpha", "beta"}) {
            assertEquals("[config.json, reports, source.json]", sortedNames(new File(root, repo)), repo + ": config, data-only reports and the source marker");
            assertEquals("[data]", sortedNames(new File(root, repo + "/reports")), repo + ": the reports folder must hold only data/");
            assertEquals("[data.zip]", sortedNames(new File(root, repo + "/reports/data")), repo);
        }

        // The landscape: its config files + data/data.zip, no index page, contributor pages, explorers or visuals.
        File landscape = new File(root, "_sokrates_landscape");
        assertEquals("[config-people.json, config-tags.json, config-teams.json, config.json, data, info.json]", sortedNames(landscape));
        assertEquals("[data.zip]", sortedNames(new File(landscape, "data")), "data/ must hold only data.zip (no data-preview.html)");
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(new File(landscape, "data/data.zip"))) {
            assertNotNull(zip.getEntry("landscapeAnalysisResults.json"), "what a parent landscape reads");
            assertNotNull(zip.getEntry("repositories.json"));
            assertNotNull(zip.getEntry("contributors.json"));
            String repositories = new String(zip.getInputStream(zip.getEntry("repositories.json")).readAllBytes(), UTF_8);
            assertTrue(repositories.contains("alpha") && repositories.contains("beta"), "both repositories are aggregated");
        }

        // A later full run over the same (data-only) repositories restores the landscape report.
        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", root.getPath()});
        assertTrue(new File(landscape, "index.html").exists());
        assertTrue(new File(landscape, "repositories.html").exists());
        assertTrue(new File(landscape, "data/data-preview.html").exists());
        String index = FileUtils.readFileToString(new File(landscape, "index.html"), UTF_8);
        assertTrue(index.contains("alpha") && index.contains("beta"));
    }

    private static String sortedNames(File folder) {
        String[] names = folder.list();
        java.util.Arrays.sort(names == null ? new String[0] : names);
        return java.util.Arrays.toString(names);
    }

    @Test
    void pruneDeletesAnalysesNoLongerListedOrWhoseRepositoryIsGone(@TempDir Path tmp) throws Exception {
        File alphaRemote = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File betaRemote = bareRepoWithHistory(tmp, "beta", "bob@example.com");
        File gammaRemote = bareRepoWithHistory(tmp, "gamma", "cy@example.com");
        File root = tmp.resolve("landscape").toFile();
        String alpha = alphaRemote.toURI().toString(), beta = betaRemote.toURI().toString(), gamma = gammaRemote.toURI().toString();

        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", root.getPath(), "-url", alpha, "-url", beta, "-url", gamma});
        for (String name : new String[]{"alpha", "beta", "gamma"}) {
            assertTrue(new File(root, name + "/config.json").exists(), name);
            assertTrue(new File(root, name + "/" + nl.obren.sokrates.cli.git.AnalysisSource.FILE_NAME).exists(), name + " carries the source marker");
        }
        nl.obren.sokrates.cli.git.AnalysisSource source = nl.obren.sokrates.cli.git.AnalysisSource.read(new File(root, "alpha"));
        assertEquals(alpha, source.getUrl());
        assertEquals("analyzeLandscape", source.getCommand());
        // An analysis placed by hand: no marker.
        FileUtils.copyDirectory(new File(root, "beta"), new File(root, "manual/beta-copy"));
        assertTrue(new File(root, "manual/beta-copy/" + nl.obren.sokrates.cli.git.AnalysisSource.FILE_NAME).delete());

        // beta leaves the list and gamma's repository disappears; without -prune nothing is deleted.
        FileUtils.deleteDirectory(gammaRemote);
        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", root.getPath(), "-url", alpha, "-url", gamma});
        assertTrue(new File(root, "beta/config.json").exists(), "without -prune a de-listed analysis stays");
        assertTrue(new File(root, "gamma/config.json").exists(), "without -prune the analysis of a gone repository stays");

        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", root.getPath(), "-url", alpha, "-url", gamma, "-prune"});
        assertTrue(new File(root, "alpha/config.json").exists(), "a listed, cloneable repository is kept");
        assertFalse(new File(root, "beta").exists(), "-prune deletes the analysis of a repository no longer listed");
        assertFalse(new File(root, "gamma").exists(), "-prune deletes the analysis of a listed repository that does not exist any more");
        assertTrue(new File(root, "manual/beta-copy/config.json").exists(), "an analysis without the marker is never touched");
        assertTrue(new File(root, "_sokrates_landscape/index.html").exists());
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(new File(root, "_sokrates_landscape/data/data.zip"))) {
            String repositories = new String(zip.getInputStream(zip.getEntry("repositories.json")).readAllBytes(), UTF_8);
            assertTrue(repositories.contains("\"name\" : \"alpha\""), repositories);
            assertFalse(repositories.contains("\"name\" : \"gamma\""), "the landscape no longer counts the pruned analysis");
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
