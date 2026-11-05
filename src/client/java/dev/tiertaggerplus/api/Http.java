package dev.tiertaggerplus.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Tiny blocking HTTP helper. Always called from {@link TierCache}'s worker pool, never the render thread. */
public final class Http {
    public static String userAgent = "Mozilla/5.0 (compatible; TierTaggerPlus/1.0; +https://modrinth.com)";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Response(int status, String body, URI finalUri) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }

        public JsonElement json() {
            return JsonParser.parseString(body);
        }
    }

    private Http() {
    }

    public static Response get(String url) throws IOException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json, text/html;q=0.9, */*;q=0.5")
                .GET()
                .build();
        try {
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(res.statusCode(), res.body(), res.uri());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        }
    }

    public static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
