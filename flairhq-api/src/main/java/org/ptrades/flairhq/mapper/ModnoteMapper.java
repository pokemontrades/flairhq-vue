package org.ptrades.flairhq.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.ptrades.flairhq.dto.ModnoteRequest;
import org.ptrades.flairhq.dto.ModnoteResponse;
import org.ptrades.flairhq.repository.domain.Modnote;

@Mapper(componentModel = "spring")
public interface ModnoteMapper {

    ModnoteResponse toResponse(Modnote modnote);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user",      source = "mod")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    Modnote toDocument(ModnoteRequest req, String mod);

}
