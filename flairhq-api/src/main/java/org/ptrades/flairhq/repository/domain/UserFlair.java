package org.ptrades.flairhq.repository.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class UserFlair {

    private SubredditFlair ptrades;

    public SubredditFlair getOrInitPtrades() {
        if (ptrades == null) ptrades = new SubredditFlair();
        return ptrades;
    }

}
