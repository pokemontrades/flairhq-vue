package org.ptrades.flairhq.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModnoteResponse {

    private String id;
    private String user;
    private String refUser;
    private String note;

    private Instant createdAt;
    private Instant updatedAt;

}
