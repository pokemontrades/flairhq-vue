package org.ptrades.flairhq.service;

import java.util.List;

import org.ptrades.flairhq.dto.EventResponse;
import org.ptrades.flairhq.dto.ModmailResponse;
import org.ptrades.flairhq.dto.ReferenceResponse;
import org.ptrades.flairhq.dto.UserResponse;
import org.ptrades.flairhq.mapper.ReferenceMapper;
import org.ptrades.flairhq.mapper.UserMapper;
import org.ptrades.flairhq.repository.domain.Event;
import org.ptrades.flairhq.repository.domain.Modmail;
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

    public List<EventResponse> searchLogs(@NonNull String keyword) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("content").regex(keyword, "i"),
                Criteria.where("user").regex(keyword, "i"),
                Criteria.where("type").regex(keyword, "i")
        )).limit(LIMIT).with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Event.class).stream()
                .map(e -> EventResponse.builder()
                        .id(e.getId())
                        .type(e.getType())
                        .user(e.getUser())
                        .content(e.getContent())
                        .createdAt(e.getCreatedAt())
                        .updatedAt(e.getUpdatedAt())
                        .build())
                .toList();
    }

    public List<ModmailResponse> searchModmails(@NonNull String keyword) {
        Query query = new Query(new Criteria().orOperator(
                Criteria.where("author").regex(keyword, "i"),
                Criteria.where("subject").regex(keyword, "i"),
                Criteria.where("body").regex(keyword, "i")
        )).limit(LIMIT).with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Modmail.class).stream()
                .map(m -> ModmailResponse.builder()
                        .id(m.getId())
                        .subject(m.getSubject())
                        .body(m.getBody())
                        .author(m.getAuthor())
                        .subreddit(m.getSubreddit())
                        .createdAt(m.getCreatedAt())
                        .updatedAt(m.getUpdatedAt())
                        .build())
                .toList();
    }

}
