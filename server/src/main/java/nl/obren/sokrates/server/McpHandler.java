package nl.obren.sokrates.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A Model Context Protocol server over the {@link SokratesQueryService}: JSON-RPC 2.0 with {@code initialize},
 * {@code ping}, {@code tools/list} and {@code tools/call}. Transport-neutral — {@link #handle(String)} takes one
 * message (or a batch) and returns the response, or null for notifications — so the stdio loop and the HTTP
 * {@code POST /mcp} endpoint share it. Hand-written rather than an SDK to keep Sokrates free of runtime
 * dependencies; the tool surface is small on purpose: coarse, task-shaped tools that return compact JSON.
 */
public class McpHandler {
    private static final Log LOG = LogFactory.getLog(McpHandler.class);
    static final List<String> PROTOCOL_VERSIONS = List.of("2025-06-18", "2025-03-26", "2024-11-05");
    private static final String INSTRUCTIONS = "Read-only access to Sokrates source-code analyses (sokrates.dev) of repositories and landscapes. "
            + "Start with get_landscape_overview for a portfolio, list_repositories to find repositories, then get_repository for one repository's size, languages, "
            + "quality profiles, ease-of-change scores, activity and components, get_hotspots for the files to look at first, "
            + "who_knows and get_contributors for the people, get_dependencies for the component dependencies, and "
            + "search_findings for the AI findings the sokrates-skills scanners wrote. Repositories are named by id or name (often owner/repo). "
            + "Numbers were measured when the analysis ran: every time window (30 days, 90 days, past year) ends at the repository's "
            + "analyzedOn date, not today, and analysisAgeDays says how old that is. When an analysis is more than a few weeks old, say so "
            + "and give the date with any 'recent' number. reportUrl values are paths on this server.";

    private final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private final SokratesQueryService service;
    private final String serverVersion;
    private final Map<String, Tool> tools;

    private record Tool(String name, String description, ObjectNode inputSchema, Function<JsonNode, Object> call) {
    }

    public McpHandler(SokratesQueryService service, String serverVersion) {
        this.service = service;
        this.serverVersion = serverVersion;
        this.tools = new java.util.LinkedHashMap<>();
        register("list_landscapes", "Lists the landscapes (portfolios of repositories) with their repository counts.",
                schema(), args -> Map.of("landscapes", service.landscapes()));
        register("get_landscape_overview",
                "A landscape (or, without landscape, every analysis) at a glance: repository counts (with code, active in 30 / 365 "
                        + "days), lines of code per scope and main language, ease-of-change grade distributions and size-weighted averages, "
                        + "duplication, commits and distinct contributors (merged as the landscape merges people; bots apart), AI findings "
                        + "per severity, and the largest, most active and lowest-scoring repositories (the last among those with 1,000+ lines). "
                        + "Start here for portfolio questions.",
                schema(prop("landscape", "string", "landscape id from list_landscapes; omit for every analysis")),
                args -> service.overview(text(args, "landscape")));
        register("list_repositories",
                "Lists repositories with main language, lines of code, ease-of-change scores (0-10, A-E, for people and for AI agents), "
                        + "30-day commits and contributors, and AI findings count. query: terms split by ';' - mainLang:java,kt (main language), "
                        + "includesLang:tf (contains any code in), any other term matches name/description. "
                        + "sort: loc (default), name, commits, humanScore, aiScore (lowest first), findings.",
                schema(prop("query", "string", "filter, e.g. \"mainLang:java;payments\""),
                        prop("landscape", "string", "landscape id from list_landscapes"),
                        prop("sort", "string", "loc | name | commits | humanScore | aiScore | findings"),
                        prop("limit", "integer", "max items (default 50)"), prop("offset", "integer", "items to skip")),
                args -> service.repositories(text(args, "query"), text(args, "landscape"), text(args, "sort"),
                        number(args, "offset"), number(args, "limit")));
        register("get_repository",
                "One repository's summary: size per scope, languages, duplication, file size / unit size / unit complexity risk profiles, "
                        + "ease-of-change scores with sub-scores, commit activity and logical components.",
                schema(required(prop("repository", "string", "repository id or name"))),
                args -> service.repository(text(args, "repository"))
                        .orElseThrow(() -> new SokratesQueryService.NotFoundException(service.notFound(text(args, "repository")))));
        register("get_hotspots",
                "The main files to look at first in a repository, ranked as the report's Highlights tab: complexity (McCabe sum of "
                        + "units, or lines of code) times the days the file changed in the past 365 days (window=90: commits in 90 days; "
                        + "window=all: days changed, all time; all time also when nothing changed in the window).",
                schema(required(prop("repository", "string", "repository id or name")),
                        prop("window", "string", "365 (default), 90 or all"), prop("limit", "integer", "max files (default 10)")),
                args -> service.hotspots(text(args, "repository"), text(args, "window"), number(args, "limit")));
        register("get_contributors",
                "A repository's contributors (as the analysis identified them) with commits in the past 30/90/365 days and all time, "
                        + "first and latest commit, most active in the past year first. Bots are left out unless includeBots.",
                schema(required(prop("repository", "string", "repository id or name")),
                        prop("query", "string", "terms split by ';' matched in email or name"),
                        prop("includeBots", "boolean", "include bots (default false)"),
                        prop("limit", "integer", "max items (default 50)"), prop("offset", "integer", "items to skip")),
                args -> service.contributors(text(args, "repository"), text(args, "query"), args.path("includeBots").asBoolean(false),
                        number(args, "offset"), number(args, "limit")));
        register("who_knows",
                "Who knows a part of the main code: the people who changed it, most active in the past 365 days first, with the "
                        + "top person's share of the past year's changes (a high share is a knowledge / bus-factor risk). The part: a file "
                        + "or folder (path), a logical component and/or languages (file extensions, e.g. tf or java,kt); all main code "
                        + "when none is given. In one repository, or without repository across a landscape (people merged as in the "
                        + "landscape report) or all analyses, then with each person's repository count and top repositories.",
                schema(prop("repository", "string", "repository id or name; omit to search across repositories"),
                        prop("landscape", "string", "landscape id, when searching across repositories"),
                        prop("path", "string", "a file or folder, relative to the repository root, e.g. src/payments"),
                        prop("component", "string", "a component name or decomposition::component (see get_repository decompositions)"),
                        prop("decomposition", "string", "limit a component name to this decomposition"),
                        prop("languages", "string", "file extensions, comma-separated, e.g. tf or java,kt"),
                        prop("limit", "integer", "max people (default 10)")),
                args -> service.experts(text(args, "repository"), text(args, "landscape"), text(args, "path"), text(args, "component"),
                        text(args, "decomposition"), text(args, "languages"), number(args, "limit")));
        register("get_dependencies",
                "The dependencies between the logical components of a repository (from -> to, number of places, cyclic when the "
                        + "reverse also exists), for one decomposition (the first by default), optionally only those of one component.",
                schema(required(prop("repository", "string", "repository id or name")),
                        prop("decomposition", "string", "decomposition name (see get_repository)"),
                        prop("component", "string", "only dependencies from or to this component")),
                args -> service.dependencies(text(args, "repository"), text(args, "decomposition"), text(args, "component")));
        register("search_findings",
                "Searches the AI findings (architecture, security, reliability, ... scanners of sokrates-skills) of one repository, a landscape "
                        + "or all repositories, most severe first. query: terms split by ';' matched in title/description/recommendation/tags.",
                schema(prop("repository", "string", "repository id or name (optional)"),
                        prop("landscape", "string", "landscape id (optional)"), prop("query", "string", "text terms"),
                        prop("minSeverity", "string", "critical | high | medium | low | info"),
                        prop("scanner", "string", "e.g. security-scan"),
                        prop("limit", "integer", "max items (default 50)"), prop("offset", "integer", "items to skip")),
                args -> service.findings(text(args, "repository"), text(args, "landscape"), text(args, "query"),
                        text(args, "minSeverity"), text(args, "scanner"), number(args, "offset"), number(args, "limit")));
    }

    private void register(String name, String description, ObjectNode schema, Function<JsonNode, Object> call) {
        tools.put(name, new Tool(name, description, schema, call));
    }

    /** Handles one JSON-RPC message or batch; returns the response text, or null when there is nothing to answer. */
    public String handle(String message) {
        JsonNode node;
        try {
            node = mapper.readTree(message);
        } catch (Exception e) {
            return write(error(null, -32700, "Parse error"));
        }
        if (node != null && node.isArray()) {
            ArrayNode responses = mapper.createArrayNode();
            node.forEach(item -> {
                JsonNode response = handle(item);
                if (response != null) {
                    responses.add(response);
                }
            });
            return responses.isEmpty() ? null : write(responses);
        }
        JsonNode response = handle(node);
        return response == null ? null : write(response);
    }

    JsonNode handle(JsonNode request) {
        if (request == null || !request.isObject() || !request.hasNonNull("method")) {
            // a response from the client, or garbage: never answered with a result
            return request != null && request.has("id") && !request.has("result") && !request.has("error")
                    ? error(request.get("id"), -32600, "Invalid request") : null;
        }
        JsonNode id = request.get("id");
        boolean notification = id == null || id.isNull();
        String method = request.get("method").asText();
        try {
            ObjectNode result;
            switch (method) {
                case "initialize":
                    result = initialize(request.path("params"));
                    break;
                case "ping":
                    result = mapper.createObjectNode();
                    break;
                case "tools/list":
                    result = toolsList();
                    break;
                case "tools/call":
                    result = callTool(request.path("params"));
                    break;
                default:
                    if (notification) {
                        return null; // notifications/initialized, notifications/cancelled, ...
                    }
                    return error(id, -32601, "Method not found: " + method);
            }
            if (notification) {
                return null;
            }
            ObjectNode response = mapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.set("id", id);
            response.set("result", result);
            return response;
        } catch (IllegalArgumentException e) {
            return notification ? null : error(id, -32602, e.getMessage());
        } catch (RuntimeException e) {
            LOG.warn("MCP " + method + " failed", e);
            return notification ? null : error(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    private ObjectNode initialize(JsonNode params) {
        String requested = params.path("protocolVersion").asText("");
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSIONS.contains(requested) ? requested : PROTOCOL_VERSIONS.get(0));
        result.putObject("capabilities").putObject("tools").put("listChanged", false);
        ObjectNode info = result.putObject("serverInfo");
        info.put("name", "sokrates");
        info.put("version", serverVersion);
        result.put("instructions", INSTRUCTIONS + (service.getPrivacy().isHidden()
                ? " People are pseudonymized (person-<id>, the same person everywhere) and report links are not available." : ""));
        return result;
    }

    private ObjectNode toolsList() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode list = result.putArray("tools");
        tools.values().forEach(tool -> {
            ObjectNode node = list.addObject();
            node.put("name", tool.name());
            node.put("description", tool.description());
            node.set("inputSchema", tool.inputSchema());
            node.putObject("annotations").put("readOnlyHint", true).put("openWorldHint", false);
        });
        return result;
    }

    // Tool failures (unknown repository, bad argument) are results with isError, so the model sees and can correct them.
    private ObjectNode callTool(JsonNode params) {
        String name = params.path("name").asText("");
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        ObjectNode result = mapper.createObjectNode();
        try {
            Object value = tool.call().apply(params.path("arguments"));
            result.putArray("content").addObject().put("type", "text").put("text", write(mapper.valueToTree(value)));
            result.put("isError", false);
        } catch (SokratesQueryService.NotFoundException | IllegalArgumentException e) {
            result.putArray("content").addObject().put("type", "text").put("text", e.getMessage());
            result.put("isError", true);
        }
        return result;
    }

    private ObjectNode error(JsonNode id, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id == null ? mapper.nullNode() : id);
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        return response;
    }

    private String write(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- schema helpers ----

    private record Prop(String name, String type, String description, boolean required) {
    }

    private static Prop prop(String name, String type, String description) {
        return new Prop(name, type, description, false);
    }

    private static Prop required(Prop prop) {
        return new Prop(prop.name(), prop.type(), prop.description(), true);
    }

    private ObjectNode schema(Prop... props) {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = mapper.createArrayNode();
        for (Prop prop : props) {
            properties.putObject(prop.name()).put("type", prop.type()).put("description", prop.description());
            if (prop.required()) {
                required.add(prop.name());
            }
        }
        if (!required.isEmpty()) {
            schema.set("required", required);
        }
        return schema;
    }

    private static String text(JsonNode args, String name) {
        JsonNode value = args.get(name);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static int number(JsonNode args, String name) {
        JsonNode value = args.get(name);
        if (value == null || value.isNull()) {
            return 0;
        }
        if (!value.canConvertToInt() && !value.isTextual()) {
            throw new IllegalArgumentException(name + " must be a number");
        }
        try {
            return value.isTextual() ? Integer.parseInt(value.asText().trim()) : value.asInt();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number");
        }
    }
}
