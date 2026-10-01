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
 * -postAnalysis / -ai: a command runs in the source tree after the analysis — in the clone for the
 * URL commands, before it is deleted — with the Sokrates environment, and what it writes under
 * _sokrates/ travels with the kept analysis. A failing command never fails the analysis. Offline:
 * the "agent" is a shell one-liner.
 */
class PostAnalysisHookTest {
    private static final String HOOK = "mkdir -p \"$SOKRATES_ANALYSIS_FOLDER/ai-insights\" && printf 'url=%s\\nname=%s\\nsrc=%s\\nout=%s\\ncwd=%s\\n' "
            + "\"$SOKRATES_REPO_URL\" \"$SOKRATES_REPO_NAME\" \"$SOKRATES_SRC_ROOT\" \"$SOKRATES_OUTPUT_FOLDER\" \"$(pwd)\" > \"$SOKRATES_ANALYSIS_FOLDER/ai-insights/hook.txt\"";

    @Test
    void presetsExpandToTheAgentsHeadlessCommands() {
        assertEquals("claude -p \"run a full scan\" --permission-mode acceptEdits --allowedTools \"Bash,Read,Write,Edit,Glob,Grep\"", PostAnalysisHook.aiPresetCommand("claude", null));
        assertEquals("codex exec --full-auto \"run a tech stack scan\"", PostAnalysisHook.aiPresetCommand("Codex", "run a tech stack scan"));
        assertEquals("gemini -p \"say \\\"hi\\\"\" --yolo", PostAnalysisHook.aiPresetCommand("gemini", "say \"hi\""));
        assertNull(PostAnalysisHook.aiPresetCommand("copilot", "x"));
    }

    @Test
    void runsInTheCloneAndItsOutputIsKeptWithTheAnalysis(@TempDir Path tmp) throws Exception {
        File remote = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File dest = tmp.resolve("kept").toFile();

        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", remote.toURI().toString(), "-destFolder", dest.getPath(), "-postAnalysis", HOOK});

        File hook = new File(dest, "ai-insights/hook.txt");
        assertTrue(hook.exists(), "what the command wrote under _sokrates/ is kept with the analysis");
        String out = FileUtils.readFileToString(hook, UTF_8);
        assertTrue(out.contains("url=" + remote.toURI()), out);
        assertTrue(out.contains("name=alpha"), out);
        assertTrue(out.contains("out=" + dest.toPath().toAbsolutePath().normalize()), out);
        String src = out.lines().filter(l -> l.startsWith("src=")).findFirst().orElse("").substring(4);
        String cwd = out.lines().filter(l -> l.startsWith("cwd=")).findFirst().orElse("").substring(4);
        // the shell's pwd may resolve symlinks (/private/var vs /var on macOS): compare the clone folder's name
        assertEquals(new File(src).getName(), new File(cwd).getName(), "the command runs in the source tree (the clone): " + out);
        assertTrue(src.contains("sokrates-clone-"), src);
        assertFalse(new File(src).exists(), "the clone is deleted afterwards");
        assertTrue(new File(dest, "reports/index.html").exists());
    }

    @Test
    void runsForEveryRepositoryOfALandscapeAndAFailureDoesNotFailTheAnalysis(@TempDir Path tmp) throws Exception {
        File alpha = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File beta = bareRepoWithHistory(tmp, "beta", "bob@example.com");
        File root = tmp.resolve("landscape").toFile();

        new CommandLineInterface().run(new String[]{"analyzeLandscape", "-analysisRoot", root.getPath(),
                "-url", alpha.toURI().toString(), "-url", beta.toURI().toString(),
                "-postAnalysis", HOOK + " && [ \"$SOKRATES_REPO_NAME\" != beta ]"});   // exits 1 for beta

        assertTrue(new File(root, "alpha/ai-insights/hook.txt").exists());
        assertTrue(new File(root, "beta/ai-insights/hook.txt").exists(), "beta's command wrote its file before failing");
        assertTrue(new File(root, "beta/reports/index.html").exists(), "a failing command keeps the analysis");
        assertTrue(new File(root, "_sokrates_landscape/index.html").exists());
    }

    @Test
    void runsForALocalAnalysisToo(@TempDir Path tmp) throws Exception {
        File project = tmp.resolve("project").toFile();
        FileUtils.write(new File(project, "src/a.ts"), "export const a = 1;\n", UTF_8);

        new CommandLineInterface().run(new String[]{"analyze", "-srcRoot", project.getPath(), "-postAnalysis", HOOK});

        String out = FileUtils.readFileToString(new File(project, "_sokrates/ai-insights/hook.txt"), UTF_8);
        assertTrue(out.contains("url=\n"), "no git remote: empty URL: " + out);
        assertTrue(out.contains("name=project"), out);
        assertTrue(out.contains("out=" + new File(project, "_sokrates").toPath().toAbsolutePath().normalize()), "the output folder is the analysis folder itself: " + out);
    }

