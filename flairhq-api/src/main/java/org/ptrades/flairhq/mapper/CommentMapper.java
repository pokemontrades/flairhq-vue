package org.ptrades.flairhq.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.ptrades.flairhq.dto.CommentRequest;
import org.ptrades.flairhq.dto.CommentResponse;
import org.ptrades.flairhq.repository.domain.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    CommentResponse toResponse(Comment comment);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "user2",     source = "commenter")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    Comment toDocument(CommentRequest req, String commenter);

}
