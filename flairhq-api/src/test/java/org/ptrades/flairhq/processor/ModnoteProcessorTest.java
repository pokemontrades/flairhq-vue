package org.ptrades.flairhq.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptrades.flairhq.dto.ModnoteRequest;
import org.ptrades.flairhq.dto.ModnoteResponse;
import org.ptrades.flairhq.mapper.ModnoteMapper;
import org.ptrades.flairhq.repository.ModnoteRepository;
import org.ptrades.flairhq.repository.domain.Modnote;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModnoteProcessorTest {

    @Mock ModnoteRepository modnoteRepository;
    @Mock ModnoteMapper     modnoteMapper;

    @InjectMocks ModnoteProcessor processor;

    // ── getModnotes ───────────────────────────────────────────────────────────

    @Test
    void getModnotes_nullRefUser_returnsAll() {
        Modnote note = new Modnote();
        when(modnoteRepository.findAll()).thenReturn(List.of(note));
        when(modnoteMapper.toResponse(note)).thenReturn(ModnoteResponse.builder().build());

        assertEquals(1, processor.getModnotes(null).size());
        verify(modnoteRepository).findAll();
        verify(modnoteRepository, never()).findByRefUser(any());
    }

    @Test
    void getModnotes_withRefUser_filtersResults() {
        Modnote note = new Modnote();
        when(modnoteRepository.findByRefUser("alice")).thenReturn(List.of(note));
        when(modnoteMapper.toResponse(note)).thenReturn(ModnoteResponse.builder().build());

        assertEquals(1, processor.getModnotes("alice").size());
        verify(modnoteRepository).findByRefUser("alice");
        verify(modnoteRepository, never()).findAll();
    }

    // ── addModnote ────────────────────────────────────────────────────────────

    @Test
    void addModnote_savesAndReturnsResponse() {
        ModnoteRequest request = new ModnoteRequest();
        Modnote saved = new Modnote();
        saved.setId("note1");
        when(modnoteMapper.toDocument(request, "mod1")).thenReturn(saved);
        when(modnoteRepository.save(saved)).thenReturn(saved);
        when(modnoteMapper.toResponse(saved)).thenReturn(ModnoteResponse.builder().id("note1").build());

        ModnoteResponse result = processor.addModnote(request, "mod1");

        assertEquals("note1", result.getId());
    }

    // ── deleteModnote ─────────────────────────────────────────────────────────

    @Test
    void deleteModnote_notFound_throwsNotFound() {
        when(modnoteRepository.existsById("note1")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.deleteModnote("note1"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void deleteModnote_found_deletes() {
        when(modnoteRepository.existsById("note1")).thenReturn(true);

        processor.deleteModnote("note1");

        verify(modnoteRepository).deleteById("note1");
    }
}
