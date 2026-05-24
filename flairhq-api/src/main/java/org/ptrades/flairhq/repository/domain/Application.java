package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document
public class Application {

    @Id
    private String id;

    private String user;
    private String flair; // should this be an enum?
    private String sub; // should this be an enum?

    private Instant createdAt;
    private Instant updatedAt;
}
