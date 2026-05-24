package org.ptrades.flairhq.processor;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

import org.ptrades.flairhq.common.EventType;
import org.ptrades.flairhq.repository.EventRepository;
import org.ptrades.flairhq.repository.UserRepository;
import org.ptrades.flairhq.repository.domain.Event;
import org.ptrades.flairhq.repository.domain.User;
import org.ptrades.flairhq.repository.domain.UserFlair;
import org.ptrades.flairhq.service.DiscordApiException;
import org.ptrades.flairhq.service.DiscordApiService;
import org.ptrades.flairhq.service.DiscordUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DiscordProcessor {

    private final DiscordApiService discordApiService;
    private final UserRepository    userRepository;
    private final EventRepository   eventRepository;

    public DiscordProcessor(DiscordApiService discordApiService,
                            UserRepository userRepository,
                            EventRepository eventRepository) {
        this.discordApiService = discordApiService;
        this.userRepository    = userRepository;
        this.eventRepository   = eventRepository;
    }

    /**
     * Handles the Discord OAuth callback, adding the user to the server if they have any flair.
     * 
     * @param code
     * @param username
     * @return
     */
    public URI discordCallback(String code, String username) {
        User user = userRepository.findById(Objects.requireNonNull(username)).orElse(null);
        if (!hasAnyFlair(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            String      accessToken = discordApiService.getAccessToken(code);
            DiscordUser discord     = discordApiService.getCurrentUser(accessToken);
            boolean     newMember   = discordApiService.addUserToGuild(accessToken, discord.id(), username);

            if (newMember) {
                Event event = new Event();
                event.setType(EventType.DISCORD_JOIN);
                event.setUser(username);
                event.setContent("Joined Discord as @" + discord.username()
                        + "#" + discord.discriminator()
                        + " (ID: " + discord.id() + ")");
                event.setCreatedAt(Instant.now());
                event.setUpdatedAt(Instant.now());
                eventRepository.save(event);
            }

            return URI.create(discordApiService.getServerUrl());

        } catch (DiscordApiException e) {
            if (e.getStatusCode() == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY);
        }
    }

    private boolean hasAnyFlair(User user) {
        if (user == null) {
            return false;
        }
        UserFlair flair = user.getFlair();
        if (flair == null) {
            return false;
        }
        return flair.getPtrades() != null && flair.getPtrades().getFlairText() != null;
    }
}
