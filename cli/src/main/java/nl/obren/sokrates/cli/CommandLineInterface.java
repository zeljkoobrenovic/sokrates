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
    private CodeHostOrgClient gitHubOrgClient = new GitHubOrgClient();
    private CodeHostOrgClient gitLabGroupClient = null;
    private final DataExporter dataExporter = new DataExporter(this.progressFeedback);

    private final Commands commands = new Commands();
    private CodeConfiguration codeConfiguration;

    private static boolean helpMode = false;

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

        try {
            if (args[0].equalsIgnoreCase(Commands.ANALYZE)) {
                analyze(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.ANALYZE_GIT_REPO)) {
                analyzeGitRepo(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.INIT)) {
                init(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.UPDATE_CONFIG)) {
                updateConfig(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.ADD_CUSTOM_TAB)) {
                addCustomTab(args);
            } else if (args[0].equalsIgnoreCase(Commands.INSTALL_SKILLS)) {
                installSkills(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.EXPORT_STANDARD_CONVENTIONS)) {
                exportConventions(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.ANALYZE_LANDSCAPE)) {
                // Same implementation as updateLandscape; analyzeLandscape is the name that mirrors analyze.
                updateLandscape(args, Commands.ANALYZE_LANDSCAPE, Commands.ANALYZE_LANDSCAPE_DESCRIPTION);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.UPDATE_LANDSCAPE)) {
                updateLandscape(args, Commands.UPDATE_LANDSCAPE, Commands.UPDATE_LANDSCAPE_DESCRIPTION);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.ANALYZE_GITHUB_ORG)) {
                analyzeGitHubOrg(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.ANALYZE_GITLAB_GROUP)) {
                analyzeGitLabGroup(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME)) {
                updateLandscapePeopleConfigByUserName(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.UPDATE_PEOPLE_CONFIG_BY_USER_NAME)) {
                updatePeopleConfigByUserName(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.INIT_CONVENTIONS)) {
                createNewConventionsFile(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.EXTRACT_GIT_SUB_HISTORY)) {
                extractGitSubHistory(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.EXTRACT_FILES)) {
                extractFiles(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.EXTRACT_GIT_HISTORY)) {
                extractGitHistory(args);
                return;
            } else if (!args[0].equalsIgnoreCase(Commands.GENERATE_REPORTS)) {
                helpMode = true;
                commands.usage();
                return;
            }

            generateReports(args);
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

    private void updateDateParam(CommandLine cmd) {
        String dateString = cmd.getOptionValue(commands.getDate().getOpt());
        if (dateString != null) {
            LOG.info("Using '" + dateString + "' as latest source code update date for active contributors reports.");
            DateUtils.setDateParam(dateString);
        }
    }

    private void updateLandscape(String[] args, String commandName, String commandDescription) throws ParseException, IOException {
        Options options = commands.getUpdateLandscapeOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(commandName, commands.getUpdateLandscapeOptions(), commandDescription);
            return;
        }

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(commands.getAnalysisRoot().getOpt());
        if (!cmd.hasOption(commands.getAnalysisRoot().getOpt())) {
            strRootPath = ".";
        }

        List<String> urls = collectRepositoryUrls(cmd);
        if (urls == null) {
            return;
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            if (urls.isEmpty()) {
                LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
                return;
            }
            // With repository URLs the root is where the analyses will be created.
            root.mkdirs();
        }

        Metadata metadata = new Metadata();

        updateMetadataFromCommandLine(cmd, metadata);

        String confFilePath = cmd.getOptionValue(commands.getConfFile().getOpt());
        updateDateParam(cmd);

        // Applies to the per-URL repository analyses (analyzeGitRepoInto reads it from cmd) AND to the
        // landscape itself: its folder then holds only the config files and data/data.zip.
        boolean dataOnly = cmd.hasOption(commands.getDataOnly().getOpt());
        if (dataOnly) {
            LOG.info("-" + Commands.ARG_DATA_ONLY + ": storing only the landscape's data/data.zip (no index page, contributor pages, explorers or visuals).");
        }

        boolean prune = cmd.hasOption(commands.getPrune().getOpt());
        if (prune && urls.isEmpty()) {
            LOG.warn("-" + Commands.ARG_PRUNE + " only applies together with -" + Commands.ARG_URL + " / -" + Commands.ARG_URLS + " (the list says which analyses are current); ignored.");
        }
        if (!urls.isEmpty()) {
            RepositoryBatch batch = analyzeRepositoriesIntoLandscape(cmd, root, urls, commandName);
            if (batch.nothingAnalyzed()) {
                return;
            }
            pruneManagedAnalyses(root, urls, batch.notFound, prune);
        }

        if (cmd.hasOption(commands.getRecursive().getOpt())) {
            List<File> landscapeConfigFiles = LandscapeAnalysisUtils.findAllSokratesLandscapeConfigFiles(root);
            landscapeConfigFiles.forEach(landscapeConfigFile -> {
                File landscapeFolder = landscapeConfigFile.getParentFile().getParentFile();
                String absolutePath = landscapeFolder.getAbsolutePath().replace("/./", "/");
                LOG.info(System.getProperty("user.dir"));
                System.setProperty("user.dir", absolutePath);
                LOG.info(System.getProperty("user.dir"));
                LandscapeAnalysisCommands.update(new File(landscapeFolder.getAbsolutePath()), null, metadata, dataOnly);
                DateUtils.reset();
                RegexUtils.reset();
                System.gc();
            });
            LOG.info("Analysed " + landscapeConfigFiles + " landscape(s):");
            landscapeConfigFiles.forEach(landscapeConfigFile -> {
                LOG.info(" -  " + landscapeConfigFile.getPath());
            });
            if (landscapeConfigFiles.size() > 0) {
                File landscapeRoot = landscapeConfigFiles.get(landscapeConfigFiles.size() - 1).getParentFile();
                saveExecutionStats(new File(landscapeRoot, "data"));
                // Fold the just-written executionTimes files into the landscape's data.zip.
                LandscapeAnalysisCommands.zipLandscapeDataFolder(landscapeRoot, !dataOnly);
            }
        } else {
            File reportsFolder = LandscapeAnalysisCommands.update(root, confFilePath != null ? new File(confFilePath) : null, metadata, dataOnly);
            saveExecutionStats(new File(reportsFolder, "data"));
            LandscapeAnalysisCommands.zipLandscapeDataFolder(reportsFolder, !dataOnly);
        }
    }

    /**
     * The git URLs given to analyzeLandscape: every -url value plus the lines of the -urls file
     * (trimmed; blank lines and # comments ignored), in order, without duplicates. Empty when none
     * were given; null (after logging) when the -urls file cannot be read.
     */
    private List<String> collectRepositoryUrls(CommandLine cmd) {
        return collectValues(cmd, commands.getUrl(), commands.getUrls());
    }

    /**
     * The values of a repeatable option plus the lines of its list-file companion (trimmed; blank
     * lines and # comments ignored), in order, without duplicates. Empty when none were given; null
     * (after logging) when the file cannot be read.
     */
    private List<String> collectValues(CommandLine cmd, Option single, Option listFile) {
        List<String> values = new ArrayList<>();
        String[] givenValues = cmd.getOptionValues(single.getOpt());
        if (givenValues != null) {
            for (String value : givenValues) {
                if (StringUtils.isNotBlank(value) && !values.contains(value.trim())) {
                    values.add(value.trim());
                }
            }
        }
        if (cmd.hasOption(listFile.getOpt())) {
            File file = new File(cmd.getOptionValue(listFile.getOpt()));
            if (!file.exists()) {
                LOG.error("The -" + listFile.getOpt() + " file \"" + file.getPath() + "\" does not exist.");
                return null;
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
                return null;
            }
        }
        return values;
    }

    /**
     * analyzeLandscape's repository step: analyzeGitRepo for every URL into <root>/<owner>/<repository>.
     * A repository whose clone fails is logged and skipped so one bad URL does not lose the batch;
     * returns false only when nothing could be analyzed (then there is nothing to aggregate).
     */
    private RepositoryBatch analyzeRepositoriesIntoLandscape(CommandLine cmd, File root, List<String> urls, String producer) throws IOException {
        return analyzeRepositoriesIntoLandscape(cmd, root, urls, url -> {
            GitRepoMetadata urlMetadata = GitRepoMetadata.fromUrl(url);
            return new File(root, urlMetadata != null ? urlMetadata.outputFolderName() : GitRepoCloner.folderNameFromUrl(url));
        }, producer);
    }

    /** Same, with the output folder of each URL chosen by {@code outputFolderFor} (analyzeGitHubOrg uses <org>/<repository>). */
    private RepositoryBatch analyzeRepositoriesIntoLandscape(CommandLine cmd, File root, List<String> urls, Function<String, File> outputFolderFor, String producer) throws IOException {
        return analyzeRepositoriesIntoLandscape(cmd, root, urls, outputFolderFor, producer, Map.of());
    }

    /** Same, with what the code host's listing said about each URL ({@code listing}: url -> repository), so the report metadata needs no extra API call. */
    private RepositoryBatch analyzeRepositoriesIntoLandscape(CommandLine cmd, File root, List<String> urls, Function<String, File> outputFolderFor, String producer,
                                                           Map<String, CodeHostRepo> listing) throws IOException {
        RepositoryBatch batch = new RepositoryBatch();
        int depth = 0;
        String depthValue = cmd.getOptionValue(commands.getDepth().getOpt());
        if (StringUtils.isNotBlank(depthValue)) {
            if (!StringUtils.isNumeric(depthValue.trim())) {
                LOG.error("-" + Commands.ARG_DEPTH + " must be a positive number, got '" + depthValue + "'.");
                return batch;
            }
            depth = Integer.parseInt(depthValue.trim());
        }
        int index = 0;
        for (String url : urls) {
            index++;
            LOG.info("");
            LOG.info("=== Repository " + index + " of " + urls.size() + ": " + url + " ===");
            File output = outputFolderFor.apply(url);
            CloneOutcome outcome;
            try {
                outcome = analyzeGitRepoInto(cmd, url, output, null, depth, producer, listing.get(url));
            } catch (Exception e) {
                LOG.error("Analysis of " + url + " failed: " + e.getMessage());
                outcome = CloneOutcome.FAILED;
            }
            (outcome == CloneOutcome.ANALYZED ? batch.analyzed : outcome == CloneOutcome.NOT_FOUND ? batch.notFound : batch.failed).add(url);
            // Per-analysis static caches (the recursive landscape update resets them the same way).
            DateUtils.reset();
            RegexUtils.reset();
        }
        LOG.info("");
        LOG.info("Analyzed " + batch.analyzed.size() + " of " + urls.size() + " repositories into " + root.getPath());
        batch.failed.forEach(url -> LOG.error(" - failed: " + url));
        batch.notFound.forEach(url -> LOG.error(" - does not exist: " + url));
        if (batch.nothingAnalyzed()) {
            LOG.error("No repository could be analyzed; the landscape is not updated.");
        }
        return batch;
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

    private void logReportLocation(File reportsFolder) {
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
     * analyzeGitHubOrg = for each GitHub organization (or user) login: list its repositories with
     * the GitHub API, filter them, analyze each and build the organization's landscape — see
     * {@link #analyzeOrganizations}.
     */
    private void analyzeGitHubOrg(String[] args) throws ParseException, IOException {
        Options options = commands.getAnalyzeGitHubOrgOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);
        List<String> logins = organizationLogins(cmd, options, commands.getOrg(), commands.getOrgs(), Commands.ANALYZE_GITHUB_ORG,
                Commands.ANALYZE_GITHUB_ORG_DESCRIPTION, CommandLineInterface::gitHubLogin);
        if (logins == null) {
            return;
        }
        analyzeOrganizations(cmd, gitHubOrgClient, logins, Commands.ANALYZE_GITHUB_ORG);
    }

    /**
     * analyzeGitLabGroup = the same for GitLab groups (with their subgroups) or users, on gitlab.com
     * or a self-hosted instance (-gitlabUrl, or the host of a -group given as a URL).
     */
    private void analyzeGitLabGroup(String[] args) throws ParseException, IOException {
        Options options = commands.getAnalyzeGitLabGroupOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);
        List<String> rawValues = cmd.hasOption(commands.getHelp().getOpt()) ? new ArrayList<>() : collectValues(cmd, commands.getGroup(), commands.getGroups());
        String baseUrl = cmd.getOptionValue(commands.getGitlabUrl().getOpt());
        if (StringUtils.isBlank(baseUrl) && rawValues != null) {
            // A group given as a URL names the instance too (https://gitlab.example.com/group/sub).
            baseUrl = rawValues.stream().map(CommandLineInterface::gitLabBaseUrl).filter(StringUtils::isNotBlank).findFirst().orElse(GitLabGroupClient.DEFAULT_BASE_URL);
        }
        List<String> groups = organizationLogins(cmd, options, commands.getGroup(), commands.getGroups(), Commands.ANALYZE_GITLAB_GROUP,
                Commands.ANALYZE_GITLAB_GROUP_DESCRIPTION, CommandLineInterface::gitLabGroupPath);
        if (groups == null) {
            return;
        }
        CodeHostOrgClient client = gitLabGroupClient != null ? gitLabGroupClient : new GitLabGroupClient(baseUrl);
        LOG.info("GitLab instance: " + baseUrl);
        analyzeOrganizations(cmd, client, groups, Commands.ANALYZE_GITLAB_GROUP);
    }

    /** The normalized, de-duplicated organization identifiers of a command; null (after help/usage) when there are none. */
    private List<String> organizationLogins(CommandLine cmd, Options options, Option single, Option listFile, String commandName,
                                            String commandDescription, Function<String, String> normalizer) {
        List<String> logins = cmd.hasOption(commands.getHelp().getOpt()) ? new ArrayList<>() : collectValues(cmd, single, listFile);
        if (logins == null) {
            return null;
        }
        logins = logins.stream().map(normalizer).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (cmd.hasOption(commands.getHelp().getOpt()) || logins.isEmpty()) {
            helpMode = true;
            if (!cmd.hasOption(commands.getHelp().getOpt())) {
                LOG.error("At least one -" + single.getOpt() + " (or a -" + listFile.getOpt() + " file) is required.");
            }
            commands.usage(commandName, options, commandDescription);
            return null;
        }
        return logins;
    }

    /**
     * The shared organization-level analysis: for each organization, list its repositories with the
     * host's API, filter them, write the selection to <root>/<org>/repos.txt, analyze each into
     * <root>/<org>/<repository folder path> (the analyzeGitRepo step) and build the organization's
     * landscape in <root>/<org>/_sokrates_landscape with metadata from the host profile (blank
     * fields only). With more than one organization landscape under the root (or a parent
     * configuration already there) the parent landscape in <root>/_sokrates_landscape is updated
     * last, so its Sub-landscapes tab reads fresh data. An organization whose listing fails is skipped.
     */
    private void analyzeOrganizations(CommandLine cmd, CodeHostOrgClient client, List<String> logins, String commandName) throws IOException {
        startTimeoutIfDefined(cmd);
        updateDateParam(cmd);

        CodeHostRepoFilter filter = repoFilterFromCommandLine(cmd);
        if (filter == null) {
            return;
        }
        boolean listOnly = cmd.hasOption(commands.getListOnly().getOpt());
        boolean prune = cmd.hasOption(commands.getPrune().getOpt());
        boolean dataOnly = cmd.hasOption(commands.getDataOnly().getOpt());

        File root = new File(cmd.hasOption(commands.getAnalysisRoot().getOpt()) ? cmd.getOptionValue(commands.getAnalysisRoot().getOpt()) : ".");
        root.mkdirs();

        LOG.info("Repository selection: " + filter.describe());
        List<String> failed = new ArrayList<>();
        List<File> landscapes = new ArrayList<>();
        int index = 0;
        for (String login : logins) {
            index++;
            LOG.info("");
            LOG.info("=== Organization " + index + " of " + logins.size() + ": " + login + " ===");
            CodeHostOrg org;
            List<CodeHostRepo> allRepos;
            try {
                org = client.fetchOrg(login);
                allRepos = client.listRepos(login);
            } catch (Exception e) {
                LOG.error("Could not list the repositories of " + login + ": " + e.getMessage());
                failed.add(login);
                continue;
            }
            List<CodeHostRepo> repos = filter.apply(allRepos, LocalDate.parse(DateUtils.getAnalysisDate()));
            LOG.info(org.displayName() + (org.isUser() ? " (user account)" : "") + ": " + allRepos.size() + " repositories found, " + repos.size() + " selected.");
            filter.getExclusions().forEach(exclusion -> LOG.info(" - skipped " + exclusion));

            File orgRoot = new File(root, org.getLogin());
            orgRoot.mkdirs();
            writeRepositoriesList(new File(orgRoot, "repos.txt"), org, allRepos.size(), repos, filter, client.hostLabel());
            if (listOnly) {
                continue;
            }
            if (repos.isEmpty()) {
                LOG.warn("No repository of " + login + " is selected; its landscape is not updated.");
                continue;
            }

            List<String> urls = repos.stream().map(CodeHostRepo::getCloneUrl).collect(Collectors.toList());
            Map<String, File> outputFolders = new LinkedHashMap<>();
            Map<String, CodeHostRepo> listing = new LinkedHashMap<>();
            repos.forEach(repo -> {
                outputFolders.put(repo.getCloneUrl(), new File(orgRoot, repo.getFolderPath()));
                if (StringUtils.isBlank(repo.getAvatarUrl())) {
                    repo.setAvatarUrl(org.getAvatarUrl());   // a GitLab project without its own avatar shows the group's
                }
                listing.put(repo.getCloneUrl(), repo);
            });
            RepositoryBatch batch = analyzeRepositoriesIntoLandscape(cmd, orgRoot, urls, outputFolders::get, commandName, listing);
            if (batch.nothingAnalyzed()) {
                failed.add(login);
                continue;
            }
            pruneManagedAnalyses(orgRoot, urls, batch.notFound, prune);

            Metadata metadata = orgLandscapeMetadata(orgRoot, org);
            File reportsFolder = LandscapeAnalysisCommands.update(orgRoot, null, metadata, dataOnly);
            saveExecutionStats(new File(reportsFolder, "data"));
            LandscapeAnalysisCommands.zipLandscapeDataFolder(reportsFolder, !dataOnly);
            landscapes.add(reportsFolder);
            DateUtils.reset();
            RegexUtils.reset();
        }

        if (listOnly) {
            LOG.info("");
            LOG.info("-" + Commands.ARG_LIST_ONLY + ": the selected repositories are listed in <org>/repos.txt under " + root.getPath() + "; nothing was cloned or analyzed.");
        } else if (!landscapes.isEmpty()) {
            updateParentLandscapeIfNeeded(cmd, root, dataOnly);
        }

        LOG.info("");
        if (listOnly) {
            LOG.info("Done: " + (logins.size() - failed.size()) + " of " + logins.size() + " organization(s) listed under " + root.toPath().toAbsolutePath().normalize());
        } else {
            LOG.info("Done: " + landscapes.size() + " of " + logins.size() + " organization landscape(s) updated under " + root.toPath().toAbsolutePath().normalize());
            landscapes.forEach(folder -> LOG.info(" - " + new File(folder, dataOnly ? "data/data.zip" : "index.html").toPath().toAbsolutePath().normalize().toUri()));
        }
        failed.forEach(login -> LOG.error(" - failed: " + login));
    }

    /** The login from a login or a GitHub URL: "junit-team", "https://github.com/junit-team/", "github.com/junit-team/junit4" all give junit-team. */
    static String gitHubLogin(String value) {
        String login = value.trim();
        login = login.replaceFirst("^(https?://)?(www\\.)?github\\.com/", "");
        login = login.replaceFirst("^@", "");
        int slash = login.indexOf('/');
        return slash >= 0 ? login.substring(0, slash) : login;
    }

    /** The group path from a path or a GitLab URL: "gitlab-org/ci-cd" and "https://gitlab.com/gitlab-org/ci-cd/" both give gitlab-org/ci-cd. */
    static String gitLabGroupPath(String value) {
        String path = value.trim();
        path = path.replaceFirst("^https?://[^/]+/", "");
        path = StringUtils.strip(path, "/");
        path = path.replaceFirst("^groups/", "");
        return path.replaceFirst("/-/.*$", "");
    }

    /** The instance URL of a group given as a URL ("https://gitlab.example.com/a/b" -> "https://gitlab.example.com"); "" for a plain path. */
    static String gitLabBaseUrl(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(https?://[^/]+)/").matcher(value.trim());
        return matcher.find() ? matcher.group(1) : "";
    }

    /** The parent landscape over the organization folders: only when there are at least two, or it already exists. */
    private void updateParentLandscapeIfNeeded(CommandLine cmd, File root, boolean dataOnly) {
        File parentConfig = new File(new File(root, "_sokrates_landscape"), "config.json");
        long children = LandscapeAnalysisUtils.findAllSokratesLandscapeConfigFiles(root).stream()
                .filter(file -> !file.getAbsoluteFile().equals(parentConfig.getAbsoluteFile()))
                .count();
        if (children < 2 && !parentConfig.exists()) {
            return;
        }
        LOG.info("");
        LOG.info("=== Parent landscape over " + children + " organization landscapes ===");
        Metadata metadata = new Metadata();
        updateMetadataFromCommandLine(cmd, metadata);
        File reportsFolder = LandscapeAnalysisCommands.update(root, null, metadata, dataOnly);
        saveExecutionStats(new File(reportsFolder, "data"));
        LandscapeAnalysisCommands.zipLandscapeDataFolder(reportsFolder, !dataOnly);
        LOG.info("Parent landscape: " + new File(reportsFolder, dataOnly ? "data/data.zip" : "index.html").toPath().toAbsolutePath().normalize().toUri());
    }

    private CodeHostRepoFilter repoFilterFromCommandLine(CommandLine cmd) {
        CodeHostRepoFilter filter = new CodeHostRepoFilter();
        filter.setIncludeForks(cmd.hasOption(commands.getIncludeForks().getOpt()));
        filter.setIncludeArchived(cmd.hasOption(commands.getIncludeArchived().getOpt()));
        Integer days = nonNegativeIntOption(cmd, commands.getPushedWithinDays());
        Integer max = nonNegativeIntOption(cmd, commands.getMaxRepos());
        if (days == null || max == null) {
            return null;
        }
        filter.setPushedWithinDays(days);
        filter.setMaxRepos(max);
        String[] include = cmd.getOptionValues(commands.getIncludeRepoNamePattern().getOpt());
        String[] exclude = cmd.getOptionValues(commands.getExcludeRepoNamePattern().getOpt());
        if (include != null) {
            Arrays.stream(include).filter(StringUtils::isNotBlank).map(String::trim).forEach(filter.getIncludeNamePatterns()::add);
        }
        if (exclude != null) {
            Arrays.stream(exclude).filter(StringUtils::isNotBlank).map(String::trim).forEach(filter.getExcludeNamePatterns()::add);
        }
        try {
            filter.apply(new ArrayList<>(List.of(new CodeHostRepo("pattern-check", ""))), LocalDate.now()); // validates the regexes early
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            return null;
        }
        return filter;
    }

    /** The option's value as a non-negative int (0 when absent); null after logging when it is not a number. */
    private Integer nonNegativeIntOption(CommandLine cmd, Option option) {
        String value = cmd.getOptionValue(option.getOpt());
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        if (!StringUtils.isNumeric(value.trim())) {
            LOG.error("-" + option.getOpt() + " must be a non-negative number, got '" + value + "'.");
            return null;
        }
        return Integer.parseInt(value.trim());
    }

    /** <org>/repos.txt: the selected clone URLs, one per line, usable as-is with analyzeLandscape -urls. */
    private void writeRepositoriesList(File file, CodeHostOrg org, int found, List<CodeHostRepo> selected, CodeHostRepoFilter filter, String hostLabel) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# " + org.displayName() + " (" + org.getHtmlUrl() + "): " + selected.size() + " of " + found + " repositories selected on " + DateUtils.getAnalysisDate());
        lines.add("# selection: " + filter.describe());
        lines.add("# generated by sokrates analyze" + hostLabel + ("GitLab".equals(hostLabel) ? "Group" : "Org") + "; re-usable with analyzeLandscape -urls " + file.getName());
        selected.forEach(repo -> lines.add(repo.getCloneUrl()));
        FileUtils.writeLines(file, UTF_8.name(), lines);
        LOG.info("Selected repositories listed in " + file.getPath());
    }

    /**
     * The organization's landscape metadata: the host profile fills only what the existing
     * landscape configuration leaves blank (name, description, logo, and a link when there are no
     * links), so a user's edits survive re-runs.
     */
    private Metadata orgLandscapeMetadata(File orgRoot, CodeHostOrg org) {
        Metadata existing = new Metadata();
        File configFile = new File(new File(orgRoot, "_sokrates_landscape"), "config.json");
        if (configFile.exists()) {
            try {
                LandscapeConfiguration configuration = (LandscapeConfiguration) new JsonMapper().getObject(FileUtils.readFileToString(configFile, UTF_8), LandscapeConfiguration.class);
                if (configuration != null && configuration.getMetadata() != null) {
                    existing = configuration.getMetadata();
                }
            } catch (IOException e) {
                LOG.warn("Could not read " + configFile.getPath() + ": " + e.getMessage());
            }
        }
        Metadata metadata = new Metadata();
        Metadata filled = new Metadata();
        filled.setName(existing.getName());
        filled.setDescription(existing.getDescription());
        filled.setLogoLink(existing.getLogoLink());
        filled.getLinks().addAll(existing.getLinks());
        org.applyTo(filled);
        // Only what the profile added is passed on; the updater overwrites just the non-blank fields it gets.
        if (StringUtils.isBlank(existing.getName())) metadata.setName(filled.getName());
        if (StringUtils.isBlank(existing.getDescription())) metadata.setDescription(filled.getDescription());
        if (StringUtils.isBlank(existing.getLogoLink())) metadata.setLogoLink(filled.getLogoLink());
        if (existing.getLinks().isEmpty()) metadata.getLinks().addAll(filled.getLinks());
        return metadata;
    }

    /**
     * analyzeGitRepo = clone the repository at -url into a temporary folder, run the analyze
     * pipeline there, and keep only the analysis (config.json + reports/) in -destFolder (default:
     * <cwd>/<owner>/<repository>); the clone is deleted. A kept config.json is reused on re-runs,
     * so edits survive even though the clone is fresh every time. The output layout (config.json
     * next to reports/) is the one analyzeLandscape expects.
     */
    private void analyzeGitRepo(String[] args) throws ParseException, IOException {
        Options options = commands.getAnalyzeGitRepoOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt()) || !cmd.hasOption(commands.getUrl().getOpt())) {
            helpMode = true;
            if (!cmd.hasOption(commands.getUrl().getOpt())) {
                LOG.error("-" + Commands.ARG_URL + " is required.");
            }
            commands.usage(Commands.ANALYZE_GIT_REPO, options, Commands.ANALYZE_GIT_REPO_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

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

    private CloneOutcome analyzeGitRepoInto(CommandLine cmd, String url, File output, String branch, int depth, String producer, CodeHostRepo listed) throws IOException {
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

            analyze(cmd, clone, false, url, output, listed);

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
            logReportLocation(new File(output, "reports"));
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
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.eclipse.jgit.errors.NoRemoteRepositoryException) {
                return true;
            }
            if (cause instanceof java.net.UnknownHostException || cause instanceof java.net.ConnectException
                    || cause instanceof java.net.SocketTimeoutException) {
                return false;
            }
        }
        GitRepoMetadata metadata = GitRepoMetadata.fromUrl(url);
        if (metadata != null && metadata.isGitHub() && StringUtils.isNotBlank(metadata.getOwner())
                && StringUtils.isBlank(System.getenv(GitRepoMetadata.ENV_OFFLINE))) {
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
        String message = StringUtils.defaultString(failure.getMessage()).toLowerCase();
        return message.contains("not found") || message.contains("does not exist");
    }

    /** The outcome of a batch of clone-and-analyze steps. */
    static class RepositoryBatch {
        final List<String> analyzed = new ArrayList<>();
        final List<String> failed = new ArrayList<>();
        final List<String> notFound = new ArrayList<>();

        boolean nothingAnalyzed() {
            return analyzed.isEmpty();
        }
    }

    /**
     * -prune: deletes the kept analyses this tool produced (folders carrying a source.json, at any
     * depth below root, _sokrates_landscape excluded) whose repository is no longer selected or no
     * longer exists; a folder emptied by that goes too. Analyses without the marker — placed by hand
     * — are never touched. Without -prune the stale analyses are only listed, with the hint.
     */
    private void pruneManagedAnalyses(File root, Collection<String> selectedUrls, Collection<String> goneUrls, boolean prune) throws IOException {
        Set<String> selected = selectedUrls.stream().map(AnalysisSource::normalizeUrl).collect(Collectors.toSet());
        Set<String> gone = goneUrls.stream().map(AnalysisSource::normalizeUrl).collect(Collectors.toSet());
        List<File> stale = new ArrayList<>();
        collectStaleManagedAnalyses(root, selected, gone, stale);
        if (stale.isEmpty()) {
            return;
        }
        if (!prune) {
            LOG.warn(stale.size() + " kept analysis folder(s) under " + root.getPath() + " belong to repositories that are no longer listed or no longer exist"
                    + " (they still count in the landscape); add -" + Commands.ARG_PRUNE + " to delete them:");
            stale.forEach(folder -> LOG.warn(" - " + root.toPath().relativize(folder.toPath())));
            return;
        }
        for (File folder : stale) {
            AnalysisSource source = AnalysisSource.read(folder);
            String reason = source != null && gone.contains(source.getNormalizedUrl()) ? "the repository does not exist any more" : "no longer listed";
            LOG.info("-" + Commands.ARG_PRUNE + ": deleting " + root.toPath().relativize(folder.toPath()) + " (" + reason + ").");
            FileUtils.deleteDirectory(folder);
            for (File parent = folder.getParentFile(); parent != null && !parent.equals(root) && isEmptyFolder(parent); parent = parent.getParentFile()) {
                FileUtils.deleteDirectory(parent);
            }
        }
    }

    private static void collectStaleManagedAnalyses(File folder, Set<String> selected, Set<String> gone, List<File> stale) {
        File[] children = folder.listFiles(File::isDirectory);
        for (File child : children == null ? new File[0] : children) {
            String name = child.getName();
            if (name.equals("_sokrates_landscape") || name.equals(".git") || name.equals("_sokrates")) {
                continue;
            }
            AnalysisSource source = AnalysisSource.read(child);
            if (source != null) {
                String url = source.getNormalizedUrl();
                if (gone.contains(url) || !selected.contains(url)) {
                    stale.add(child);
                }
            } else if (!new File(child, "config.json").exists()) {
                collectStaleManagedAnalyses(child, selected, gone, stale);
            }
        }
    }

    private static boolean isEmptyFolder(File folder) {
        String[] names = folder.list();
        return names != null && names.length == 0;
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
    private File analyze(CommandLine cmd, File root, boolean skipGitHistory, String repoUrl, File keptFolder, CodeHostRepo listed) throws IOException {
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
        PostAnalysisState previous = PostAnalysisState.read(keptFolder != null ? keptFolder : analysisFolder);
        if (previous != null && previous.covers(command, head) && !cmd.hasOption(commands.getAiForce().getOpt())) {
            LOG.info("Post-analysis command skipped: unchanged since it ran on " + previous.getRanOn() + " (head " + head.substring(0, Math.min(10, head.length()))
                    + "); -" + Commands.ARG_AI_FORCE + " runs it anyway.");
            return;
        }
        Integer max = nonNegativeIntOption(cmd, commands.getAiMaxRepos());
        if (max != null && max > 0 && postAnalysisRuns >= max) {
            LOG.info("Post-analysis command skipped: the -" + Commands.ARG_AI_MAX_REPOS + " budget of " + max + " repositories is used up for this run.");
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

    private void startTimeoutIfDefined(CommandLine cmd) {
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

        Metadata metadata = codeConfiguration.getMetadata();
        updateMetadataFromCommandLine(cmd, metadata);

        if (cmd.hasOption(commands.getSetCacheFiles().getOpt())) {
            String cacheFileValue = cmd.getOptionValue(commands.getSetCacheFiles().getOpt());
            if (StringUtils.isNotBlank(cacheFileValue)) {
                codeConfiguration.getAnalysis().setSaveSourceFiles(cacheFileValue.equalsIgnoreCase("true"));
            }
        }

        FileUtils.write(confFile, new JsonGenerator().generate(codeConfiguration), UTF_8);
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
        SkillsInstaller installer = new SkillsInstaller();
        File root;
        try {
            root = installer.fetch(source, ref, cache);
        } catch (GitAPIException | IOException e) {
            LOG.error("Could not fetch the skills from " + source + ": " + e.getMessage());
            return;
        }
        List<File> skills = SkillsInstaller.findSkills(root);
        if (skills.isEmpty()) {
            LOG.error("No skills (folders with a SKILL.md under skills/) found in " + root.getPath());
            return;
        }
        LOG.info(skills.size() + " skills in " + root.getPath() + ": " + skills.stream().map(File::getName).collect(Collectors.joining(", ")));
        if (cmd.hasOption(commands.getListOnly().getOpt())) {
            return;
        }
        boolean copy = cmd.hasOption(commands.getCopy().getOpt());
        for (File target : targets) {
            List<File> installed = installer.installInto(skills, target, copy);
            LOG.info((copy ? "Copied " : "Linked ") + installed.size() + " skills into " + target.getPath());
        }
        LOG.info("Done. Ask your agent to \"use the sokrates skill\" in a repository, or run an analysis with -ai claude|codex|gemini.");
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

    private void updateMetadataFromCommandLine(CommandLine cmd, Metadata metadata) {
        if (cmd.hasOption(commands.getSetName().getOpt())) {
            String name = cmd.getOptionValue(commands.getSetName().getOpt());
            if (StringUtils.isNotBlank(name)) {
                metadata.setName(name);
            }
        }

        if (cmd.hasOption(commands.getSetDescription().getOpt())) {
            String description = cmd.getOptionValue(commands.getSetDescription().getOpt());
            if (StringUtils.isNotBlank(description)) {
                metadata.setDescription(description);
            }
        }

        if (cmd.hasOption(commands.getSetLogoLink().getOpt())) {
            String logoLink = cmd.getOptionValue(commands.getSetLogoLink().getOpt());
            if (StringUtils.isNotBlank(logoLink)) {
                metadata.setLogoLink(logoLink);
            }
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

    private void saveExecutionStats(File dataFolder) {
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

    public void setGitHubOrgClient(CodeHostOrgClient gitHubOrgClient) {
        this.gitHubOrgClient = gitHubOrgClient;
    }

    public void setGitLabGroupClient(CodeHostOrgClient gitLabGroupClient) {
        this.gitLabGroupClient = gitLabGroupClient;
    }


}
