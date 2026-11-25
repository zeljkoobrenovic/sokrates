package nl.obren.sokrates.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The MCP stdio transport: newline-delimited JSON-RPC on stdin/stdout until stdin closes. stdout carries only
 * protocol messages, so {@link #protocolOut()} must run before anything logs: it routes the logging and any
 * stray {@code System.out} output to stderr and returns the real stdout.
 */
public class McpStdio {
    public static PrintStream protocolOut() {
        PrintStream protocol = new PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, StandardCharsets.UTF_8);
        System.setOut(System.err);
        // Every logger context (commons-logging may bind to a different one than LogManager's default): console → stderr.
        redirectConsoleLogging((LoggerContext) LogManager.getContext(false));
        redirectConsoleLogging((LoggerContext) LogManager.getContext(true));
        redirectConsoleLogging(LoggerContext.getContext(McpStdio.class.getClassLoader(), false, null));
        return protocol;
    }

    private static void redirectConsoleLogging(LoggerContext context) {
        Configuration configuration = context.getConfiguration();
        ConsoleAppender stderr = ConsoleAppender.newBuilder().setName("mcp-stderr")
                .setTarget(ConsoleAppender.Target.SYSTEM_ERR)
                .setLayout(PatternLayout.newBuilder().withPattern("%d{HH:mm:ss} %-5level %logger{36} - %msg%n").build())
                .setConfiguration(configuration).build();
        stderr.start();
        configuration.addAppender(stderr);
        for (LoggerConfig logger : configuration.getLoggers().values()) {
            for (String name : List.copyOf(logger.getAppenders().keySet())) {
                if (logger.getAppenders().get(name) instanceof ConsoleAppender && !name.equals("mcp-stderr")) {
                    logger.removeAppender(name);
                }
            }
        }
        LoggerConfig root = configuration.getRootLogger();
        if (!root.getAppenders().containsKey("mcp-stderr")) {
            root.addAppender(stderr, null, null);
        }
        context.updateLoggers();
    }

    public static void run(McpHandler handler, InputStream in, PrintStream out) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            String response = handler.handle(line);
            if (response != null) {
                out.println(response);
                out.flush();
            }
        }
    }
}
