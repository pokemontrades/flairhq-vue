package org.ptrades.flairhq.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private String id;
    private String user;
    private String user2;
    private String message;

    private Instant createdAt;
    private Instant updatedAt;

}
