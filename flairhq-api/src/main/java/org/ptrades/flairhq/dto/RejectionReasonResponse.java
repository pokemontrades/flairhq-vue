package org.ptrades.flairhq.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RejectionReasonResponse {

    private String id;
    private String label;
    private String reason;

    private Instant createdAt;
    private Instant updatedAt;

}
