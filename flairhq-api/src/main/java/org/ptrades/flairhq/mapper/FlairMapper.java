package org.ptrades.flairhq.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.ptrades.flairhq.dto.FlairRequest;
import org.ptrades.flairhq.dto.FlairResponse;
import org.ptrades.flairhq.repository.domain.Flair;

@Mapper(componentModel = "spring")
public interface FlairMapper {

    FlairResponse toResponse(Flair flair);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    Flair toDocument(FlairRequest req);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    void applyUpdate(FlairRequest req, @MappingTarget Flair flair);

}
