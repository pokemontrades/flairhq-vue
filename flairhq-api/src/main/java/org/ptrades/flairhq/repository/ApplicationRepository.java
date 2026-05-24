package org.ptrades.flairhq.repository;

import java.util.List;

import org.ptrades.flairhq.repository.domain.Application;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ApplicationRepository extends MongoRepository<Application, String> {

    List<Application> findByUser(String user);
    Application findByUserAndFlair(String user, String flair);
    Application findByUserAndFlairAndSub(String user, String flair, String sub);

}
