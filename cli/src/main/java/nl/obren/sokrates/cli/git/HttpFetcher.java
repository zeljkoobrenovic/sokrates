package nl.obren.sokrates.cli.git;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * One HTTP GET returning the status code and body: the injectable network layer of the code-host
 * API clients, so their parsing and pagination are unit-tested offline against canned responses.
 */
public interface HttpFetcher {
    Response get(String url) throws IOException;

    class Response {
        public final int status;
        public final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    /** A real fetcher sending the given headers (plus a User-Agent) with every request; 20 s timeouts. */
    static HttpFetcher create(Map<String, String> headers) {
        Duration timeout = Duration.ofSeconds(20);
        HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build();
        return url -> {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).header("User-Agent", "sokrates");
            headers.forEach((name, value) -> {
                if (StringUtils.isNotBlank(value)) {
                    request.header(name, value);
                }
            });
            try {
                HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
                return new Response(response.statusCode(), response.body());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while calling " + url, e);
            }
        };
    }
}
