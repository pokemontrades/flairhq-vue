package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

// Can we get rid of this one? Last used 12/2022
@Getter
@Setter
@Document
public class Modmail {

    @Id
    private String id;

    private String subject;
    private String body;
    private String author;
    private String subreddit; // should this be an enum?
    private String firstMessageName; // first_message_name
    private String parentId; // parent_id
    private String distinguished; // should this be an enum?

    private int createdUtc; // created_utc
    private Instant createdAt;
    private Instant updatedAt;
    
}
