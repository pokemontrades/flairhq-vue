package org.ptrades.flairhq.controller;

import java.net.URI;
import java.util.Objects;

import org.ptrades.flairhq.processor.DiscordProcessor;
import org.ptrades.flairhq.service.DiscordApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discord")
public class DiscordController {

    private static final Logger log = LoggerFactory.getLogger(DiscordController.class);

    private final DiscordProcessor  discordProcessor;
    private final DiscordApiService discordApiService;

    public DiscordController(DiscordProcessor discordProcessor, DiscordApiService discordApiService) {
        this.discordProcessor  = discordProcessor;
        this.discordApiService = discordApiService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Void> discordAuthorize() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(discordApiService.getAuthorizeUrl()))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> discordCallback(
            @RequestParam(required = false) String code,
            @AuthenticationPrincipal OAuth2User principal) {

        if (code == null || code.isBlank()) {
            log.warn("Discord callback received with missing code");
            return ResponseEntity.badRequest().build();
        }

        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("Processing Discord callback for user='{}'", username);
        URI location = discordProcessor.discordCallback(code, username);
        return ResponseEntity.status(HttpStatus.FOUND).location(Objects.requireNonNull(location)).build();
    }
}
