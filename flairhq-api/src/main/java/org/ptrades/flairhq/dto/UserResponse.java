package org.ptrades.flairhq.dto;

import java.time.Instant;

import org.ptrades.flairhq.repository.domain.UserFlair;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private String id;
    private String iconImg;
    private UserFlair flair;
    private Boolean isMod;
    private String[] modPermissions;
    private Boolean banned;
    private String[] friendCodes;
    private String intro;
    private Instant createdAt;
    private Instant updatedAt;

}
