package org.ptrades.flairhq.repository;

import java.util.List;

import org.ptrades.flairhq.repository.domain.Modmail;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ModmailRepository extends MongoRepository<Modmail, String> {

    List<Modmail> findByAuthor(String author);
    List<Modmail> findBySubreddit(String subreddit);

}
