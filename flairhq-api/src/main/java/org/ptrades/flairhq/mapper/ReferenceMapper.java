package org.ptrades.flairhq.mapper;

import java.util.Objects;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.ptrades.flairhq.dto.ReferenceRequest;
import org.ptrades.flairhq.dto.ReferenceResponse;
import org.ptrades.flairhq.repository.domain.Reference;

@Mapper(componentModel = "spring")
public interface ReferenceMapper {

    ReferenceResponse toResponse(Reference ref);

    default ReferenceResponse toResponse(Reference ref, boolean includePrivateNotes) {
        ReferenceResponse response = toResponse(ref);
        return includePrivateNotes ? response : response.toBuilder().privateNotes(null).build();
    }

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "user",          source = "username")
    @Mapping(target = "verified",      constant = "false")
    @Mapping(target = "approved",      constant = "false")
    @Mapping(target = "edited",        constant = "false")
    @Mapping(target = "rejected",       constant = "false")
    @Mapping(target = "rejectedReason", ignore = true)
    @Mapping(target = "mustFix",        constant = "false")
    @Mapping(target = "mustFixReason",  ignore = true)
    @Mapping(target = "number",    expression = "java(req.getNumber() != null ? req.getNumber() : 0)")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    Reference toDocument(ReferenceRequest req, String username);

    default void applyUpdate(ReferenceRequest req, Reference existing) {
        boolean keyFieldChanged = !Objects.equals(req.getUrl(),    existing.getUrl())
                               || !Objects.equals(req.getType(),   existing.getType())
                               || !Objects.equals(req.getNumber(), existing.getNumber());
        mapUpdate(req, existing);
        if (keyFieldChanged) {
            existing.setApproved(false);
        }
    }

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "user",          ignore = true)
    @Mapping(target = "verified",      ignore = true)
    @Mapping(target = "approved",      ignore = true)
    @Mapping(target = "rejected",       ignore = true)
    @Mapping(target = "rejectedReason", ignore = true)
    @Mapping(target = "mustFix",        ignore = true)
    @Mapping(target = "mustFixReason",  ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "edited",    constant = "true")
    @Mapping(target = "number",    expression = "java(req.getNumber() != null ? req.getNumber() : 0)")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    void mapUpdate(ReferenceRequest req, @MappingTarget Reference existing);

}
