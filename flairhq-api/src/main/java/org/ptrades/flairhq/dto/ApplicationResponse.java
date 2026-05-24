package org.ptrades.flairhq.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationResponse {

    private String id;
    private String user;
    private String flair;
    private String sub;
    private int approvedTrades;
    private int requiredTrades;
    private Instant createdAt;
    private Instant updatedAt;

}
