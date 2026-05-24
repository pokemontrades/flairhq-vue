package org.ptrades.flairhq.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.ptrades.flairhq.dto.UserRequest;
import org.ptrades.flairhq.dto.UserResponse;
import org.ptrades.flairhq.repository.domain.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // redToken and loggedFriendCodes have no counterpart in UserResponse — MapStruct skips them automatically
    UserResponse toResponse(User user);

    @Mapping(target = "id",               source = "username")
    @Mapping(target = "isMod",            constant = "false")
    @Mapping(target = "banned",           constant = "false")
    @Mapping(target = "flair",            ignore = true)
    @Mapping(target = "iconImg",          ignore = true)
    @Mapping(target = "modPermissions",   ignore = true)
    @Mapping(target = "redToken",         ignore = true)
    @Mapping(target = "loggedFriendCodes", ignore = true)
    @Mapping(target = "createdAt",        expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt",        expression = "java(java.time.Instant.now())")
    User toNewUser(UserRequest req, String username);

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "flair",            ignore = true)
    @Mapping(target = "iconImg",          ignore = true)
    @Mapping(target = "isMod",            ignore = true)
    @Mapping(target = "banned",           ignore = true)
    @Mapping(target = "modPermissions",   ignore = true)
    @Mapping(target = "redToken",         ignore = true)
    @Mapping(target = "loggedFriendCodes", ignore = true)
    @Mapping(target = "createdAt",        ignore = true)
    @Mapping(target = "updatedAt",        expression = "java(java.time.Instant.now())")
    void applyUpdate(UserRequest req, @MappingTarget User user);

}
