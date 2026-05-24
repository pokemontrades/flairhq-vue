package org.ptrades.flairhq.controller;

import java.util.List;

import org.ptrades.flairhq.dto.RejectionReasonRequest;
import org.ptrades.flairhq.dto.RejectionReasonResponse;
import org.ptrades.flairhq.processor.RejectionReasonProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/rejection-reasons")
@PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
public class RejectionReasonController {

    private static final Logger log = LoggerFactory.getLogger(RejectionReasonController.class);

    private final RejectionReasonProcessor processor;

    public RejectionReasonController(RejectionReasonProcessor processor) {
        this.processor = processor;
    }

    @GetMapping
    public ResponseEntity<List<RejectionReasonResponse>> getAll() {
        return ResponseEntity.ok(processor.getAll());
    }

    @PostMapping
    public ResponseEntity<RejectionReasonResponse> create(@RequestBody RejectionReasonRequest req) {
        log.info("Creating rejection reason label='{}'", req.getLabel());
        return ResponseEntity.status(201).body(processor.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RejectionReasonResponse> update(
            @PathVariable String id, @RequestBody RejectionReasonRequest req) {
        log.info("Updating rejection reason id='{}'", id);
        return ResponseEntity.ok(processor.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("Deleting rejection reason id='{}'", id);
        processor.delete(id);
        return ResponseEntity.noContent().build();
    }

}
