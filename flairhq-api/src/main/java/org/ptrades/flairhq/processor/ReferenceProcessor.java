package org.ptrades.flairhq.processor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.ptrades.flairhq.common.ReferenceType;
import org.ptrades.flairhq.dto.ReferenceRequest;
import org.ptrades.flairhq.dto.ReferenceResponse;
import org.ptrades.flairhq.mapper.ReferenceMapper;
import org.ptrades.flairhq.repository.ReferenceRepository;
import org.ptrades.flairhq.repository.domain.Reference;
import org.ptrades.flairhq.service.UrlNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReferenceProcessor {

    private final ReferenceRepository referenceRepository;
    private final ReferenceMapper     referenceMapper;
    private final UrlNormalizer       urlNormalizer;

    public ReferenceProcessor(ReferenceRepository referenceRepository,
                              ReferenceMapper referenceMapper,
                              UrlNormalizer urlNormalizer) {
        this.referenceRepository = referenceRepository;
        this.referenceMapper     = referenceMapper;
        this.urlNormalizer       = urlNormalizer;
    }

    public List<ReferenceResponse> getByUser(String username, String requestingUser) {
        Set<String> approvedPartnerUrlBases = referenceRepository.findByUser2(username).stream()
                .filter(r -> Boolean.TRUE.equals(r.getApproved()))
                .map(Reference::getUrl)
                .filter(Objects::nonNull)
                .map(url -> UrlNormalizer.permalinkBase(urlNormalizer.normalize(url)))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return referenceRepository.findByUser(username).stream()
                .map(ref -> {
                    boolean reciprocalApproved = ref.getUrl() != null &&
                            approvedPartnerUrlBases.contains(
                                    UrlNormalizer.permalinkBase(urlNormalizer.normalize(ref.getUrl())));
                    return referenceMapper.toResponse(ref, ref.getUser().equals(requestingUser))
                            .toBuilder().reciprocalApproved(reciprocalApproved).build();
                })
                .toList();
    }

    /**
     * Handles adding of a reference to user's profile.
     * 
     * @param request
     * @param username
     * @return
     */
    public ReferenceResponse add(ReferenceRequest request, String username) {
        String normalizedUrl = urlNormalizer.normalize(request.getUrl());
        List<Reference> duplicates = referenceRepository.findByUserAndUrl(username, normalizedUrl);
        if (!duplicates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        request.setUrl(normalizedUrl);
        Reference saved = referenceRepository.save(Objects.requireNonNull(referenceMapper.toDocument(request, username)));
        return referenceMapper.toResponse(saved, true);
    }

    /**
     * Handles editing of an existing reference.
     * 
     * @param id
     * @param request
     * @param username
     * @return
     */
    public ReferenceResponse edit(String id, ReferenceRequest request, String username) {
        Reference ref = referenceRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ref.getUser().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        request.setUrl(urlNormalizer.normalize(request.getUrl()));
        referenceMapper.applyUpdate(request, ref);
        ref.setApproved(false);
        ref.setVerified(false);
        ref.setMustFix(false);
        ref.setMustFixReason(null);
        ref.setUpdatedAt(Instant.now());
        return referenceMapper.toResponse(referenceRepository.save(ref), true);
    }

    public void delete(@NonNull String id, String username) {
        Reference ref = referenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!ref.getUser().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (Boolean.TRUE.equals(ref.getRejected())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete a rejected reference");
        }
        referenceRepository.deleteById(id);
    }

    /**
     * Moderator function to mark a reference as must fix with an optional reason.
     * 
     * @param id
     * @param reason
     * @return
     */
    public ReferenceResponse markMustFix(@NonNull String id, String reason) {
        Reference ref = referenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ref.setMustFix(true);
        ref.setMustFixReason(reason != null && !reason.isBlank() ? reason.strip() : null);
        ref.setUpdatedAt(Instant.now());
        return referenceMapper.toResponse(referenceRepository.save(ref), true);
    }

    public void remove(@NonNull String id) {
        if (!referenceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        referenceRepository.deleteById(id);
    }

    /**
     * Moderator function to unapprove a reference.
     * 
     * @param id
     * @param moderator
     * @return
     */
    public ReferenceResponse unapprove(@NonNull String id, @NonNull String moderator) {
        Reference ref = referenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (moderator.equalsIgnoreCase(ref.getUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderators cannot unapprove references on their own profile");
        }
        ref.setApproved(false);
        ref.setVerified(false);
        ref.setUpdatedAt(Instant.now());
        return referenceMapper.toResponse(referenceRepository.save(ref), true);
    }

    /**
     * Moderator function to reject a reference with an optional reason.
     * 
     * @param id
     * @param reason
     * @return
     */
    public ReferenceResponse reject(@NonNull String id, String reason) {
        Reference ref = referenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        ref.setRejected(true);
        ref.setRejectedReason(reason != null && !reason.isBlank() ? reason.strip() : null);
        ref.setApproved(false);
        ref.setUpdatedAt(Instant.now());
        return referenceMapper.toResponse(referenceRepository.save(ref), true);
    }

    public Map<String, Long> getApprovedCountsByType(String username) {
        return Map.of(
                ReferenceType.CASUAL,      referenceRepository.countByUserAndApprovedTrueAndType(username, ReferenceType.CASUAL),
                ReferenceType.GIVEAWAY,    referenceRepository.countByUserAndApprovedTrueAndType(username, ReferenceType.GIVEAWAY),
                ReferenceType.INVOLVEMENT, referenceRepository.countByUserAndApprovedTrueAndType(username, ReferenceType.INVOLVEMENT)
        );
    }

    /**
     * Returns all references where the user is the "other" trader.
     * 
     * @param username
     * @return
     */
    public List<ReferenceResponse> getPendingReciprocal(String username) {
        Set<String> existingBases = referenceRepository.findByUser(username).stream()
                .map(Reference::getUrl)
                .filter(Objects::nonNull)
                .map(urlNormalizer::normalize)
                .map(UrlNormalizer::permalinkBase)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return referenceRepository.findByUser2(username).stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getApproved()))
                .filter(ref -> {
                    if (ref.getUrl() == null) return true;
                    String base = UrlNormalizer.permalinkBase(urlNormalizer.normalize(ref.getUrl()));
                    return base == null || !existingBases.contains(base);
                })
                .map(ref -> referenceMapper.toResponse(ref, false))
                .toList();
    }

    private static final Set<String> VERIFIABLE_TYPES = Set.of(
            ReferenceType.CASUAL, ReferenceType.SHINY, ReferenceType.EVENT, ReferenceType.BANK);

    /**
     * Marks a reference as approved. Moderators may not approve their own trades (either as the
     * submitter or the counter-party). After approval, if the reference type is verifiable and a
     * matching approved reciprocal exists at the same permalink base, both sides are marked verified.
     * 
     * @param id
     * @param moderator
     * @return
     */
    public ReferenceResponse approve(@NonNull String id, @NonNull String moderator) {
        Reference ref = referenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (moderator.equalsIgnoreCase(ref.getUser()) || moderator.equalsIgnoreCase(ref.getUser2())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderators cannot approve their own trades");
        }

        ref.setApproved(true);
        ref.setUpdatedAt(Instant.now());

        if (VERIFIABLE_TYPES.contains(ref.getType()) && ref.getUrl() != null) {
            String refBase = UrlNormalizer.permalinkBase(urlNormalizer.normalize(ref.getUrl()));
            referenceRepository.findByUserAndUser2(ref.getUser2(), ref.getUser()).stream()
                    .filter(other -> VERIFIABLE_TYPES.contains(other.getType()))
                    .filter(other -> other.getUrl() != null &&
                            refBase.equals(UrlNormalizer.permalinkBase(urlNormalizer.normalize(other.getUrl()))))
                    .findFirst()
                    .ifPresent(other -> markVerifiedPair(ref, other));
        }

        return referenceMapper.toResponse(referenceRepository.save(ref), true);
    }

    private void markVerifiedPair(Reference ref, Reference other) {
        ref.setVerified(true);
        other.setVerified(true);
        other.setUpdatedAt(Instant.now());
        referenceRepository.save(other);
    }
}
