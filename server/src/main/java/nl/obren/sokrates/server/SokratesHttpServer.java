package nl.obren.sokrates.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import nl.obren.sokrates.server.api.Api;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The HTTP front-end (JDK {@link HttpServer}, no framework): the read-only REST API under {@code /api/v1}, the MCP
 * Streamable HTTP endpoint at {@code POST /mcp}, and the static HTML reports under {@code /reports/} (only files
 * inside a repository's reports folder or a landscape folder, never other files below the root, such as sources).
 * With a token every route needs {@code Authorization: Bearer <token>}.
 */
public class SokratesHttpServer {
    private static final Log LOG = LogFactory.getLog(SokratesHttpServer.class);
    private static final String API = "/api/" + Api.VERSION;
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=utf-8"), Map.entry("htm", "text/html; charset=utf-8"),
            Map.entry("js", "text/javascript; charset=utf-8"), Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("json", "application/json; charset=utf-8"), Map.entry("txt", "text/plain; charset=utf-8"),
            Map.entry("svg", "image/svg+xml"), Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"), Map.entry("gif", "image/gif"), Map.entry("ico", "image/x-icon"),
            Map.entry("zip", "application/zip"), Map.entry("woff2", "font/woff2"));

    private final SokratesQueryService service;
    private final McpHandler mcp;
    private final String token;
    private final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private HttpServer server;
    private ExecutorService executor;

    public SokratesHttpServer(SokratesQueryService service, McpHandler mcp, String token) {
        this.service = service;
        this.mcp = mcp;
        this.token = StringUtils.trimToNull(token);
    }

    public void start(String host, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.createContext("/", this::dispatch);
        server.start();
        boolean hidden = service.getPrivacy().isHidden();
        LOG.info("Sokrates server on http://" + host + ":" + getPort() + "  (REST " + API + ", MCP /mcp"
                + (hidden ? "; people pseudonymized, reports not served" : ", reports /reports/") + ")"
                + (token == null ? "" : ", bearer token required"));
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        try {
            if (!authorized(exchange)) {
                exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer");
                sendJson(exchange, 401, Map.of("error", "missing or wrong bearer token"));
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if (path.equals("/mcp")) {
                handleMcp(exchange, method);
            } else if (path.startsWith(SokratesQueryService.REPORTS_PATH)) {
                if (service.getPrivacy().isHidden()) {
                    sendJson(exchange, 403, Map.of("error", "the HTML reports are not served with -hidePeople: they list contributor names and emails"));
                    return;
                }
                serveReport(exchange, path.substring(SokratesQueryService.REPORTS_PATH.length()));
            } else if (!method.equals("GET")) {
                sendJson(exchange, 405, Map.of("error", "read-only API: GET only"));
            } else {
                handleApi(exchange, path, query(exchange.getRequestURI()));
            }
        } catch (SokratesQueryService.NotFoundException e) {
            sendJson(exchange, 404, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LOG.warn("Request failed: " + exchange.getRequestURI(), e);
            sendJson(exchange, 500, Map.of("error", "internal error"));
        } finally {
            exchange.close();
        }
    }

    private void handleApi(HttpExchange exchange, String path, Map<String, String> q) throws IOException {
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        switch (path) {
            case "":
            case "/":
            case API:
                sendJson(exchange, 200, index());
                return;
            case API + "/landscapes":
                sendJson(exchange, 200, service.landscapes());
                return;
            case API + "/overview":
                sendJson(exchange, 200, service.overview(q.get("landscape")));
                return;
            case API + "/repositories":
                sendJson(exchange, 200, service.repositories(q.get("query"), q.get("landscape"), q.get("sort"),
                        number(q, "offset"), number(q, "limit")));
                return;
            case API + "/repository":
                String name = requireParam(q, "name");
                sendJson(exchange, 200, service.repository(name)
                        .orElseThrow(() -> new SokratesQueryService.NotFoundException(service.notFound(name))));
                return;
            case API + "/repository/hotspots":
                sendJson(exchange, 200, service.hotspots(requireParam(q, "name"), q.get("window"), number(q, "limit")));
                return;
            case API + "/repository/contributors":
                sendJson(exchange, 200, service.contributors(requireParam(q, "name"), q.get("query"), "true".equalsIgnoreCase(q.get("includeBots")),
                        number(q, "offset"), number(q, "limit")));
                return;
            case API + "/repository/experts":
                sendJson(exchange, 200, service.experts(requireParam(q, "name"), null, q.get("path"), q.get("component"), q.get("decomposition"),
                        q.get("languages"), number(q, "limit")));
                return;
            case API + "/experts":
                sendJson(exchange, 200, service.experts(null, q.get("landscape"), q.get("path"), q.get("component"), q.get("decomposition"),
                        q.get("languages"), number(q, "limit")));
                return;
            case API + "/repository/dependencies":
                sendJson(exchange, 200, service.dependencies(requireParam(q, "name"), q.get("decomposition"), q.get("component")));
                return;
            case API + "/findings":
                sendJson(exchange, 200, service.findings(q.get("repository"), q.get("landscape"), q.get("query"),
                        q.get("minSeverity"), q.get("scanner"), number(q, "offset"), number(q, "limit")));
                return;
            default:
                sendJson(exchange, 404, Map.of("error", "unknown path " + path, "see", API));
        }
    }

    private Map<String, Object> index() {
        Map<String, Object> index = new LinkedHashMap<>();
        index.put("name", "Sokrates analysis API");
        index.put("version", Api.VERSION);
        index.put("root", service.getCatalog().getRoot().getPath());
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("GET " + API + "/landscapes", "landscapes with repository counts");
        endpoints.put("GET " + API + "/overview?landscape=", "a landscape (or every analysis) at a glance: totals, grades, activity, short lists");
        endpoints.put("GET " + API + "/repositories?query=&landscape=&sort=&limit=&offset=",
                "repositories; query: mainLang:java;includesLang:tf;<text>, sort: loc|name|commits|humanScore|aiScore|findings");
        endpoints.put("GET " + API + "/repository?name=", "one repository's summary (name = id or repository name)");
        endpoints.put("GET " + API + "/repository/hotspots?name=&window=365|90|all&limit=", "files to look at first");
        endpoints.put("GET " + API + "/repository/contributors?name=&query=&includeBots=&limit=&offset=", "contributors, most active in the past year first");
        endpoints.put("GET " + API + "/repository/experts?name=&path=&component=&decomposition=&languages=&limit=", "who knows a file, folder, component or language");
        endpoints.put("GET " + API + "/experts?landscape=&path=&component=&decomposition=&languages=&limit=", "who knows it across a landscape (or all analyses)");
        endpoints.put("GET " + API + "/repository/dependencies?name=&decomposition=&component=", "dependencies between logical components");
        endpoints.put("GET " + API + "/findings?repository=&landscape=&query=&minSeverity=&scanner=&limit=&offset=", "AI findings");
        endpoints.put("POST /mcp", "Model Context Protocol (Streamable HTTP, JSON responses)");
        if (!service.getPrivacy().isHidden()) {
            endpoints.put("GET /reports/<path>", "the HTML reports (paths as in reportUrl)");
        }
        index.put("endpoints", endpoints);
        index.put("peopleHidden", service.getPrivacy().isHidden());
        return index;
    }

    // Streamable HTTP without server-initiated streams: POST answers with JSON (202 for notifications), GET is not offered.
    private void handleMcp(HttpExchange exchange, String method) throws IOException {
        if (!method.equals("POST")) {
            exchange.getResponseHeaders().set("Allow", "POST");
            sendJson(exchange, 405, Map.of("error", "POST JSON-RPC messages to /mcp"));
            return;
        }
        if (!sameOrigin(exchange)) {
            sendJson(exchange, 403, Map.of("error", "cross-origin MCP requests are not allowed"));
            return;
        }
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String response = mcp.handle(body);
        if (response == null) {
            exchange.sendResponseHeaders(202, -1);
            return;
        }
        send(exchange, 200, "application/json; charset=utf-8", response.getBytes(StandardCharsets.UTF_8));
    }

    // Against DNS rebinding (the MCP spec asks servers to check Origin): a browser origin must match the Host it calls.
    static boolean sameOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null || origin.equals("null")) {
            return origin == null;
        }
        String host = exchange.getRequestHeaders().getFirst("Host");
        try {
            URI uri = URI.create(origin);
            String originHost = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            return host != null && host.equalsIgnoreCase(originHost);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void serveReport(HttpExchange exchange, String relativePath) throws IOException {
        if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("HEAD")) {
            sendJson(exchange, 405, Map.of("error", "GET only"));
            return;
        }
        File file = reportFile(relativePath);
        if (file == null) {
            sendJson(exchange, 404, Map.of("error", "not a report file: " + relativePath));
            return;
        }
        String extension = StringUtils.substringAfterLast(file.getName(), ".").toLowerCase(Locale.ROOT);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPES.getOrDefault(extension, "application/octet-stream"));
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        long length = file.length();
        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }
        exchange.sendResponseHeaders(200, length == 0 ? -1 : length);
        try (OutputStream out = exchange.getResponseBody()) {
            Files.copy(file.toPath(), out);
        }
    }

    /** The file for a /reports/ path when it lies inside a known reports or landscape folder, else null. */
    File reportFile(String relativePath) {
        Path root = service.getCatalog().getRoot().toPath();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            return null;
        }
        if (Files.isDirectory(resolved)) {
            resolved = resolved.resolve("index.html");
        }
        if (!Files.isRegularFile(resolved)) {
            return null;
        }
        Path target = resolved;
        AnalysisCatalog.Snapshot snapshot = service.getCatalog().snapshot();
        boolean allowed = snapshot.repositories().stream().anyMatch(r -> target.startsWith(r.reportsFolder().toPath()))
                || snapshot.landscapes().stream().anyMatch(l -> target.startsWith(l.landscapeFolder().toPath()));
        return allowed ? target.toFile() : null;
    }

    private boolean authorized(HttpExchange exchange) {
        if (token == null) {
            return true;
        }
        String header = StringUtils.defaultString(exchange.getRequestHeaders().getFirst("Authorization"));
        String given = header.regionMatches(true, 0, "Bearer ", 0, 7) ? header.substring(7).trim() : "";
        return MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }

    private void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", mapper.writeValueAsBytes(value));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }

    static Map<String, String> query(URI uri) {
        Map<String, String> params = new LinkedHashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null) {
            return params;
        }
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
            String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            params.putIfAbsent(key, value);
        }
        return params;
    }

    private static String requireParam(Map<String, String> q, String name) {
        String value = q.get(name);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("missing parameter: " + name);
        }
        return value;
    }

    private static int number(Map<String, String> q, String name) {
        String value = q.get(name);
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a number");
        }
    }
}
