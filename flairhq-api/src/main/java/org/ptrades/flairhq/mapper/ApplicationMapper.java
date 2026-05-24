package org.ptrades.flairhq.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.ptrades.flairhq.dto.ApplicationRequest;
import org.ptrades.flairhq.dto.ApplicationResponse;
import org.ptrades.flairhq.repository.domain.Application;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    @Mapping(target = "approvedTrades", ignore = true)
    @Mapping(target = "requiredTrades", ignore = true)
    ApplicationResponse toResponse(Application application);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    Application toDocument(ApplicationRequest req);

}
