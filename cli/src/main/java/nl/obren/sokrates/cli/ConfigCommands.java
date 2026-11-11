package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.*;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CustomTab;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomConventionsHelper;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;

/** The updateConfig, addCustomTab, createConventionsFile and exportStandardConventions commands. Moved out of {@link CommandLineInterface}. */
class ConfigCommands {
    private static final Log LOG = LogFactory.getLog(ConfigCommands.class);
    private final CommandLineInterface cli;
    private final Commands commands;

    ConfigCommands(CommandLineInterface cli, Commands commands) {
        this.cli = cli;
        this.commands = commands;
    }

    void updateConfig(String[] args) throws ParseException, IOException {
        Options options = commands.getUpdateConfigOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            CommandLineInterface.helpMode = true;
            commands.usage(Commands.UPDATE_CONFIG, commands.getUpdateConfigOptions(), Commands.UPDATE_CONFIG_DESCRIPTION);
            return;
        }

        cli.startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(commands.getSrcRoot().getOpt());
        if (!cmd.hasOption(commands.getSrcRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File confFile = cli.getConfigFile(cmd, root);
        LOG.info("Configuration file '" + confFile.getPath() + "'.");

        String jsonContent = FileUtils.readFileToString(confFile, UTF_8);
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);

        applyAnalysisOptions(cmd, codeConfiguration);

        Metadata metadata = codeConfiguration.getMetadata();
        cli.updateMetadataFromCommandLine(cmd, metadata);

        String cacheFileValue = CommandLineInterface.optionValueOrNull(cmd, commands.getSetCacheFiles());
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

    void addCustomTab(String[] args) throws ParseException, IOException {
        Options options = commands.getAddCustomTabOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            CommandLineInterface.helpMode = true;
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

        File confFile = cli.getConfigFile(cmd, new File("."));
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

    void exportConventions(String[] args) throws ParseException, IOException {
        File file = new File("standard_analysis_conventions.json");

        CustomConventionsHelper.saveStandardConventionsToFile(file);

        LOG.info("A standard conventions file saved to '" + file.getPath() + "'.");
    }

    void createNewConventionsFile(String[] args) throws ParseException, IOException {
        File file = new File("analysis_conventions.json");

        CustomConventionsHelper.saveEmptyConventionsToFile(file);

        LOG.info("A new conventions file saved to '" + file.getPath() + "'.");
    }
}
