package org.ptrades.flairhq.repository.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document
public class User {

    @Id
    private String id;

    private String redToken;
    private String iconImg;
    private UserFlair flair;
    private String[] modPermissions; // test around this
    private String[] loggedFriendCodes; // test around this
    private Boolean isMod;
    private Boolean banned;
    private String[] friendCodes; // test around this
    private String intro;
    private Boolean hideReciprocalSection;

    private Instant createdAt;
    private Instant updatedAt;

}
