package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document
public class Event {

    @Id
    private String id;

    private String type; // should this be an enum?
    private String user;
    private String content;

    private Instant createdAt;
    private Instant updatedAt;
    
}
