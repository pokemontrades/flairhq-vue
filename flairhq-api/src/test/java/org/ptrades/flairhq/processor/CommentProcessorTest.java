package org.ptrades.flairhq.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptrades.flairhq.dto.CommentResponse;
import org.ptrades.flairhq.mapper.CommentMapper;
import org.ptrades.flairhq.repository.CommentRepository;
import org.ptrades.flairhq.repository.domain.Comment;
import org.ptrades.flairhq.service.ModSecurityService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentProcessorTest {

    @Mock CommentRepository  commentRepository;
    @Mock CommentMapper      commentMapper;
    @Mock ModSecurityService modSecurity;

    @InjectMocks CommentProcessor processor;

    // ── getComments ───────────────────────────────────────────────────────────

    @Test
    void getComments_nullUser_returnsAll() {
        Comment c = new Comment();
        when(commentRepository.findAll()).thenReturn(List.of(c));
        when(commentMapper.toResponse(c)).thenReturn(CommentResponse.builder().build());

        assertEquals(1, processor.getComments(null).size());
        verify(commentRepository).findAll();
        verify(commentRepository, never()).findByUser(any());
    }

    @Test
    void getComments_withUser_returnsFilteredByUser() {
        Comment c = new Comment();
        when(commentRepository.findByUser("alice")).thenReturn(List.of(c));
        when(commentMapper.toResponse(c)).thenReturn(CommentResponse.builder().build());

        assertEquals(1, processor.getComments("alice").size());
        verify(commentRepository).findByUser("alice");
        verify(commentRepository, never()).findAll();
    }

    // ── deleteComment ─────────────────────────────────────────────────────────

    @Test
    void deleteComment_notFound_throwsNotFound() {
        when(commentRepository.findById("c1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.deleteComment("c1", "alice", mock(Authentication.class)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void deleteComment_isUser2_deletesWithoutModCheck() {
        Comment comment = makeComment("c1", "bob", "alice"); // alice is user2
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment));

        processor.deleteComment("c1", "alice", mock(Authentication.class));

        verify(commentRepository).deleteById("c1");
        verifyNoInteractions(modSecurity);
    }

    @Test
    void deleteComment_notUser2ButHasModPermission_deletes() {
        Comment comment = makeComment("c1", "alice", "bob"); // charlie is neither
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment));
        Authentication auth = mock(Authentication.class);
        when(modSecurity.hasPermission(auth, "flair")).thenReturn(true);

        processor.deleteComment("c1", "charlie", auth);

        verify(commentRepository).deleteById("c1");
    }

    @Test
    void deleteComment_notUser2AndNoModPermission_throwsForbidden() {
        Comment comment = makeComment("c1", "alice", "bob"); // charlie has no claim
        when(commentRepository.findById("c1")).thenReturn(Optional.of(comment));
        Authentication auth = mock(Authentication.class);
        when(modSecurity.hasPermission(auth, "flair")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.deleteComment("c1", "charlie", auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(commentRepository, never()).deleteById(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Comment makeComment(String id, String user, String user2) {
        Comment c = new Comment();
        c.setId(id);
        c.setUser(user);
        c.setUser2(user2);
        return c;
    }
}
