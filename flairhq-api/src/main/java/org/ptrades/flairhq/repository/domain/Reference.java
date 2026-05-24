package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document
public class Reference {

    @Id
    private String id;

    private String url;
    private String user;
    private String user2;
    private String description;
    private String type; // should this be an enum?
    private String gave;
    private String got;
    private String notes;
    private String privateNotes;
    private Integer number;

    private Boolean verified;
    private Boolean approved;
    private Boolean edited;
    private Boolean rejected;
    private String  rejectedReason;
    private Boolean mustFix;
    private String  mustFixReason;

    private Instant createdAt;
    private Instant updatedAt; 
}
