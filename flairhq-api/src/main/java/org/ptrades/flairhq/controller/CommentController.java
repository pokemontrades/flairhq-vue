package org.ptrades.flairhq.controller;

import java.util.List;
import java.util.Objects;

import org.ptrades.flairhq.dto.CommentRequest;
import org.ptrades.flairhq.dto.CommentResponse;
import org.ptrades.flairhq.processor.CommentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/comments")
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    private final CommentProcessor commentProcessor;

    public CommentController(CommentProcessor commentProcessor) {
        this.commentProcessor = commentProcessor;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(
            @RequestParam(required = false) String user) {
        log.debug("Fetching comments for user='{}'", user);
        return ResponseEntity.ok(commentProcessor.getComments(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> getComment(@PathVariable String id) {
        log.debug("Fetching comment id='{}'", id);
        return commentProcessor.getComment(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal OAuth2User principal) {
        String author = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("User='{}' posting comment on profile='{}'", author, request.getUser());
        return ResponseEntity.status(201)
                .body(commentProcessor.addComment(request, author));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String id,
            @AuthenticationPrincipal OAuth2User principal,
            Authentication authentication) {
        String requester = Objects.requireNonNull(principal.getAttribute("name"));
        log.info("User='{}' deleting comment id='{}'", requester, id);
        commentProcessor.deleteComment(id, requester, authentication);
        return ResponseEntity.noContent().build();
    }
}
