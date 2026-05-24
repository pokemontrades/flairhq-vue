package org.ptrades.flairhq.repository;

import java.util.List;

import org.ptrades.flairhq.repository.domain.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findByUser(String user);
    List<Event> findByUserAndType(String user, String type, Pageable pageable);

}
