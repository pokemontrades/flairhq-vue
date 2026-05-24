package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

// Do we need this anymore? Last 9/2021
@Getter
@Setter
@Document
public class Modnote {

    @Id
    private String id;

    private String note;
    private String user;
    private String refUser;

    private Instant createdAt;
    private Instant updatedAt;
    
}
