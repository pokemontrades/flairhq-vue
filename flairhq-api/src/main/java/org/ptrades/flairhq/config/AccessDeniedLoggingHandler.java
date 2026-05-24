package org.ptrades.flairhq.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AccessDeniedLoggingHandler implements AccessDeniedHandler {

    /**
     * Handle logging for instances where a 403 would be served to a user.
     * 
     * @param request
     * @param response
     * @param ex
     * @throws IOException
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";

        List<String> modPerms = auth == null ? List.of()
                : auth.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .filter(a -> a.startsWith("MOD_PERM_"))
                        .map(a -> a.substring("MOD_PERM_".length()))
                        .collect(Collectors.toList());

        // If we ever see this log statement, we need to investigate ASAP
        log.warn("403 Forbidden — user='{}' modPermissions={} method={} uri='{}'",
                username, modPerms, request.getMethod(), request.getRequestURI());

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
