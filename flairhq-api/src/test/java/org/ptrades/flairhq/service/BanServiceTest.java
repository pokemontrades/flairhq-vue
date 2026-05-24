package org.ptrades.flairhq.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptrades.flairhq.dto.BanRequest;
import org.ptrades.flairhq.repository.UserRepository;
import org.ptrades.flairhq.repository.domain.SubredditFlair;
import org.ptrades.flairhq.repository.domain.UserFlair;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BanServiceTest {

    @Mock RedditApiService redditApiService;
    @Mock UsernoteService  usernoteService;
    @Mock UserRepository   userRepository;

    @InjectMocks BanService banService;

    // ── temporary ban ─────────────────────────────────────────────────────────

    @Test
    void executeBan_temporaryBan_onlyBansFromSubAndReturns() {
        BanRequest request = new BanRequest();
        request.setUsername("baduser");
        request.setBanMessage("temp ban");
        request.setBanNote("rule violation");
        request.setDuration(7); // 7-day ban

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        UserFlair flairs = makeFlairs(null, null);
        when(redditApiService.getPtradesFlairs("token", "baduser")).thenReturn(flairs);
        when(userRepository.findById("baduser")).thenReturn(Optional.empty());

        banService.executeBan(request);

        verify(redditApiService).banUser("token", "baduser", "temp ban", "rule violation", "pokemontrades", 7);
        // permanent-ban-only steps must not run
        verify(redditApiService, never()).setUserFlair(any(), any(), any(), any(), any());
        verify(usernoteService, never()).addUsernote(any(), any(), any(), any(), any(), any(), any());
    }

    // ── permanent ban ─────────────────────────────────────────────────────────

    @Test
    void executeBan_permanentBan_executesAllSixSteps() {
        BanRequest request = new BanRequest();
        request.setUsername("baduser");
        request.setBanMessage("msg");
        request.setBanNote("note");
        request.setDuration(null); // permanent
        request.setBanlistEntry("bad trader");

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");

        SubredditFlair ptrades = new SubredditFlair();
        ptrades.setFlairCssClass("pokeball");
        ptrades.setFlairText(":10: 9876-5432-1000"); // FC in flair text triggers step 4 (automod)
        UserFlair flairs = makeFlairs(ptrades, null);
        when(redditApiService.getPtradesFlairs("token", "baduser")).thenReturn(flairs);
        when(userRepository.findById("baduser")).thenReturn(Optional.empty());

        String automodWiki = buildAutomodWiki("existing|pattern", "[\"existing-fc\"]");
        when(redditApiService.getWikiPage("token", "pokemontrades", "config/automoderator"))
                .thenReturn(automodWiki);

        String banlistWiki = buildBanlistWiki();
        when(redditApiService.getWikiPage("token", "pokemontrades", "banlist"))
                .thenReturn(banlistWiki);

        banService.executeBan(request);

        // Step 1: ban from sub
        verify(redditApiService).banUser("token", "baduser", "msg", "note", "pokemontrades", null);
        // Step 2: give banned flair
        verify(redditApiService).setUserFlair(eq("token"), eq("baduser"), eq("banned"), anyString(), eq("pokemontrades"));
        // Step 3: usernote
        verify(usernoteService).addUsernote(eq("token"), eq("FlairHQ"), eq("pokemontrades"),
                eq("baduser"), contains("Banned"), eq("permban"), eq(""));
        // Step 5: banlist update
        verify(redditApiService).editWikiPage(eq("token"), eq("pokemontrades"), eq("banlist"), anyString(), any());
        // Step 6: local ban (findById is called twice — once for logged FCs, once for localBanUser)
        verify(userRepository, times(2)).findById("baduser");
    }

    // ── deleted account (403) tolerated ───────────────────────────────────────

    @Test
    void executeBan_deletedAccount_tolerates403AndSkipsBanUser() {
        BanRequest request = new BanRequest();
        request.setUsername("deleted");
        request.setDuration(null);

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(redditApiService.getPtradesFlairs("token", "deleted"))
                .thenThrow(new RedditApiException(403, "Forbidden"));
        when(userRepository.findById("deleted")).thenReturn(Optional.empty());

        String banlistWiki = buildBanlistWiki();
        when(redditApiService.getWikiPage("token", "pokemontrades", "banlist")).thenReturn(banlistWiki);

        // Should not throw — 403 is tolerated for deleted accounts
        assertDoesNotThrow(() -> banService.executeBan(request));

        verify(redditApiService, never()).banUser(any(), any(), any(), any(), any(), any());
        verify(redditApiService, never()).setUserFlair(any(), any(), any(), any(), any());
    }

    @Test
    void executeBan_nonDeletedApiError_propagates() {
        BanRequest request = new BanRequest();
        request.setUsername("user");
        request.setDuration(null);

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(redditApiService.getPtradesFlairs("token", "user"))
                .thenThrow(new RedditApiException(500, "Server Error"));

        assertThrows(RedditApiException.class, () -> banService.executeBan(request));
    }

    // ── addFCsFromFlair ───────────────────────────────────────────────────────

    @Test
    void executeBan_fcInFlairText_extractedAndAddedToAutomod() {
        BanRequest request = new BanRequest();
        request.setUsername("fcuser");
        request.setDuration(null);

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");

        SubredditFlair ptrades = new SubredditFlair();
        ptrades.setFlairCssClass("pokeball");
        ptrades.setFlairText(":10: 1234-5678-9012");
        UserFlair flairs = makeFlairs(ptrades, null);
        when(redditApiService.getPtradesFlairs("token", "fcuser")).thenReturn(flairs);
        when(userRepository.findById("fcuser")).thenReturn(Optional.empty());

        String automodWiki = buildAutomodWiki("existing", "[\"other\"]");
        when(redditApiService.getWikiPage("token", "pokemontrades", "config/automoderator"))
                .thenReturn(automodWiki);
        String banlistWiki = buildBanlistWiki();
        when(redditApiService.getWikiPage("token", "pokemontrades", "banlist")).thenReturn(banlistWiki);

        banService.executeBan(request);

        ArgumentCaptor<String> automodCaptor = ArgumentCaptor.forClass(String.class);
        verify(redditApiService).editWikiPage(eq("token"), eq("pokemontrades"),
                eq("config/automoderator"), automodCaptor.capture(), any());

        String updatedAutomod = automodCaptor.getValue();
        assertTrue(updatedAutomod.contains("1234 ?-? ?5678 ?-? ?9012"),
                "FCList1 should contain regex-formatted FC");
        assertTrue(updatedAutomod.contains("\"1234-5678-9012\""),
                "FCList2 should contain quoted FC");
    }

    @Test
    void executeBan_fcAlreadyInAutomod_notAddedAgain() {
        BanRequest request = new BanRequest();
        request.setUsername("fcuser");
        request.setDuration(null);

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");

        SubredditFlair ptrades = new SubredditFlair();
        ptrades.setFlairCssClass("pokeball");
        ptrades.setFlairText(":10: 1234-5678-9012");
        UserFlair flairs = makeFlairs(ptrades, null);
        when(redditApiService.getPtradesFlairs("token", "fcuser")).thenReturn(flairs);
        when(userRepository.findById("fcuser")).thenReturn(Optional.empty());

        // FC is already present in both lists — content won't change
        String alreadyContainsFC = buildAutomodWikiWithFC("1234-5678-9012");
        when(redditApiService.getWikiPage("token", "pokemontrades", "config/automoderator"))
                .thenReturn(alreadyContainsFC);
        String banlistWiki = buildBanlistWiki();
        when(redditApiService.getWikiPage("token", "pokemontrades", "banlist")).thenReturn(banlistWiki);

        banService.executeBan(request);

        // No change → editWikiPage must NOT be called for automoderator
        verify(redditApiService, never()).editWikiPage(eq("token"), eq("pokemontrades"),
                eq("config/automoderator"), any(), any());
    }

    // ── extractIgns ───────────────────────────────────────────────────────────

    @Test
    void executeBan_flairTextWithIGNs_usedInBanlist() {
        BanRequest request = new BanRequest();
        request.setUsername("ignuser");
        request.setDuration(null);

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");

        SubredditFlair ptrades = new SubredditFlair();
        ptrades.setFlairText(":10: text || SomeIGN");
        ptrades.setFlairCssClass("pokeball");
        UserFlair flairs = makeFlairs(ptrades, null);
        when(redditApiService.getPtradesFlairs("token", "ignuser")).thenReturn(flairs);
        when(userRepository.findById("ignuser")).thenReturn(Optional.empty());

        String banlistWiki = buildBanlistWiki();
        when(redditApiService.getWikiPage("token", "pokemontrades", "banlist")).thenReturn(banlistWiki);

        banService.executeBan(request);

        ArgumentCaptor<String> banlistCaptor = ArgumentCaptor.forClass(String.class);
        verify(redditApiService).editWikiPage(eq("token"), eq("pokemontrades"),
                eq("banlist"), banlistCaptor.capture(), any());

        assertTrue(banlistCaptor.getValue().contains("SomeIGN"),
                "Banlist entry should include extracted IGN");
    }

    // ── updateBanlist ─────────────────────────────────────────────────────────

    @Test
    void executeBan_banlistMarkersMissing_throwsRedditApiException() {
        BanRequest request = new BanRequest();
        request.setUsername("user");
        request.setDuration(null);

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");
        when(redditApiService.getPtradesFlairs("token", "user")).thenReturn(makeFlairs(null, null));
        when(userRepository.findById("user")).thenReturn(Optional.empty());
        when(redditApiService.getWikiPage("token", "pokemontrades", "banlist"))
                .thenReturn("no markers here");

        assertThrows(RedditApiException.class, () -> banService.executeBan(request));
    }

    // ── giveBannedUserFlair ────────────────────────────────────────────────────

    @Test
    void executeBan_bannedUserFlairText_stripsEmojiPrefixAndPrepends() {
        BanRequest request = new BanRequest();
        request.setUsername("user");
        request.setDuration(null);

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");

        SubredditFlair ptrades = new SubredditFlair();
        ptrades.setFlairCssClass("ultraball");
        ptrades.setFlairText(":40: Alice");
        UserFlair flairs = makeFlairs(ptrades, null);
        when(redditApiService.getPtradesFlairs("token", "user")).thenReturn(flairs);
        when(userRepository.findById("user")).thenReturn(Optional.empty());

        String banlistWiki = buildBanlistWiki();
        when(redditApiService.getWikiPage("token", "pokemontrades", "banlist")).thenReturn(banlistWiki);

        banService.executeBan(request);

        ArgumentCaptor<String> flairTextCaptor = ArgumentCaptor.forClass(String.class);
        verify(redditApiService).setUserFlair(eq("token"), eq("user"), eq("banned"),
                flairTextCaptor.capture(), eq("pokemontrades"));

        assertTrue(flairTextCaptor.getValue().startsWith("BANNED USER"),
                "Banned flair text should start with 'BANNED USER'");
        assertTrue(flairTextCaptor.getValue().contains("Alice"),
                "Banned flair text should retain original text");
    }

    @Test
    void executeBan_longFlairText_truncatedTo64Chars() {
        BanRequest request = new BanRequest();
        request.setUsername("user");
        request.setDuration(null);

        when(redditApiService.getAdminRefreshToken()).thenReturn("token");

        SubredditFlair ptrades = new SubredditFlair();
        ptrades.setFlairCssClass("pokeball");
        ptrades.setFlairText("A".repeat(80));
        UserFlair flairs = makeFlairs(ptrades, null);
        when(redditApiService.getPtradesFlairs("token", "user")).thenReturn(flairs);
        when(userRepository.findById("user")).thenReturn(Optional.empty());

        String banlistWiki = buildBanlistWiki();
        when(redditApiService.getWikiPage("token", "pokemontrades", "banlist")).thenReturn(banlistWiki);

        banService.executeBan(request);

        ArgumentCaptor<String> flairTextCaptor = ArgumentCaptor.forClass(String.class);
        verify(redditApiService).setUserFlair(eq("token"), eq("user"), eq("banned"),
                flairTextCaptor.capture(), eq("pokemontrades"));

        assertTrue(flairTextCaptor.getValue().length() <= 64,
                "Banned flair text must not exceed 64 characters");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static UserFlair makeFlairs(SubredditFlair ptrades, SubredditFlair pvp) {
        UserFlair uf = new UserFlair();
        uf.setPtrades(ptrades);
        return uf;
    }

    private static String buildAutomodWiki(String fcList1Content, String fcList2Content) {
        return String.join("\n",
                "# automoderator config",
                "#FCList1",
                "  \"" + fcList1Content + "\"",
                "# more config",
                "#FCList2",
                "  [" + fcList2Content + "]",
                "# end"
        );
    }

    private static String buildAutomodWikiWithFC(String fc) {
        String formatted = fc.replace("-", " ?-? ?");
        return String.join("\n",
                "# automoderator config",
                "#FCList1",
                "  \"existing|" + formatted + "\"",
                "# more config",
                "#FCList2",
                "  [\"" + fc + "\"]",
                "# end"
        );
    }

    private static String buildBanlistWiki() {
        return String.join("\n",
                "# banlist",
                "[//]:# (BEGIN BANLIST)",
                "Username | FCs | Entry | IGNs | Note",
                "---|---|---|---|---",
                "existing | 0000-0000-0000 | old entry |  | ",
                "[//]:# (END BANLIST)",
                "# footer"
        );
    }
}
