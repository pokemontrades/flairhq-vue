package org.ptrades.flairhq.config;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.ptrades.flairhq.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ptrades.flairhq.repository.domain.User;
import org.ptrades.flairhq.service.RedditApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.HtmlUtils;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    private static final int SESSION_MAX_AGE_SECONDS = 60 * 60 * 24 * 180; // 6 months

    private final UserRepository   userRepository;
    private final RedditApiService redditApiService;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(UserRepository userRepository, RedditApiService redditApiService) {
        this.userRepository   = userRepository;
        this.redditApiService = redditApiService;
    }

    /**
     * Runs after every successful Reddit OAuth2 login. Fetches the user's current moderator
     * permissions from Reddit, enriches the Spring Security token with those authorities (so
     * {@code @PreAuthorize} checks are session-scoped and cannot be spoofed via the DB), then
     * upserts the user record in MongoDB for audit purposes and redirects to the frontend.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token    = (OAuth2AuthenticationToken) authentication;
        String                    username = token.getPrincipal().getAttribute("name");

        if (username == null) {
            response.sendRedirect(frontendUrl + "/login?error=true");
            return;
        }

        List<String> modPermissions;
        try {
            modPermissions = redditApiService.getModeratorPermissions(
                    redditApiService.getAdminRefreshToken(), username, "pokemontrades");
        } catch (Exception e) {
            log.warn("Could not fetch mod permissions for user='{}' — proceeding without: {}", username, e.getMessage());
            modPermissions = List.of();
        }

        // Build authorities from Reddit mod permissions — these live in the session,
        // not the DB, so a DB write cannot grant privileged access.
        List<GrantedAuthority> authorities = new ArrayList<>(token.getAuthorities());
        modPermissions.forEach(perm ->
                authorities.add(new SimpleGrantedAuthority("MOD_PERM_" + perm)));

        OAuth2AuthenticationToken enrichedToken = new OAuth2AuthenticationToken(
                token.getPrincipal(), authorities, token.getAuthorizedClientRegistrationId());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(enrichedToken);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        request.getSession().setMaxInactiveInterval(SESSION_MAX_AGE_SECONDS);

        // Keep DB record for audit history only — not used for access control decisions.
        boolean isMod = !modPermissions.isEmpty();
        User user = userRepository.findById(username).orElseGet(() -> {
            User u = new User();
            u.setId(username);
            u.setCreatedAt(Instant.now());
            return u;
        });
        user.setIsMod(isMod);
        user.setModPermissions(isMod ? modPermissions.toArray(String[]::new) : null);
        String rawIconImg = token.getPrincipal().getAttribute("icon_img");
        if (rawIconImg != null && !rawIconImg.isBlank()) user.setIconImg(HtmlUtils.htmlUnescape(rawIconImg));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        response.sendRedirect(frontendUrl + "/");
    }
}
