package org.ptrades.flairhq.dto;

import lombok.Data;

@Data
public class FlairRequest {

    private String name;
    private String sub;
    private int trades;
    private int shinyEvents;
    private int events;
    private int eggs;
    private int giveaways;
    private int involvement;

}
