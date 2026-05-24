package org.ptrades.flairhq.processor;

import java.util.List;

import org.ptrades.flairhq.dto.RejectionReasonRequest;
import org.ptrades.flairhq.dto.RejectionReasonResponse;
import org.ptrades.flairhq.mapper.RejectionReasonMapper;
import org.ptrades.flairhq.repository.RejectionReasonRepository;
import org.ptrades.flairhq.repository.domain.RejectionReason;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RejectionReasonProcessor {

    private final RejectionReasonRepository repository;
    private final RejectionReasonMapper     mapper;

    public RejectionReasonProcessor(RejectionReasonRepository repository, RejectionReasonMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    public List<RejectionReasonResponse> getAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    public RejectionReasonResponse create(RejectionReasonRequest req) {
        RejectionReason saved = repository.save(mapper.toDocument(req));
        return mapper.toResponse(saved);
    }

    public RejectionReasonResponse update(String id, RejectionReasonRequest req) {
        RejectionReason existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        mapper.updateDocument(req, existing);
        return mapper.toResponse(repository.save(existing));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }

}
