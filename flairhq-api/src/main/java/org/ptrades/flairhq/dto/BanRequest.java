package org.ptrades.flairhq.dto;

import java.util.List;

import lombok.Data;

@Data
public class BanRequest {

    private String       username;
    private String       banNote;
    private String       banMessage;
    private String       banlistEntry;
    private String       tradeNote;
    private Integer      duration;
    private String       knownAlt;
    private List<String> additionalFCs;

}
