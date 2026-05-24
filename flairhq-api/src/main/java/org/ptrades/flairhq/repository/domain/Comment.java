package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document
public class Comment {

    @Id
    private String id;

    private String user;
    private String user2;
    private String message;
    
    private Instant createdAt;
    private Instant updatedAt;
}
