package org.ptrades.flairhq.repository;

import java.util.List;

import org.ptrades.flairhq.repository.domain.Reference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReferenceRepository extends MongoRepository<Reference, String> {

    List<Reference> findByUrl(String url);
    List<Reference> findByUser(String user);
    Page<Reference> findByUser(String user, Pageable pageable);
    List<Reference> findByUser2(String user2);
    List<Reference> findByUserOrUser2(String user, String user2);
    List<Reference> findByUserAndUrl(String user, String url);
    List<Reference> findByUserAndUser2(String user, String user2);
    long countByUserAndApprovedTrue(String user);
    long countByUserAndApprovedTrueAndType(String user, String type);
    long countByUserAndType(String user, String type);

}
