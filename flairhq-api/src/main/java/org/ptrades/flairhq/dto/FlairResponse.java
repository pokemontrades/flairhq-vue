package org.ptrades.flairhq.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FlairResponse {

    private String id;
    private String name;
    private String sub;
    private int trades;
    private int shinyEvents;
    private int events;
    private int eggs;
    private int giveaways;
    private int involvement;
    private Instant createdAt;
    private Instant updatedAt;

}
