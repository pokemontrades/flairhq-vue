package org.ptrades.flairhq.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptrades.flairhq.mapper.ApplicationMapper;
import org.ptrades.flairhq.repository.ApplicationRepository;
import org.ptrades.flairhq.repository.FlairRepository;
import org.ptrades.flairhq.repository.ReferenceRepository;
import org.ptrades.flairhq.repository.UserRepository;
import org.ptrades.flairhq.repository.domain.Application;
import org.ptrades.flairhq.repository.domain.Flair;
import org.ptrades.flairhq.repository.domain.SubredditFlair;
import org.ptrades.flairhq.repository.domain.User;
import org.ptrades.flairhq.repository.domain.UserFlair;
import org.ptrades.flairhq.service.FlairService;
import org.ptrades.flairhq.service.RedditApiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ApplicationProcessorTest {

    @Mock ApplicationRepository applicationRepository;
    @Mock ApplicationMapper     applicationMapper;
    @Mock UserRepository        userRepository;
    @Mock FlairRepository       flairRepository;
    @Mock ReferenceRepository   referenceRepository;
    @Mock FlairService          flairService;
    @Mock RedditApiService      redditApiService;

    @InjectMocks ApplicationProcessor processor;

    // ── Requirement 1: only highest trade flair approvable ───────────────────

    @Test
    void approveApplication_higherFlairPending_throwsUnprocessableEntity() {
        Application app = makeApp("app1", "user1", "pokeball", "pokemontrades");
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(app));

        Flair pokeball = makeFlair("pokeball", "pokemontrades", 10, 0, 0);
        when(flairRepository.findByNameAndSub("pokeball", "pokemontrades")).thenReturn(pokeball);
        when(referenceRepository.countByUserAndApprovedTrueAndType(anyString(), anyString())).thenReturn(100L);

        Application higher = makeApp("app2", "user1", "ultraball", "pokemontrades");
        Flair ultraball = makeFlair("ultraball", "pokemontrades", 50, 0, 0);
        when(flairRepository.findByNameAndSub("ultraball", "pokemontrades")).thenReturn(ultraball);
        when(applicationRepository.findByUser("user1")).thenReturn(List.of(app, higher));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.approveApplication("app1"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void approveApplication_isHighestFlair_approves() {
        Application app = makeApp("app1", "user1", "ultraball", "pokemontrades");
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(app));

        Flair ultraball = makeFlair("ultraball", "pokemontrades", 50, 0, 0);
        when(flairRepository.findByNameAndSub("ultraball", "pokemontrades")).thenReturn(ultraball);
        when(referenceRepository.countByUserAndApprovedTrueAndType(anyString(), anyString())).thenReturn(100L);

        Application lower = makeApp("app2", "user1", "pokeball", "pokemontrades");
        Flair pokeball = makeFlair("pokeball", "pokemontrades", 10, 0, 0);
        when(flairRepository.findByNameAndSub("pokeball", "pokemontrades")).thenReturn(pokeball);
        when(applicationRepository.findByUser("user1"))
                .thenReturn(List.of(app, lower))   // call in checkNoHigherFlairPending
                .thenReturn(List.of(lower));         // call after deleteById for soft-deny

        User user = makeUser("user1");
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(flairService.makeNewCssClass(anyString(), anyString())).thenReturn("ultraball");
        when(flairService.makeNewFlairText(anyString(), anyString())).thenReturn("🔵");
        when(flairService.formattedName(anyString())).thenReturn("Ultra Ball");
        when(referenceRepository.findByUser("user1")).thenReturn(List.of());

        processor.approveApplication("app1");

        verify(applicationRepository).deleteById("app1");
    }

    @Test
    void approveApplication_involvementFlair_notBlockedByHigherTrades() {
        Application involvementApp = makeApp("app1", "user1", "involvement", "pokemontrades");
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(involvementApp));

        Flair involvementFlair = makeFlair("involvement", "pokemontrades", 0, 5, 0);
        when(flairRepository.findByNameAndSub("involvement", "pokemontrades")).thenReturn(involvementFlair);
        when(referenceRepository.countByUserAndApprovedTrueAndType(anyString(), anyString())).thenReturn(100L);

        when(applicationRepository.findByUser("user1"))
                .thenReturn(List.of());  // after deleteById (involvement skips the higher-flair check)

        User user = makeUser("user1");
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(flairService.makeNewCssClass(anyString(), anyString())).thenReturn("involvement1");
        when(flairService.makeNewFlairText(anyString(), anyString())).thenReturn("🌀");
        when(flairService.formattedName(anyString())).thenReturn("Involvement");
        when(referenceRepository.findByUser("user1")).thenReturn(List.of());

        // involvement flair (trades=0) skips the higher-flair check — no exception expected
        processor.approveApplication("app1");

        verify(applicationRepository).deleteById("app1");
        // verify ultraball app's flair repo was never queried (check was skipped)
        verify(flairRepository, never()).findByNameAndSub("ultraball", "pokemontrades");
    }

    // ── Requirement 3: soft-deny remaining apps on approval ──────────────────

    @Test
    void approveApplication_tradeFlair_softDeniesLowerTradeFlair_notInvolvement() {
        Application app = makeApp("app1", "user1", "ultraball", "pokemontrades");
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(app));

        Flair ultraball = makeFlair("ultraball", "pokemontrades", 50, 0, 0);
        when(flairRepository.findByNameAndSub("ultraball", "pokemontrades")).thenReturn(ultraball);
        when(referenceRepository.countByUserAndApprovedTrueAndType(anyString(), anyString())).thenReturn(100L);

        Application lowerTrade = makeApp("app2", "user1", "pokeball", "pokemontrades");
        Flair pokeball = makeFlair("pokeball", "pokemontrades", 10, 0, 0);
        when(flairRepository.findByNameAndSub("pokeball", "pokemontrades")).thenReturn(pokeball);

        Application invApp = makeApp("app3", "user1", "involvement", "pokemontrades");
        Flair invFlair = makeFlair("involvement", "pokemontrades", 0, 5, 0);
        when(flairRepository.findByNameAndSub("involvement", "pokemontrades")).thenReturn(invFlair);

        when(applicationRepository.findByUser("user1"))
                .thenReturn(List.of(app, lowerTrade, invApp))  // checkNoHigherFlairPending
                .thenReturn(List.of(lowerTrade, invApp));       // soft-deny after deleteById

        User user = makeUser("user1");
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(flairService.makeNewCssClass(anyString(), anyString())).thenReturn("ultraball");
        when(flairService.makeNewFlairText(anyString(), anyString())).thenReturn("🔵");
        when(flairService.formattedName(anyString())).thenReturn("Ultra Ball");
        when(referenceRepository.findByUser("user1")).thenReturn(List.of());

        processor.approveApplication("app1");

        verify(applicationRepository).deleteAll(new java.util.ArrayList<>(List.of(lowerTrade)));
    }

    @Test
    void approveApplication_tradeFlair_doesNotSoftDenyInvolvementFlair() {
        Application app = makeApp("app1", "user1", "pokeball", "pokemontrades");
        when(applicationRepository.findById("app1")).thenReturn(Optional.of(app));

        Flair pokeball = makeFlair("pokeball", "pokemontrades", 10, 0, 0);
        when(flairRepository.findByNameAndSub("pokeball", "pokemontrades")).thenReturn(pokeball);
        when(referenceRepository.countByUserAndApprovedTrueAndType(anyString(), anyString())).thenReturn(100L);

        Application invApp = makeApp("app2", "user1", "involvement", "pokemontrades");
        Flair invFlair = makeFlair("involvement", "pokemontrades", 0, 5, 0);
        when(flairRepository.findByNameAndSub("involvement", "pokemontrades")).thenReturn(invFlair);

        when(applicationRepository.findByUser("user1"))
                .thenReturn(List.of(app, invApp))  // checkNoHigherFlairPending
                .thenReturn(List.of(invApp));       // soft-deny scan after deleteById

        User user = makeUser("user1");
        when(userRepository.findById("user1")).thenReturn(Optional.of(user));
        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(flairService.makeNewCssClass(anyString(), anyString())).thenReturn("pokeball");
        when(flairService.makeNewFlairText(anyString(), anyString())).thenReturn("🔴");
        when(flairService.formattedName(anyString())).thenReturn("Poké Ball");
        when(referenceRepository.findByUser("user1")).thenReturn(List.of());

        processor.approveApplication("app1");

        // involvement app is filtered out — deleteAll must not be called
        verify(applicationRepository, never()).deleteAll(org.mockito.ArgumentMatchers.any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Application makeApp(String id, String user, String flair, String sub) {
        Application a = new Application();
        a.setId(id);
        a.setUser(user);
        a.setFlair(flair);
        a.setSub(sub);
        return a;
    }

    private static Flair makeFlair(String name, String sub, int trades, int involvement, int giveaways) {
        Flair f = new Flair();
        f.setName(name);
        f.setSub(sub);
        f.setTrades(trades);
        f.setInvolvement(involvement);
        f.setGiveaways(giveaways);
        return f;
    }

    private static User makeUser(String id) {
        User u = new User();
        u.setId(id);
        UserFlair uf = new UserFlair();
        SubredditFlair sf = new SubredditFlair();
        sf.setFlairCssClass("");
        sf.setFlairText("");
        uf.setPtrades(sf);
        u.setFlair(uf);
        return u;
    }
}
