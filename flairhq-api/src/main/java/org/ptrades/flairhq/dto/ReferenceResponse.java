package org.ptrades.flairhq.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class ReferenceResponse {

    private String id;
    private String url;
    private String user;
    private String user2;
    private String gave;
    private String got;
    private String description;
    private String type;
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
    private Boolean reciprocalApproved;

    private Instant createdAt;
    private Instant updatedAt;

}
