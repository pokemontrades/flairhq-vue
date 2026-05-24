package org.ptrades.flairhq.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.ptrades.flairhq.dto.RejectionReasonRequest;
import org.ptrades.flairhq.dto.RejectionReasonResponse;
import org.ptrades.flairhq.repository.domain.RejectionReason;

@Mapper(componentModel = "spring")
public interface RejectionReasonMapper {

    RejectionReasonResponse toResponse(RejectionReason reason);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    RejectionReason toDocument(RejectionReasonRequest req);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    void updateDocument(RejectionReasonRequest req, @MappingTarget RejectionReason target);

}
