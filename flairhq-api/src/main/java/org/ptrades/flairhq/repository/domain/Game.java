package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document
public class Game {

    @Id
    private String id;

    private String ign;
    private String tsv;
    private String user;

    private Instant createdAt;
    private Instant updatedAt;
    
}
