package org.ptrades.flairhq.controller;

import java.util.List;
import java.util.Objects;

import org.ptrades.flairhq.dto.BanRequest;
import org.ptrades.flairhq.dto.SetFlairTextRequest;
import org.ptrades.flairhq.dto.UserRequest;
import org.ptrades.flairhq.dto.UserResponse;
import org.ptrades.flairhq.processor.UserProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserProcessor userProcessor;

    public UserController(UserProcessor userProcessor) {
        this.userProcessor = userProcessor;
    }

    @GetMapping
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(required = false) Boolean banned) {
        log.info("Fetching users banned={}", banned);
        return ResponseEntity.ok(userProcessor.getUsers(banned));
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String username) {
        log.debug("Fetching profile for user='{}'", username);
        return userProcessor.getUser(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.debug("Fetching own profile for user='{}'", username);
        return userProcessor.getUser(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> upsertMe(
            @RequestBody UserRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("Upserting profile for user='{}'", username);
        return ResponseEntity.ok(userProcessor.upsertMe(request, username));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/flair")
    public ResponseEntity<Void> setFlairText(
            @RequestBody SetFlairTextRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        String ptrades = request.getPtrades();
        if (ptrades == null || ptrades.isBlank() || ptrades.length() > 55) {
            return ResponseEntity.badRequest().build();
        }
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("Setting flair text for user='{}'", username);
        userProcessor.setFlairText(request, username);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    @PostMapping("/{username}/ban")
    public ResponseEntity<Void> banUser(
            @PathVariable String username,
            @RequestBody BanRequest request) {
        log.info("Banning user='{}'", username);
        userProcessor.banUser(username, request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    @PutMapping("/{username}/local-ban")
    // TODO: Review usage — not referenced by flairhq-vue frontend.
    public ResponseEntity<UserResponse> setLocalBan(
            @PathVariable String username,
            @RequestParam boolean banned) {
        log.info("Setting local ban for user='{}' banned={}", username, banned);
        return ResponseEntity.ok(userProcessor.setLocalBan(username, banned));
    }

    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    @PostMapping("/{username}/invalidate-session")
    // TODO: Review usage — not referenced by flairhq-vue frontend.
    public ResponseEntity<Void> invalidateSession(@PathVariable String username) {
        int count = userProcessor.invalidateSessions(username);
        log.info("Invalidated {} session(s) for user='{}'", count, username);
        return ResponseEntity.noContent().build();
    }
}
