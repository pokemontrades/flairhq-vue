package org.ptrades.flairhq.processor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.ptrades.flairhq.common.ReferenceType;
import org.ptrades.flairhq.dto.PagedResponse;
import org.ptrades.flairhq.dto.ReferenceRequest;
import org.ptrades.flairhq.dto.ReferenceResponse;
import org.ptrades.flairhq.mapper.ReferenceMapper;
import org.ptrades.flairhq.repository.ReferenceRepository;
import org.ptrades.flairhq.repository.domain.Reference;
import org.ptrades.flairhq.service.UrlNormalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReferenceProcessor {

    private static final Pattern SUBREDDIT_PATTERN =
            Pattern.compile("^https?://(www\\.|old\\.)?reddit\\.com/r/pokemontrades/", Pattern.CASE_INSENSITIVE);

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

    /** Loads a reference or throws 404. */
    private Reference findRefOr404(@NonNull String id) {
        return referenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Persists the reference and maps it to a response (with private fields included). */
    private ReferenceResponse saveAndRespond(Reference ref) {
        ref.setUpdatedAt(Instant.now());
        return referenceMapper.toResponse(referenceRepository.save(ref), true);
    }

    /** Canonical permalink comparison key for a raw URL, or null if there is none. */
    private String normalizedBase(String url) {
        return url != null ? UrlNormalizer.permalinkBase(urlNormalizer.normalize(url)) : null;
    }

    /** Permalink bases of the given references' URLs (nulls dropped). */
    private Set<String> permalinkBases(List<Reference> refs) {
        return refs.stream()
                .map(Reference::getUrl)
                .map(this::normalizedBase)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Returns one page of a user's references. Each item is flagged {@code reciprocalApproved}
     * when the partner has an approved reference for the same trade (matched by permalink base),
     * which the UI uses to badge references awaiting this side's mod approval.
     */
    public PagedResponse<ReferenceResponse> getByUser(String username, String requestingUser, int page, int size) {
        Set<String> approvedPartnerUrlBases = permalinkBases(
                referenceRepository.findByUser2(username).stream()
                        .filter(r -> Boolean.TRUE.equals(r.getApproved()))
                        .toList());

        Page<Reference> refPage = referenceRepository.findByUser(
                username, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<ReferenceResponse> items = refPage.getContent().stream()
                .map(ref -> {
                    boolean reciprocalApproved = ref.getUrl() != null &&
                            approvedPartnerUrlBases.contains(normalizedBase(ref.getUrl()));
                    return referenceMapper.toResponse(ref, ref.getUser().equals(requestingUser))
                            .toBuilder().reciprocalApproved(reciprocalApproved).build();
                })
                .toList();

        return new PagedResponse<>(items, refPage.getTotalElements(), page, size, refPage.getTotalPages());
    }

    /**
     * Handles adding of a reference to the user's profile. Rejects duplicates both by exact
     * normalized URL and by permalink base (same trade linked via post vs. comment URL).
     *
     * @param request
     * @param username
     * @return
     */
    public ReferenceResponse add(ReferenceRequest request, String username) {
        validateUrl(request.getUrl());
        validateRequest(request);
        String normalizedUrl = urlNormalizer.normalize(request.getUrl());
        List<Reference> duplicates = referenceRepository.findByUserAndUrl(username, normalizedUrl);
        if (!duplicates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reference with that URL already exists.");
        }
        String newBase = UrlNormalizer.permalinkBase(normalizedUrl);
        if (newBase != null && permalinkBases(referenceRepository.findByUser(username)).contains(newBase)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already have a reference for this trade.");
        }
        request.setUrl(normalizedUrl);
        Reference saved = referenceRepository.save(Objects.requireNonNull(referenceMapper.toDocument(request, username)));
        return referenceMapper.toResponse(saved, true);
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is required");
        }
        if (!SUBREDDIT_PATTERN.matcher(url).find()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL must be from the pokemontrades subreddit");
        }
    }

    /**
     * Validates the type-dependent required fields. Mirrors the visibility rules in the
     * frontend's useReferenceForm composable: giveaways need a description and count,
     * involvement/misc need a partner and description, everything else partner + gave/got.
     */
    private void validateRequest(ReferenceRequest request) {
        String type = request.getType();
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type is required");
        }

        boolean isGiveaway = ReferenceType.GIVEAWAY.equals(type);
        boolean isDescType  = ReferenceType.INVOLVEMENT.equals(type) || ReferenceType.MISC.equals(type);
        boolean showPartner = !isGiveaway;
        boolean showGaveGot = !isGiveaway && !isDescType;
        boolean showDesc    = isGiveaway || isDescType;
        boolean showNumber  = isGiveaway;

        if (showPartner && (request.getUser2() == null || request.getUser2().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trading partner is required");
        }
        if (showGaveGot) {
            if (request.getGave() == null || request.getGave().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\"Gave\" is required");
            }
            if (request.getGot() == null || request.getGot().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\"Got\" is required");
            }
        }
        if (showDesc && (request.getDescription() == null || request.getDescription().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Description is required");
        }
        if (showNumber && (request.getNumber() == null || request.getNumber() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Number given must be at least 1");
        }
    }

    /**
     * Handles editing of an existing reference by its owner. Any edit clears approval/verification
     * (the reference must be re-reviewed); edits to substantive trade fields also clear must-fix,
     * since the user has presumably addressed the issue.
     */
    public ReferenceResponse edit(String id, ReferenceRequest request, String username) {
        validateUrl(request.getUrl());
        validateRequest(request);
        Reference ref = findRefOr404(Objects.requireNonNull(id));
        if (!ref.getUser().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        request.setUrl(urlNormalizer.normalize(request.getUrl()));
        int reqNumber = request.getNumber() != null ? request.getNumber() : 0;
        int refNumber = ref.getNumber()     != null ? ref.getNumber()     : 0;
        boolean substantiveFieldChanged = !Objects.equals(request.getUrl(),         ref.getUrl())
                                       || !Objects.equals(request.getUser2(),       ref.getUser2())
                                       || !Objects.equals(request.getGave(),        ref.getGave())
                                       || !Objects.equals(request.getGot(),         ref.getGot())
                                       || !Objects.equals(request.getDescription(), ref.getDescription())
                                       || !Objects.equals(request.getType(),        ref.getType())
                                       || reqNumber != refNumber;
        referenceMapper.applyUpdate(request, ref);
        ref.setApproved(false);
        ref.setVerified(false);
        if (substantiveFieldChanged) {
            ref.setMustFix(false);
            ref.setMustFixReason(null);
        }
        return saveAndRespond(ref);
    }

    public void delete(@NonNull String id, String username) {
        Reference ref = findRefOr404(id);
        if (!ref.getUser().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (Boolean.TRUE.equals(ref.getRejected())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete a rejected reference");
        }
        referenceRepository.deleteById(id);
    }

    /**
     * Moderator function to set a reference back to pending status, removing any approval, rejection, or must-fix flags.
     *
     * @param id
     * @return
     */
    public ReferenceResponse setPending(@NonNull String id) {
        Reference ref = findRefOr404(id);
        ref.setMustFix(false);
        ref.setMustFixReason(null);
        ref.setRejected(false);
        ref.setRejectedReason(null);
        ref.setApproved(false);
        ref.setVerified(false);
        return saveAndRespond(ref);
    }

    /**
     * Moderator function to mark a reference as must fix with an optional reason.
     *
     * @param id
     * @param reason
     * @return
     */
    public ReferenceResponse markMustFix(@NonNull String id, String reason) {
        Reference ref = findRefOr404(id);
        ref.setMustFix(true);
        ref.setMustFixReason(reason != null && !reason.isBlank() ? reason.strip() : null);
        return saveAndRespond(ref);
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
        Reference ref = findRefOr404(id);
        if (moderator.equalsIgnoreCase(ref.getUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderators cannot unapprove references on their own profile");
        }
        ref.setApproved(false);
        ref.setVerified(false);
        return saveAndRespond(ref);
    }

    /**
     * Moderator function to reject a reference with an optional reason.
     *
     * @param id
     * @param reason
     * @return
     */
    public ReferenceResponse reject(@NonNull String id, String reason) {
        Reference ref = findRefOr404(id);
        ref.setRejected(true);
        ref.setRejectedReason(reason != null && !reason.isBlank() ? reason.strip() : null);
        ref.setApproved(false);
        return saveAndRespond(ref);
    }

    public Map<String, Long> getApprovedCountsByType(String username) {
        return Map.of(
                ReferenceType.CASUAL,      referenceRepository.countByUserAndApprovedTrueAndType(username, ReferenceType.CASUAL),
                ReferenceType.GIVEAWAY,    referenceRepository.countByUserAndApprovedTrueAndType(username, ReferenceType.GIVEAWAY),
                ReferenceType.INVOLVEMENT, referenceRepository.countByUserAndApprovedTrueAndType(username, ReferenceType.INVOLVEMENT)
        );
    }

    /**
     * Returns approved references naming the user as the "other" trader for which the user
     * has not yet logged their own side (matched by permalink base) — i.e. the trades the UI
     * prompts them to add a reciprocal reference for.
     *
     * @param username
     * @return
     */
    public List<ReferenceResponse> getPendingReciprocal(String username) {
        Set<String> existingBases = permalinkBases(referenceRepository.findByUser(username));

        return referenceRepository.findByUser2(username).stream()
                .filter(ref -> Boolean.TRUE.equals(ref.getApproved()))
                .filter(ref -> {
                    String base = normalizedBase(ref.getUrl());
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
        Reference ref = findRefOr404(id);

        if (moderator.equalsIgnoreCase(ref.getUser()) || moderator.equalsIgnoreCase(ref.getUser2())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Moderators cannot approve their own trades");
        }

        ref.setApproved(true);
        ref.setMustFix(false);
        ref.setMustFixReason(null);

        String refBase = normalizedBase(ref.getUrl());
        if (VERIFIABLE_TYPES.contains(ref.getType()) && refBase != null) {
            referenceRepository.findByUserAndUser2(ref.getUser2(), ref.getUser()).stream()
                    .filter(other -> VERIFIABLE_TYPES.contains(other.getType()))
                    .filter(other -> refBase.equals(normalizedBase(other.getUrl())))
                    .findFirst()
                    .ifPresent(other -> markVerifiedPair(ref, other));
        }

        return saveAndRespond(ref);
    }

    private void markVerifiedPair(Reference ref, Reference other) {
        ref.setVerified(true);
        other.setVerified(true);
        other.setUpdatedAt(Instant.now());
        referenceRepository.save(other);
    }
}
