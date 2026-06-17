package org.ptrades.flairhq.controller;

import java.util.List;

import org.ptrades.flairhq.dto.ReferenceResponse;
import org.ptrades.flairhq.dto.UserResponse;
import org.ptrades.flairhq.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@PreAuthorize("isAuthenticated()")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String q) {
        if (q.isBlank()) return ResponseEntity.badRequest().build();
        log.info("Searching users q='{}'", q);
        return ResponseEntity.ok(searchService.searchUsers(q));
    }

    @GetMapping("/references")
    public ResponseEntity<List<ReferenceResponse>> searchReferences(@RequestParam String q) {
        if (q.isBlank()) return ResponseEntity.badRequest().build();
        log.info("Searching references q='{}'", q);
        return ResponseEntity.ok(searchService.searchReferences(q));
    }

}
