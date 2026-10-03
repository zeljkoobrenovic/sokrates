package nl.obren.sokrates.cli;

import nl.obren.sokrates.cli.git.AnalysisSource;
import nl.obren.sokrates.cli.git.GitHubOrgClient;
import nl.obren.sokrates.cli.git.HttpFetcher;
import nl.obren.sokrates.cli.git.CodeHostRepo;
import nl.obren.sokrates.cli.git.GitRepoCloner;
import nl.obren.sokrates.cli.git.GitRepoMetadata;
import nl.obren.sokrates.common.utils.*;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

/**
 * The analyzeGitRepo command and the clone-and-analyze step it shares with the landscape and organization
 * commands: clone into a temporary folder, run the analyze pipeline there, keep only the analysis, and decide
 * whether a failed clone means the repository is gone. Moved out of {@link CommandLineInterface}, which keeps
 * the analyze pipeline itself.
 */
class GitRepoCommands {
    private static final Log LOG = LogFactory.getLog(GitRepoCommands.class);
    private final CommandLineInterface cli;
    private final Commands commands;

    GitRepoCommands(CommandLineInterface cli, Commands commands) {
        this.cli = cli;
        this.commands = commands;
    }

    /**
     * analyzeGitRepo = clone the repository at -url into a temporary folder, run the analyze
     * pipeline there, and keep only the analysis (config.json + reports/) in -destFolder (default:
     * <cwd>/<owner>/<repository>); the clone is deleted. A kept config.json is reused on re-runs,
     * so edits survive even though the clone is fresh every time. The output layout (config.json
     * next to reports/) is the one analyzeLandscape expects.
     */
    void analyzeGitRepo(String[] args) throws ParseException, IOException {
        Options options = commands.getAnalyzeGitRepoOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt()) || !cmd.hasOption(commands.getUrl().getOpt())) {
            CommandLineInterface.helpMode = true;
            if (!cmd.hasOption(commands.getUrl().getOpt())) {
                LOG.error("-" + Commands.ARG_URL + " is required.");
            }
            commands.usage(Commands.ANALYZE_GIT_REPO, options, Commands.ANALYZE_GIT_REPO_DESCRIPTION);
            return;
        }

        cli.startTimeoutIfDefined(cmd);

        String url = cmd.getOptionValue(commands.getUrl().getOpt()).trim();
        GitRepoMetadata urlMetadata = GitRepoMetadata.fromUrl(url);
        File output = cmd.hasOption(commands.getDestRoot().getOpt())
                ? new File(cmd.getOptionValue(commands.getDestRoot().getOpt()))
                : new File(urlMetadata != null ? urlMetadata.outputFolderName() : GitRepoCloner.folderNameFromUrl(url));
        String branch = cmd.getOptionValue(commands.getBranch().getOpt());
        int depth = 0;
        String depthValue = cmd.getOptionValue(commands.getDepth().getOpt());
        if (StringUtils.isNotBlank(depthValue)) {
            if (!StringUtils.isNumeric(depthValue.trim())) {
                LOG.error("-" + Commands.ARG_DEPTH + " must be a positive number, got '" + depthValue + "'.");
                return;
            }
            depth = Integer.parseInt(depthValue.trim());
        }

        analyzeGitRepoInto(cmd, url, output, branch, depth, Commands.ANALYZE_GIT_REPO, null);
    }

    /**
     * The analyzeGitRepo step for one repository: clone into a temporary folder, analyze there, keep
     * only the analysis in {@code output} (reusing a config.json already kept there). Returns false
     * when the clone fails; the analysis itself reports its own errors.
     */
    /** What the clone-and-analyze step did with one URL. */
    enum CloneOutcome {
        ANALYZED,
        /** The clone failed for a reason that may pass (network, credentials, a bad branch): keep any earlier analysis. */
        FAILED,
        /** The remote repository does not exist (any more, or for these credentials): -prune may delete its analysis. */
        NOT_FOUND
    }

    CloneOutcome analyzeGitRepoInto(CommandLine cmd, String url, File output, String branch, int depth, String producer, CodeHostRepo listed) throws IOException {
        File clone = Files.createTempDirectory("sokrates-clone-").toFile();
        try {
            ProcessingStopwatch.start("cloning");
            try {
                new GitRepoCloner().cloneOrUpdate(url, clone, branch, depth);
            } catch (Exception e) {
                boolean gone = repositoryGone(url, e);
                LOG.error("Could not clone " + url + ": " + e.getMessage() + (gone ? " (the repository does not exist)" : ""));
                return gone ? CloneOutcome.NOT_FOUND : CloneOutcome.FAILED;
            } finally {
                ProcessingStopwatch.end("cloning");
            }

            File analysisFolder = CodeConfigurationUtils.getDefaultSokratesFolder(clone);
            File keptConfig = new File(output, CodeConfigurationUtils.getDefaultSokratesConfigFile(clone).getName());
            if (keptConfig.exists()) {
                LOG.info("Reusing the configuration kept in " + keptConfig.getPath());
                FileUtils.copyFile(keptConfig, CodeConfigurationUtils.getDefaultSokratesConfigFile(clone));
            }

            cli.analyze(cmd, clone, false, url, output, listed);

            // Keep only the analysis: everything in the clone's _sokrates folder (config.json, reports/,
            // any config-*.json) replaces its namesake in the output folder; the clone goes.
            output.mkdirs();
            File[] produced = analysisFolder.listFiles();
            for (File file : produced == null ? new File[0] : produced) {
                File target = new File(output, file.getName());
                FileUtils.deleteQuietly(target);
                if (file.isDirectory()) {
                    FileUtils.moveDirectory(file, target);
                } else {
                    FileUtils.moveFile(file, target);
                }
            }
            new AnalysisSource(url, producer, DateUtils.getAnalysisDate()).save(output);
            LOG.info("Analysis kept in " + output.toPath().toAbsolutePath().normalize() + " (config.json + reports/); the source clone is deleted.");
            cli.logReportLocation(new File(output, "reports"));
            return CloneOutcome.ANALYZED;
        } finally {
            FileUtils.deleteQuietly(clone);
        }
    }

    /**
     * Whether a clone failure means the repository is gone rather than temporarily unreachable.
     * JGit reports a missing repository as a NoRemoteRepositoryException ("not found"); GitHub
     * answers an anonymous clone of a missing (or private) repository with "authentication is
     * required", so for github.com the REST API is asked as well (404 = gone, anything else = keep).
     * Network errors (unknown host, cannot open git-upload-pack, timeouts) never count as gone.
     */
    static boolean repositoryGone(String url, Throwable failure) {
        Boolean byCause = goneByCause(failure);
        if (byCause != null) {
            return byCause;
        }
        GitRepoMetadata metadata = GitRepoMetadata.fromUrl(url);
        if (metadata != null && metadata.isGitHub() && StringUtils.isNotBlank(metadata.getOwner())
                && StringUtils.isBlank(System.getenv(GitRepoMetadata.ENV_OFFLINE))) {
            return gitHubRepositoryMissing(metadata);
        }
        String message = StringUtils.defaultString(failure.getMessage()).toLowerCase();
        return message.contains("not found") || message.contains("does not exist");
    }

    /** true for JGit's "no remote repository", false for a network failure, null when the cause chain does not decide. */
    private static Boolean goneByCause(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.eclipse.jgit.errors.NoRemoteRepositoryException) {
                return true;
            }
            if (cause instanceof java.net.UnknownHostException || cause instanceof java.net.ConnectException
                    || cause instanceof java.net.SocketTimeoutException) {
                return false;
            }
        }
        return null;
    }

    /** Asks the GitHub REST API whether the repository exists; false on any failure (a network error never counts as gone). */
    private static boolean gitHubRepositoryMissing(GitRepoMetadata metadata) {
        try {
            HttpFetcher.Response response = HttpFetcher.create(Map.of(
                    "Accept", "application/vnd.github+json",
                    "Authorization", StringUtils.isNotBlank(System.getenv(GitRepoCloner.ENV_TOKEN)) ? "Bearer " + System.getenv(GitRepoCloner.ENV_TOKEN) : ""))
                    .get(GitHubOrgClient.DEFAULT_API_BASE + "/repos/" + metadata.getOwner() + "/" + metadata.getName());
            return response.status == 404;
        } catch (Exception e) {
            return false;
        }
    }
}
