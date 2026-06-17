package org.ptrades.flairhq.service;

import java.util.List;

import org.ptrades.flairhq.dto.ReferenceResponse;
import org.ptrades.flairhq.dto.UserResponse;
import org.ptrades.flairhq.mapper.ReferenceMapper;
import org.ptrades.flairhq.mapper.UserMapper;
import org.ptrades.flairhq.repository.domain.Reference;
import org.ptrades.flairhq.repository.domain.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final int LIMIT = 20;

    private final MongoTemplate   mongoTemplate;
    private final UserMapper      userMapper;
    private final ReferenceMapper referenceMapper;

    public SearchService(MongoTemplate mongoTemplate, UserMapper userMapper, ReferenceMapper referenceMapper) {
        this.mongoTemplate   = mongoTemplate;
        this.userMapper      = userMapper;
        this.referenceMapper = referenceMapper;
    }

    public List<UserResponse> searchUsers(@NonNull String keyword) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("_id").regex(keyword, "i"),
                Criteria.where("flair.ptrades.flairText").regex(keyword, "i")
        )).limit(LIMIT);
        return mongoTemplate.find(query, User.class).stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public List<ReferenceResponse> searchReferences(@NonNull String keyword) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("description").regex(keyword, "i"),
                Criteria.where("gave").regex(keyword, "i"),
                Criteria.where("got").regex(keyword, "i"),
                Criteria.where("user").regex(keyword, "i"),
                Criteria.where("user2").regex(keyword, "i")
        )).limit(LIMIT).with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Reference.class).stream()
                .map(ref -> referenceMapper.toResponse(ref, false))
                .toList();
    }

}
