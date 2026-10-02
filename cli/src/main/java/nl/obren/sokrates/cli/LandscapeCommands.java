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

/**
 * The analyzeLandscape / updateLandscape command and the landscape batch steps the organization commands
 * share: the clone-and-analyze loop over repository URLs ({@link #analyzeRepositoriesIntoLandscape}) and the
 * -prune bookkeeping of the kept analyses ({@link #pruneManagedAnalyses}). Moved out of
 * {@link CommandLineInterface}, which keeps the per-repository pipeline and the option plumbing.
 */
class LandscapeCommands {
    private static final Log LOG = LogFactory.getLog(LandscapeCommands.class);
    private final CommandLineInterface cli;
    private final Commands commands;

    LandscapeCommands(CommandLineInterface cli, Commands commands) {
        this.cli = cli;
        this.commands = commands;
    }


    void updateLandscape(String[] args, String commandName, String commandDescription) throws ParseException, IOException {
        Options options = commands.getUpdateLandscapeOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            CommandLineInterface.helpMode = true;
            commands.usage(commandName, commands.getUpdateLandscapeOptions(), commandDescription);
            return;
        }

        cli.startTimeoutIfDefined(cmd);

        List<String> urls = collectRepositoryUrls(cmd);
        if (urls == null) {
            return;
        }
        File root = landscapeRoot(cmd, urls);
        if (root == null) {
            return;
        }

        Metadata metadata = new Metadata();

        cli.updateMetadataFromCommandLine(cmd, metadata);

        String confFilePath = cmd.getOptionValue(commands.getConfFile().getOpt());
        cli.updateDateParam(cmd);

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
            updateLandscapesRecursively(root, metadata, dataOnly);
        } else {
            File reportsFolder = LandscapeAnalysisCommands.update(root, confFilePath != null ? new File(confFilePath) : null, metadata, dataOnly);
            cli.saveExecutionStats(new File(reportsFolder, "data"));
            LandscapeAnalysisCommands.zipLandscapeDataFolder(reportsFolder, !dataOnly);
        }
    }

    /** The -analysisRoot folder (default "."); created when repository URLs are given, else it must exist (null after logging when it does not). */
    private File landscapeRoot(CommandLine cmd, List<String> urls) {
        String strRootPath = cmd.getOptionValue(commands.getAnalysisRoot().getOpt());
        if (!cmd.hasOption(commands.getAnalysisRoot().getOpt())) {
            strRootPath = ".";
        }
        File root = new File(strRootPath);
        if (!root.exists()) {
            if (urls.isEmpty()) {
                LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
                return null;
            }
            // With repository URLs the root is where the analyses will be created.
            root.mkdirs();
        }
        return root;
    }

    /** -recursive: updates every landscape found below the root, then packages the last one's execution stats. */
    private void updateLandscapesRecursively(File root, Metadata metadata, boolean dataOnly) {
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
            cli.saveExecutionStats(new File(landscapeRoot, "data"));
            // Fold the just-written executionTimes files into the landscape's data.zip.
            LandscapeAnalysisCommands.zipLandscapeDataFolder(landscapeRoot, !dataOnly);
        }
    }

    /**
     * The git URLs given to analyzeLandscape: every -url value plus the lines of the -urls file
     * (trimmed; blank lines and # comments ignored), in order, without duplicates. Empty when none
     * were given; null (after logging) when the -urls file cannot be read.
     */
    private List<String> collectRepositoryUrls(CommandLine cmd) {
        return cli.collectValues(cmd, commands.getUrl(), commands.getUrls());
    }

    /**
     * analyzeLandscape's repository step: analyzeGitRepo for every URL into <root>/<owner>/<repository>.
     * A repository whose clone fails is logged and skipped so one bad URL does not lose the batch;
     * returns false only when nothing could be analyzed (then there is nothing to aggregate).
     */
    RepositoryBatch analyzeRepositoriesIntoLandscape(CommandLine cmd, File root, List<String> urls, String producer) throws IOException {
        return analyzeRepositoriesIntoLandscape(cmd, root, urls, url -> {
            GitRepoMetadata urlMetadata = GitRepoMetadata.fromUrl(url);
            return new File(root, urlMetadata != null ? urlMetadata.outputFolderName() : GitRepoCloner.folderNameFromUrl(url));
        }, producer);
    }

    /** Same, with the output folder of each URL chosen by {@code outputFolderFor} (analyzeGitHubOrg uses <org>/<repository>). */
    RepositoryBatch analyzeRepositoriesIntoLandscape(CommandLine cmd, File root, List<String> urls, Function<String, File> outputFolderFor, String producer) throws IOException {
        return analyzeRepositoriesIntoLandscape(cmd, root, urls, outputFolderFor, producer, Map.of());
    }

    /** Same, with what the code host's listing said about each URL ({@code listing}: url -> repository), so the report metadata needs no extra API call. */
    RepositoryBatch analyzeRepositoriesIntoLandscape(CommandLine cmd, File root, List<String> urls, Function<String, File> outputFolderFor, String producer,
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
            GitRepoCommands.CloneOutcome outcome;
            try {
                outcome = cli.gitRepoCommands.analyzeGitRepoInto(cmd, url, output, null, depth, producer, listing.get(url));
            } catch (Exception e) {
                LOG.error("Analysis of " + url + " failed: " + e.getMessage());
                outcome = GitRepoCommands.CloneOutcome.FAILED;
            }
            (outcome == GitRepoCommands.CloneOutcome.ANALYZED ? batch.analyzed : outcome == GitRepoCommands.CloneOutcome.NOT_FOUND ? batch.notFound : batch.failed).add(url);
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
    void pruneManagedAnalyses(File root, Collection<String> selectedUrls, Collection<String> goneUrls, boolean prune) throws IOException {
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
}
