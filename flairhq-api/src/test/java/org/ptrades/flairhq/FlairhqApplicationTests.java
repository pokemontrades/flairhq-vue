package org.ptrades.flairhq;

import org.junit.jupiter.api.Test;
import org.ptrades.flairhq.repository.ApplicationRepository;
import org.ptrades.flairhq.repository.CommentRepository;
import org.ptrades.flairhq.repository.EventRepository;
import org.ptrades.flairhq.repository.FlairRepository;
import org.ptrades.flairhq.repository.GameRepository;
import org.ptrades.flairhq.repository.ModmailRepository;
import org.ptrades.flairhq.repository.ModnoteRepository;
import org.ptrades.flairhq.repository.ReferenceRepository;
import org.ptrades.flairhq.repository.RejectionReasonRepository;
import org.ptrades.flairhq.repository.UserRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration"
})
@ActiveProfiles("test")
class FlairhqApplicationTests {

    @MockitoBean MongoTemplate mongoTemplate;

    @MockitoBean FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @MockitoBean ApplicationRepository applicationRepository;
    @MockitoBean CommentRepository commentRepository;
    @MockitoBean EventRepository eventRepository;
    @MockitoBean FlairRepository flairRepository;
    @MockitoBean GameRepository gameRepository;
    @MockitoBean ModmailRepository modmailRepository;
    @MockitoBean ModnoteRepository modnoteRepository;
    @MockitoBean ReferenceRepository referenceRepository;
    @MockitoBean RejectionReasonRepository rejectionReasonRepository;
    @MockitoBean UserRepository userRepository;

    @Test
    void contextLoads() {
    }

}
