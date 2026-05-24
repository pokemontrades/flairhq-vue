package org.ptrades.flairhq.repository;

import java.util.List;

import org.ptrades.flairhq.repository.domain.Flair;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FlairRepository extends MongoRepository<Flair, String> {

    List<Flair> findBySub(String sub);
    Flair findByNameAndSub(String name, String sub);

}
