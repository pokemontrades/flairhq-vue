package org.ptrades.flairhq.controller;

import java.util.List;
import java.util.Map;

import org.ptrades.flairhq.dto.EventResponse;
import org.ptrades.flairhq.repository.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
@PreAuthorize("@modSecurity.hasPermission(authentication, 'flair')")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final MongoTemplate mongoTemplate;

    public EventController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getEvents(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {

        size = Math.min(size, 100);
        log.info("Fetching events user='{}' type='{}' page={} size={}", user, type, page, size);

        final String userFilter = user;
        final String typeFilter = type;

        Criteria criteria = new Criteria();
        if (userFilter != null && !userFilter.isBlank()) {
            criteria = criteria.and("user").regex(userFilter.trim(), "i");
        }
        if (typeFilter != null && !typeFilter.isBlank()) {
            criteria = criteria.and("type").is(typeFilter.trim());
        }

        Query countQuery = new Query(criteria);
        Query dataQuery  = new Query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .skip((long) page * size)
                .limit(size);

        long total      = mongoTemplate.count(countQuery, Event.class);
        List<Event> events = mongoTemplate.find(dataQuery, Event.class);

        List<EventResponse> responses = events.stream()
                .map(e -> EventResponse.builder()
                        .id(e.getId())
                        .type(e.getType())
                        .user(e.getUser())
                        .content(e.getContent())
                        .createdAt(e.getCreatedAt())
                        .updatedAt(e.getUpdatedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(Map.of(
                "events",     responses,
                "total",      total,
                "page",       page,
                "size",       size,
                "totalPages", (int) Math.ceil((double) total / size)
        ));
    }
}
