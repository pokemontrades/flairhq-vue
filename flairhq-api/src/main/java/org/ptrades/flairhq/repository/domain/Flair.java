package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document
public class Flair {

    @Id
    private String id;

    private String name; // should this be an enum?
    private String sub; // should this be an enum?
    private int trades;
    private int shinyEvents; // needed?
    private int events; // needed?
    private int eggs;
    private int giveaways;
    private int involvement;

    private Instant createdAt;
    private Instant updatedAt;
    
}
