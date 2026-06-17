package org.ptrades.flairhq.processor;

import java.util.List;
import java.util.Objects;

import org.ptrades.flairhq.dto.ModnoteRequest;
import org.ptrades.flairhq.dto.ModnoteResponse;
import org.ptrades.flairhq.mapper.ModnoteMapper;
import org.ptrades.flairhq.repository.ModnoteRepository;
import org.ptrades.flairhq.repository.domain.Modnote;
import org.springframework.stereotype.Service;

@Service
public class ModnoteProcessor {

    private final ModnoteRepository modnoteRepository;
    private final ModnoteMapper     modnoteMapper;

    public ModnoteProcessor(ModnoteRepository modnoteRepository, ModnoteMapper modnoteMapper) {
        this.modnoteRepository = modnoteRepository;
        this.modnoteMapper     = modnoteMapper;
    }

    public List<ModnoteResponse> getModnotes(String refUser) {
        List<Modnote> notes = (refUser != null)
                ? modnoteRepository.findByRefUser(refUser)
                : modnoteRepository.findAll();
        return notes.stream().map(modnoteMapper::toResponse).toList();
    }

    public ModnoteResponse addModnote(ModnoteRequest request, String mod) {
        Modnote saved = modnoteRepository.save(
                Objects.requireNonNull(modnoteMapper.toDocument(request, mod)));
        return modnoteMapper.toResponse(saved);
    }

    public void deleteModnote(String id) {
        ProcessorUtils.deleteOrThrow(modnoteRepository, Objects.requireNonNull(id));
    }
}
