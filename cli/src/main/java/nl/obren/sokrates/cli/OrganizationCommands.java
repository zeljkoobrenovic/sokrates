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
 * The organization commands: analyzeGitHubOrg and analyzeGitLabGroup list an organization's repositories
 * through its code host's API, filter them, analyze each (the analyzeGitRepo step, shared with the
 * landscape commands of {@link CommandLineInterface}) and build one landscape per organization, named,
 * described and branded from the organization's profile. Moved out of CommandLineInterface, which only
 * dispatches to {@link #analyzeGitHubOrg} and {@link #analyzeGitLabGroup}.
 */
public class OrganizationCommands {
    private static final Log LOG = LogFactory.getLog(OrganizationCommands.class);

    private final CommandLineInterface cli;
    private final Commands commands;
    private CodeHostOrgClient gitHubOrgClient = new GitHubOrgClient();
    private CodeHostOrgClient gitLabGroupClient = null;

    public OrganizationCommands(CommandLineInterface cli, Commands commands) {
        this.cli = cli;
        this.commands = commands;
    }

    public void setGitHubOrgClient(CodeHostOrgClient client) {
        this.gitHubOrgClient = client;
    }

    public void setGitLabGroupClient(CodeHostOrgClient client) {
        this.gitLabGroupClient = client;
    }

    /**
     * analyzeGitHubOrg = for each GitHub organization (or user) login: list its repositories with
     * the GitHub API, filter them, analyze each and build the organization's landscape — see
     * {@link #analyzeOrganizations}.
     */
    public void analyzeGitHubOrg(String[] args) throws ParseException, IOException {
        Options options = commands.getAnalyzeGitHubOrgOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);
        List<String> logins = organizationLogins(cmd, options, commands.getOrg(), commands.getOrgs(), Commands.ANALYZE_GITHUB_ORG,
                Commands.ANALYZE_GITHUB_ORG_DESCRIPTION, OrganizationCommands::gitHubLogin);
        if (logins == null) {
            return;
        }
        analyzeOrganizations(cmd, gitHubOrgClient, logins, Commands.ANALYZE_GITHUB_ORG);
    }

    /**
     * analyzeGitLabGroup = the same for GitLab groups (with their subgroups) or users, on gitlab.com
     * or a self-hosted instance (-gitlabUrl, or the host of a -group given as a URL).
     */
    public void analyzeGitLabGroup(String[] args) throws ParseException, IOException {
        Options options = commands.getAnalyzeGitLabGroupOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);
        List<String> rawValues = cmd.hasOption(commands.getHelp().getOpt()) ? new ArrayList<>() : cli.collectValues(cmd, commands.getGroup(), commands.getGroups());
        String baseUrl = cmd.getOptionValue(commands.getGitlabUrl().getOpt());
        if (StringUtils.isBlank(baseUrl) && rawValues != null) {
            // A group given as a URL names the instance too (https://gitlab.example.com/group/sub).
            baseUrl = rawValues.stream().map(OrganizationCommands::gitLabBaseUrl).filter(StringUtils::isNotBlank).findFirst().orElse(GitLabGroupClient.DEFAULT_BASE_URL);
        }
        List<String> groups = organizationLogins(cmd, options, commands.getGroup(), commands.getGroups(), Commands.ANALYZE_GITLAB_GROUP,
                Commands.ANALYZE_GITLAB_GROUP_DESCRIPTION, OrganizationCommands::gitLabGroupPath);
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
        List<String> logins = cmd.hasOption(commands.getHelp().getOpt()) ? new ArrayList<>() : cli.collectValues(cmd, single, listFile);
        if (logins == null) {
            return null;
        }
        logins = logins.stream().map(normalizer).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (cmd.hasOption(commands.getHelp().getOpt()) || logins.isEmpty()) {
            CommandLineInterface.helpMode = true;
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
        cli.startTimeoutIfDefined(cmd);
        cli.updateDateParam(cmd);

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
            CommandLineInterface.RepositoryBatch batch = cli.analyzeRepositoriesIntoLandscape(cmd, orgRoot, urls, outputFolders::get, commandName, listing);
            if (batch.nothingAnalyzed()) {
                failed.add(login);
                continue;
            }
            cli.pruneManagedAnalyses(orgRoot, urls, batch.notFound, prune);

            Metadata metadata = orgLandscapeMetadata(orgRoot, org);
            File reportsFolder = LandscapeAnalysisCommands.update(orgRoot, null, metadata, dataOnly);
            cli.saveExecutionStats(new File(reportsFolder, "data"));
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
        cli.updateMetadataFromCommandLine(cmd, metadata);
        File reportsFolder = LandscapeAnalysisCommands.update(root, null, metadata, dataOnly);
        cli.saveExecutionStats(new File(reportsFolder, "data"));
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
    static Integer nonNegativeIntOption(CommandLine cmd, Option option) {
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
}
