/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.cli;

import nl.obren.sokrates.cli.git.GitHistoryExtractor;
import nl.obren.sokrates.cli.git.CodeHostOrgClient;
import nl.obren.sokrates.cli.git.CodeHostRepo;
import nl.obren.sokrates.cli.git.GitRepoMetadata;
import nl.obren.sokrates.cli.skills.SkillsInstaller;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.*;
import nl.obren.sokrates.reports.generators.explorers.AiInsightsExplorerGenerator;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.scoping.ScopeCreator;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomConventionsHelper;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomScopingConventions;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommandLineInterface {
    private static final Log LOG = LogFactory.getLog(CommandLineInterface.class);
    private ProgressFeedback progressFeedback;
    // The code-host APIs behind analyzeGitHubOrg / analyzeGitLabGroup; replaceable so tests run offline
    // against local repositories (the GitLab one is created per run from -gitlabUrl unless injected).

    private final Commands commands = new Commands();
    final GitHistoryCommands gitHistoryCommands = new GitHistoryCommands(this, commands);
    final PeopleConfigCommands peopleConfigCommands = new PeopleConfigCommands(this, commands);
    final SkillsCommands skillsCommands = new SkillsCommands(this, commands);
    final ReportsCommands reportsCommands = new ReportsCommands(this, commands);
    final ConfigCommands configCommands = new ConfigCommands(this, commands);
    final GitRepoCommands gitRepoCommands = new GitRepoCommands(this, commands);
    final LandscapeCommands landscapeCommands = new LandscapeCommands(this, commands);
    private final OrganizationCommands organizationCommands = new OrganizationCommands(this, commands);

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
        handlers.put(Commands.GENERATE_REPORTS, reportsCommands::generateReports);
        // Same implementation as updateLandscape; analyzeLandscape is the name that mirrors analyze.
        handlers.put(Commands.ANALYZE_LANDSCAPE, args -> landscapeCommands.updateLandscape(args, Commands.ANALYZE_LANDSCAPE, Commands.ANALYZE_LANDSCAPE_DESCRIPTION));
        handlers.put(Commands.UPDATE_LANDSCAPE, args -> landscapeCommands.updateLandscape(args, Commands.UPDATE_LANDSCAPE, Commands.UPDATE_LANDSCAPE_DESCRIPTION));
        handlers.put(Commands.ANALYZE_GITHUB_ORG, organizationCommands::analyzeGitHubOrg);
        handlers.put(Commands.ANALYZE_GITLAB_GROUP, organizationCommands::analyzeGitLabGroup);
        handlers.put(Commands.UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME, peopleConfigCommands::updateLandscapePeopleConfigByUserName);
        handlers.put(Commands.UPDATE_PEOPLE_CONFIG_BY_USER_NAME, peopleConfigCommands::updatePeopleConfigByUserName);
        handlers.put(Commands.UPDATE_CONFIG, configCommands::updateConfig);
        handlers.put(Commands.ADD_CUSTOM_TAB, configCommands::addCustomTab);
        handlers.put(Commands.INSTALL_SKILLS, skillsCommands::installSkills);
        handlers.put(Commands.EXTRACT_GIT_HISTORY, gitHistoryCommands::extractGitHistory);
        handlers.put(Commands.INIT_CONVENTIONS, configCommands::createNewConventionsFile);
        handlers.put(Commands.EXPORT_STANDARD_CONVENTIONS, configCommands::exportConventions);
        handlers.put(Commands.EXTRACT_GIT_SUB_HISTORY, gitHistoryCommands::extractGitSubHistory);
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
            reportsFolder = reportsCommands.prepareReportsFolder(cmd.getOptionValue(commands.getOutputFolder().getOpt()));
        } else {
            reportsFolder = reportsCommands.prepareReportsFolder(new File(conf.getParentFile(), "reports").getPath());
        }

        reportsCommands.generateReports(cmd, conf, reportsFolder);
        if (runPostAnalysisHook(cmd, root, conf, reportsFolder, repoUrl, keptFolder)
                && !cmd.hasOption(commands.getDataOnly().getOpt())) {
            // The reports were written before the hook ran; render them again so they link the new findings.
            LOG.info("The post-analysis command changed the AI findings in " + new File(reportsFolder, AiInsightsExplorerGenerator.INSIGHTS_FOLDER).getPath()
                    + ": generating the reports again to include them.");
            reportsCommands.generateReports(cmd, conf, reportsFolder);
        }
        return reportsFolder;
    }

    // The AI findings files in <reports>/ai-insights (name, size and time of each *.json), to see whether the hook changed them.
    static String aiFindingsFingerprint(File reportsFolder) {
        File[] files = new File(reportsFolder, AiInsightsExplorerGenerator.INSIGHTS_FOLDER).listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) {
            return "";
        }
        Arrays.sort(files);
        StringBuilder fingerprint = new StringBuilder();
        for (File file : files) {
            fingerprint.append(file.getName()).append(':').append(file.length()).append(':').append(file.lastModified()).append('\n');
        }
        return fingerprint.toString();
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
     *
     * @return whether the command ran and changed the AI findings in the reports folder
     */
    private boolean runPostAnalysisHook(CommandLine cmd, File root, File conf, File reportsFolder, String repoUrl, File keptFolder) {
        String command = postAnalysisCommand(cmd);
        if (command == null) {
            return false;
        }
        File analysisFolder = conf.getParentFile();
        String head = PostAnalysisState.headCommit(root);
        String skipReason = postAnalysisSkipReason(cmd, command, head, PostAnalysisState.read(keptFolder != null ? keptFolder : analysisFolder));
        if (skipReason != null) {
            LOG.info("Post-analysis command skipped: " + skipReason);
            return false;
        }
        postAnalysisRuns++;
        String findingsBefore = aiFindingsFingerprint(reportsFolder);
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
        return !findingsBefore.equals(aiFindingsFingerprint(reportsFolder));
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
                LOG.info("Report metadata taken from the git remote " + gitMetadata.getRemoteUrl() + ": " + appliedMetadataSummary(configuration.getMetadata()));
            }
        } catch (IOException e) {
            LOG.info("Could not update the report metadata from the git remote: " + e.getMessage());
        }
    }

    /** "name '<name>'" plus which of logo, description and link the metadata now carries. */
    private static String appliedMetadataSummary(Metadata metadata) {
        return "name '" + metadata.getName() + "'"
                + (StringUtils.isNotBlank(metadata.getLogoLink()) ? ", logo" : "")
                + (StringUtils.isNotBlank(metadata.getDescription()) ? ", description" : "")
                + (metadata.getLinks().isEmpty() ? "" : ", link");
    }

    File getSrcRoot(CommandLine cmd) {
        String strRootPath = cmd.getOptionValue(commands.getSrcRoot().getOpt());
        if (!cmd.hasOption(commands.getSrcRoot().getOpt())) {
            strRootPath = ".";
        }
        return new File(strRootPath);
    }

    private void createConfiguration(CommandLine cmd, File root, File conf) throws IOException {
        CustomScopingConventions customScopingConventions = customScopingConventions(cmd);
        String nameValue = optionValueOrEmpty(cmd, commands.getName());
        String descriptionValue = optionValueOrEmpty(cmd, commands.getDescription());
        String logoLinkValue = optionValueOrEmpty(cmd, commands.getLogoLink());
        Link link = linkFromCommandLine(cmd);

        new ScopeCreator(root, conf, customScopingConventions).createScopeFromConventions(nameValue, descriptionValue, logoLinkValue, link);

        LOG.info("Configuration stored in " + conf.getPath());
    }

    /** The -conventionsFile conventions when the option names an existing file, else null (the standard conventions). */
    private CustomScopingConventions customScopingConventions(CommandLine cmd) throws IOException {
        if (cmd.hasOption(commands.getConventionsFile().getOpt())) {
            File scopingConventionsFile = new File(cmd.getOptionValue(commands.getConventionsFile().getOpt()));
            if (scopingConventionsFile.exists()) {
                return CustomConventionsHelper.readFromFile(scopingConventionsFile);
            }
        }
        return null;
    }

    /** The option's value when given, else the empty string. */
    private static String optionValueOrEmpty(CommandLine cmd, Option option) {
        return cmd.hasOption(option.getOpt()) ? cmd.getOptionValue(option.getOpt()) : "";
    }

    /** The -addLink option as a link (href, optional label), null when absent or without an href. */
    private Link linkFromCommandLine(CommandLine cmd) {
        if (cmd.hasOption(commands.getAddLink().getOpt())) {
            String[] linkData = cmd.getOptionValues(commands.getAddLink().getOpt());
            if (linkData.length >= 1 && StringUtils.isNotBlank(linkData[0])) {
                String href = linkData[0];
                String label = linkData.length > 1 ? linkData[1] : "";
                return new Link(label, href);
            }
        }
        return null;
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
        Link link = linkFromCommandLine(cmd);
        if (link != null) {
            metadata.getLinks().add(link);
        }
    }

    /** The option's value when given and not blank, else null. */
    static String optionValueOrNull(CommandLine cmd, Option option) {
        if (!cmd.hasOption(option.getOpt())) {
            return null;
        }
        String value = cmd.getOptionValue(option.getOpt());
        return StringUtils.isNotBlank(value) ? value : null;
    }

    File getConfigFile(CommandLine cmd, File root) {
        File conf;
        if (cmd.hasOption(commands.getConfFile().getOpt())) {
            conf = new File(cmd.getOptionValue(commands.getConfFile().getOpt()));
        } else {
            conf = CodeConfigurationUtils.getDefaultSokratesConfigFile(root);
        }
        return conf;
    }

    void info(String text) {
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

    ProgressFeedback progressFeedback() {
        return progressFeedback;
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
