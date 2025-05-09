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
    public static final String INIT = "init";
    public static final String INIT_DESCRIPTION = "Creates a new Sokrates analysis configuration file based on standard and optional custom conventions";

    public static final String GENERATE_REPORTS = "generateReports";
    public static final String GENERATE_REPORTS_DESCRIPTION = "Generates Sokrates reports based on the analysis configuration";

    public static final String UPDATE_CONFIG = "updateConfig";
    public static final String UPDATE_CONFIG_DESCRIPTION = "Updates an analysis configuration file and completes missing fields";

    public static final String UPDATE_LANDSCAPE = "updateLandscape";
    public static final String UPDATE_LANDSCAPE_DESCRIPTION = "Updates or creates a Sokrates landscape report, aggregating results of multiple analyses";

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
    public static final String ARG_USE_INTERNAL_GRAPHVIZ = "internalGraphviz";
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

    public static final String RECURSIVE = "recursive";

    public static final String ARG_SET_NAME = "setName";
    public static final String ARG_SET_LOGO_LINK = "setLogoLink";
    public static final String ARG_SET_DESCRIPTION = "setDescription";
    public static final String ARG_ADD_LINK = "addLink";

    // options
    private Option srcRoot = new Option(ARG_SRC_ROOT, true, "[OPTIONAL] the folder where reports will be stored (default is \"<currentFolder>/_sokrates/reports/)");
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
    private Option destRoot = new Option(ARG_DEST_FOLDER, true, "the destination folder");
    private Option destParent = new Option(ARG_DEST_PARENT, true, "[OPTIONAL] the destination parent folder");
    private Option internalGraphviz = new Option(ARG_USE_INTERNAL_GRAPHVIZ, false, "[OPTIONAL] use internal Graphviz library (by default external dot program is used, you may specify the external dot path via the system variable GRAPHVIZ_DOT)");

    private Option outputFolder = new Option(ARG_OUTPUT_FOLDER, true, "[OPTIONAL] the folder where reports will be stored (default value is <currentFolder/_sokrates/reports>)");

    private Option skipComplexAnalyses = new Option(ARG_SKIP_COMPLEX_ANALYSES, false, "[OPTIONAL] skips complex analyses (duplication, dependencies, file caching)");
    private Option recursive = new Option(RECURSIVE, false, "[OPTIONAL] performs the operation recursively in all sub-folders");

    private Option skipDuplicationAnalyses = new Option(ARG_SKIP_DUPLICATION_ANALYSES, false, "[OPTIONAL] skips duplication analyses");
    private Option skipCorrelationAnalyses = new Option(ARG_SKIP_CORRELATION_ANALYSES, false, "[OPTIONAL] skips correlations analyses");
    private Option enableDuplicationAnalyses = new Option(ARG_ENABLE_DUPLICATION_ANALYSES, false, "[OPTIONAL] enables duplication analyses");

    private Option setName = new Option(ARG_SET_NAME, true, "[OPTIONAL] sets a repository name");
    private Option setDescription = new Option(ARG_SET_DESCRIPTION, true, "[OPTIONAL] sets a repository description");
    private Option setLogoLink = new Option(ARG_SET_LOGO_LINK, true, "[OPTIONAL] sets a repository logo link");
    private Option setCacheFiles = new Option(ARG_SET_CACHE_FILES, true, "[OPTIONAL] sets a cache file flag ('true' or 'false')");
    private Option addLink = new Option(ARG_ADD_LINK, true, "[OPTIONAL] adds a new link");
    private Option help = new Option(ARG_HELP, true, "[OPTIONAL] gives extra details about a command usage");

    private List<CommandUsage> usageInfo() {
        List<CommandUsage> commands = new ArrayList<>();

        commands.add(new CommandUsage(INIT, INIT_DESCRIPTION, getInitOptions()));
        commands.add(new CommandUsage(GENERATE_REPORTS, GENERATE_REPORTS_DESCRIPTION, getReportingOptions()));
        commands.add(new CommandUsage(UPDATE_LANDSCAPE, UPDATE_LANDSCAPE_DESCRIPTION, getUpdateLandscapeOptions()));
        commands.add(new CommandUsage(UPDATE_CONFIG, UPDATE_CONFIG_DESCRIPTION, getUpdateConfigOptions()));
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
                        text = "-" + o.getOpt() + " <arg>";
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
        options.addOption(internalGraphviz);
        options.addOption(timeout);
        options.addOption(date);
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

    public Options getUpdateLandscapeOptions() {
        Options options = new Options();
        options.addOption(analysisRoot);
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

    public Option getInternalGraphviz() {
        return internalGraphviz;
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
