package org.ptrades.flairhq.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.ptrades.flairhq.dto.ReasonRequest;
import org.ptrades.flairhq.dto.ReferenceRequest;
import org.ptrades.flairhq.dto.ReferenceResponse;
import org.ptrades.flairhq.processor.ReferenceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/references")
public class ReferenceController {

    private static final Logger log = LoggerFactory.getLogger(ReferenceController.class);

    private final ReferenceProcessor referenceProcessor;

    public ReferenceController(ReferenceProcessor referenceProcessor) {
        this.referenceProcessor = referenceProcessor;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReferenceResponse>> getByUser(
            @RequestParam String user,
            @AuthenticationPrincipal OAuth2User principal) {
        String requestingUser = Objects.requireNonNull(principal.getAttribute("name"));
        log.debug("Fetching references for user='{}' requested by '{}'", user, requestingUser);
        return ResponseEntity.ok(referenceProcessor.getByUser(user, requestingUser));
    }

    @GetMapping("/me/counts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getMyCounts(
            @AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.debug("Fetching approved counts for user='{}'", username);
        return ResponseEntity.ok(referenceProcessor.getApprovedCountsByType(username));
    }

    @GetMapping("/pending-reciprocal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReferenceResponse>> getPendingReciprocal(
            @AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.debug("Fetching pending reciprocal references for user='{}'", username);
        return ResponseEntity.ok(referenceProcessor.getPendingReciprocal(username));
    }

    @PostMapping
    public ResponseEntity<ReferenceResponse> add(
            @RequestBody ReferenceRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("User='{}' adding reference url='{}'", username, request.getUrl());
        return ResponseEntity.status(201)
                .body(referenceProcessor.add(request, username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReferenceResponse> edit(
            @PathVariable String id,
            @RequestBody ReferenceRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("User='{}' editing reference id='{}'", username, id);
        return ResponseEntity.ok(referenceProcessor.edit(id, request, username));
    }

    @PostMapping("/{id}/unapprove")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<ReferenceResponse> unapprove(
            @PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        String moderator = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("Mod='{}' unapproving reference id='{}'", moderator, id);
        return ResponseEntity.ok(referenceProcessor.unapprove(Objects.requireNonNull(id), moderator));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<ReferenceResponse> approve(
            @PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        String moderator = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("Mod='{}' approving reference id='{}'", moderator, id);
        return ResponseEntity.ok(referenceProcessor.approve(Objects.requireNonNull(id), moderator));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<ReferenceResponse> reject(
            @PathVariable String id,
            @RequestBody(required = false) ReasonRequest body) {
        String reason = body != null ? body.getReason() : null;
        log.info("Rejecting reference id='{}' hasReason={}", id, reason != null && !reason.isBlank());
        return ResponseEntity.ok(referenceProcessor.reject(Objects.requireNonNull(id), reason));
    }

    @PostMapping("/{id}/pending")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<ReferenceResponse> setPending(@PathVariable String id) {
        log.info("Setting reference id='{}' to pending", id);
        return ResponseEntity.ok(referenceProcessor.setPending(Objects.requireNonNull(id)));
    }

    @PostMapping("/{id}/must-fix")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<ReferenceResponse> mustFix(
            @PathVariable String id,
            @RequestBody(required = false) ReasonRequest body) {
        String reason = body != null ? body.getReason() : null;
        log.info("Marking reference id='{}' as must-fix hasReason={}", id, reason != null && !reason.isBlank());
        return ResponseEntity.ok(referenceProcessor.markMustFix(Objects.requireNonNull(id), reason));
    }

    @PostMapping("/{id}/remove")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<Void> remove(@PathVariable String id) {
        log.info("Removing reference id='{}'", id);
        referenceProcessor.remove(Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal) {
        String username = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("User='{}' deleting reference id='{}'", username, id);
        referenceProcessor.delete(Objects.requireNonNull(id), username);
        return ResponseEntity.noContent().build();
    }
}
