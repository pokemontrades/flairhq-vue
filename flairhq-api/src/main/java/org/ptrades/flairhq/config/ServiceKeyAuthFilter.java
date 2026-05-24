package org.ptrades.flairhq.config;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates requests that carry a pre-shared service key as a Bearer token.
 * A valid key is granted MOD_PERM_all authority, giving full moderator access.
 * An invalid key (wrong value) is rejected immediately with 401.
 * Requests without an Authorization header pass through untouched so the normal
 * session-based OAuth2 auth can handle them.
 * 
 * This class is utilized to provide a way for other apps (such as the Discord bot) to
 * authenticate with the API for performing actions.
 */
@Component
public class ServiceKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ServiceKeyAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${api.service-key:}")
    private String serviceKey;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        if (serviceKey.isBlank() || !serviceKey.equals(token)) {
            log.warn("Rejected invalid service key on {} {}", request.getMethod(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid service key");
            return;
        }

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "service-account", null,
                List.of(new SimpleGrantedAuthority("MOD_PERM_all"))));
        SecurityContextHolder.setContext(ctx);
        log.debug("Service key auth granted: {} {}", request.getMethod(), request.getRequestURI());

        chain.doFilter(request, response);
    }
}
