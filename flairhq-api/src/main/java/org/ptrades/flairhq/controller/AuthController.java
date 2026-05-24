package org.ptrades.flairhq.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.HtmlUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            @AuthenticationPrincipal OAuth2User principal,
            Authentication authentication) {
        if (principal == null) {
            log.debug("Auth check — no active session");
            return ResponseEntity.status(401).build();
        }

        String username = orEmpty(principal.getAttribute("name")).toString();
        log.debug("Auth check for user='{}'", username);

        boolean isMod = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("MOD_PERM_"));

        Object rawIconImg = principal.getAttribute("icon_img");
        String iconImg = rawIconImg instanceof String s && !s.isBlank()
                ? HtmlUtils.htmlUnescape(s) : "";

        return ResponseEntity.ok(Map.of(
            "name",        username,
            "icon_img",    iconImg,
            "total_karma", orEmpty(principal.getAttribute("total_karma")),
            "isMod",       isMod
        ));
    }

    private Object orEmpty(Object value) {
        return value != null ? value : "";
    }
}
