package org.ptrades.flairhq.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModmailResponse {

    private String  id;
    private String  subject;
    private String  body;
    private String  author;
    private String  subreddit;
    private Instant createdAt;
    private Instant updatedAt;

}
