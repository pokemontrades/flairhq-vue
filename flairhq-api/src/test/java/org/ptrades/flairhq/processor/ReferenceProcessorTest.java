package org.ptrades.flairhq.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptrades.flairhq.common.ReferenceType;
import org.ptrades.flairhq.dto.ReferenceRequest;
import org.ptrades.flairhq.dto.ReferenceResponse;
import org.ptrades.flairhq.mapper.ReferenceMapper;
import org.ptrades.flairhq.repository.ReferenceRepository;
import org.ptrades.flairhq.repository.domain.Reference;
import org.ptrades.flairhq.service.UrlNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferenceProcessorTest {

    @Mock ReferenceRepository referenceRepository;
    @Mock ReferenceMapper     referenceMapper;
    @Mock UrlNormalizer       urlNormalizer;

    @InjectMocks ReferenceProcessor processor;

    // ── add ──────────────────────────────────────────────────────────────────

    @Test
    void add_nullUrl_throwsBadRequest() {
        ReferenceRequest request = new ReferenceRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.add(request, "alice"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void add_blankUrl_throwsBadRequest() {
        ReferenceRequest request = new ReferenceRequest();
        request.setUrl("   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.add(request, "alice"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void add_wrongSubredditUrl_throwsBadRequest() {
        ReferenceRequest request = new ReferenceRequest();
        request.setUrl("https://www.reddit.com/r/pokemon/comments/abc/");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.add(request, "alice"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void add_duplicateUrl_throwsBadRequest() {
        String url = "https://www.reddit.com/r/pokemontrades/comments/abc/";
        ReferenceRequest request = makeCasualRequest(url);

        when(urlNormalizer.normalize(url)).thenReturn(url);
        when(referenceRepository.findByUserAndUrl("alice", url)).thenReturn(List.of(new Reference()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.add(request, "alice"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void add_uniqueUrl_savesAndReturnsResponse() {
        String url = "https://www.reddit.com/r/pokemontrades/comments/abc/";
        ReferenceRequest request = makeCasualRequest(url);

        when(urlNormalizer.normalize(url)).thenReturn(url);
        when(referenceRepository.findByUserAndUrl("alice", url)).thenReturn(List.of());

        Reference saved = new Reference();
        saved.setId("ref1");
        when(referenceRepository.save(any())).thenReturn(saved);

        ReferenceResponse expected = ReferenceResponse.builder().id("ref1").build();
        when(referenceMapper.toDocument(request, "alice")).thenReturn(saved);
        when(referenceMapper.toResponse(saved, true)).thenReturn(expected);

        ReferenceResponse result = processor.add(request, "alice");
        assertEquals("ref1", result.getId());
    }

    // ── edit ─────────────────────────────────────────────────────────────────

    @Test
    void edit_notFound_throwsNotFound() {
        String url = "https://www.reddit.com/r/pokemontrades/comments/abc/";
        when(referenceRepository.findById("ref1")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> processor.edit("ref1", makeCasualRequest(url), "alice"));
    }

    @Test
    void edit_wrongOwner_throwsForbidden() {
        String url = "https://www.reddit.com/r/pokemontrades/comments/abc/";
        Reference ref = makeRef("ref1", "bob", "alice");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.edit("ref1", makeCasualRequest(url), "alice"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void edit_ownRef_resetsApprovalFlags() {
        String url = "https://www.reddit.com/r/pokemontrades/comments/abc/";
        Reference ref = makeRef("ref1", "alice", "bob");
        ref.setApproved(true);
        ref.setVerified(true);
        ref.setMustFix(true);
        ref.setMustFixReason("old reason");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(urlNormalizer.normalize(url)).thenReturn(url);
        when(referenceRepository.save(ref)).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.edit("ref1", makeCasualRequest(url), "alice");

        assertFalse(ref.getApproved());
        assertFalse(ref.getVerified());
        assertFalse(ref.getMustFix());
        assertNull(ref.getMustFixReason());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_notFound_throwsNotFound() {
        when(referenceRepository.findById("ref1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.delete("ref1", "alice"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void delete_wrongOwner_throwsForbidden() {
        Reference ref = makeRef("ref1", "bob", "alice");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.delete("ref1", "alice"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void delete_rejectedRef_throwsForbidden() {
        Reference ref = makeRef("ref1", "alice", "bob");
        ref.setRejected(true);
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.delete("ref1", "alice"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void delete_ownNonRejectedRef_deletes() {
        Reference ref = makeRef("ref1", "alice", "bob");
        ref.setRejected(false);
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));

        processor.delete("ref1", "alice");

        verify(referenceRepository).deleteById("ref1");
    }

    // ── markMustFix ───────────────────────────────────────────────────────────

    @Test
    void markMustFix_notFound_throwsNotFound() {
        when(referenceRepository.findById("ref1")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> processor.markMustFix("ref1", "reason"));
    }

    @Test
    void markMustFix_withReason_setsMustFixAndReason() {
        Reference ref = makeRef("ref1", "alice", "bob");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(referenceRepository.save(ref)).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.markMustFix("ref1", "  fix your link  ");

        assertTrue(ref.getMustFix());
        assertEquals("fix your link", ref.getMustFixReason());
    }

    @Test
    void markMustFix_blankReason_setsNullReason() {
        Reference ref = makeRef("ref1", "alice", "bob");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(referenceRepository.save(ref)).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.markMustFix("ref1", "   ");

        assertTrue(ref.getMustFix());
        assertNull(ref.getMustFixReason());
    }

    // ── unapprove ─────────────────────────────────────────────────────────────

    @Test
    void unapprove_notFound_throwsNotFound() {
        when(referenceRepository.findById("ref1")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> processor.unapprove("ref1", "mod1"));
    }

    @Test
    void unapprove_modIsOwner_throwsForbidden() {
        Reference ref = makeRef("ref1", "mod1", "bob");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.unapprove("ref1", "mod1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void unapprove_modIsOwnerCaseInsensitive_throwsForbidden() {
        Reference ref = makeRef("ref1", "MOD1", "bob");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.unapprove("ref1", "mod1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void unapprove_differentMod_clearsApprovalAndVerified() {
        Reference ref = makeRef("ref1", "alice", "bob");
        ref.setApproved(true);
        ref.setVerified(true);
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(referenceRepository.save(ref)).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.unapprove("ref1", "mod1");

        assertFalse(ref.getApproved());
        assertFalse(ref.getVerified());
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Test
    void reject_notFound_throwsNotFound() {
        when(referenceRepository.findById("ref1")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> processor.reject("ref1", "bad trade"));
    }

    @Test
    void reject_setsRejectedAndClearsApproved() {
        Reference ref = makeRef("ref1", "alice", "bob");
        ref.setApproved(true);
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(referenceRepository.save(ref)).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.reject("ref1", "  bad trade  ");

        assertTrue(ref.getRejected());
        assertFalse(ref.getApproved());
        assertEquals("bad trade", ref.getRejectedReason());
    }

    @Test
    void reject_blankReason_setsNullReason() {
        Reference ref = makeRef("ref1", "alice", "bob");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(referenceRepository.save(ref)).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.reject("ref1", "");

        assertTrue(ref.getRejected());
        assertNull(ref.getRejectedReason());
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    void approve_notFound_throwsNotFound() {
        when(referenceRepository.findById("ref1")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> processor.approve("ref1", "mod1"));
    }

    @Test
    void approve_modIsUser_throwsForbidden() {
        Reference ref = makeRef("ref1", "mod1", "bob");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.approve("ref1", "mod1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void approve_modIsUser2_throwsForbidden() {
        Reference ref = makeRef("ref1", "alice", "mod1");
        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.approve("ref1", "mod1"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void approve_noReciprocalFound_setsApprovedNotVerified() {
        Reference ref = makeRef("ref1", "alice", "bob");
        ref.setType(ReferenceType.CASUAL);
        ref.setUrl("https://www.reddit.com/r/sub/comments/abc/title/xyz/");

        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(urlNormalizer.normalize(ref.getUrl())).thenReturn(ref.getUrl());
        when(referenceRepository.findByUserAndUser2("bob", "alice")).thenReturn(List.of());
        when(referenceRepository.save(ref)).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.approve("ref1", "mod1");

        assertTrue(ref.getApproved());
        assertNull(ref.getVerified()); // not set to true
        verify(referenceRepository, never()).save(argThat(r -> r != ref));
    }

    @Test
    void approve_matchingReciprocalFound_marksBothVerified() {
        String sharedUrl = "https://www.reddit.com/r/sub/comments/abc/title/xyz/";

        Reference ref = makeRef("ref1", "alice", "bob");
        ref.setType(ReferenceType.CASUAL);
        ref.setUrl(sharedUrl);

        Reference other = makeRef("ref2", "bob", "alice");
        other.setType(ReferenceType.CASUAL);
        other.setUrl(sharedUrl);

        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(urlNormalizer.normalize(sharedUrl)).thenReturn(sharedUrl);
        when(referenceRepository.findByUserAndUser2("bob", "alice")).thenReturn(List.of(other));
        when(referenceRepository.save(any())).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.approve("ref1", "mod1");

        assertTrue(ref.getVerified());
        assertTrue(other.getVerified());
        // other must also be saved
        verify(referenceRepository).save(other);
    }

    @Test
    void approve_nonVerifiableType_doesNotCheckForReciprocal() {
        Reference ref = makeRef("ref1", "alice", "bob");
        ref.setType(ReferenceType.MISC); // not in VERIFIABLE_TYPES
        ref.setUrl("https://example.com");

        when(referenceRepository.findById("ref1")).thenReturn(Optional.of(ref));
        when(referenceRepository.save(ref)).thenReturn(ref);
        when(referenceMapper.toResponse(ref, true)).thenReturn(ReferenceResponse.builder().build());

        processor.approve("ref1", "mod1");

        assertTrue(ref.getApproved());
        verify(referenceRepository, never()).findByUserAndUser2(any(), any());
    }

    // ── remove ────────────────────────────────────────────────────────────────

    @Test
    void remove_notFound_throwsNotFound() {
        when(referenceRepository.existsById("ref1")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.remove("ref1"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void remove_exists_deletes() {
        when(referenceRepository.existsById("ref1")).thenReturn(true);

        processor.remove("ref1");

        verify(referenceRepository).deleteById("ref1");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Reference makeRef(String id, String user, String user2) {
        Reference r = new Reference();
        r.setId(id);
        r.setUser(user);
        r.setUser2(user2);
        return r;
    }

    private static ReferenceRequest makeCasualRequest(String url) {
        ReferenceRequest r = new ReferenceRequest();
        r.setUrl(url);
        r.setType(ReferenceType.CASUAL);
        r.setUser2("bob");
        r.setGave("pikachu");
        r.setGot("eevee");
        return r;
    }
}
