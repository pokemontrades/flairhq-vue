package org.ptrades.flairhq.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.ptrades.flairhq.repository.domain.SubredditFlair;
import org.ptrades.flairhq.repository.domain.UserFlair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class RedditApiService {

    private static final Logger log = LoggerFactory.getLogger(RedditApiService.class);

    private static final String OAUTH_BASE    = "https://oauth.reddit.com";
    private static final String API_BASE      = "https://www.reddit.com";
    private static final String TOKEN_URL     = API_BASE + "/api/v1/access_token";
    private static final Duration TOKEN_TTL   = Duration.ofMinutes(58);

    private final RestClient restClient;

    @Value("${spring.security.oauth2.client.registration.reddit.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.reddit.client-secret}")
    private String clientSecret;

    @Value("${reddit.admin-refresh-token}")
    private String adminRefreshToken;

    @Value("${reddit.user-agent}")
    private String userAgent;

    @Value("${reddit.debug-subreddit:}")
    private String debugSubreddit;

    @Value("${fhq.env:dev}")
    private String env;

    // Rate-limit state (updated from response headers after every request)
    private final AtomicInteger rateLimitRemaining = new AtomicInteger(600);
    private volatile Instant rateLimitReset = Instant.now().plusSeconds(600);

    // Access-token cache: refreshToken -> (accessToken, expiresAt)
    private record TokenEntry(String accessToken, Instant expiresAt) {}
    private final ConcurrentHashMap<String, TokenEntry> tokenCache = new ConcurrentHashMap<>();

    public RedditApiService() {
        this.restClient = RestClient.create();
    }

    // -------------------------------------------------------------------------
    // Public API — exposed for use by other services and controllers
    // -------------------------------------------------------------------------

    /** Returns the configured admin refresh token for use in moderator operations. */
    public String getAdminRefreshToken() {
        return adminRefreshToken;
    }

    /**
     * Fetches a user's avatar URL from Reddit's public API.
     * Uses raw_json=1 to avoid &amp; HTML entity encoding in the returned URL.
     * Returns null if the user doesn't exist or has no avatar.
     */
    public String getUserIconImg(String username) {
        try {
            JsonNode body = restClient.get()
                    .uri(API_BASE + "/user/" + username + "/about.json?raw_json=1")
                    .header(HttpHeaders.USER_AGENT, userAgent)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null) return null;
            String iconImg = body.path("data").path("icon_img").asText(null);
            return (iconImg != null && !iconImg.isBlank()) ? iconImg : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Exchanges a refresh token for a short-lived access token.
     * Results are cached for 58 minutes (Reddit tokens last ~60 minutes).
     */
    public String refreshAccessToken(String refreshToken) {
        TokenEntry cached = tokenCache.get(refreshToken);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.accessToken();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        JsonNode body = restClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .header(HttpHeaders.USER_AGENT, userAgent)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (body == null || !body.hasNonNull("access_token")) {
            throw new RedditApiException(502, "Error retrieving access token from Reddit");
        }

        String accessToken = body.get("access_token").asText();
        tokenCache.put(refreshToken, new TokenEntry(accessToken, Instant.now().plus(TOKEN_TTL)));
        return accessToken;
    }

    /** Gets a user's current flair on a given subreddit. */
    public SubredditFlair getFlair(String refreshToken, String user, String subreddit) {
        String url = OAUTH_BASE + "/r/" + subreddit + "/api/flairselector";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", user);
        JsonNode response = execute(refreshToken, "POST", url, form, 20);
        return parseSubredditFlair(response.path("current"));
    }

    /** Gets a user's current flair on r/pokemontrades. */
    public UserFlair getPtradesFlairs(String refreshToken, String user) {
        UserFlair result = new UserFlair();
        result.setPtrades(getFlair(refreshToken, user, "pokemontrades"));
        return result;
    }

    /** Sets a user's flair on a subreddit. Write operations respect the debug subreddit override. */
    public void setUserFlair(String refreshToken, String name, String cssClass, String text, String subreddit) {
        String target = resolveWriteSubreddit(subreddit);
        String url = OAUTH_BASE + "/r/" + target + "/api/flair";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("api_type", "json");
        form.add("css_class", cssClass);
        form.add("name", name);
        form.add("text", text);
        execute(refreshToken, "POST", url, form, 5);
    }

    /** Sets a link/post flair on a subreddit. */
    public void setLinkFlair(String refreshToken, String subreddit, String linkId, String cssClass, String text) {
        String target = resolveWriteSubreddit(subreddit);
        String url = OAUTH_BASE + "/r/" + target + "/api/flair";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("api_type", "json");
        form.add("css_class", cssClass);
        form.add("link", "t3_" + linkId);
        form.add("text", text);
        execute(refreshToken, "POST", url, form, 5);
    }

    /** Bans a user from a subreddit. Pass {@code null} duration for a permanent ban. In dev mode, suppresses the ban message so it isn't delivered to the real user. */
    public void banUser(String refreshToken, String username, String banMessage, String note,
                        String subreddit, Integer duration) {
        String target = resolveWriteSubreddit(subreddit);
        String effectiveBanMessage = isDevMode() ? "" : banMessage;
        if (isDevMode() && banMessage != null && !banMessage.isBlank()) {
            log.warn("[DEV] Suppressing ban_message to user='{}' — would have sent: {}", username, banMessage);
        }
        String url = OAUTH_BASE + "/r/" + target + "/api/friend";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("api_type", "json");
        form.add("ban_message", effectiveBanMessage != null ? effectiveBanMessage : "");
        form.add("duration", duration != null ? duration.toString() : "");
        form.add("name", username);
        form.add("note", note);
        form.add("type", "banned");
        execute(refreshToken, "POST", url, form, 5);
    }

    /** Returns the content (markdown) of a wiki page. */
    public String getWikiPage(String refreshToken, String subreddit, String page) {
        String url = OAUTH_BASE + "/r/" + subreddit + "/wiki/" + page;
        JsonNode response = execute(refreshToken, "GET", url, null, 5);
        return response.path("data").path("content_md").asText();
    }

    /** Edits a wiki page. */
    public void editWikiPage(String refreshToken, String subreddit, String page,
                             String content, String reason) {
        String target = resolveWriteSubreddit(subreddit);
        String url = OAUTH_BASE + "/r/" + target + "/api/wiki/edit";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("content", content);
        form.add("page", page);
        form.add("reason", reason);
        execute(refreshToken, "POST", url, form, 5);
    }

    /** Searches a subreddit and returns all matching post data nodes (auto-paginated). */
    public List<JsonNode> search(String refreshToken, String subreddit, String query,
                                 boolean restrictSr, String sort, String time, String syntax) {
        String querystring = "?q=" + encodeUriComponent(query)
                + (restrictSr ? "&restrict_sr=on" : "")
                + (sort   != null ? "&sort=" + sort   : "")
                + (time   != null ? "&t="    + time   : "")
                + "&syntax=" + (syntax != null ? syntax : "lucene");
        String endpoint = OAUTH_BASE + "/r/" + subreddit + "/search";
        return getEntireListing(refreshToken, endpoint, querystring, 10);
    }

    /** Removes a post. */
    public void removePost(String refreshToken, String id, boolean isSpam) {
        String url = OAUTH_BASE + "/api/remove";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", "t3_" + id);
        form.add("spam", Boolean.toString(isSpam));
        execute(refreshToken, "POST", url, form, 5);
    }

    /**
     * Locks a post. A 400 response (archived post) is tolerated and does not throw.
     */
    public void lockPost(String refreshToken, String postId) {
        String url = OAUTH_BASE + "/api/lock";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", "t3_" + postId);
        try {
            execute(refreshToken, "POST", url, form, 5);
        } catch (RedditApiException e) {
            if (e.getStatusCode() != 400) {
                throw e;
            }
            // 400 on archived posts is expected — treat as success
        }
    }

    /** Marks a post NSFW. */
    public void markNsfw(String refreshToken, String postId) {
        String url = OAUTH_BASE + "/api/marknsfw";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", "t3_" + postId);
        execute(refreshToken, "POST", url, form, 5);
    }

    /** Sends a private message via Reddit. In dev mode, always sends to the current logged-in user. */
    public void sendPrivateMessage(String refreshToken, String subject, String text, String recipient) {
        String target = resolveMessageRecipient(recipient);
        String url = OAUTH_BASE + "/api/compose";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("api_type", "json");
        form.add("subject", subject);
        form.add("text", text);
        form.add("to", target);
        execute(refreshToken, "POST", url, form, 25);
    }

    /** Replies to a post or comment. */
    public void sendReply(String refreshToken, String text, String parentId) {
        String url = OAUTH_BASE + "/api/comment";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("api_type", "json");
        form.add("text", text);
        form.add("thing_id", parentId);
        execute(refreshToken, "POST", url, form, 30);
    }

    /**
     * Returns the list of mod permission strings for a user on a subreddit,
     * or an empty list if the user is not a moderator there.
     */
    public List<String> getModeratorPermissions(String refreshToken, String username, String subreddit) {
        String url = OAUTH_BASE + "/r/" + subreddit + "/about/moderators?user=" + username;
        JsonNode response = execute(refreshToken, "GET", url, null, 5);
        JsonNode children = response.path("data").path("children");
        for (JsonNode child : children) {
            if (username.equals(child.path("name").asText())) {
                List<String> perms = new ArrayList<>();
                child.path("mod_permissions").forEach(p -> perms.add(p.asText()));
                return perms;
            }
        }
        return List.of();
    }

    /** Returns all modmail messages for a subreddit (auto-paginated). */
    public List<JsonNode> getModmail(String refreshToken, String subreddit) {
        String endpoint = OAUTH_BASE + "/r/" + subreddit + "/message/moderator";
        return getEntireListing(refreshToken, endpoint, "", 20);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Fetches all pages of a Reddit listing endpoint and returns the accumulated
     * list of {@code data.children[].data} nodes.
     */
    private List<JsonNode> getEntireListing(String refreshToken, String endpoint,
                                             String query, int rateThreshold) {
        List<JsonNode> results = new ArrayList<>();
        String after = null;
        do {
            String url = endpoint + query
                    + (query.isEmpty() ? "?" : "&")
                    + "count=102&limit=100"
                    + (after != null ? "&after=" + after : "");
            JsonNode batch = execute(refreshToken, "GET", url, null, rateThreshold);
            JsonNode children = batch.path("data").path("children");
            after = null;
            if (children.isArray()) {
                children.forEach(child -> results.add(child.path("data")));
                JsonNode afterNode = batch.path("data").path("after");
                if (afterNode.isTextual() && !afterNode.asText().isEmpty()) {
                    after = afterNode.asText();
                }
            }
        } while (after != null);
        return results;
    }

    /**
     * Core request executor — handles rate-limit gating, auth headers,
     * raw_json parameter, response parsing, and rate-limit state updates.
     */
    private JsonNode execute(String refreshToken, String method, String url,
                             MultiValueMap<String, String> formData, int rateLimitThreshold) {
        if (rateLimitRemaining.get() < rateLimitThreshold && Instant.now().isBefore(rateLimitReset)) {
            throw new RedditApiException(504, "Reddit rate limit reached — try again later");
        }

        // Reddit's raw_json=1 prevents HTML-encoding of > < & in responses
        String finalUrl = url + (url.contains("?") ? "&" : "?") + "raw_json=1";

        try {
            ResponseEntity<JsonNode> response;
            if ("GET".equalsIgnoreCase(method)) {
                var spec = restClient.get()
                        .uri(finalUrl)
                        .header(HttpHeaders.USER_AGENT, userAgent);
                if (finalUrl.contains("oauth.reddit.com")) {
                    spec = spec.header(HttpHeaders.AUTHORIZATION,
                            "bearer " + refreshAccessToken(refreshToken));
                }
                response = spec.retrieve().toEntity(JsonNode.class);
            } else {
                var spec = restClient.post()
                        .uri(finalUrl)
                        .header(HttpHeaders.USER_AGENT, userAgent)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(formData != null ? formData : new LinkedMultiValueMap<>());
                if (finalUrl.contains("oauth.reddit.com")) {
                    spec = spec.header(HttpHeaders.AUTHORIZATION,
                            "bearer " + refreshAccessToken(refreshToken));
                }
                response = spec.retrieve().toEntity(JsonNode.class);
            }

            updateRateLimits(response.getHeaders());
            return response.getBody() != null ? response.getBody()
                    : com.fasterxml.jackson.databind.node.NullNode.getInstance();

        } catch (HttpClientErrorException e) {
            throw new RedditApiException(e.getStatusCode().value(),
                    "Reddit API error: " + e.getStatusCode());
        }
    }

    private void updateRateLimits(HttpHeaders headers) {
        String remaining = headers.getFirst("x-ratelimit-remaining");
        String reset     = headers.getFirst("x-ratelimit-reset");
        if (remaining != null) {
            rateLimitRemaining.set((int) Double.parseDouble(remaining));
        }
        if (reset != null) {
            rateLimitReset = Instant.now().plusSeconds(Long.parseLong(reset));
        }
    }

    private SubredditFlair parseSubredditFlair(JsonNode node) {
        SubredditFlair flair = new SubredditFlair();
        flair.setFlairCssClass(node.path("flair_css_class").asText(null));
        flair.setFlairPosition(node.path("flair_position").asText(null));
        flair.setFlairTemplateId(node.path("flair_template_id").asText(null));
        flair.setFlairText(node.path("flair_text").asText(null));
        return flair;
    }

    private static final String DEV_DEFAULT_SUBREDDIT = "notpokemontrades";

    /**
     * Returns the debug subreddit if configured, the dev default if in dev mode,
     * or the intended subreddit in prod.
     */
    private String resolveWriteSubreddit(String intended) {
        if (debugSubreddit != null && !debugSubreddit.isBlank()) return debugSubreddit;
        if (isDevMode()) return DEV_DEFAULT_SUBREDDIT;
        return intended;
    }

    private boolean isDevMode() {
        return "dev".equals(env);
    }

    private static final String DEV_FALLBACK_RECIPIENT = "devilman6555";

    /**
     * In dev mode, redirects PM recipients to the currently logged-in user so that
     * no real users are messaged during local development. Falls back to
     * {@value DEV_FALLBACK_RECIPIENT} if no authenticated user is present.
     */
    private String resolveMessageRecipient(String intended) {
        if (!isDevMode()) return intended;
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof OAuth2User principal) {
                String currentUser = principal.getAttribute("name");
                if (currentUser != null && !currentUser.isBlank()) {
                    log.warn("[DEV] Redirecting PM intended for '{}' → current user '{}'", intended, currentUser);
                    return currentUser;
                }
            }
        } catch (Exception e) {
            log.warn("[DEV] Could not resolve current user for PM redirect, falling back to '{}'", DEV_FALLBACK_RECIPIENT);
        }
        log.warn("[DEV] No authenticated user found for PM redirect, falling back to '{}'", DEV_FALLBACK_RECIPIENT);
        return DEV_FALLBACK_RECIPIENT;
    }

    private static String encodeUriComponent(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

}
