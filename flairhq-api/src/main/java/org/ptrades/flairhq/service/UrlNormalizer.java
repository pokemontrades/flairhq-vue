package org.ptrades.flairhq.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class UrlNormalizer {

    private static final Pattern MOBILE_REDDIT = Pattern.compile(
            "https?://(www\\.)?reddit\\.com/r/[^/]+/s/[^/?#]+"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    /**
     * Strips query params from the URL. If it matches the Reddit mobile share URL
     * pattern (/r/{sub}/s/{id}), follows the redirect to obtain the canonical
     * desktop URL. Falls back to the cleaned URL on any error.
     */
    public String normalize(String url) {
        if (url == null || url.isBlank()) return url;
        int q = url.indexOf('?');
        String clean = q >= 0 ? url.substring(0, q) : url;
        if (!MOBILE_REDDIT.matcher(clean).find()) return clean;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(clean))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", "FlairHQ/1.0")
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            String resolved = resp.uri().toString();
            int rq = resolved.indexOf('?');
            return rq >= 0 ? resolved.substring(0, rq) : resolved;
        } catch (Exception ignored) {
            return clean;
        }
    }

    /**
     * Extracts a canonical comparison key from a Reddit permalink.
     * Strips query params, then returns the URL up to and including the comment ID
     * (2nd unique segment after /comments/) when present; otherwise returns up to
     * the post ID only. Non-Reddit URLs (no /comments/ segment) are returned as-is.
     *
     * Examples:
     *   .../comments/abc123/title-slug/xyz789/  →  .../comments/abc123/title-slug/xyz789/
     *   .../comments/abc123/title-slug/         →  .../comments/abc123/
     *   .../comments/abc123/                    →  .../comments/abc123/
     */
    public static String permalinkBase(String url) {
        if (url == null) return null;
        int q = url.indexOf('?');
        String s = q >= 0 ? url.substring(0, q) : url;

        int commentsIdx = s.indexOf("/comments/");
        if (commentsIdx < 0) return s;

        // Segment 1: post ID
        int idStart = commentsIdx + "/comments/".length();
        int idEnd   = s.indexOf('/', idStart);
        if (idEnd < 0) return s + '/';

        // Segment 2: title slug
        int titleEnd = s.indexOf('/', idEnd + 1);
        if (titleEnd < 0 || titleEnd == idEnd + 1) return s.substring(0, idEnd + 1);

        // Segment 3: comment ID
        int commentStart = titleEnd + 1;
        if (commentStart >= s.length()) return s.substring(0, idEnd + 1);

        int commentEnd = s.indexOf('/', commentStart);
        if (commentEnd < 0)             return s + '/';
        if (commentEnd == commentStart) return s.substring(0, idEnd + 1);

        return s.substring(0, commentEnd + 1);
    }
}
