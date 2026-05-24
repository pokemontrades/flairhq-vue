package org.ptrades.flairhq.repository;

import java.util.List;

import org.ptrades.flairhq.repository.domain.Game;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GameRepository extends MongoRepository<Game, String> {

    List<Game> findByUser(String user);

}
