package org.ptrades.flairhq.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptrades.flairhq.repository.EventRepository;
import org.ptrades.flairhq.repository.UserRepository;
import org.ptrades.flairhq.repository.domain.SubredditFlair;
import org.ptrades.flairhq.repository.domain.User;
import org.ptrades.flairhq.repository.domain.UserFlair;
import org.ptrades.flairhq.service.DiscordApiException;
import org.ptrades.flairhq.service.DiscordApiService;
import org.ptrades.flairhq.service.DiscordUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordProcessorTest {

    @Mock DiscordApiService discordApiService;
    @Mock UserRepository    userRepository;
    @Mock EventRepository   eventRepository;

    @InjectMocks DiscordProcessor processor;

    // ── discordCallback — access control ──────────────────────────────────────

    @Test
    void discordCallback_userNotFound_throwsForbidden() {
        when(userRepository.findById("alice")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.discordCallback("code", "alice"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void discordCallback_userHasNoFlair_throwsForbidden() {
        User user = new User();
        user.setFlair(null);
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.discordCallback("code", "alice"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void discordCallback_userFlairHasNullPtrades_throwsForbidden() {
        User user = new User();
        UserFlair uf = new UserFlair();
        uf.setPtrades(null);
        user.setFlair(uf);
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.discordCallback("code", "alice"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void discordCallback_userFlairTextNull_throwsForbidden() {
        User user = makeUserWithFlair(null);
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.discordCallback("code", "alice"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ── discordCallback — happy paths ─────────────────────────────────────────

    @Test
    void discordCallback_newMember_savesEventAndRedirects() {
        User user = makeUserWithFlair(":10:text");
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));

        when(discordApiService.getAccessToken("code")).thenReturn("access");
        DiscordUser discord = new DiscordUser("123", "Alice", "0");
        when(discordApiService.getCurrentUser("access")).thenReturn(discord);
        when(discordApiService.addUserToGuild("access", "123", "alice")).thenReturn(true); // new member
        when(discordApiService.getServerUrl()).thenReturn("https://discordapp.com/channels/999");

        URI result = processor.discordCallback("code", "alice");

        assertEquals(URI.create("https://discordapp.com/channels/999"), result);
        verify(eventRepository).save(any());
    }

    @Test
    void discordCallback_existingMember_doesNotSaveEvent() {
        User user = makeUserWithFlair(":10:text");
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));

        when(discordApiService.getAccessToken("code")).thenReturn("access");
        DiscordUser discord = new DiscordUser("123", "Alice", "0");
        when(discordApiService.getCurrentUser("access")).thenReturn(discord);
        when(discordApiService.addUserToGuild("access", "123", "alice")).thenReturn(false); // already member
        when(discordApiService.getServerUrl()).thenReturn("https://discordapp.com/channels/999");

        processor.discordCallback("code", "alice");

        verify(eventRepository, never()).save(any());
    }

    // ── discordCallback — error handling ──────────────────────────────────────

    @Test
    void discordCallback_discordRateLimit_throwsTooManyRequests() {
        User user = makeUserWithFlair(":10:text");
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(discordApiService.getAccessToken("code")).thenThrow(new DiscordApiException(429, "rate limited"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.discordCallback("code", "alice"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void discordCallback_discordOtherError_throwsBadGateway() {
        User user = makeUserWithFlair(":10:text");
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(discordApiService.getAccessToken("code")).thenThrow(new DiscordApiException(500, "server error"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.discordCallback("code", "alice"));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static User makeUserWithFlair(String flairText) {
        User user = new User();
        UserFlair uf = new UserFlair();
        SubredditFlair sf = new SubredditFlair();
        sf.setFlairText(flairText);
        uf.setPtrades(sf);
        user.setFlair(uf);
        return user;
    }
}
