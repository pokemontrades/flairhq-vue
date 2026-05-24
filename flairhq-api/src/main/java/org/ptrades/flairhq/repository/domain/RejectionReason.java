package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document
public class RejectionReason {

    @Id
    private String id;

    private String label;
    private String reason;

    private Instant createdAt;
    private Instant updatedAt;

}
