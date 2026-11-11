package nl.obren.sokrates.cli;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The -postAnalysis hook: a shell command Sokrates runs in the analyzed source tree right after
 * its own analysis — for a cloned repository while the clone still exists, so an AI coding agent
 * (or any script) can read the source and the fresh {@code _sokrates/} analysis and write its
 * results next to them; whatever lands under {@code _sokrates/} is kept with the analysis. Agent
 * agnostic: Sokrates only runs a command line. {@code -ai claude|codex|gemini} expands to that
 * agent's headless invocation of {@code -aiPrompt} (default: a basic scan through the sokrates-skills entry skill, validated and rendered — {@link #DEFAULT_PROMPT}; the sokrates-skills
 * full scan), and an explicit {@code -postAnalysis} always wins.
 * <p>
 * Environment of the command: {@code SOKRATES_REPO_URL}, {@code SOKRATES_REPO_NAME},
 * {@code SOKRATES_SRC_ROOT}, {@code SOKRATES_ANALYSIS_FOLDER} ({@code _sokrates/}),
 * {@code SOKRATES_REPORTS_FOLDER} and {@code SOKRATES_OUTPUT_FOLDER} (where the analysis will be
 * kept — the same as the analysis folder for a local analysis). A failing command is logged and
 * never fails the analysis.
 */
public class PostAnalysisHook {
    public static final String DEFAULT_PROMPT = "Use the sokrates skill: run a basic scan of this repository with the full-scan skill, validate and render the findings, and do not change any source file.";
    public static final List<String> AGENTS = List.of("claude", "codex", "gemini");

    private static final Log LOG = LogFactory.getLog(PostAnalysisHook.class);

    /** The headless command line of a known agent for a prompt; null for an unknown agent. */
    public static String aiPresetCommand(String agent, String prompt) {
        String text = StringUtils.defaultIfBlank(prompt, DEFAULT_PROMPT).replace("\"", "\\\"");
        switch (StringUtils.defaultString(agent).trim().toLowerCase()) {
            case "claude":
                return "claude -p \"" + text + "\" --permission-mode acceptEdits --allowedTools \"Bash,Read,Write,Edit,Glob,Grep\"";
            case "codex":
                return "codex exec --full-auto \"" + text + "\"";
            case "gemini":
                return "gemini -p \"" + text + "\" --yolo";
            default:
                return null;
        }
    }

    /** Runs {@code command} in {@code srcRoot} with the Sokrates environment; returns the exit code (-1 when it could not start). */
    public static int run(String command, File srcRoot, Map<String, String> environment) {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        ProcessBuilder builder = new ProcessBuilder(windows ? List.of("cmd", "/c", command) : List.of("/bin/sh", "-c", command));
        builder.directory(srcRoot);
        builder.environment().putAll(environment);
        builder.inheritIO();
        LOG.info("");
        LOG.info("=== Post-analysis command in " + srcRoot.getPath() + ": " + command + " ===");
        long start = System.currentTimeMillis();
        try {
            Process process = builder.start();
            int exit = process.waitFor();
            long seconds = (System.currentTimeMillis() - start) / 1000;
            if (exit == 0) {
                LOG.info("Post-analysis command finished in " + seconds + " s.");
            } else {
                LOG.error("Post-analysis command exited with " + exit + " after " + seconds + " s; the analysis is kept as it is.");
            }
            return exit;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Post-analysis command interrupted.");
            return -1;
        } catch (Exception e) {
            LOG.error("Could not run the post-analysis command: " + e.getMessage());
            return -1;
        }
    }

    /** The variables the command sees, all absolute paths. */
    public static Map<String, String> environment(String repoUrl, String repoName, File srcRoot, File analysisFolder, File reportsFolder, File outputFolder) {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("SOKRATES_REPO_URL", StringUtils.defaultString(repoUrl));
        env.put("SOKRATES_REPO_NAME", StringUtils.defaultString(repoName));
        env.put("SOKRATES_SRC_ROOT", absolute(srcRoot));
        env.put("SOKRATES_ANALYSIS_FOLDER", absolute(analysisFolder));
        env.put("SOKRATES_REPORTS_FOLDER", absolute(reportsFolder));
        env.put("SOKRATES_OUTPUT_FOLDER", absolute(outputFolder != null ? outputFolder : analysisFolder));
        return env;
    }

    private static String absolute(File file) {
        return file == null ? "" : file.toPath().toAbsolutePath().normalize().toString();
    }
}
