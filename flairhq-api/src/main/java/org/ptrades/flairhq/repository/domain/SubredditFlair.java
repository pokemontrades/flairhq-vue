package org.ptrades.flairhq.repository.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class SubredditFlair {

    private String flairCssClass; // flair_css_class
    private String flairPosition; // flair_position
    private String flairTemplateId; // flair_template_id
    private String flairText; // flair_text
    
}
