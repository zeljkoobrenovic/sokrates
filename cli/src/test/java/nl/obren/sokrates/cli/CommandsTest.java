/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class CommandsTest {

    private final Commands commands = new Commands();

    @Test
    public void initOptionsExposeExpectedFlags() {
        Options options = commands.getInitOptions();

        assertTrue(options.hasOption(Commands.ARG_SRC_ROOT));
        assertTrue(options.hasOption(Commands.ARG_CONF_FILE));
        assertTrue(options.hasOption(Commands.ARG_CONVENTIONS_FILE));
        assertTrue(options.hasOption(Commands.ARG_NAME));
    }

    @Test
    public void reportingOptionsExposeExpectedFlags() {
        Options options = commands.getReportingOptions();

        assertTrue(options.hasOption(Commands.ARG_CONF_FILE));
        assertTrue(options.hasOption(Commands.ARG_OUTPUT_FOLDER));
        assertTrue(options.hasOption(Commands.ARG_TIMEOUT));
    }

    @Test
    public void parserAcceptsValidInitArguments() throws Exception {
        Options options = commands.getInitOptions();

        CommandLine cmd = new DefaultParser().parse(options,
                new String[]{"-" + Commands.ARG_SRC_ROOT, "src", "-" + Commands.ARG_NAME, "demo"});

        assertEquals("src", cmd.getOptionValue(Commands.ARG_SRC_ROOT));
        assertEquals("demo", cmd.getOptionValue(Commands.ARG_NAME));
    }

    @Test
    public void srcRootOptionTakesAnArgument() {
        assertTrue(commands.getSrcRoot().hasArg());
        assertEquals(Commands.ARG_SRC_ROOT, commands.getSrcRoot().getOpt());
    }

    @Test
    public void commandNamesAreStable() {
        // these strings are the CLI's public contract; changing them breaks user scripts
        assertEquals("init", Commands.INIT);
        assertEquals("generateReports", Commands.GENERATE_REPORTS);
        assertEquals("updateConfig", Commands.UPDATE_CONFIG);
        assertEquals("updateLandscape", Commands.UPDATE_LANDSCAPE);
        assertEquals("extractGitHistory", Commands.EXTRACT_GIT_HISTORY);
    }
}
