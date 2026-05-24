package org.ptrades.flairhq.repository;

import java.util.List;

import org.ptrades.flairhq.repository.domain.Modnote;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ModnoteRepository extends MongoRepository<Modnote, String> {

    List<Modnote> findByRefUser(String refUser);

}
