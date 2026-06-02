package org.ptrades.flairhq.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptrades.flairhq.common.EventType;
import org.ptrades.flairhq.dto.SetFlairTextRequest;
import org.ptrades.flairhq.dto.UserRequest;
import org.ptrades.flairhq.dto.UserResponse;
import org.ptrades.flairhq.mapper.UserMapper;
import org.ptrades.flairhq.repository.EventRepository;
import org.ptrades.flairhq.repository.UserRepository;
import org.ptrades.flairhq.repository.domain.Event;
import org.ptrades.flairhq.repository.domain.SubredditFlair;
import org.ptrades.flairhq.repository.domain.User;
import org.ptrades.flairhq.repository.domain.UserFlair;
import org.ptrades.flairhq.service.BanService;
import org.ptrades.flairhq.service.FlairService;
import org.ptrades.flairhq.service.RedditApiService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProcessorTest {

    @Mock UserRepository   userRepository;
    @Mock EventRepository  eventRepository;
    @Mock UserMapper       userMapper;
    @Mock BanService       banService;
    @Mock FlairService     flairService;
    @Mock RedditApiService redditApiService;
    @Mock SessionRegistry  sessionRegistry;

    @InjectMocks UserProcessor processor;

    // ── getUsers ──────────────────────────────────────────────────────────────

    @Test
    void getUsers_nullBanned_callsFindAll() {
        when(userRepository.findAll()).thenReturn(List.of());

        processor.getUsers(null);

        verify(userRepository).findAll();
        verify(userRepository, never()).findByBanned(anyBoolean());
    }

    @Test
    void getUsers_filteredByBanned_callsFindByBanned() {
        when(userRepository.findByBanned(true)).thenReturn(List.of());

        processor.getUsers(true);

        verify(userRepository).findByBanned(true);
        verify(userRepository, never()).findAll();
    }

    // ── upsertMe ──────────────────────────────────────────────────────────────

    @Test
    void upsertMe_existingUser_appliesUpdateAndSaves() {
        User user = makeUser("alice");
        UserRequest request = new UserRequest();
        request.setHideReciprocalSection(true);

        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().build());

        processor.upsertMe(request, "alice");

        verify(userMapper).applyUpdate(request, user);
        verify(userMapper, never()).toNewUser(any(), any());
        verify(userRepository).save(user);
    }

    @Test
    void upsertMe_newUser_createsViaToNewUser() {
        User user = makeUser("alice");
        UserRequest request = new UserRequest();

        when(userRepository.findById("alice")).thenReturn(Optional.empty());
        when(userMapper.toNewUser(request, "alice")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().build());

        processor.upsertMe(request, "alice");

        verify(userMapper).toNewUser(request, "alice");
        verify(userMapper, never()).applyUpdate(any(), any());
    }

    // ── setFlairText ──────────────────────────────────────────────────────────

    @Test
    void setFlairText_recentEventWithin120s_throwsTooManyRequests() {
        Event recent = new Event();
        recent.setCreatedAt(Instant.now().minusSeconds(60)); // 60 s ago — within cooldown
        when(eventRepository.findByUserAndType(eq("alice"), eq(EventType.FLAIR_TEXT_CHANGE), any()))
                .thenReturn(List.of(recent));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.setFlairText(new SetFlairTextRequest(), "alice"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    @Test
    void setFlairText_oldEventBeyond120s_proceeds() {
        Event old = new Event();
        old.setCreatedAt(Instant.now().minusSeconds(200)); // 200 s ago — cooldown expired
        when(eventRepository.findByUserAndType(eq("alice"), eq(EventType.FLAIR_TEXT_CHANGE), any()))
                .thenReturn(List.of(old));

        User user = makeUser("alice");
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(flairService.makeNewFlairText(anyString(), any())).thenReturn(":10:text");
        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(userRepository.save(any())).thenReturn(user);

        SetFlairTextRequest request = new SetFlairTextRequest();
        request.setPtrades("text");

        processor.setFlairText(request, "alice");

        verify(redditApiService).setUserFlair(eq("token"), eq("alice"), any(), eq(":10:text"), eq("pokemontrades"));
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void setFlairText_noRecentEvents_proceeds() {
        when(eventRepository.findByUserAndType(eq("alice"), eq(EventType.FLAIR_TEXT_CHANGE), any()))
                .thenReturn(List.of());

        User user = makeUser("alice");
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(flairService.makeNewFlairText(anyString(), any())).thenReturn(":0:text");
        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(userRepository.save(any())).thenReturn(user);

        SetFlairTextRequest request = new SetFlairTextRequest();
        request.setPtrades("text");

        processor.setFlairText(request, "alice");

        verify(eventRepository).save(any(Event.class));
    }

    // ── setLocalBan ───────────────────────────────────────────────────────────

    @Test
    void setLocalBan_userNotFound_throwsNotFound() {
        when(userRepository.findById("alice")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.setLocalBan("alice", true));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void setLocalBan_found_updatesBannedFlag() {
        User user = makeUser("alice");
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().build());

        processor.setLocalBan("alice", true);

        assertTrue(user.getBanned());
        verify(userRepository).save(user);
    }

    @Test
    void setLocalBan_unban_clearsBannedFlag() {
        User user = makeUser("alice");
        user.setBanned(true);
        when(userRepository.findById("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().build());

        processor.setLocalBan("alice", false);

        assertFalse(user.getBanned());
    }

    // ── invalidateSessions ────────────────────────────────────────────────────

    @Test
    void invalidateSessions_matchingPrincipal_expiresSessionsAndReturnsCount() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("name")).thenReturn("alice");

        SessionInformation session = new SessionInformation(principal, "sess-1", new Date());

        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessionRegistry.getAllSessions(principal, false)).thenReturn(List.of(session));

        int count = processor.invalidateSessions("alice");

        assertEquals(1, count);
        assertTrue(session.isExpired());
    }

    @Test
    void invalidateSessions_noMatchingPrincipal_returnsZero() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("name")).thenReturn("bob");

        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));

        int count = processor.invalidateSessions("alice");

        assertEquals(0, count);
        verify(sessionRegistry, never()).getAllSessions(any(), anyBoolean());
    }

    @Test
    void invalidateSessions_nonOAuth2Principal_skipped() {
        Object notOAuth = new Object();
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(notOAuth));

        int count = processor.invalidateSessions("alice");

        assertEquals(0, count);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static User makeUser(String id) {
        User u = new User();
        u.setId(id);
        UserFlair uf = new UserFlair();
        SubredditFlair sf = new SubredditFlair();
        sf.setFlairCssClass("default");
        sf.setFlairText("");
        uf.setPtrades(sf);
        u.setFlair(uf);
        return u;
    }
}
