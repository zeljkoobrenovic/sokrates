package nl.obren.sokrates.cli;

import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Commands {
    private static final Log LOG = LogFactory.getLog(Commands.class);

    // commands
    public static final String ANALYZE = "analyze";
    public static final String ANALYZE_DESCRIPTION = "One-shot analysis: extracts the git history (when the source root is a git repository), creates the analysis configuration if none exists (init), and generates the reports. The recommended way to get a first report: run it from the root of the code base without any options.";

    public static final String ANALYZE_GIT_REPO = "analyzeGitRepo";
    public static final String ANALYZE_GIT_REPO_DESCRIPTION = "Clones a git repository from its URL into a temporary folder (JGit, no git binary needed), runs analyze on it, and keeps only the analysis — config.json and reports/ — in <destFolder> (default: <currentFolder>/<owner>/<repository>, e.g. junit-team/junit4); the clone is deleted. Re-runs clone again and reuse the kept config.json, so edits survive. The output layout is what analyzeLandscape expects, so several analyzeGitRepo runs in one folder plus analyzeLandscape make a landscape. For private HTTPS repositories set the SOKRATES_GIT_TOKEN (and optionally SOKRATES_GIT_USER) environment variable.";

    public static final String INIT = "init";
    public static final String INIT_DESCRIPTION = "Creates a new Sokrates analysis configuration file based on standard and optional custom conventions";

    public static final String GENERATE_REPORTS = "generateReports";
    public static final String GENERATE_REPORTS_DESCRIPTION = "Generates Sokrates reports based on the analysis configuration";

    public static final String UPDATE_CONFIG = "updateConfig";
    public static final String UPDATE_CONFIG_DESCRIPTION = "Updates an analysis configuration file and completes missing fields";

    public static final String ADD_CUSTOM_TAB = "addCustomTab";
    public static final String ADD_CUSTOM_TAB_DESCRIPTION = "Adds a custom iframe tab to the repository report configuration (config.json customTabs). If a custom tab with the same label already exists, it is overwritten instead of added.";

    public static final String ANALYZE_LANDSCAPE = "analyzeLandscape";
    public static final String ANALYZE_LANDSCAPE_DESCRIPTION = "Creates or updates a Sokrates landscape report aggregating the repository analyses found under the analysis root (the landscape counterpart of analyze). With -url (repeatable) and/or -urls <file> (one git URL per line, # comments), it first runs analyzeGitRepo for each URL into <analysisRoot>/<owner>/<repository> (a failing repository is logged and skipped), then builds the landscape; without URLs it aggregates what is already there. Same options as updateLandscape, which is kept as the older name.";

    public static final String ANALYZE_GITHUB_ORG = "analyzeGitHubOrg";
    public static final String ANALYZE_GITHUB_ORG_DESCRIPTION = "Analyzes whole GitHub organizations (or user accounts): for every -org (repeatable) and/or login in -orgs <file>, lists its repositories with the GitHub REST API, filters them (forks and archived repositories are excluded unless -includeForks / -includeArchived; -pushedWithinDays, -includeRepoNamePattern / -excludeRepoNamePattern and -maxRepos narrow further), writes the selection to <analysisRoot>/<org>/repos.txt, analyzes each repository into <analysisRoot>/<org>/<repository> (the analyzeGitRepo step) and builds a landscape per organization in <analysisRoot>/<org>/_sokrates_landscape, named, described, linked and branded from the organization's GitHub profile (only fields you have not set). With several organizations a parent landscape in <analysisRoot>/_sokrates_landscape lists them as sub-landscapes. -listOnly just writes repos.txt; -prune deletes analyses of repositories no longer selected. Set SOKRATES_GIT_TOKEN for private repositories and the higher API rate limit.";

    public static final String ANALYZE_GITLAB_GROUP = "analyzeGitLabGroup";
    public static final String ANALYZE_GITLAB_GROUP_DESCRIPTION = "The GitLab counterpart of analyzeGitHubOrg: for every -group (repeatable; a full path like gitlab-org/ci-cd or a URL, a username also works) and/or path in -groups <file>, lists the projects of the group and all its subgroups with the GitLab REST API (gitlab.com, or the instance given with -gitlabUrl or by a -group URL), filters them with the same options (forks and archived excluded unless -includeForks / -includeArchived; -pushedWithinDays, -includeRepoNamePattern / -excludeRepoNamePattern, -maxRepos), writes the selection to <analysisRoot>/<group path>/repos.txt, analyzes each project into <analysisRoot>/<group path>/<project path> (subgroups kept as folders) and builds a landscape per group in <analysisRoot>/<group path>/_sokrates_landscape, named, described, linked and branded from the group's profile (only fields you have not set); several groups get a parent landscape. -listOnly and -prune as for analyzeGitHubOrg. Set SOKRATES_GIT_TOKEN (sent as PRIVATE-TOKEN) for private groups.";

    public static final String UPDATE_LANDSCAPE = "updateLandscape";
    public static final String UPDATE_LANDSCAPE_DESCRIPTION = "Updates or creates a Sokrates landscape report, aggregating results of multiple analyses; with -url / -urls it first clones and analyzes those git repositories (the older name of analyzeLandscape, same options)";

    public static final String UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME = "updateLandscapePeopleConfigByUserName";
    public static final String UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME_DESCRIPTION = "Updates (or creates) the landscape config-people.json by grouping all contributor emails sharing the same display name (userName) under one entry, joining the emails in the email field with ';'. Purely additive: appends only new emails to existing entries, never removes emails or entries.";

    public static final String UPDATE_PEOPLE_CONFIG_BY_USER_NAME = "updatePeopleConfigByUserName";
    public static final String UPDATE_PEOPLE_CONFIG_BY_USER_NAME_DESCRIPTION = "Single-repository version of updateLandscapePeopleConfigByUserName: updates (or creates) _sokrates/config-people.json by grouping all contributor emails sharing the same display name (userName) under one entry. Reads only the repository's git-history.txt, so run it after extractGitHistory (no generateReports needed). Same config file and people-config format as landscapes. Purely additive.";

    public static final String INIT_CONVENTIONS = "createConventionsFile";
    public static final String INIT_CONVENTIONS_DESCRIPTION = "Create a new analysis conventions file and saves it in <current-folder>/analysis_conventions.json ";

    public static final String EXPORT_STANDARD_CONVENTIONS = "exportStandardConventions";
    public static final String EXPORT_STANDARD_CONVENTIONS_DESCRIPTION = "Export standard Sokrates analysis convention to <current-folder>/standard_analysis_conventions.json.";

    public static final String EXTRACT_GIT_HISTORY = "extractGitHistory";
    public static final String EXTRACT_GIT_HISTORY_DESCRIPTION = "Extract a git history in a format used by Sokrates and saves it in the git-history.txt file";

    public static final String EXTRACT_GIT_SUB_HISTORY = "extractGitSubHistory";
    public static final String EXTRACT_GIT_SUB_HISTORY_DESCRIPTION = "A utility function to split a git history file (git-history.txt) into smaller ones based on a commit file path prefix, removing the prefix from file path in split files";

    public static final String EXTRACT_FILES = "extractFiles";
    public static final String EXTRACT_FILES_DESCRIPTION = "A utility function to extract specific files from a code based based on a path regex pattern. Used to simplify new analysis on a subset of the code base (e.g. only on files with a specific extension).";

    // arguments
    public static final String ARG_SRC_ROOT = "srcRoot";
    public static final String ARG_CONVENTIONS_FILE = "conventionsFile";
    public static final String ARG_NAME = "name";
    public static final String ARG_DESCRIPTION = "description";
    public static final String ARG_LOGO_LINK = "logoLink";
    public static final String ARG_CONF_FILE = "confFile";
    public static final String ARG_DATE = "date";
    public static final String ARG_OUTPUT_FOLDER = "outputFolder";
    public static final String ARG_HTML_REPORTS_FOLDER_NAME = "html";
    public static final String ARG_ANALYSIS_ROOT = "analysisRoot";
    public static final String ARG_TIMEOUT = "timeout";
    public static final String ARG_PREFIX = "prefix";
    public static final String ARG_PATTERN = "pattern";
    public static final String ARG_DEST_FOLDER = "destFolder";
    public static final String ARG_DEST_PARENT = "destParent";
    public static final String ARG_HELP = "help";

    public static final String ARG_SKIP_DUPLICATION_ANALYSES = "skipDuplication";
    public static final String ARG_SKIP_CORRELATION_ANALYSES = "skipCorrelations";
    public static final String ARG_ENABLE_DUPLICATION_ANALYSES = "enableDuplication";
    public static final String ARG_SKIP_COMPLEX_ANALYSES = "skipComplexAnalyses";
    public static final String ARG_SET_CACHE_FILES = "setCacheFiles";
    public static final String ARG_SKIP_GIT_HISTORY = "skipGitHistory";
    public static final String ARG_DATA_ONLY = "dataOnly";
    public static final String ARG_URL = "url";
    public static final String ARG_URLS = "urls";
    public static final String ARG_ORG = "org";
    public static final String ARG_ORGS = "orgs";
    public static final String ARG_GROUP = "group";
    public static final String ARG_GROUPS = "groups";
    public static final String ARG_GITLAB_URL = "gitlabUrl";
    public static final String ARG_INCLUDE_FORKS = "includeForks";
    public static final String ARG_INCLUDE_ARCHIVED = "includeArchived";
    public static final String ARG_PUSHED_WITHIN_DAYS = "pushedWithinDays";
    public static final String ARG_INCLUDE_REPO_NAME_PATTERN = "includeRepoNamePattern";
    public static final String ARG_EXCLUDE_REPO_NAME_PATTERN = "excludeRepoNamePattern";
    public static final String ARG_MAX_REPOS = "maxRepos";
    public static final String ARG_LIST_ONLY = "listOnly";
    public static final String ARG_PRUNE = "prune";
    public static final String ARG_BRANCH = "branch";
    public static final String ARG_DEPTH = "depth";

    public static final String RECURSIVE = "recursive";

    public static final String ARG_SET_NAME = "setName";
    public static final String ARG_SET_LOGO_LINK = "setLogoLink";
    public static final String ARG_SET_DESCRIPTION = "setDescription";
    public static final String ARG_ADD_LINK = "addLink";
    public static final String ARG_LABEL = "label";
    public static final String ARG_IFRAME_LINK = "iframeLink";

    // options
    private Option srcRoot = new Option(ARG_SRC_ROOT, true, "[OPTIONAL] the root folder of the code base to analyze (default is the current folder)");
    private Option conventionsFile = new Option(ARG_CONVENTIONS_FILE, true, "[OPTIONAL] the custom conventions JSON file path");
    private Option name = new Option(ARG_NAME, true, "[OPTIONAL] the repository name");
    private Option description = new Option(ARG_DESCRIPTION, true, "[OPTIONAL] the repository description");
    private Option logoLink = new Option(ARG_LOGO_LINK, true, "[OPTIONAL] the repository logo link");
    private Option confFile = new Option(ARG_CONF_FILE, true, "[OPTIONAL] the path to configuration file (default is \"<currentFolder>/_sokrates/config.json\")");
    private Option date = new Option(ARG_DATE, true, "[OPTIONAL] last date of source code update (default today), used for reports on active contributors. " +
            "You can also specify this date via the system variable \"" + DateUtils.ENV_SOKRATES_ANALYSIS_DATE + "\".");
    private Option analysisRoot = new Option(ARG_ANALYSIS_ROOT, true, "[OPTIONAL] the path to configuration file (default is \"<currentFolder>/_sokrates/config.json\")");
    private Option timeout = new Option(ARG_TIMEOUT, true, "[OPTIONAL] timeout in seconds");
    private Option prefix = new Option(ARG_PREFIX, true, "the path prefix");
    private Option pattern = new Option(ARG_PATTERN, true, "the file path regex pattern");
    private Option destRoot = new Option(ARG_DEST_FOLDER, true, "the destination folder (for analyzeGitRepo: [OPTIONAL] where the analysis (config.json + reports/) is kept, default is <currentFolder>/<owner>/<repository name from the URL>)");
    private Option destParent = new Option(ARG_DEST_PARENT, true, "[OPTIONAL] the destination parent folder");

    private Option outputFolder = new Option(ARG_OUTPUT_FOLDER, true, "[OPTIONAL] the folder where reports will be stored (default value is <currentFolder/_sokrates/reports>)");

    private Option skipComplexAnalyses = new Option(ARG_SKIP_COMPLEX_ANALYSES, false, "[OPTIONAL] skips complex analyses (duplication, dependencies, file caching)");
    private Option recursive = new Option(RECURSIVE, false, "[OPTIONAL] performs the operation recursively in all sub-folders");

    private Option skipDuplicationAnalyses = new Option(ARG_SKIP_DUPLICATION_ANALYSES, false, "[OPTIONAL] skips duplication analyses");
    private Option skipCorrelationAnalyses = new Option(ARG_SKIP_CORRELATION_ANALYSES, false, "[OPTIONAL] skips correlations analyses");
    private Option enableDuplicationAnalyses = new Option(ARG_ENABLE_DUPLICATION_ANALYSES, false, "[OPTIONAL] enables duplication analyses");

    private Option setName = new Option(ARG_SET_NAME, true, "[OPTIONAL] sets a repository name");
    private Option setDescription = new Option(ARG_SET_DESCRIPTION, true, "[OPTIONAL] sets a repository description");
    private Option setLogoLink = new Option(ARG_SET_LOGO_LINK, true, "[OPTIONAL] sets a repository logo link");
    private Option url = new Option(ARG_URL, true, "the git repository URL to clone (https://..., git@host:org/repo.git, file:///...)");
    private Option urls = new Option(ARG_URLS, true, "[OPTIONAL] a text file with one git repository URL per line (blank lines and # comments ignored); each is analyzed with analyzeGitRepo into <analysisRoot>/<owner>/<repository> before the landscape is built");

    {
        // Shown in the usage overview as -url <gitUrl> / -urls <file>, so the git nature of the
        // landscape's repository inputs is visible without opening the per-command help.
        url.setArgName("gitUrl");
        urls.setArgName("file");
    }
    private Option org = new Option(ARG_ORG, true, "a GitHub organization (or user) login, e.g. junit-team; repeatable");
    private Option orgs = new Option(ARG_ORGS, true, "[OPTIONAL] a text file with one GitHub organization (or user) login per line (blank lines and # comments ignored)");
    private Option group = new Option(ARG_GROUP, true, "a GitLab group path (e.g. gitlab-org/ci-cd, subgroups included) or URL, or a username; repeatable");
    private Option groups = new Option(ARG_GROUPS, true, "[OPTIONAL] a text file with one GitLab group path (or URL, or username) per line (blank lines and # comments ignored)");
    private Option gitlabUrl = new Option(ARG_GITLAB_URL, true, "[OPTIONAL] the GitLab instance (default https://gitlab.com, or the host of a -group given as a URL)");
    private Option includeForks = new Option(ARG_INCLUDE_FORKS, false, "[OPTIONAL] also analyzes forks (excluded by default)");
    private Option includeArchived = new Option(ARG_INCLUDE_ARCHIVED, false, "[OPTIONAL] also analyzes archived repositories (excluded by default)");
    private Option pushedWithinDays = new Option(ARG_PUSHED_WITHIN_DAYS, true, "[OPTIONAL] only repositories pushed to in the last N days (relative to -date, default today)");
    private Option includeRepoNamePattern = new Option(ARG_INCLUDE_REPO_NAME_PATTERN, true, "[OPTIONAL] only repositories whose name (or owner/name) matches this regex entirely, case-insensitive; repeatable, any match keeps the repository");
    private Option excludeRepoNamePattern = new Option(ARG_EXCLUDE_REPO_NAME_PATTERN, true, "[OPTIONAL] skips repositories whose name (or owner/name) matches this regex entirely, case-insensitive; repeatable");
    private Option maxRepos = new Option(ARG_MAX_REPOS, true, "[OPTIONAL] keeps at most N repositories per organization, the most recently pushed ones");
    private Option listOnly = new Option(ARG_LIST_ONLY, false, "[OPTIONAL] only lists and filters the repositories into <analysisRoot>/<org>/repos.txt, without cloning or analyzing anything (a dry run to review the selection and its size)");
    private Option prune = new Option(ARG_PRUNE, false, "[OPTIONAL] deletes the kept analyses (<analysisRoot>/<org>/<repository>) of repositories that are no longer selected, so the landscape stops showing them");

    {
        org.setArgName("login");
        orgs.setArgName("file");
        group.setArgName("path");
        groups.setArgName("file");
        gitlabUrl.setArgName("url");
        pushedWithinDays.setArgName("days");
        includeRepoNamePattern.setArgName("regex");
        excludeRepoNamePattern.setArgName("regex");
        maxRepos.setArgName("count");
    }
    private Option branch = new Option(ARG_BRANCH, true, "[OPTIONAL] the branch to analyze (default is the remote's default branch)");
    private Option depth = new Option(ARG_DEPTH, true, "[OPTIONAL] shallow clone depth (default is the full history; a shallow history makes the contributor and trend reports incomplete)");
    private Option dataOnly = new Option(ARG_DATA_ONLY, false, "[OPTIONAL] stores only the analysis data — for a repository reports/data/data.zip (which landscapes read), for a landscape _sokrates_landscape/data/data.zip (which a parent landscape reads) — no HTML reports, contributor pages, explorers, visuals, source viewer or index page");
    private Option skipGitHistory = new Option(ARG_SKIP_GIT_HISTORY, false, "[OPTIONAL] does not (re)extract the git history; an existing git-history.txt is still used");
    private Option setCacheFiles = new Option(ARG_SET_CACHE_FILES, true, "[OPTIONAL] sets a cache file flag ('true' or 'false')");
    private Option addLink = new Option(ARG_ADD_LINK, true, "[OPTIONAL] adds a new link");
    private Option label = new Option(ARG_LABEL, true, "the custom tab label (unique; an existing tab with the same label is overwritten)");
    private Option iframeLink = new Option(ARG_IFRAME_LINK, true, "the URL shown in the tab's iframe (absolute, or relative to the report's html/ folder)");
    private Option help = new Option(ARG_HELP, true, "[OPTIONAL] gives extra details about a command usage");

    private List<CommandUsage> usageInfo() {
        List<CommandUsage> commands = new ArrayList<>();

        commands.add(new CommandUsage(ANALYZE, ANALYZE_DESCRIPTION, getAnalyzeOptions()));
        commands.add(new CommandUsage(ANALYZE_GIT_REPO, ANALYZE_GIT_REPO_DESCRIPTION, getAnalyzeGitRepoOptions()));
        commands.add(new CommandUsage(INIT, INIT_DESCRIPTION, getInitOptions()));
        commands.add(new CommandUsage(GENERATE_REPORTS, GENERATE_REPORTS_DESCRIPTION, getReportingOptions()));
        commands.add(new CommandUsage(ANALYZE_LANDSCAPE, ANALYZE_LANDSCAPE_DESCRIPTION, getUpdateLandscapeOptions()));
        commands.add(new CommandUsage(UPDATE_LANDSCAPE, UPDATE_LANDSCAPE_DESCRIPTION, getUpdateLandscapeOptions()));
        commands.add(new CommandUsage(ANALYZE_GITHUB_ORG, ANALYZE_GITHUB_ORG_DESCRIPTION, getAnalyzeGitHubOrgOptions()));
        commands.add(new CommandUsage(ANALYZE_GITLAB_GROUP, ANALYZE_GITLAB_GROUP_DESCRIPTION, getAnalyzeGitLabGroupOptions()));
        commands.add(new CommandUsage(UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME, UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME_DESCRIPTION, getUpdateLandscapePeopleConfigByUserNameOptions()));
        commands.add(new CommandUsage(UPDATE_PEOPLE_CONFIG_BY_USER_NAME, UPDATE_PEOPLE_CONFIG_BY_USER_NAME_DESCRIPTION, getUpdatePeopleConfigByUserNameOptions()));
        commands.add(new CommandUsage(UPDATE_CONFIG, UPDATE_CONFIG_DESCRIPTION, getUpdateConfigOptions()));
        commands.add(new CommandUsage(ADD_CUSTOM_TAB, ADD_CUSTOM_TAB_DESCRIPTION, getAddCustomTabOptions()));
        commands.add(new CommandUsage(EXTRACT_GIT_HISTORY, EXTRACT_GIT_HISTORY_DESCRIPTION, getExtractGitHistoryOption()));

        commands.add(new CommandUsage(INIT_CONVENTIONS, INIT_CONVENTIONS_DESCRIPTION, null));
        commands.add(new CommandUsage(EXPORT_STANDARD_CONVENTIONS, EXPORT_STANDARD_CONVENTIONS_DESCRIPTION, null));
        commands.add(new CommandUsage(EXTRACT_GIT_SUB_HISTORY, EXTRACT_GIT_SUB_HISTORY_DESCRIPTION, getExtractGitSubHistoryOption()));

        return commands;
    }

    public void usage() {
        List<CommandUsage> commandUsages = usageInfo();
        printlnUsage("");
        printlnUsage("Usage: java -jar sokrates.jar <command> <options>");
        printlnUsage("");
        printlnUsage("Help: java -jar sokrates.jar <command> -help");
        printlnUsage("");
        printlnUsage("Commands: " + commandUsages.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        commandUsages.forEach(commandUsage -> {
            printlnUsage("");
            printlnUsage("* " + commandUsage.getName() + ": " + commandUsage.getDescription());
            if (commandUsage.getOptions() != null) {
                String options = commandUsage.getOptions().getOptions().stream().map(o -> {
                    String text = "";
                    if (o.hasArg()) {
                        // an option's argument name (when set, e.g. <gitUrl>, <file>) says what the value is
                        text = "-" + o.getOpt() + " <" + (o.getArgName() != null ? o.getArgName() : "arg") + ">";
                    } else if (o.hasArgs()) {
                        text = "-" + o.getOpt() + " <args>";
                    } else {
                        text = "-" + o.getOpt() + "";
                    }
                    if (!o.isRequired()) {
                        text = "[" + text + "]";
                    }
                    return text;
                }).collect(Collectors.joining(" "));
                printlnUsage("   - options: " + options);
            }
        });
        printlnUsage("");
        printlnUsage("");
    }

    private void printlnUsage(String line) {
        System.out.println(line);
    }

    public void usage(String prefix, Options options, String description) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(80);
        String cmdLineSyntax = "java -jar sokrates.jar " + prefix + "\n\ndescription: " + description + "\n\noptions:\n";
        printlnUsage("");
        if (options != null) {
            formatter.printHelp(cmdLineSyntax + "", options);
        } else {
            formatter.printHelp(cmdLineSyntax, new Options());
        }
        printlnUsage("");
    }


    public Options getReportingOptions() {
        Options options = new Options();
        options.addOption(confFile);
        options.addOption(outputFolder);
        options.addOption(dataOnly);
        options.addOption(timeout);
        options.addOption(date);
        options.addOption(help);

        help.setArgs(0);

        return options;
    }

    public Options getAnalyzeOptions() {
        Options options = new Options();
        options.addOption(srcRoot);
        options.addOption(confFile);
        options.addOption(outputFolder);
        options.addOption(dataOnly);
        options.addOption(conventionsFile);
        options.addOption(name);
        options.addOption(description);
        options.addOption(logoLink);
        options.addOption(addLink);
        options.addOption(skipGitHistory);
        options.addOption(date);
        options.addOption(timeout);
        options.addOption(help);

        help.setArgs(0);

        return options;
    }

    public Options getAnalyzeGitRepoOptions() {
        Options options = new Options();
        options.addOption(url);
        options.addOption(destRoot);
        options.addOption(branch);
        options.addOption(depth);
        options.addOption(dataOnly);
        options.addOption(conventionsFile);
        options.addOption(name);
        options.addOption(description);
        options.addOption(logoLink);
        options.addOption(addLink);
        options.addOption(date);
        options.addOption(timeout);
        options.addOption(help);

        help.setArgs(0);

        return options;
    }

    public Options getInitOptions() {
        Options options = new Options();
        options.addOption(srcRoot);
        options.addOption(confFile);
        options.addOption(conventionsFile);
        options.addOption(name);
        options.addOption(description);
        options.addOption(logoLink);
        options.addOption(addLink);
        options.addOption(timeout);
        options.addOption(help);

        addLink.setArgs(2);
        help.setArgs(0);

        return options;
    }

    public Options getAddCustomTabOptions() {
        Options options = new Options();
        options.addOption(confFile);
        options.addOption(label);
        options.addOption(iframeLink);
        options.addOption(help);
        return options;
    }

    public Options getUpdateConfigOptions() {
        Options options = new Options();
        options.addOption(confFile);
        options.addOption(skipComplexAnalyses);
        options.addOption(setCacheFiles);
        options.addOption(setName);
        options.addOption(setDescription);
        options.addOption(setLogoLink);
        options.addOption(addLink);
        options.addOption(timeout);
        options.addOption(help);

        addLink.setArgs(2);
        confFile.setRequired(false);
        skipComplexAnalyses.setRequired(false);
        setName.setRequired(false);
        setDescription.setRequired(false);
        setLogoLink.setRequired(false);
        help.setArgs(0);

        return options;
    }

    public Options getExtractFilesOption() {
        Options options = new Options();
        options.addOption(analysisRoot);
        options.addOption(pattern);
        options.addOption(destRoot);
        options.addOption(destParent);
        options.addOption(help);

        help.setArgs(0);

        return options;
    }

    public Options getExtractGitHistoryOption() {
        Options options = new Options();
        options.addOption(analysisRoot);
        options.addOption(help);

        analysisRoot.setRequired(false);
        help.setArgs(0);

        return options;
    }

    public Options getExtractGitSubHistoryOption() {
        Options options = new Options();
        options.addOption(prefix);
        options.addOption(analysisRoot);
        options.addOption(help);

        help.setArgs(0);

        return options;
    }

    public Options getAnalyzeGitLabGroupOptions() {
        Options options = new Options();
        options.addOption(group);
        options.addOption(groups);
        options.addOption(gitlabUrl);
        addOrganizationAnalysisOptions(options);
        return options;
    }

    public Options getAnalyzeGitHubOrgOptions() {
        Options options = new Options();
        options.addOption(org);
        options.addOption(orgs);
        addOrganizationAnalysisOptions(options);
        return options;
    }

    // The options analyzeGitHubOrg and analyzeGitLabGroup share: selection, dry run, pruning, and the pass-through analysis options.
    private void addOrganizationAnalysisOptions(Options options) {
        options.addOption(analysisRoot);
        options.addOption(includeForks);
        options.addOption(includeArchived);
        options.addOption(pushedWithinDays);
        options.addOption(includeRepoNamePattern);
        options.addOption(excludeRepoNamePattern);
        options.addOption(maxRepos);
        options.addOption(listOnly);
        options.addOption(prune);
        options.addOption(depth);
        options.addOption(dataOnly);
        options.addOption(conventionsFile);
        options.addOption(setName);
        options.addOption(setDescription);
        options.addOption(setLogoLink);
        options.addOption(addLink);
        options.addOption(timeout);
        options.addOption(date);
        options.addOption(help);

        help.setArgs(0);
    }

    public Option getGroup() {
        return group;
    }

    public Option getGroups() {
        return groups;
    }

    public Option getGitlabUrl() {
        return gitlabUrl;
    }

    public Option getOrg() {
        return org;
    }

    public Option getOrgs() {
        return orgs;
    }

    public Option getIncludeForks() {
        return includeForks;
    }

    public Option getIncludeArchived() {
        return includeArchived;
    }

    public Option getPushedWithinDays() {
        return pushedWithinDays;
    }

    public Option getIncludeRepoNamePattern() {
        return includeRepoNamePattern;
    }

    public Option getExcludeRepoNamePattern() {
        return excludeRepoNamePattern;
    }

    public Option getMaxRepos() {
        return maxRepos;
    }

    public Option getListOnly() {
        return listOnly;
    }

    public Option getPrune() {
        return prune;
    }

    public Options getUpdateLandscapeOptions() {
        Options options = new Options();
        options.addOption(analysisRoot);
        options.addOption(url);
        options.addOption(urls);
        options.addOption(depth);
        options.addOption(dataOnly);
        options.addOption(conventionsFile);
        options.addOption(confFile);
        options.addOption(recursive);
        options.addOption(setName);
        options.addOption(setDescription);
        options.addOption(setLogoLink);
        options.addOption(addLink);
        options.addOption(timeout);
        options.addOption(date);
        options.addOption(help);

        setName.setRequired(false);
        setDescription.setRequired(false);
        setLogoLink.setRequired(false);
        addLink.setRequired(false);

        addLink.setArgs(2);
        help.setArgs(0);

        return options;
    }

    public Options getUpdatePeopleConfigByUserNameOptions() {
        Options options = new Options();
        options.addOption(confFile);
        options.addOption(timeout);
        options.addOption(help);

        confFile.setRequired(false);
        help.setArgs(0);

        return options;
    }

    public Options getUpdateLandscapePeopleConfigByUserNameOptions() {
        Options options = new Options();
        options.addOption(analysisRoot);
        options.addOption(confFile);
        options.addOption(timeout);
        options.addOption(help);

        analysisRoot.setRequired(false);
        confFile.setRequired(false);
        help.setArgs(0);

        return options;
    }

    public Option getSrcRoot() {
        return srcRoot;
    }

    public Option getConventionsFile() {
        return conventionsFile;
    }

    public Option getName() {
        return name;
    }

    public Option getDescription() {
        return description;
    }

    public Option getLogoLink() {
        return logoLink;
    }

    public Option getLabel() {
        return label;
    }

    public Option getIframeLink() {
        return iframeLink;
    }

    public Option getConfFile() {
        return confFile;
    }

    public Option getDate() {
        return date;
    }

    public Option getAnalysisRoot() {
        return analysisRoot;
    }

    public Option getTimeout() {
        return timeout;
    }

    public Option getPrefix() {
        return prefix;
    }

    public Option getPattern() {
        return pattern;
    }

    public Option getDestRoot() {
        return destRoot;
    }

    public Option getDestParent() {
        return destParent;
    }


    public Option getOutputFolder() {
        return outputFolder;
    }

    public Option getSkipComplexAnalyses() {
        return skipComplexAnalyses;
    }

    public Option getSkipDuplicationAnalyses() {
        return skipDuplicationAnalyses;
    }
    public Option getSkipCorrelationAnalyses() {
        return skipCorrelationAnalyses;
    }

    public Option getEnableDuplicationAnalyses() {
        return enableDuplicationAnalyses;
    }

    public Option getSetName() {
        return setName;
    }

    public Option getSetDescription() {
        return setDescription;
    }

    public Option getSetLogoLink() {
        return setLogoLink;
    }

    public Option getUrl() {
        return url;
    }

    public Option getUrls() {
        return urls;
    }

    public Option getBranch() {
        return branch;
    }

    public Option getDepth() {
        return depth;
    }

    public Option getDataOnly() {
        return dataOnly;
    }

    public Option getSkipGitHistory() {
        return skipGitHistory;
    }

    public Option getSetCacheFiles() {
        return setCacheFiles;
    }

    public Option getAddLink() {
        return addLink;
    }

    public Option getHelp() {
        return help;
    }

    public Option getRecursive() {
        return recursive;
    }
}
