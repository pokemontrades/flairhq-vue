package org.ptrades.flairhq.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptrades.flairhq.dto.FlairRequest;
import org.ptrades.flairhq.dto.FlairResponse;
import org.ptrades.flairhq.mapper.FlairMapper;
import org.ptrades.flairhq.repository.FlairRepository;
import org.ptrades.flairhq.repository.domain.Flair;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlairProcessorTest {

    @Mock FlairRepository flairRepository;
    @Mock FlairMapper     flairMapper;

    @InjectMocks FlairProcessor processor;

    // ── getFlairs ─────────────────────────────────────────────────────────────

    @Test
    void getFlairs_delegatesToRepository() {
        Flair flair = new Flair();
        when(flairRepository.findAll()).thenReturn(List.of(flair));
        when(flairMapper.toResponse(flair)).thenReturn(FlairResponse.builder().build());

        assertEquals(1, processor.getFlairs().size());
    }

    // ── getFlair ──────────────────────────────────────────────────────────────

    @Test
    void getFlair_found_returnsResponse() {
        Flair flair = new Flair();
        when(flairRepository.findById("f1")).thenReturn(Optional.of(flair));
        when(flairMapper.toResponse(flair)).thenReturn(FlairResponse.builder().id("f1").build());

        assertTrue(processor.getFlair("f1").isPresent());
    }

    @Test
    void getFlair_notFound_returnsEmpty() {
        when(flairRepository.findById("f1")).thenReturn(Optional.empty());

        assertTrue(processor.getFlair("f1").isEmpty());
    }

    // ── updateFlair ───────────────────────────────────────────────────────────

    @Test
    void updateFlair_notFound_throwsNotFound() {
        when(flairRepository.findById("f1")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.updateFlair("f1", new FlairRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateFlair_found_appliesUpdateAndSaves() {
        Flair flair = new Flair();
        when(flairRepository.findById("f1")).thenReturn(Optional.of(flair));
        when(flairRepository.save(flair)).thenReturn(flair);
        when(flairMapper.toResponse(flair)).thenReturn(FlairResponse.builder().build());

        processor.updateFlair("f1", new FlairRequest());

        verify(flairMapper).applyUpdate(any(), eq(flair));
        verify(flairRepository).save(flair);
    }

    // ── deleteFlair ───────────────────────────────────────────────────────────

    @Test
    void deleteFlair_notFound_throwsNotFound() {
        when(flairRepository.existsById("f1")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> processor.deleteFlair("f1"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void deleteFlair_found_deletes() {
        when(flairRepository.existsById("f1")).thenReturn(true);

        processor.deleteFlair("f1");

        verify(flairRepository).deleteById("f1");
    }
}
