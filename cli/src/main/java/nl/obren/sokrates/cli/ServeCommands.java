package nl.obren.sokrates.cli;

import nl.obren.sokrates.server.AnalysisCatalog;
import nl.obren.sokrates.server.McpHandler;
import nl.obren.sokrates.server.McpStdio;
import nl.obren.sokrates.server.PeoplePrivacy;
import nl.obren.sokrates.server.SokratesHttpServer;
import nl.obren.sokrates.server.SokratesQueryService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

/** The serve command: the server module's HTTP server (REST + MCP + reports), or MCP over stdio with -mcpStdio. */
class ServeCommands {
    static final String VERSION = "1.0";
    // How often the analysis folder is rescanned for added or removed analyses (changed data is noticed per request).
    static final long RESCAN_MILLIS = 60_000;
    private final Commands commands;

    ServeCommands(Commands commands) {
        this.commands = commands;
    }

    void serve(String[] args) throws ParseException, IOException {
        Options options = commands.getServeOptions();
        CommandLine cmd = new DefaultParser().parse(options, args);
        if (cmd.hasOption(commands.getHelp().getOpt())) {
            CommandLineInterface.helpMode = true;
            commands.usage(Commands.SERVE, options, Commands.SERVE_DESCRIPTION);
            return;
        }
        boolean stdio = cmd.hasOption(commands.getMcpStdio().getOpt());
        // Before anything logs: stdout belongs to the protocol.
        PrintStream protocolOut = stdio ? McpStdio.protocolOut() : null;
        CommandLineInterface.helpMode = true; // no processing-time summary on exit (it would print to stdout)

        File root = new File(StringUtils.defaultIfBlank(cmd.getOptionValue(commands.getServeRoot().getOpt()), "."));
        if (!root.isDirectory()) {
            throw new ParseException("The -root folder \"" + root.getPath() + "\" does not exist.");
        }
        PeoplePrivacy privacy = cmd.hasOption(commands.getHidePeople().getOpt())
                ? PeoplePrivacy.hidden(System.getenv("SOKRATES_PSEUDONYM_KEY")) : PeoplePrivacy.visible();
        SokratesQueryService service = new SokratesQueryService(new AnalysisCatalog(root, RESCAN_MILLIS), privacy);
        McpHandler mcp = new McpHandler(service, VERSION);
        if (stdio) {
            McpStdio.run(mcp, System.in, protocolOut);
            return;
        }
        String token = StringUtils.defaultIfBlank(cmd.getOptionValue(commands.getToken().getOpt()), System.getenv("SOKRATES_SERVER_TOKEN"));
        String host = StringUtils.defaultIfBlank(cmd.getOptionValue(commands.getHost().getOpt()), "127.0.0.1");
        int port;
        try {
            port = Integer.parseInt(StringUtils.defaultIfBlank(cmd.getOptionValue(commands.getPort().getOpt()), "8080").trim());
        } catch (NumberFormatException e) {
            throw new ParseException("-port must be a number");
        }
        SokratesHttpServer server = new SokratesHttpServer(service, mcp, token);
        server.start(host, port);
        service.getCatalog().snapshot();
        try {
            new CountDownLatch(1).await(); // until the process is stopped
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            server.stop();
        }
    }
}
