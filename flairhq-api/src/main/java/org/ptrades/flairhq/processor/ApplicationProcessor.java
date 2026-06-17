package org.ptrades.flairhq.processor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ptrades.flairhq.common.ReferenceType;
import org.ptrades.flairhq.dto.ApplicationRequest;
import org.ptrades.flairhq.dto.ApplicationResponse;
import org.ptrades.flairhq.dto.UserApplicationRequest;
import org.ptrades.flairhq.mapper.ApplicationMapper;
import org.ptrades.flairhq.repository.ApplicationRepository;
import org.ptrades.flairhq.repository.FlairRepository;
import org.ptrades.flairhq.repository.ReferenceRepository;
import org.ptrades.flairhq.repository.UserRepository;
import org.ptrades.flairhq.repository.domain.Application;
import org.ptrades.flairhq.repository.domain.Flair;
import org.ptrades.flairhq.repository.domain.Reference;
import org.ptrades.flairhq.repository.domain.SubredditFlair;
import org.ptrades.flairhq.repository.domain.User;
import org.ptrades.flairhq.repository.domain.UserFlair;
import org.ptrades.flairhq.service.FlairService;
import org.ptrades.flairhq.service.RedditApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationProcessor {

    private static final Logger log = LoggerFactory.getLogger(ApplicationProcessor.class);

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper     applicationMapper;
    private final UserRepository        userRepository;
    private final FlairRepository       flairRepository;
    private final ReferenceRepository   referenceRepository;
    private final FlairService          flairService;
    private final RedditApiService      redditApiService;

    public ApplicationProcessor(ApplicationRepository applicationRepository,
                                ApplicationMapper applicationMapper,
                                UserRepository userRepository,
                                FlairRepository flairRepository,
                                ReferenceRepository referenceRepository,
                                FlairService flairService,
                                RedditApiService redditApiService) {
        this.applicationRepository = applicationRepository;
        this.applicationMapper     = applicationMapper;
        this.userRepository        = userRepository;
        this.flairRepository       = flairRepository;
        this.referenceRepository   = referenceRepository;
        this.flairService          = flairService;
        this.redditApiService      = redditApiService;
    }

    /**
     * Fetches all pending flair applications with calculated approved trade counts and required thresholds for each application.
     * 
     * @return
     */
    public List<ApplicationResponse> getApplications() {
        return applicationRepository.findAll().stream()
                .map(app -> {
                    int approvedTrades = (int) referenceRepository.countByUserAndApprovedTrue(app.getUser());
                    Flair flairConfig  = flairRepository.findByNameAndSub(app.getFlair(), app.getSub());
                    int requiredTrades = flairConfig != null ? flairConfig.getTrades() : 0;
                    return ApplicationResponse.builder()
                            .id(app.getId())
                            .user(app.getUser())
                            .flair(app.getFlair())
                            .sub(app.getSub())
                            .approvedTrades(approvedTrades)
                            .requiredTrades(requiredTrades)
                            .createdAt(app.getCreatedAt())
                            .updatedAt(app.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    /**
     * Fetches Flair Application for the user.
     * 
     * @param username
     * @return
     */
    public List<ApplicationResponse> getApplicationsForUser(String username) {
        return applicationRepository.findByUser(username).stream()
                .map(applicationMapper::toResponse)
                .toList();
    }

    /**
     * User flair application flow. Threshold check counts all submitted trades regardless of approval
     * status — a user only needs to have submitted enough trades to apply. Mod approval requires that
     * same count to be fully approved.
     *
     * @param request
     * @param username
     * @return
     */
    public ApplicationResponse applyFlairForSelf(UserApplicationRequest request, String username) {
        Application existing = applicationRepository.findByUserAndFlair(username, request.getFlair());
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        checkThresholds(username, request.getFlair(), "pokemontrades", false);
        ApplicationRequest appRequest = new ApplicationRequest();
        appRequest.setUser(username);
        appRequest.setFlair(request.getFlair());
        appRequest.setSub("pokemontrades");
        Application saved = applicationRepository.save(
                Objects.requireNonNull(applicationMapper.toDocument(appRequest)));
        return applicationMapper.toResponse(saved);
    }

    /**
     * Approves a pending flair application: validates trade thresholds, applies the new CSS class and
     * flair text on Reddit, sends an approval PM to the user (including any must-fix or rejected refs),
     * updates the user record in the DB, and deletes the application.
     * 
     * @param id
     */
    public void approveApplication(String id) {
        Application app = applicationRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        checkThresholds(app.getUser(), app.getFlair(), app.getSub(), true);
        checkNoHigherFlairPending(id, app);

        User user = userRepository.findById(Objects.requireNonNull(app.getUser()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        UserFlair      userFlair = user.getOrInitFlair();
        SubredditFlair ptrades   = userFlair.getOrInitPtrades();
        String currentCss        = ptrades.getFlairCssClass() != null ? ptrades.getFlairCssClass() : "";
        String newCss            = flairService.makeNewCssClass(currentCss, app.getFlair());

        String rawText   = ptrades.getFlairText() != null
                ? ptrades.getFlairText().replaceAll(":[a-zA-Z0-9_-]*:", "") : "";
        String flairText    = flairService.makeNewFlairText(newCss, rawText);
        boolean flairTrimmed = flairText.length() > 64;
        if (flairTrimmed) {
            flairText = flairService.makeNewFlairText(newCss, rawText.substring(0, Math.min(rawText.length(), 55)));
        }

        String adminToken = redditApiService.getAdminRefreshToken();
        redditApiService.setUserFlair(adminToken, user.getId(), newCss, flairText, app.getSub());

        redditApiService.sendPrivateMessage(adminToken, "FlairHQ Notification",
                buildApprovalMessage(app, flairTrimmed), user.getId());

        ptrades.setFlairCssClass(newCss);
        ptrades.setFlairText(flairText);
        userFlair.setPtrades(ptrades);
        user.setFlair(userFlair);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        applicationRepository.deleteById(id);

        // Soft-deny remaining apps of the same type: trade flairs don't cancel involvement apps and vice versa.
        Flair approvedConfig = flairRepository.findByNameAndSub(app.getFlair(), app.getSub());
        boolean approvedIsInvolvement = approvedConfig == null || approvedConfig.getTrades() == 0;
        List<Application> remaining = applicationRepository.findByUser(app.getUser()).stream()
                .filter(a -> {
                    Flair otherConfig = flairRepository.findByNameAndSub(a.getFlair(), a.getSub());
                    boolean otherIsInvolvement = otherConfig == null || otherConfig.getTrades() == 0;
                    return approvedIsInvolvement == otherIsInvolvement;
                })
                .toList();
        if (!remaining.isEmpty()) {
            log.info("Soft-denying {} remaining application(s) for user='{}'", remaining.size(), app.getUser());
            applicationRepository.deleteAll(remaining);
        }
    }

    /**
     * Assembles the approval PM body, including an addendum for Poké Ball flair and a summary of any must-fix
     * or rejected references with reasons, to encourage users to review their references and re-apply if needed.
     * 
     * @param app
     * @param flairTrimmed
     * @return
     */
    private String buildApprovalMessage(Application app, boolean flairTrimmed) {
        String pmBody = "Your application for " + flairService.formattedName(app.getFlair())
                + " flair on /r/" + app.getSub() + " has been approved.";
        if (flairTrimmed) {
            pmBody += " However, the length of your flair text was too long, so it was trimmed automatically."
                    + " Please visit FlairHQ to update your flair text.";
        }
        if ("pokeball".equals(app.getFlair())) {
            pmBody += POKEBALL_APPROVAL_ADDENDUM;
        }
        RefFlags flags = loadRefFlags(app.getUser());
        if (!flags.mustFix().isEmpty() || !flags.rejected().isEmpty()) {
            StringBuilder builder = new StringBuilder(pmBody);
            builder.append("\n\nPlease review the following trades before your next flair application:");
            if (!flags.mustFix().isEmpty()) {
                appendMustFixBlock(builder, flags.mustFix());
            }
            if (!flags.rejected().isEmpty()) {
                builder.append("\n**The following references have been rejected:**");
                appendRejectedBlock(builder, flags.rejected());
            }
            pmBody = builder.toString();
        }
        return pmBody;
    }

    /**
     * Denies a pending flair application and deletes from the DB
     * 
     * @param id
     * @param note
     */
    public void denyApplication(String id, String note) {
        Application app = applicationRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            String adminToken = redditApiService.getAdminRefreshToken();
            redditApiService.sendPrivateMessage(adminToken, "FlairHQ Notification",
                    buildDenialMessage(app, note), app.getUser());
        } catch (Exception e) {
            log.warn("Failed to send denial PM to user='{}': {}", app.getUser(), e.getMessage());
        }
        applicationRepository.deleteById(id);
    }

    private static final String POKEBALL_APPROVAL_ADDENDUM =
            "\n\nNow that you have Poké Ball flair (congrats!), please make sure to review the"
            + " [r/pokemontrades](https://www.reddit.com/r/pokemontrades) rules in full, with close attention"
            + " to Rules 1-3. There are [specific details](https://www.reddit.com/r/pokemontrades/wiki/rules"
            + "#wiki_rule_3_-_full_details_must_be_posted_for_valuable_pok.E9mon.) that **always need to be"
            + " provided as soon as you offer for trade or tradeback any shiny or event Pokémon regardless of"
            + " your flair level.** This requirement helps all traders be fully informed about the origin and"
            + " legitimacy status of the Pokémon they may negotiate a trade for in the interest of ensuring"
            + " legitimate and trustworthy trades."
            + "\n\nPlease report offers you see, or receive, that are missing the required details (OT, ID,"
            + " Origin) to help moderators be aware of potential hacked, cloned, or other Pokémon that should"
            + " not be traded on this subreddit."
            + "\n\nAlso, please remain mindful of the Trade flair level of potential traders (Rule 10), as"
            + " even though you now have Poké Ball flair, you should not conduct trades or tradebacks involving"
            + " shiny or event Pokémon with users who lack it."
            + "\n\n---"
            + "\n\nIf you have any additional questions about Trade Flair, don't hesitate to reply to this"
            + " modmail. If you have any other rule-related questions,"
            + " [message the /r/pokemontrades moderation team](https://www.reddit.com/message/compose"
            + "?to=%2Fr%2Fpokemontrades)."
            + "\n\nMany thanks & happy trading!";

    private static final String[][] CATEGORY_ORDER = {
        { ReferenceType.CASUAL,      "Casual Trades" },
        { ReferenceType.SHINY,       "Shiny Trades" },
        { ReferenceType.EVENT,       "Event Trades" },
        { ReferenceType.BANK,        "Bank Services" },
        { ReferenceType.GIVEAWAY,    "Giveaway / Contest" },
        { ReferenceType.INVOLVEMENT, "Free Tradeback / Free Redemption" },
        { ReferenceType.MISC,        "Miscellaneous" },
    };

    /**
     * Assembles the denial PM body, listing must-fix and rejected references grouped by category
     * and appending any optional moderator note at the end.
     * 
     * @param app
     * @param note
     * @return
     */
    private String buildDenialMessage(Application app, String note) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hello ").append(app.getUser()).append(",\n\n");
        builder.append("Unfortunately your ").append(flairService.formattedName(app.getFlair()))
          .append(" application has been denied due to not meeting the threshold for approved trades")
          .append(" due to the following:\n");

        RefFlags flags = loadRefFlags(app.getUser());
        if (!flags.mustFix().isEmpty()) {
            appendMustFixBlock(builder, flags.mustFix());
        }
        if (!flags.rejected().isEmpty()) {
            builder.append("\n**The following references have been rejected:**");
            appendRejectedBlock(builder, flags.rejected());
        }

        if (note != null && !note.isBlank()) {
            builder.append("\n**Additional Moderator Notes:** ").append(note.strip());
        }

        return builder.toString();
    }

    private void appendMustFixBlock(StringBuilder sb, List<Reference> mustFixRefs) {
        appendRefBlock(sb, mustFixRefs, Reference::getMustFixReason);
    }

    private void appendRejectedBlock(StringBuilder sb, List<Reference> rejectedRefs) {
        appendRefBlock(sb, rejectedRefs, Reference::getRejectedReason);
    }

    /**
     * Appends a formatted markdown block of references to {@code sb}, grouped by category in
     * {@link #CATEGORY_ORDER} order. {@code reasonFn} extracts the per-ref reason string so this
     * method can serve both must-fix and rejected sections without duplication.
     */
    private void appendRefBlock(StringBuilder sb, List<Reference> refs, Function<Reference, String> reasonFn) {
        Map<String, List<Reference>> byType = refs.stream()
                .collect(Collectors.groupingBy(Reference::getType));

        for (String[] category : CATEGORY_ORDER) {
            List<Reference> group = byType.get(category[0]);
            if (group == null || group.isEmpty()) continue;

            sb.append("\n**").append(category[1]).append(":**\n\n");
            for (Reference ref : group) {
                sb.append("* ");
                if (ref.getUrl() != null) {
                    sb.append("[").append(ref.getUrl()).append("](").append(ref.getUrl()).append(")");
                } else {
                    sb.append("*(no URL)*");
                }
                String reason = reasonFn.apply(ref);
                if (reason != null && !reason.isBlank()) {
                    sb.append(" - ").append(reason);
                }
                sb.append("\n");
            }
        }
    }

    private record RefFlags(List<Reference> mustFix, List<Reference> rejected) {}

    private RefFlags loadRefFlags(String username) {
        List<Reference> all = referenceRepository.findByUser(username);
        return new RefFlags(
                all.stream().filter(r -> Boolean.TRUE.equals(r.getMustFix())).toList(),
                all.stream().filter(r -> Boolean.TRUE.equals(r.getRejected())).toList()
        );
    }

    /**
     * Blocks approval when a pending application for the same user requires more trades than this one.
     * Involvement flairs (trades == 0) are exempt and can always be approved.
     */
    private void checkNoHigherFlairPending(String id, Application app) {
        Flair flairConfig = flairRepository.findByNameAndSub(app.getFlair(), app.getSub());
        int appTrades = flairConfig != null ? flairConfig.getTrades() : 0;
        if (appTrades == 0) return;

        applicationRepository.findByUser(app.getUser()).stream()
                .filter(a -> !a.getId().equals(id))
                .forEach(other -> {
                    Flair otherConfig = flairRepository.findByNameAndSub(other.getFlair(), other.getSub());
                    int otherTrades = otherConfig != null ? otherConfig.getTrades() : 0;
                    if (otherTrades > appTrades) {
                        log.warn("Approval blocked — user='{}' flair='{}' trades={} < pending flair='{}' trades={}",
                                app.getUser(), app.getFlair(), appTrades, other.getFlair(), otherTrades);
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                "Cannot approve lower flair level while a higher flair application is pending");
                    }
                });
    }

    /**
     * Checks flair eligibility thresholds for the given user. When {@code forMod} is false
     * (user self-applying), failures throw 403. When true (mod approving), failures throw 422
     * and are logged as warnings so approval cannot be blocked silently.
     */
    private void checkThresholds(String username, String flair, String sub, boolean forMod) {
        Flair flairConfig = flairRepository.findByNameAndSub(flair, sub);
        if (flairConfig == null) {
            return;
        }
        checkTradeType(username, ReferenceType.CASUAL,      flairConfig.getTrades(),      "trades",     forMod);
        checkTradeType(username, ReferenceType.GIVEAWAY,    flairConfig.getGiveaways(),   "giveaways",  forMod);
        checkTradeType(username, ReferenceType.INVOLVEMENT, flairConfig.getInvolvement(), "tradebacks", forMod);
    }

    /**
     * Validates trades for specified type and count needed for the flair.
     * 
     * @param user
     * @param type
     * @param required
     * @param label
     * @param forMod
     */
    private void checkTradeType(String user, String type, int required, String label, boolean forMod) {
        if (required <= 0) {
            return;
        }
        long actual = forMod
                ? referenceRepository.countByUserAndApprovedTrueAndType(user, type)
                : referenceRepository.countByUserAndType(user, type);
        if (actual < required) {
            if (forMod) {
                log.warn("Approval blocked — user='{}' type='{}' approved={} required={}", user, label, actual, required);
            }
            HttpStatus status = forMod ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.FORBIDDEN;
            String prefix     = forMod ? "Approval blocked" : "Not eligible";
            throw new ResponseStatusException(status,
                    prefix + " — " + actual + " of " + required + " required " + label);
        }
    }
}
