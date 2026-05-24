package org.ptrades.flairhq.controller;

import java.util.List;

import org.ptrades.flairhq.dto.FlairRequest;
import org.ptrades.flairhq.dto.FlairResponse;
import org.ptrades.flairhq.processor.FlairProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flairs")
public class FlairController {

    private static final Logger log = LoggerFactory.getLogger(FlairController.class);

    private final FlairProcessor flairProcessor;

    public FlairController(FlairProcessor flairProcessor) {
        this.flairProcessor = flairProcessor;
    }

    @GetMapping
    public ResponseEntity<List<FlairResponse>> getFlairs() {
        log.debug("Fetching flairs");
        return ResponseEntity.ok(flairProcessor.getFlairs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlairResponse> getFlair(@PathVariable String id) {
        log.debug("Fetching flair id='{}'", id);
        return flairProcessor.getFlair(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<FlairResponse> createFlair(@RequestBody FlairRequest request) {
        log.info("Creating flair name='{}' sub='{}'", request.getName(), request.getSub());
        return ResponseEntity.status(HttpStatus.CREATED).body(flairProcessor.createFlair(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<FlairResponse> updateFlair(
            @PathVariable String id,
            @RequestBody FlairRequest request) {
        log.info("Updating flair id='{}'", id);
        return ResponseEntity.ok(flairProcessor.updateFlair(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
    public ResponseEntity<Void> deleteFlair(@PathVariable String id) {
        log.info("Deleting flair id='{}'", id);
        flairProcessor.deleteFlair(id);
        return ResponseEntity.noContent().build();
    }
}
