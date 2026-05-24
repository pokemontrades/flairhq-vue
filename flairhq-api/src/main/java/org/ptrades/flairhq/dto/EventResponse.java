package org.ptrades.flairhq.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventResponse {

    private String id;
    private String type;
    private String user;
    private String content;
    private Instant createdAt;
    private Instant updatedAt;

}
