package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.utils.*;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeAnalysisCommands;
import nl.obren.sokrates.reports.landscape.statichtml.RepositoryPeopleConfigCommands;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;

/** The updateLandscapePeopleConfigByUserName and updatePeopleConfigByUserName commands. Moved out of {@link CommandLineInterface}, which keeps the option plumbing and the analyze pipeline. */
class PeopleConfigCommands {
    private static final Log LOG = LogFactory.getLog(PeopleConfigCommands.class);
    private final CommandLineInterface cli;
    private final Commands commands;

    PeopleConfigCommands(CommandLineInterface cli, Commands commands) {
        this.cli = cli;
        this.commands = commands;
    }

    void updateLandscapePeopleConfigByUserName(String[] args) throws ParseException {
        Options options = commands.getUpdateLandscapePeopleConfigByUserNameOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            CommandLineInterface.helpMode = true;
            commands.usage(Commands.UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME,
                    commands.getUpdateLandscapePeopleConfigByUserNameOptions(),
                    Commands.UPDATE_LANDSCAPE_PEOPLE_CONFIG_BY_USER_NAME_DESCRIPTION);
            return;
        }

        cli.startTimeoutIfDefined(cmd);

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

    void updatePeopleConfigByUserName(String[] args) throws ParseException {
        Options options = commands.getUpdatePeopleConfigByUserNameOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            CommandLineInterface.helpMode = true;
            commands.usage(Commands.UPDATE_PEOPLE_CONFIG_BY_USER_NAME,
                    commands.getUpdatePeopleConfigByUserNameOptions(),
                    Commands.UPDATE_PEOPLE_CONFIG_BY_USER_NAME_DESCRIPTION);
            return;
        }

        cli.startTimeoutIfDefined(cmd);

        // Same default as generateReports: ./_sokrates/config.json when -confFile is not given.
        File sokratesConfigFile;
        if (cmd.hasOption(commands.getConfFile().getOpt())) {
            sokratesConfigFile = new File(cmd.getOptionValue(commands.getConfFile().getOpt()));
        } else {
            sokratesConfigFile = new File("./_sokrates/config.json");
        }

        RepositoryPeopleConfigCommands.updatePeopleConfigByUserName(sokratesConfigFile);
    }
}
