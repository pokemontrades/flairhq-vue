package org.ptrades.flairhq.processor;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.ptrades.flairhq.common.EventType;
import org.ptrades.flairhq.dto.BanRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProcessor {

    private static final Logger log = LoggerFactory.getLogger(UserProcessor.class);

    private final UserRepository   userRepository;
    private final EventRepository  eventRepository;
    private final UserMapper       userMapper;
    private final BanService       banService;
    private final FlairService     flairService;
    private final RedditApiService redditApiService;
    private final SessionRegistry  sessionRegistry;

    public UserProcessor(UserRepository userRepository, EventRepository eventRepository,
                         UserMapper userMapper, BanService banService,
                         FlairService flairService, RedditApiService redditApiService,
                         SessionRegistry sessionRegistry) {
        this.userRepository   = userRepository;
        this.eventRepository  = eventRepository;
        this.userMapper       = userMapper;
        this.banService       = banService;
        this.flairService     = flairService;
        this.redditApiService = redditApiService;
        this.sessionRegistry  = sessionRegistry;
    }

    public List<UserResponse> getUsers(Boolean banned) {
        List<User> users = (banned != null)
                ? userRepository.findByBanned(banned)
                : userRepository.findAll();
        return users.stream().map(userMapper::toResponse).toList();
    }

    public Optional<UserResponse> getUser(String username) {
        return userRepository.findById(Objects.requireNonNull(username))
                .map(user -> {
                    if (user.getIconImg() == null) {
                        String iconImg = redditApiService.getUserIconImg(username);
                        if (iconImg != null) {
                            user.setIconImg(iconImg);
                            user.setUpdatedAt(Instant.now());
                            userRepository.save(user);
                        }
                    }
                    return userMapper.toResponse(user);
                });
    }

    public UserResponse upsertMe(UserRequest request, String username) {
        Optional<User> existing = userRepository.findById(Objects.requireNonNull(username));

        User user;
        if (existing.isPresent()) {
            user = existing.get();
            userMapper.applyUpdate(request, user);
        } else {
            user = userMapper.toNewUser(request, username);
        }

        return userMapper.toResponse(userRepository.save(Objects.requireNonNull(user)));
    }

    /**
     * Updates a user's flair text on r/pokemontrades. Enforces a 120-second cooldown (checked via
     * the event log) to prevent rapid changes, constructs the new text with the correct emoji prefix
     * for the user's current CSS class, pushes the change to Reddit, then persists it to MongoDB and
     * logs a {@code flairTextChange} event.
     * 
     * @param request
     * @param username
     */
    public void setFlairText(SetFlairTextRequest request, String username) {
        List<Event> recent = eventRepository.findByUserAndType(username, EventType.FLAIR_TEXT_CHANGE,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
        if (!recent.isEmpty() && recent.get(0).getCreatedAt() != null
                && recent.get(0).getCreatedAt().plusSeconds(120).isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }

        User user = userRepository.findById(Objects.requireNonNull(username)).orElseGet(() -> {
            User u = new User();
            u.setId(username);
            u.setCreatedAt(Instant.now());
            return u;
        });

        UserFlair      userFlair = user.getFlair() != null ? user.getFlair() : new UserFlair();
        SubredditFlair ptrades   = userFlair.getPtrades() != null ? userFlair.getPtrades() : new SubredditFlair();

        String pCss      = ptrades.getFlairCssClass() != null ? ptrades.getFlairCssClass() : "default";
        String flairText = flairService.makeNewFlairText(pCss, request.getPtrades());

        redditApiService.setUserFlair(
                redditApiService.getAdminRefreshToken(), username, pCss, flairText, "pokemontrades");

        ptrades.setFlairText(flairText);
        userFlair.setPtrades(ptrades);
        user.setFlair(userFlair);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        Event event = new Event();
        event.setType(EventType.FLAIR_TEXT_CHANGE);
        event.setUser(username);
        event.setContent("Changed flair text to: " + request.getPtrades());
        event.setCreatedAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        eventRepository.save(event);
    }

    public void banUser(String username, BanRequest request) {
        request.setUsername(username);
        banService.executeBan(request);
    }

    public UserResponse setLocalBan(String username, boolean banned) {
        User user = userRepository.findById(Objects.requireNonNull(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setBanned(banned);
        user.setUpdatedAt(Instant.now());
        return userMapper.toResponse(userRepository.save(user));
    }

    /** Expires all active sessions for the given username. The next request from that user returns 401.
     * 
     * @param username
     * @return
     */
    public int invalidateSessions(String username) {
        return (int) sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof OAuth2User o && username.equals(o.getAttribute("name")))
                .flatMap(p -> sessionRegistry.getAllSessions(p, false).stream())
                .peek(s -> { log.info("Expiring session id='{}' for user='{}'", s.getSessionId(), username); s.expireNow(); })
                .count();
    }
}
