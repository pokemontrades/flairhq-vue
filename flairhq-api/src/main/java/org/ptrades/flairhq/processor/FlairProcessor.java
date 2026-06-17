package org.ptrades.flairhq.processor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.ptrades.flairhq.dto.FlairRequest;
import org.ptrades.flairhq.dto.FlairResponse;
import org.ptrades.flairhq.mapper.FlairMapper;
import org.ptrades.flairhq.repository.FlairRepository;
import org.ptrades.flairhq.repository.domain.Flair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FlairProcessor {

    private final FlairRepository flairRepository;
    private final FlairMapper     flairMapper;

    public FlairProcessor(FlairRepository flairRepository, FlairMapper flairMapper) {
        this.flairRepository = flairRepository;
        this.flairMapper     = flairMapper;
    }

    public List<FlairResponse> getFlairs() {
        return flairRepository.findAll().stream().map(flairMapper::toResponse).toList();
    }

    public Optional<FlairResponse> getFlair(String id) {
        return flairRepository.findById(Objects.requireNonNull(id))
                .map(flairMapper::toResponse);
    }

    public FlairResponse createFlair(FlairRequest request) {
        return flairMapper.toResponse(
                flairRepository.save(Objects.requireNonNull(flairMapper.toDocument(request))));
    }

    public FlairResponse updateFlair(String id, FlairRequest request) {
        Flair flair = flairRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        flairMapper.applyUpdate(request, flair);
        return flairMapper.toResponse(flairRepository.save(Objects.requireNonNull(flair)));
    }

    public void deleteFlair(String id) {
        ProcessorUtils.deleteOrThrow(flairRepository, Objects.requireNonNull(id));
    }
}
