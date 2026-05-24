package org.ptrades.flairhq.controller;

import java.util.List;
import java.util.Objects;

import org.ptrades.flairhq.dto.ModnoteRequest;
import org.ptrades.flairhq.dto.ModnoteResponse;
import org.ptrades.flairhq.processor.ModnoteProcessor;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modnotes")
@PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
public class ModnoteController {

    private static final Logger log = LoggerFactory.getLogger(ModnoteController.class);

    private final ModnoteProcessor modnoteProcessor;

    public ModnoteController(ModnoteProcessor modnoteProcessor) {
        this.modnoteProcessor = modnoteProcessor;
    }

    @GetMapping
    public ResponseEntity<List<ModnoteResponse>> getModnotes(
            @RequestParam(required = false) String refUser) {
        log.debug("Fetching modnotes for refUser='{}'", refUser);
        return ResponseEntity.ok(modnoteProcessor.getModnotes(refUser));
    }

    @PostMapping
    public ResponseEntity<ModnoteResponse> addModnote(
            @RequestBody ModnoteRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        String author = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("Mod='{}' adding note for refUser='{}'", author, request.getRefUser());
        return ResponseEntity.status(201)
                .body(modnoteProcessor.addModnote(request, author));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModnote(@PathVariable String id) {
        log.info("Deleting modnote id='{}'", id);
        modnoteProcessor.deleteModnote(id);
        return ResponseEntity.noContent().build();
    }
}
