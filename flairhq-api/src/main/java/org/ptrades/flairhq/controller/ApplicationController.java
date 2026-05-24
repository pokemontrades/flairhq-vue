package org.ptrades.flairhq.controller;

import java.util.List;
import java.util.Objects;

import org.ptrades.flairhq.dto.ApplicationResponse;
import org.ptrades.flairhq.dto.UserApplicationRequest;
import org.ptrades.flairhq.processor.ApplicationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationController.class);

    private final ApplicationProcessor applicationProcessor;

    public ApplicationController(ApplicationProcessor applicationProcessor) {
        this.applicationProcessor = applicationProcessor;
    }

    @GetMapping
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<List<ApplicationResponse>> getApplications() {
        log.info("Fetching all pending applications");
        return ResponseEntity.ok(applicationProcessor.getApplications());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("Fetching applications for user='{}'", username);
        return ResponseEntity.ok(applicationProcessor.getApplicationsForUser(username));
    }

    @PostMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApplicationResponse> applyFlairForSelf(
            @RequestBody UserApplicationRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("User='{}' applying for flair='{}'", username, request.getFlair());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                applicationProcessor.applyFlairForSelf(request, username));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<Void> approveApplication(@PathVariable String id) {
        log.info("Approving application id='{}'", id);
        applicationProcessor.approveApplication(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deny")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<Void> denyApplication(
            @PathVariable String id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        log.info("Denying application id='{}' hasNote={}", id, note != null && !note.isBlank());
        applicationProcessor.denyApplication(id, note);
        return ResponseEntity.noContent().build();
    }
}
