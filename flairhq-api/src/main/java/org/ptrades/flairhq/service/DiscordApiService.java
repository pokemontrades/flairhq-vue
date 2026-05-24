package org.ptrades.flairhq.service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class DiscordApiService {

    private static final String DISCORD_API = "https://discordapp.com/api";

    private final RestClient restClient;

    @Value("${discord.client-id}")
    private String clientId;

    @Value("${discord.client-secret}")
    private String clientSecret;

    @Value("${discord.redirect-host}")
    private String redirectHost;

    @Value("${discord.server-id}")
    private String serverId;

    @Value("${discord.authenticated-role-ids}")
    private List<String> authenticatedRoleIds;

    @Value("${discord.bot-token}")
    private String botToken;

    // Rate-limit state
    private volatile boolean globallyRateLimited = false;
    private final ConcurrentHashMap<String, Instant> rateLimitedRoutes = new ConcurrentHashMap<>();

    public DiscordApiService() {
        this.restClient = RestClient.create();
    }

    /** Returns the URL users should be redirected to after joining the guild. */
    public String getServerUrl() {
        return "https://discordapp.com/channels/" + serverId;
    }

    /** Builds the Discord OAuth2 authorization URL to initiate the join flow. */
    public String getAuthorizeUrl() {
        String redirectUri = java.net.URLEncoder.encode(
                redirectHost + "/api/discord/callback",
                java.nio.charset.StandardCharsets.UTF_8);
        return "https://discordapp.com/api/oauth2/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=identify%20guilds.join";
    }

    /**
     * Exchanges a Discord OAuth2 authorization code for an access token.
     * Returns the access_token string from Discord's response.
     */
    public String getAccessToken(String code) {
        String redirectUri = redirectHost + "/api/discord/callback";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("scope", "identify guilds.join");

        JsonNode response = execute("POST", DISCORD_API + "/oauth2/token", null, form,
                DISCORD_API + "/oauth2/token");

        JsonNode tokenNode = response.path("access_token");
        if (tokenNode.isMissingNode()) {
            throw new DiscordApiException(502, "Discord did not return an access token");
        }
        return tokenNode.asText();
    }

    /**
     * Fetches the authenticated Discord user's profile.
     */
    public DiscordUser getCurrentUser(String accessToken) {
        String url = DISCORD_API + "/users/@me";
        JsonNode response = execute("GET", url, "Bearer " + accessToken, null, url);
        return new DiscordUser(
                response.path("id").asText(),
                response.path("username").asText(),
                response.path("discriminator").asText("0"));
    }

    /**
     * Adds a Discord user to the configured guild with the given nickname.
     * Returns {@code true} if the user was newly added (201), or {@code false}
     * if they were already a member (204 — no event should be logged).
     */
    public boolean addUserToGuild(String accessToken, String discordUserId, String nick) {
        String route = DISCORD_API + "/guilds/" + serverId + "/members";
        String url   = route + "/" + discordUserId;

        Map<String, Object> body = Map.of(
                "access_token", accessToken,
                "nick", nick,
                "roles", authenticatedRoleIds);

        try {
            ResponseEntity<JsonNode> response = restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(JsonNode.class);

            updateRateLimits(response.getHeaders(), route);
            // 201 = newly added, 204 = already a member (body will be null/empty)
            return response.getStatusCode() == HttpStatus.CREATED;

        } catch (HttpClientErrorException e) {
            handleRateLimitError(e, route);
            throw new DiscordApiException(e.getStatusCode().value(),
                    "Discord API error: " + e.getStatusCode());
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Core Discord API request executor. Checks global and per-route rate limits before sending,
     * dispatches GET or POST requests with optional auth and form body, updates rate-limit state
     * from response headers, and translates HTTP errors into {@link DiscordApiException}.
     */
    private JsonNode execute(String method, String url, String authHeader,
                             MultiValueMap<String, String> form, String routeKey) {
        cleanExpiredRateLimits();

        if (globallyRateLimited) {
            Instant globalReset = rateLimitedRoutes.get("global");
            long secondsRemaining = globalReset != null
                    ? Math.max(0, globalReset.getEpochSecond() - Instant.now().getEpochSecond()) : 0;
            throw new DiscordApiException(429,
                    "Globally rate limited by Discord — retry in " + secondsRemaining + " seconds");
        }
        if (rateLimitedRoutes.containsKey(routeKey)) {
            long secondsRemaining = Math.max(0,
                    rateLimitedRoutes.get(routeKey).getEpochSecond() - Instant.now().getEpochSecond());
            throw new DiscordApiException(429,
                    "Rate limited on route " + routeKey + " — retry in " + secondsRemaining + " seconds");
        }

        try {
            ResponseEntity<JsonNode> response;
            if ("GET".equalsIgnoreCase(method)) {
                var spec = restClient.get().uri(URI.create(url));
                if (authHeader != null) {
                    spec = spec.header(HttpHeaders.AUTHORIZATION, authHeader);
                }
                response = spec.retrieve().toEntity(JsonNode.class);
            } else {
                var spec = restClient.post()
                        .uri(URI.create(url))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form != null ? form : new LinkedMultiValueMap<>());
                if (authHeader != null) {
                    spec = spec.header(HttpHeaders.AUTHORIZATION, authHeader);
                }
                response = spec.retrieve().toEntity(JsonNode.class);
            }

            updateRateLimits(response.getHeaders(), routeKey);
            return response.getBody() != null ? response.getBody()
                    : com.fasterxml.jackson.databind.node.NullNode.getInstance();

        } catch (HttpClientErrorException e) {
            handleRateLimitError(e, routeKey);
            throw new DiscordApiException(e.getStatusCode().value(),
                    "Discord API error on " + url + ": " + e.getStatusCode());
        }
    }

    private void updateRateLimits(HttpHeaders headers, String routeKey) {
        String remaining = headers.getFirst("x-ratelimit-remaining");
        String reset     = headers.getFirst("x-ratelimit-reset");
        if ("0".equals(remaining) && reset != null) {
            rateLimitedRoutes.put(routeKey, Instant.ofEpochSecond(Long.parseLong(reset) + 1));
        }
    }

    private void handleRateLimitError(HttpClientErrorException e, String routeKey) {
        if (e.getStatusCode().value() == 429) {
            try {
                JsonNode errorBody = e.getResponseBodyAs(JsonNode.class);
                if (errorBody != null && errorBody.path("global").asBoolean(false)) {
                    long retryAfterMs = errorBody.path("retry_after").asLong(1000);
                    Instant resetAt = Instant.now().plusMillis(retryAfterMs).plusSeconds(1);
                    rateLimitedRoutes.put("global", resetAt);
                    globallyRateLimited = true;
                }
            } catch (Exception ignored) {
                // If we can't parse the body, treat it as a non-global rate limit
            }
        }
    }

    private void cleanExpiredRateLimits() {
        Instant now = Instant.now();
        rateLimitedRoutes.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isBefore(now);
            if (expired && "global".equals(entry.getKey())) {
                globallyRateLimited = false;
            }
            return expired;
        });
    }

}