    @Test
    void skipsAnUnchangedRepositoryUntilItsHeadMovesOrForced(@TempDir Path tmp) throws Exception {
        File remote = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File dest = tmp.resolve("kept").toFile();
        File runs = tmp.resolve("runs.txt").toFile();
        String countingHook = "echo run >> \"" + runs.getPath() + "\"";
        String[] base = {"analyzeGitRepo", "-url", remote.toURI().toString(), "-destFolder", dest.getPath(), "-postAnalysis", countingHook};

        new CommandLineInterface().run(base);
        assertEquals(1, FileUtils.readLines(runs, UTF_8).size());
        PostAnalysisState state = PostAnalysisState.read(dest);
        assertNotNull(state, "the run is recorded with the kept analysis");
        assertEquals(countingHook, state.getCommand());
        assertEquals(40, state.getHead().length(), "the head commit it ran on");
        assertEquals(0, state.getExitCode());

        new CommandLineInterface().run(base);
        assertEquals(1, FileUtils.readLines(runs, UTF_8).size(), "same head, same command: skipped");
        assertNotNull(PostAnalysisState.read(dest), "the skipped repository keeps its state file");

        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", remote.toURI().toString(), "-destFolder", dest.getPath(), "-postAnalysis", countingHook, "-aiForce"});
        assertEquals(2, FileUtils.readLines(runs, UTF_8).size(), "-aiForce runs it anyway");

        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", remote.toURI().toString(), "-destFolder", dest.getPath(), "-postAnalysis", countingHook + " && true"});
        assertEquals(3, FileUtils.readLines(runs, UTF_8).size(), "a different command runs again");

        addCommit(tmp, "alpha", "ada@example.com");
        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", remote.toURI().toString(), "-destFolder", dest.getPath(), "-postAnalysis", countingHook + " && true"});
        assertEquals(4, FileUtils.readLines(runs, UTF_8).size(), "a new commit runs it again");
        assertNotEquals(state.getHead(), PostAnalysisState.read(dest).getHead());

        // A failed run is not a reason to skip next time.
        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", remote.toURI().toString(), "-destFolder", dest.getPath(), "-postAnalysis", countingHook + " && false"});
        assertEquals(5, FileUtils.readLines(runs, UTF_8).size());
        assertEquals(1, PostAnalysisState.read(dest).getExitCode());
        new CommandLineInterface().run(new String[]{"analyzeGitRepo", "-url", remote.toURI().toString(), "-destFolder", dest.getPath(), "-postAnalysis", countingHook + " && false"});
        assertEquals(6, FileUtils.readLines(runs, UTF_8).size(), "retried after a failure");
    }

    @Test
    void aiMaxReposBoundsTheRunsPerInvocation(@TempDir Path tmp) throws Exception {
        File alpha = bareRepoWithHistory(tmp, "alpha", "ada@example.com");
        File beta = bareRepoWithHistory(tmp, "beta", "bob@example.com");
        File gamma = bareRepoWithHistory(tmp, "gamma", "cy@example.com");
        File root = tmp.resolve("landscape").toFile();
        File runs = tmp.resolve("runs.txt").toFile();
        String countingHook = "echo \"$SOKRATES_REPO_NAME\" >> \"" + runs.getPath() + "\"";
        String[] args = {"analyzeLandscape", "-analysisRoot", root.getPath(), "-url", alpha.toURI().toString(), "-url", beta.toURI().toString(),
                "-url", gamma.toURI().toString(), "-postAnalysis", countingHook, "-aiMaxRepos", "2"};

        new CommandLineInterface().run(args);
        assertEquals(java.util.List.of("alpha", "beta"), FileUtils.readLines(runs, UTF_8), "the first two repositories get the command");
        assertNull(PostAnalysisState.read(new File(root, "gamma")), "the third is analyzed without it and keeps no state");

        new CommandLineInterface().run(args);
        assertEquals(java.util.List.of("alpha", "beta", "gamma"), FileUtils.readLines(runs, UTF_8), "the next run skips the two done ones and reaches the third");
    }

    private static void addCommit(Path tmp, String name, String email) throws Exception {
        File work = tmp.resolve("work-" + name).toFile();
        FileUtils.write(new File(work, "src/" + name + "-more.ts"), "export const more = 1;\n", UTF_8);
        try (Git git = Git.open(work)) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("more").setAuthor(name, email).setCommitter(name, email).call();
            git.push().setRemote("origin").setPushAll().call();
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
