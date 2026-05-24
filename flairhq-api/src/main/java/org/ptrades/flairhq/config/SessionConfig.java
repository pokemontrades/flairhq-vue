package org.ptrades.flairhq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;

@Profile("!test")
@Configuration
@EnableMongoHttpSession(maxInactiveIntervalInSeconds = 300)
public class SessionConfig {
}
