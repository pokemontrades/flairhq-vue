package org.ptrades.flairhq.repository;

import org.ptrades.flairhq.repository.domain.RejectionReason;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RejectionReasonRepository extends MongoRepository<RejectionReason, String> {
}
