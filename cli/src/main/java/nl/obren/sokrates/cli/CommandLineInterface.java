/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.cli;

import nl.obren.sokrates.cli.git.GitHistoryExtractor;
import nl.obren.sokrates.cli.git.AnalysisSource;
import nl.obren.sokrates.cli.git.CodeHostOrg;
import nl.obren.sokrates.cli.git.CodeHostOrgClient;
import nl.obren.sokrates.cli.git.GitHubOrgClient;
import nl.obren.sokrates.cli.git.GitLabGroupClient;
import nl.obren.sokrates.cli.git.HttpFetcher;
import nl.obren.sokrates.cli.git.CodeHostRepo;
import nl.obren.sokrates.cli.git.CodeHostRepoFilter;
import nl.obren.sokrates.cli.git.GitRepoCloner;
import nl.obren.sokrates.cli.git.GitRepoMetadata;
import nl.obren.sokrates.cli.skills.SkillsInstaller;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.renderingutils.Thresholds;
import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DLink;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DNode;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import nl.obren.sokrates.common.renderingutils.x3d.Unit3D;
import nl.obren.sokrates.common.renderingutils.x3d.X3DomExporter;
import nl.obren.sokrates.common.utils.*;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.dataexporters.DataExporter;
import nl.obren.sokrates.reports.generators.explorers.CommitsExplorerGenerators;
import nl.obren.sokrates.reports.generators.explorers.FilesExplorerGenerators;
import nl.obren.sokrates.reports.generators.explorers.UnitsExplorerGenerators;
import nl.obren.sokrates.reports.generators.statichtml.BasicSourceCodeReportGenerator;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeAnalysisCommands;
import nl.obren.sokrates.reports.landscape.statichtml.RepositoryPeopleConfigCommands;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzer;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzerSettings;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CustomTab;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.ExtractGitHistoryFileHandler;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.scoping.ScopeCreator;
import nl.obren.sokrates.sourcecode.scoping.ScopingConventions;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomConventionsHelper;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomScopingConventions;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.errors.GitAPIException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommandLineInterface {
    private static final Log LOG = LogFactory.getLog(CommandLineInterface.class);
    private ProgressFeedback progressFeedback;
    // The code-host APIs behind analyzeGitHubOrg / analyzeGitLabGroup; replaceable so tests run offline
    // against local repositories (the GitLab one is created per run from -gitlabUrl unless injected).
    private final DataExporter dataExporter = new DataExporter(this.progressFeedback);

    private final Commands commands = new Commands();
    final GitRepoCommands gitRepoCommands = new GitRepoCommands(this, commands);
    final LandscapeCommands landscapeCommands = new LandscapeCommands(this, commands);
    private final OrganizationCommands organizationCommands = new OrganizationCommands(this, commands);
    private CodeConfiguration codeConfiguration;

    static boolean helpMode = false;

    public static void main(String[] args) throws IOException {
        ProcessingStopwatch.startAsReference("everything");

        CommandLineInterface commandLineInterface = new CommandLineInterface();
        commandLineInterface.run(args);

        ProcessingStopwatch.end("everything");

        if (!helpMode) {
            ProcessingStopwatch.print();
        }

        System.exit(0);
    }

    /** A command handler: the command's arguments, including the command name itself. */
    interface CommandHandler {
        void run(String[] args) throws ParseException, IOException;
    }

    /** Every command by name (matched case-insensitively), in the order of the usage text. */
    private Map<String, CommandHandler> commandHandlers() {
        Map<String, CommandHandler> handlers = new LinkedHashMap<>();
        handlers.put(Commands.ANALYZE, this::analyze);
        handlers.put(Commands.ANALYZE_GIT_REPO, gitRepoCommands::analyzeGitRepo);
        handlers.put(Commands.INIT, this::init);
        handlers.put(Commands.GENERATE_REPORTS, this::generateReports);
        // Same implementation as updateLandscape; analyzeLandscape is the name that mirrors analyze.
        handlers.put(Commands.ANALYZE_LANDSCAPE, args -> landscapeCommands.updateLandscape(args, Commands.ANALYZE_LANDSCAPE, Commands.ANALYZE_LANDSCAPE_DESCRIPTION));
        handlers.put(Commands.UPDATE_LANDSCAPE, args -> landscapeCommands.updateLandscape(args, Commands.UPDATE_LANDSCAPE, Commands.UPDATE_LANDSCAPE_DESCRIPTION));
        handlers.put(Commands.ANALYZE_GITHUB_ORG, organizationCommands::analyzeGitHubOrg);
        handlers.put(Commands.ANALYZE_GITLAB_GROUP, organizationCommands::analyzeGitLabGroup);
        handlers.put(Commands.UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME, this::updateLandscapePeopleConfigByUserName);
        handlers.put(Commands.UPDATE_PEOPLE_CONFIG_BY_USER_NAME, this::updatePeopleConfigByUserName);
        handlers.put(Commands.UPDATE_CONFIG, this::updateConfig);
        handlers.put(Commands.ADD_CUSTOM_TAB, this::addCustomTab);
        handlers.put(Commands.INSTALL_SKILLS, this::installSkills);
        handlers.put(Commands.EXTRACT_GIT_HISTORY, this::extractGitHistory);
        handlers.put(Commands.INIT_CONVENTIONS, this::createNewConventionsFile);
        handlers.put(Commands.EXPORT_STANDARD_CONVENTIONS, this::exportConventions);
        handlers.put(Commands.EXTRACT_GIT_SUB_HISTORY, this::extractGitSubHistory);
        handlers.put(Commands.EXTRACT_FILES, this::extractFiles);
        return handlers;
    }

    public void run(String[] args) throws IOException {
        postAnalysisRuns = 0;
        if (args.length == 0) {
            helpMode = true;
            commands.usage();
            return;
        }
        if (progressFeedback != null) {
            progressFeedback.clear();
        }
        CommandHandler handler = commandHandlers().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(args[0]))
                .map(Map.Entry::getValue).findFirst().orElse(null);
        if (handler == null) {
            helpMode = true;
            commands.usage();
            return;
        }
        try {
            handler.run(args);
        } catch (ParseException e) {
            LOG.info("ERROR: " + e.getMessage() + "\n");
            e.printStackTrace();
            helpMode = true;
            commands.usage();
        }
    }

    private void extractGitHistory(String[] args) throws ParseException {
        Options options = commands.getExtractGitHistoryOption();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.EXTRACT_GIT_HISTORY, commands.getExtractGitHistoryOption(), Commands.EXTRACT_GIT_HISTORY_DESCRIPTION);
            return;
        }

        String strRootPath = cmd.getOptionValue(commands.getAnalysisRoot().getOpt());
        if (!cmd.hasOption(commands.getAnalysisRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        new GitHistoryExtractor().extractGitHistory(root);
    }

    private void extractGitSubHistory(String[] args) throws ParseException, IOException {
        Options options = commands.getExtractGitSubHistoryOption();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.EXTRACT_GIT_SUB_HISTORY, commands.getExtractGitSubHistoryOption(), Commands.EXTRACT_GIT_SUB_HISTORY_DESCRIPTION);
            return;
        }

        String strRootPath = cmd.getOptionValue(commands.getAnalysisRoot().getOpt());
        if (!cmd.hasOption(commands.getAnalysisRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        String prefixValue = cmd.getOptionValue(commands.getPrefix().getOpt());

        new ExtractGitHistoryFileHandler().extractSubHistory(new File(root, GitHistoryUtils.GIT_HISTORY_FILE_NAME), prefixValue);
    }

    private void extractFiles(String[] args) throws ParseException, IOException {
        Options options = commands.getExtractFilesOption();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.EXTRACT_FILES, commands.getExtractFilesOption(), Commands.EXTRACT_FILES_DESCRIPTION);
            return;
        }

        File root = cmd.hasOption(commands.getAnalysisRoot().getOpt()) ? new File(cmd.getOptionValue(commands.getAnalysisRoot().getOpt())) : new File(".");
        String patternValue = cmd.getOptionValue(commands.getPattern().getOpt());
        String dest = cmd.getOptionValue(commands.getDestRoot().getOpt());
        String destParentValue = cmd.getOptionValue(commands.getDestParent().getOpt());

        if (patternValue == null) {
            LOG.info("the pattern value is missing");
            return;
        }
        if (dest == null) {
            LOG.info("the destination folder value is missing");
            return;
        }
        if (destParentValue == null) {
            destParentValue = dest;
        }

        SokratesFileUtils.extractFiles(root, new File(root, dest), new File(root, destParentValue), patternValue);
    }

    void updateDateParam(CommandLine cmd) {
        String dateString = cmd.getOptionValue(commands.getDate().getOpt());
        if (dateString != null) {
            LOG.info("Using '" + dateString + "' as latest source code update date for active contributors reports.");
            DateUtils.setDateParam(dateString);
        }
    }

    /**
     * The values of a repeatable option plus the lines of its list-file companion (trimmed; blank
     * lines and # comments ignored), in order, without duplicates. Empty when none were given; null
     * (after logging) when the file cannot be read.
     */
    List<String> collectValues(CommandLine cmd, Option single, Option listFile) {
        List<String> values = new ArrayList<>();
        String[] givenValues = cmd.getOptionValues(single.getOpt());
        if (givenValues != null) {
            for (String value : givenValues) {
                if (StringUtils.isNotBlank(value) && !values.contains(value.trim())) {
                    values.add(value.trim());
                }
            }
        }
        if (cmd.hasOption(listFile.getOpt()) && !addValuesFromFile(new File(cmd.getOptionValue(listFile.getOpt())), listFile, values)) {
            return null;
        }
        return values;
    }

    /** Appends the file's lines (trimmed; blank lines, # comments and duplicates skipped); false (after logging) when it cannot be read. */
    private static boolean addValuesFromFile(File file, Option listFile, List<String> values) {
        if (!file.exists()) {
            LOG.error("The -" + listFile.getOpt() + " file \"" + file.getPath() + "\" does not exist.");
            return false;
        }
        try {
            for (String line : FileUtils.readLines(file, UTF_8)) {
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#") || values.contains(value)) {
                    continue;
                }
                values.add(value);
            }
        } catch (IOException e) {
            LOG.error("Could not read the -" + listFile.getOpt() + " file \"" + file.getPath() + "\": " + e.getMessage());
            return false;
        }
        return true;
    }

    private void updateLandscapePeopleConfigByUserName(String[] args) throws ParseException {
        Options options = commands.getUpdateLandscapePeopleConfigByUserNameOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME,
                    commands.getUpdateLandscapePeopleConfigByUserNameOptions(),
                    Commands.UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(commands.getAnalysisRoot().getOpt());
        if (!cmd.hasOption(commands.getAnalysisRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        String confFilePath = cmd.getOptionValue(commands.getConfFile().getOpt());
        LandscapeAnalysisCommands.updatePeopleConfigByUserName(root,
                confFilePath != null ? new File(confFilePath) : null);
    }

    private void updatePeopleConfigByUserName(String[] args) throws ParseException {
        Options options = commands.getUpdatePeopleConfigByUserNameOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.UPDATE_PEOPLE_CONFIG_BY_USER_NAME,
                    commands.getUpdatePeopleConfigByUserNameOptions(),
                    Commands.UPDATE_PEOPLE_CONFIG_BY_USER_NAME_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        // Same default as generateReports: ./_sokrates/config.json when -confFile is not given.
        File sokratesConfigFile;
        if (cmd.hasOption(commands.getConfFile().getOpt())) {
            sokratesConfigFile = new File(cmd.getOptionValue(commands.getConfFile().getOpt()));
        } else {
            sokratesConfigFile = new File("./_sokrates/config.json");
        }

        RepositoryPeopleConfigCommands.updatePeopleConfigByUserName(sokratesConfigFile);
    }

    private void generateReports(String[] args) throws ParseException, IOException {
        Options options = commands.getReportingOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.GENERATE_REPORTS, commands.getReportingOptions(), Commands.GENERATE_REPORTS_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        generateReports(cmd);
    }

    private void init(String[] args) throws ParseException, IOException {
        Options options = commands.getInitOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.INIT, commands.getInitOptions(), Commands.INIT_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        File root = getSrcRoot(cmd);
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File conf = getConfigFile(cmd, root);

        updateDateParam(cmd);

        createConfiguration(cmd, root, conf);
        applyGitRepoMetadata(root, conf);
    }

    /**
     * One-shot analysis (the recommended first contact with Sokrates): extract the git history
     * (JGit, so no git binary is needed) when the root is a git repository, create the
     * configuration when there is none yet (an existing config.json is kept, so edits survive
     * re-runs), then generate the reports. All paths default relative to -srcRoot, not the
     * current folder, so `analyze -srcRoot ../x` behaves like running `analyze` inside x.
     */
    private void analyze(String[] args) throws ParseException, IOException {
        Options options = commands.getAnalyzeOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.ANALYZE, options, Commands.ANALYZE_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        File root = getSrcRoot(cmd);
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File reportsFolder = analyze(cmd, root, cmd.hasOption(commands.getSkipGitHistory().getOpt()));
        logReportLocation(reportsFolder);
    }

    void logReportLocation(File reportsFolder) {
        if (reportsFolder == null) {
            return;
        }
        File index = new File(reportsFolder, "index.html");
        File dataZip = new File(new File(reportsFolder, "data"), "data.zip");
        LOG.info("");
        if (index.exists()) {
            LOG.info("Done. Open the report: " + index.toPath().toAbsolutePath().normalize().toUri());
        } else if (dataZip.exists()) {
            LOG.info("Done. Analysis data stored in " + dataZip.toPath().toAbsolutePath().normalize());
        }
    }

    /**
     * The analyze pipeline on a root folder: git history extraction (unless skipped or not a git
     * repository), init when the configuration does not exist yet, report generation. Paths default
     * relative to the root; -confFile / -outputFolder on {@code cmd} override them.
     */
    private File analyze(CommandLine cmd, File root, boolean skipGitHistory) throws IOException {
        GitRepoMetadata origin = GitRepoMetadata.fromLocalRepository(root);
        return analyze(cmd, root, skipGitHistory, origin != null ? origin.getRemoteUrl() : "", null, null);
    }

    /**
     * @param repoUrl    the repository URL for the post-analysis hook's environment ("" for a plain folder)
     * @param keptFolder where the analysis will be kept when it is moved out of a clone; null = it stays in place
     */
    File analyze(CommandLine cmd, File root, boolean skipGitHistory, String repoUrl, File keptFolder, CodeHostRepo listed) throws IOException {
        updateDateParam(cmd);

        ProcessingStopwatch.start("extracting git history");
        if (skipGitHistory) {
            LOG.info("Skipping git history extraction (-" + Commands.ARG_SKIP_GIT_HISTORY + ").");
        } else if (new File(root, ".git").exists()) {
            new GitHistoryExtractor().extractGitHistory(root);
        } else {
            LOG.info("No .git folder in " + root.getPath() + ": skipping git history extraction (commit, contributor and trend reports will be empty).");
        }
        ProcessingStopwatch.end("extracting git history");

        File conf = getConfigFile(cmd, root);
        if (conf.exists()) {
            LOG.info("Using the existing configuration " + conf.getPath());
        } else {
            createConfiguration(cmd, root, conf);
        }
        applyGitRepoMetadata(root, conf, listed);

        File reportsFolder;
        if (cmd.hasOption(commands.getOutputFolder().getOpt())) {
            reportsFolder = prepareReportsFolder(cmd.getOptionValue(commands.getOutputFolder().getOpt()));
        } else {
            reportsFolder = prepareReportsFolder(new File(conf.getParentFile(), "reports").getPath());
        }

        generateReports(cmd, conf, reportsFolder);
        runPostAnalysisHook(cmd, root, conf, reportsFolder, repoUrl, keptFolder);
        return reportsFolder;
    }

    /** The -postAnalysis / -ai command, if any: an explicit command wins over the agent preset. Null when neither is given. */
    String postAnalysisCommand(CommandLine cmd) {
        String explicit = cmd.getOptionValue(commands.getPostAnalysis().getOpt());
        if (StringUtils.isNotBlank(explicit)) {
            return explicit.trim();
        }
        String agent = cmd.getOptionValue(commands.getAi().getOpt());
        if (StringUtils.isBlank(agent)) {
            return null;
        }
        String preset = PostAnalysisHook.aiPresetCommand(agent, cmd.getOptionValue(commands.getAiPrompt().getOpt()));
        if (preset == null) {
            LOG.error("-" + Commands.ARG_AI + " must be one of " + PostAnalysisHook.AGENTS + ", got '" + agent + "'; no post-analysis command is run.");
        } else {
            String hint = SkillsInstaller.missingSkillsHint(agent);
            if (hint != null) {
                LOG.warn(hint);
            }
        }
        return preset;
    }

    // How many times this run has executed the post-analysis command (bounded by -aiMaxRepos).
    private int postAnalysisRuns = 0;

    /**
     * Runs the -postAnalysis / -ai command unless it would redo work: the kept analysis'
     * post-analysis.json says which command last ran on which head commit, and an unchanged
     * repository is skipped (-aiForce runs it anyway); -aiMaxRepos bounds the runs per invocation.
     * The state is written into the analysis folder, so for a clone it moves to the kept folder
     * with the rest; a skipped repository keeps its earlier state file.
     */
    private void runPostAnalysisHook(CommandLine cmd, File root, File conf, File reportsFolder, String repoUrl, File keptFolder) {
        String command = postAnalysisCommand(cmd);
        if (command == null) {
            return;
        }
        File analysisFolder = conf.getParentFile();
        String head = PostAnalysisState.headCommit(root);
        String skipReason = postAnalysisSkipReason(cmd, command, head, PostAnalysisState.read(keptFolder != null ? keptFolder : analysisFolder));
        if (skipReason != null) {
            LOG.info("Post-analysis command skipped: " + skipReason);
            return;
        }
        postAnalysisRuns++;
        GitRepoMetadata metadata = StringUtils.isNotBlank(repoUrl) ? GitRepoMetadata.fromUrl(repoUrl) : null;
        String repoName = metadata != null ? metadata.reportName() : root.toPath().toAbsolutePath().normalize().getFileName().toString();
        ProcessingStopwatch.start("post-analysis command");
        int exit = PostAnalysisHook.run(command, root, PostAnalysisHook.environment(repoUrl, repoName, root, analysisFolder, reportsFolder, keptFolder));
        ProcessingStopwatch.end("post-analysis command");
        try {
            new PostAnalysisState(command, head, DateUtils.getAnalysisDate(), exit).save(analysisFolder);
        } catch (IOException e) {
            LOG.warn("Could not record the post-analysis run in " + analysisFolder.getPath() + ": " + e.getMessage());
        }
    }

    /** Why the hook is not run this time: an earlier successful run at the same head (unless -aiForce), or the -aiMaxRepos budget; null to run it. */
    private String postAnalysisSkipReason(CommandLine cmd, String command, String head, PostAnalysisState previous) {
        if (previous != null && previous.covers(command, head) && !cmd.hasOption(commands.getAiForce().getOpt())) {
            return "unchanged since it ran on " + previous.getRanOn() + " (head " + head.substring(0, Math.min(10, head.length()))
                    + "); -" + Commands.ARG_AI_FORCE + " runs it anyway.";
        }
        Integer max = OrganizationCommands.nonNegativeIntOption(cmd, commands.getAiMaxRepos());
        if (max != null && max > 0 && postAnalysisRuns >= max) {
            return "the -" + Commands.ARG_AI_MAX_REPOS + " budget of " + max + " repositories is used up for this run.";
        }
        return null;
    }

    /**
     * Titles and links the report after the repository rather than the folder: fills the metadata
     * of the configuration from the git origin remote (name when it is blank or still the
     * folder-derived init default — under Docker every code base sits in /code and used to be
     * called "Code" — description, logo and a link to the repository when blank). User-set values
     * are never overwritten; a configuration without a git origin is left untouched.
     */
    private void applyGitRepoMetadata(File root, File conf) {
        applyGitRepoMetadata(root, conf, null);
    }

    /** @param listed what the code host's listing said about this repository (description, avatar), or null to ask the GitHub API */
    private void applyGitRepoMetadata(File root, File conf, CodeHostRepo listed) {
        if (!conf.exists()) {
            return;
        }
        GitRepoMetadata gitMetadata = GitRepoMetadata.fromLocalRepository(root);
        if (gitMetadata == null) {
            return;
        }
        if (listed != null) {
            gitMetadata.withDetails(listed.getDescription(), listed.getAvatarUrl());
        }
        try {
            String folderDefaultName = StringUtils.capitalize(root.getCanonicalFile().getName().toLowerCase());
            String json = FileUtils.readFileToString(conf, UTF_8);
            CodeConfiguration configuration = (CodeConfiguration) new JsonMapper().getObject(json, CodeConfiguration.class);
            if (configuration == null) {
                return;
            }
            if (gitMetadata.fetchDetails().applyTo(configuration.getMetadata(), folderDefaultName)) {
                FileUtils.writeStringToFile(conf, new JsonGenerator().generate(configuration), UTF_8);
                LOG.info("Report metadata taken from the git remote " + gitMetadata.getRemoteUrl() + ": name '" + configuration.getMetadata().getName() + "'"
                        + (StringUtils.isNotBlank(configuration.getMetadata().getLogoLink()) ? ", logo" : "")
                        + (StringUtils.isNotBlank(configuration.getMetadata().getDescription()) ? ", description" : "")
                        + (configuration.getMetadata().getLinks().isEmpty() ? "" : ", link"));
            }
        } catch (IOException e) {
            LOG.info("Could not update the report metadata from the git remote: " + e.getMessage());
        }
    }

    private File getSrcRoot(CommandLine cmd) {
        String strRootPath = cmd.getOptionValue(commands.getSrcRoot().getOpt());
        if (!cmd.hasOption(commands.getSrcRoot().getOpt())) {
            strRootPath = ".";
        }
        return new File(strRootPath);
    }

    private void createConfiguration(CommandLine cmd, File root, File conf) throws IOException {
        CustomScopingConventions customScopingConventions = null;
        if (cmd.hasOption(commands.getConventionsFile().getOpt())) {
            File scopingConventionsFile = new File(cmd.getOptionValue(commands.getConventionsFile().getOpt()));
            if (scopingConventionsFile.exists()) {
                customScopingConventions = CustomConventionsHelper.readFromFile(scopingConventionsFile);
            }
        }
        String nameValue = "";
        String descriptionValue = "";
        String logoLinkValue = "";
        if (cmd.hasOption(commands.getName().getOpt())) {
            nameValue = cmd.getOptionValue(commands.getName().getOpt());
        }
        if (cmd.hasOption(commands.getDescription().getOpt())) {
            descriptionValue = cmd.getOptionValue(commands.getDescription().getOpt());
        }
        if (cmd.hasOption(commands.getLogoLink().getOpt())) {
            logoLinkValue = cmd.getOptionValue(commands.getLogoLink().getOpt());
        }
        Link link = null;
        if (cmd.hasOption(commands.getAddLink().getOpt())) {
            String[] linkData = cmd.getOptionValues(commands.getAddLink().getOpt());
            if (linkData.length >= 1 && StringUtils.isNotBlank(linkData[0])) {
                String href = linkData[0];
                String label = linkData.length > 1 ? linkData[1] : "";
                link = new Link(label, href);
            }
        }

        new ScopeCreator(root, conf, customScopingConventions).createScopeFromConventions(nameValue, descriptionValue, logoLinkValue, link);

        LOG.info("Configuration stored in " + conf.getPath());
    }

    void startTimeoutIfDefined(CommandLine cmd) {
        String timeoutSeconds = cmd.getOptionValue(commands.getTimeout().getOpt());
        if (StringUtils.isNumeric(timeoutSeconds)) {
            int seconds = Integer.parseInt(timeoutSeconds);
            LOG.info("Timeout timer set to " + seconds + " seconds.");
            Executors.newCachedThreadPool().execute(() -> {
                try {
                    Thread.sleep(seconds * 1000L);
                    LOG.info("Timeout after " + seconds + " seconds.");
                    System.exit(-1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void updateConfig(String[] args) throws ParseException, IOException {
        Options options = commands.getUpdateConfigOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.UPDATE_CONFIG, commands.getUpdateConfigOptions(), Commands.UPDATE_CONFIG_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(commands.getSrcRoot().getOpt());
        if (!cmd.hasOption(commands.getSrcRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File confFile = getConfigFile(cmd, root);
        LOG.info("Configuration file '" + confFile.getPath() + "'.");

        String jsonContent = FileUtils.readFileToString(confFile, UTF_8);
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);

        applyAnalysisOptions(cmd, codeConfiguration);

        Metadata metadata = codeConfiguration.getMetadata();
        updateMetadataFromCommandLine(cmd, metadata);

        String cacheFileValue = optionValueOrNull(cmd, commands.getSetCacheFiles());
        if (cacheFileValue != null) {
            codeConfiguration.getAnalysis().setSaveSourceFiles(cacheFileValue.equalsIgnoreCase("true"));
        }

        FileUtils.write(confFile, new JsonGenerator().generate(codeConfiguration), UTF_8);
    }

    /** The -skipComplexAnalyses / -skipDuplicationAnalyses / -skipCorrelationAnalyses / -enableDuplicationAnalyses switches. */
    private void applyAnalysisOptions(CommandLine cmd, CodeConfiguration codeConfiguration) {
        if (cmd.hasOption(commands.getSkipComplexAnalyses().getOpt())) {
            codeConfiguration.getAnalysis().setSkipDependencies(true);
            codeConfiguration.getAnalysis().setSkipDuplication(true);
            codeConfiguration.getAnalysis().setSkipCorrelations(true);
            codeConfiguration.getAnalysis().setSaveSourceFiles(false);
        }

        if (cmd.hasOption(commands.getSkipDuplicationAnalyses().getOpt())) {
            codeConfiguration.getAnalysis().setSkipDuplication(true);
        }

        if (cmd.hasOption(commands.getSkipCorrelationAnalyses().getOpt())) {
            codeConfiguration.getAnalysis().setSkipCorrelations(true);
        }

        if (cmd.hasOption(commands.getEnableDuplicationAnalyses().getOpt())) {
            codeConfiguration.getAnalysis().setSkipDuplication(false);
        }
    }

    private void installSkills(String[] args) throws ParseException, IOException {
        Options options = commands.getInstallSkillsOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);
        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.INSTALL_SKILLS, options, Commands.INSTALL_SKILLS_DESCRIPTION);
            return;
        }
        String source = StringUtils.defaultIfBlank(cmd.getOptionValue(commands.getSource().getOpt()), SkillsInstaller.DEFAULT_SOURCE);
        String ref = StringUtils.defaultIfBlank(cmd.getOptionValue(commands.getRef().getOpt()), SkillsInstaller.DEFAULT_REF);
        File cache = cmd.hasOption(commands.getCacheFolder().getOpt()) ? new File(cmd.getOptionValue(commands.getCacheFolder().getOpt())) : SkillsInstaller.defaultCacheFolder();
        List<File> targets = skillTargets(cmd);
        SkillsInstaller installer = new SkillsInstaller();
        List<File> skills = fetchSkills(installer, source, ref, cache);
        if (skills == null || cmd.hasOption(commands.getListOnly().getOpt())) {
            return;
        }
        boolean copy = cmd.hasOption(commands.getCopy().getOpt());
        for (File target : targets) {
            List<File> installed = installer.installInto(skills, target, copy);
            LOG.info((copy ? "Copied " : "Linked ") + installed.size() + " skills into " + target.getPath());
        }
        LOG.info("Done. Ask your agent to \"use the sokrates skill\" in a repository, or run an analysis with -ai claude|codex|gemini.");
    }

    /** The -target folders, else the project's skill folders with -project, else the agents' default folders. */
    private List<File> skillTargets(CommandLine cmd) {
        List<File> targets = new ArrayList<>();
        if (cmd.hasOption(commands.getTarget().getOpt())) {
            for (String folder : cmd.getOptionValues(commands.getTarget().getOpt())) {
                targets.add(new File(folder));
            }
        } else if (cmd.hasOption(commands.getProject().getOpt())) {
            targets.addAll(SkillsInstaller.projectTargets(new File(".")));
        } else {
            targets.addAll(SkillsInstaller.defaultTargets());
        }
        return targets;
    }

    /** The skills of the fetched source, logged; null (after logging) when the fetch fails or the source has none. */
    private static List<File> fetchSkills(SkillsInstaller installer, String source, String ref, File cache) {
        File root;
        try {
            root = installer.fetch(source, ref, cache);
        } catch (GitAPIException | IOException e) {
            LOG.error("Could not fetch the skills from " + source + ": " + e.getMessage());
            return null;
        }
        List<File> skills = SkillsInstaller.findSkills(root);
        if (skills.isEmpty()) {
            LOG.error("No skills (folders with a SKILL.md under skills/) found in " + root.getPath());
            return null;
        }
        LOG.info(skills.size() + " skills in " + root.getPath() + ": " + skills.stream().map(File::getName).collect(Collectors.joining(", ")));
        return skills;
    }

    private void addCustomTab(String[] args) throws ParseException, IOException {
        Options options = commands.getAddCustomTabOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.ADD_CUSTOM_TAB, options, Commands.ADD_CUSTOM_TAB_DESCRIPTION);
            return;
        }

        String label = cmd.getOptionValue(commands.getLabel().getOpt());
        String iframeLink = cmd.getOptionValue(commands.getIframeLink().getOpt());
        if (StringUtils.isBlank(label) || StringUtils.isBlank(iframeLink)) {
            LOG.error("Both -" + Commands.ARG_LABEL + " and -" + Commands.ARG_IFRAME_LINK + " are required.");
            commands.usage(Commands.ADD_CUSTOM_TAB, options, Commands.ADD_CUSTOM_TAB_DESCRIPTION);
            return;
        }

        File confFile = getConfigFile(cmd, new File("."));
        if (!confFile.exists()) {
            LOG.error("The configuration file \"" + confFile.getPath() + "\" does not exist.");
            return;
        }
        LOG.info("Configuration file '" + confFile.getPath() + "'.");

        String jsonContent = FileUtils.readFileToString(confFile, UTF_8);
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);

        boolean replaced = codeConfiguration.addOrReplaceCustomTab(new CustomTab(label.trim(), iframeLink.trim()));
        LOG.info((replaced ? "Replaced" : "Added") + " custom tab '" + label.trim() + "' -> " + iframeLink.trim());

        FileUtils.write(confFile, new JsonGenerator().generate(codeConfiguration), UTF_8);
    }

    void updateMetadataFromCommandLine(CommandLine cmd, Metadata metadata) {
        String name = optionValueOrNull(cmd, commands.getSetName());
        if (name != null) {
            metadata.setName(name);
        }
        String description = optionValueOrNull(cmd, commands.getSetDescription());
        if (description != null) {
            metadata.setDescription(description);
        }
        String logoLink = optionValueOrNull(cmd, commands.getSetLogoLink());
        if (logoLink != null) {
            metadata.setLogoLink(logoLink);
        }
        if (cmd.hasOption(commands.getAddLink().getOpt())) {
            String[] linkData = cmd.getOptionValues(commands.getAddLink().getOpt());
            if (linkData.length >= 1 && StringUtils.isNotBlank(linkData[0])) {
                String href = linkData[0];
                String label = linkData.length > 1 ? linkData[1] : "";
                metadata.getLinks().add(new Link(label, href));
            }
        }
    }

    /** The option's value when given and not blank, else null. */
    private static String optionValueOrNull(CommandLine cmd, Option option) {
        if (!cmd.hasOption(option.getOpt())) {
            return null;
        }
        String value = cmd.getOptionValue(option.getOpt());
        return StringUtils.isNotBlank(value) ? value : null;
    }

    private void exportConventions(String[] args) throws ParseException, IOException {
        File file = new File("standard_analysis_conventions.json");

        CustomConventionsHelper.saveStandardConventionsToFile(file);

        LOG.info("A standard conventions file saved to '" + file.getPath() + "'.");
    }

    private void createNewConventionsFile(String[] args) throws ParseException, IOException {
        File file = new File("analysis_conventions.json");

        CustomConventionsHelper.saveEmptyConventionsToFile(file);

        LOG.info("A new conventions file saved to '" + file.getPath() + "'.");
    }

    private File getConfigFile(CommandLine cmd, File root) {
        File conf;
        if (cmd.hasOption(commands.getConfFile().getOpt())) {
            conf = new File(cmd.getOptionValue(commands.getConfFile().getOpt()));
        } else {
            conf = CodeConfigurationUtils.getDefaultSokratesConfigFile(root);
        }
        return conf;
    }

    private void generateReports(CommandLine cmd) throws IOException {
        File sokratesConfigFile;
        if (!cmd.hasOption(commands.getConfFile().getOpt())) {
            String confFilePath = "./_sokrates/config.json";
            sokratesConfigFile = new File(confFilePath);
        } else {
            sokratesConfigFile = new File(cmd.getOptionValue(commands.getConfFile().getOpt()));
        }

        File reportsFolder;
        if (!cmd.hasOption(commands.getOutputFolder().getOpt())) {
            reportsFolder = prepareReportsFolder("./_sokrates/reports");
        } else {
            reportsFolder = prepareReportsFolder(cmd.getOptionValue(commands.getOutputFolder().getOpt()));
        }

        generateReports(cmd, sokratesConfigFile, reportsFolder);
    }

    private void generateReports(CommandLine cmd, File sokratesConfigFile, File reportsFolder) throws IOException {
        updateDateParam(cmd);

        LOG.info("Configuration file: " + sokratesConfigFile.getPath());
        if (noFileError(sokratesConfigFile)) return;

        ProcessingStopwatch.start("configuring");
        String jsonContent = FileUtils.readFileToString(sokratesConfigFile, UTF_8);
        this.codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);
        // Safety net for configurations written before init always ignored the analysis output (in memory only; the file is the user's).
        ScopingConventions.ensureSokratesOutputIgnored(this.codeConfiguration.getIgnore());
        LanguageAnalyzerFactory.getInstance().setOverrides(codeConfiguration.getAnalysis().getAnalyzerOverrides());

        detailedInfo("Starting analysis based on the configuration file " + sokratesConfigFile.getPath());

        LOG.info("Reports folder: " + reportsFolder.getPath());
        ProcessingStopwatch.end("configuring");
        if (noFileError(reportsFolder)) return;

        if (this.progressFeedback == null) {
            this.progressFeedback = new ProgressFeedback() {
                public void setText(String text) {
                    LOG.info(text.replaceAll("<.*?>", ""));
                }

                public void setDetailedText(String text) {
                    LOG.info(text.replaceAll("<.*?>", ""));
                }
            };
        }

        boolean dataOnly = cmd.hasOption(commands.getDataOnly().getOpt());
        dataExporter.setDataOnly(dataOnly);
        if (dataOnly) {
            LOG.info("-" + Commands.ARG_DATA_ONLY + ": storing only data/data.zip (no HTML reports, explorers, visuals or source viewer).");
        }

        try {
            CodeAnalyzer codeAnalyzer = new CodeAnalyzer(getCodeAnalyzerSettings(cmd), codeConfiguration, sokratesConfigFile);
            CodeAnalysisResults analysisResults = codeAnalyzer.analyze(progressFeedback);

            ProcessingStopwatch.start("saving data");
            dataExporter.saveData(sokratesConfigFile, codeConfiguration, reportsFolder, analysisResults);
            saveTextualSummary(reportsFolder, analysisResults);
            ProcessingStopwatch.end("saving data");

            if (!dataOnly) {
                ProcessingStopwatch.start("generating visuals");
                new ReportVisualsGenerator(codeConfiguration).generateVisuals(reportsFolder, analysisResults);
                ProcessingStopwatch.end("generating visuals");

                generateAndSaveReports(sokratesConfigFile, reportsFolder, sokratesConfigFile.getParentFile(), codeAnalyzer, analysisResults);
            }
            saveExecutionStats(dataExporter.getDataFolder());
            // Final data step: package the whole data/ folder (incl. textual summary + execution
            // stats just written) into a single data/data.zip; the reports + landscape read from it.
            dataExporter.zipDataFolder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void saveExecutionStats(File dataFolder) {
        try {
            List<ProcessingTimes> monitors = ProcessingStopwatch.getMonitors();

            String json = new JsonGenerator().generate(monitors);
            List<String> lines = monitors.stream().map(m -> m.getDurationMs() / 1000.0 + "s => " + m.getProcessing() + " " + ProcessingStopwatch.getPercentage(m.getDurationMs())).collect(Collectors.toList());
            String text = lines.stream().map(l -> StringUtils.repeat("  ", StringUtils.countMatches(l, '/')) + l).collect(Collectors.joining("\n"));

            FileUtils.write(new File(dataFolder, "executionTimes.json"), json, UTF_8);
            FileUtils.write(new File(dataFolder, "executionTimes.txt"), text, UTF_8);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private boolean noFileError(File inputFile) {
        if (!inputFile.exists()) {
            LOG.info("ERROR: " + inputFile.getPath() + " does not exist.");
            return true;
        }
        return false;
    }

    private boolean noReportingOptions(CommandLine cmd) {
        for (Option arg : cmd.getOptions()) {
            if (arg.getOpt().toLowerCase().startsWith("report")) {
                return false;
            }
        }
        return true;
    }

    private void info(String text) {
        if (progressFeedback != null) {
            progressFeedback.setText(text);
        } else {
            LOG.info(text.replaceAll("<.*?>", ""));
        }
    }

    public void detailedInfo(String text) {
        LOG.info(text);
        if (progressFeedback != null) {
            progressFeedback.setDetailedText(text);
        }
    }

    private void generateAndSaveReports(File inputFile, File reportsFolder, File sokratesConfigFolder, CodeAnalyzer codeAnalyzer, CodeAnalysisResults analysisResults) {
        File htmlReports = getHtmlFolder(reportsFolder);
        File dataReports = dataExporter.getDataFolder();
        File srcCache = dataExporter.getCodeCacheFolder();
        CodeAnalyzerSettings codeAnalyzerSettings = codeAnalyzer.getCodeAnalyzerSettings();
        if (new File(htmlReports, "index.html").exists() || codeAnalyzerSettings.isUpdateIndex()) {
            info("HTML reports: <a href='" + htmlReports.getPath() + "/index.html'>" + htmlReports.getPath() + "</a>");
        } else {
            info("HTML reports: <a href='" + htmlReports.getPath() + "'>" + htmlReports.getPath() + "</a>");
        }
        info("Raw data: <a href='" + dataReports.getPath() + "'>" + dataReports.getPath() + "</a>");
        if (analysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles()) {
            info("Source code cache : <a href='" + srcCache.getPath() + "'>" + srcCache.getPath() + "</a>");
        }
        ProcessingStopwatch.start("reporting");
        BasicSourceCodeReportGenerator generator = new BasicSourceCodeReportGenerator(codeAnalyzerSettings, analysisResults, inputFile, reportsFolder);
        List<RichTextReport> reports = generator.report();
        ProcessingStopwatch.end("reporting");

        ProcessingStopwatch.start("saving report");
        reports.forEach(report -> {
            info("Generating the '" + report.getId().toUpperCase() + "' report...");
            String processingName = "saving report/" + report.getId().toLowerCase() + "";
            ProcessingStopwatch.start(processingName);
            ReportFileExporter.exportHtml(reportsFolder, "html", report, analysisResults.getCodeConfiguration().getAnalysis().getCustomHtmlReportHeaderFragment());
            ProcessingStopwatch.end(processingName);
        });
        ProcessingStopwatch.start("saving report/index");
        if (!codeAnalyzerSettings.isDataOnly() && codeAnalyzerSettings.isUpdateIndex()) {
            ReportFileExporter.exportReportsIndexFile(reportsFolder, analysisResults, sokratesConfigFolder);
        }
        ProcessingStopwatch.end("saving report/index");
        ProcessingStopwatch.start("saving report/explorer");
        FilesExplorerGenerators filesExplorerGenerators = new FilesExplorerGenerators(reportsFolder);
        filesExplorerGenerators.exportJson(analysisResults);
        UnitsExplorerGenerators unitsExplorerGenerators = new UnitsExplorerGenerators(reportsFolder);
        unitsExplorerGenerators.exportJson(analysisResults);
        CommitsExplorerGenerators commitsExplorerGenerators = new CommitsExplorerGenerators(reportsFolder);
        commitsExplorerGenerators.exportJson(analysisResults, sokratesConfigFolder);
        ProcessingStopwatch.end("saving report/explorer");
        ProcessingStopwatch.end("saving report");
    }

    private File getHtmlFolder(File reportsFolder) {
        File folder = new File(reportsFolder, Commands.ARG_HTML_REPORTS_FOLDER_NAME);
        folder.mkdirs();
        return folder;
    }

    private void saveTextualSummary(File reportsFolder, CodeAnalysisResults analysisResults) throws IOException {
        File jsonFile = new File(dataExporter.getTextDataFolder(), "textualSummary.txt");
        FileUtils.write(jsonFile, analysisResults.getTextSummary().toString(), UTF_8);
    }

    private File prepareReportsFolder(String path) throws IOException {
        File reportsFolder = new File(path);
        reportsFolder.mkdirs();

        return reportsFolder;
    }

    private CodeAnalyzerSettings getCodeAnalyzerSettings(CommandLine cmd) {
        CodeAnalyzerSettings settings = new CodeAnalyzerSettings();
        settings.setDataOnly(cmd.hasOption(commands.getDataOnly().getOpt()));

        if (codeConfiguration.getAnalysis().isSkipDependencies()) {
            settings.setAnalyzeStaticDependencies(false);
        }

        return settings;
    }

    public void setProgressFeedback(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }

    public void setGitHubOrgClient(CodeHostOrgClient client) {
        organizationCommands.setGitHubOrgClient(client);
    }

    public void setGitLabGroupClient(CodeHostOrgClient client) {
        organizationCommands.setGitLabGroupClient(client);
    }

}
